package dev.chaik.remember.core.backup

import java.io.DataInputStream
import java.io.EOFException
import java.io.FilterOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.security.SecureRandom
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Шифрование файла резервной копии паролем.
 *
 * Ключ — PBKDF2-SHA256 от пароля (много итераций: перебор паролей дорогой).
 * Данные режутся на куски по [CHUNK_SIZE], каждый шифруется AES-GCM отдельно —
 * так копию с тысячей фото не нужно держать в памяти целиком. Номер куска и признак
 * «последний» входят в проверку подлинности: куски нельзя переставить, а обрезанный
 * файл не примется за целый.
 *
 * Формат: «RMBK» · версия (1 байт) · итерации (4) · соль (16) · nonce (12) ·
 * куски [длина (4) · шифротекст с тегом]…
 */
object BackupCrypto {
    private val MAGIC = byteArrayOf('R'.code.toByte(), 'M'.code.toByte(), 'B'.code.toByte(), 'K'.code.toByte())
    private const val VERSION: Byte = 1
    const val ITERATIONS = 310_000
    private const val SALT_SIZE = 16
    private const val NONCE_SIZE = 12
    private const val TAG_BITS = 128
    const val CHUNK_SIZE = 1 shl 20

    /** Поток, который шифрует всё записанное в [out]. Закрыть обязательно — пишется последний кусок. */
    fun encrypt(out: OutputStream, password: CharArray, iterations: Int = ITERATIONS, chunkSize: Int = CHUNK_SIZE): OutputStream {
        val random = SecureRandom()
        val salt = ByteArray(SALT_SIZE).also(random::nextBytes)
        val nonce = ByteArray(NONCE_SIZE).also(random::nextBytes)
        out.write(MAGIC)
        out.write(VERSION.toInt())
        out.write(ByteBuffer.allocate(4).putInt(iterations).array())
        out.write(salt)
        out.write(nonce)
        return ChunkedOutput(out, deriveKey(password, salt, iterations), nonce, chunkSize)
    }

    /** Поток расшифровки. Неверный пароль — [WrongPasswordException] уже здесь, на первом куске. */
    fun decrypt(input: InputStream, password: CharArray): InputStream {
        val data = DataInputStream(input)
        val magic = ByteArray(MAGIC.size).also { readFully(data, it) }
        if (!magic.contentEquals(MAGIC)) throw NotBackupException()
        if (data.readByte() != VERSION) throw NotBackupException()
        val iterations = data.readInt()
        if (iterations !in 1_000..10_000_000) throw NotBackupException()
        val salt = ByteArray(SALT_SIZE).also { readFully(data, it) }
        val nonce = ByteArray(NONCE_SIZE).also { readFully(data, it) }
        return ChunkedInput(data, deriveKey(password, salt, iterations), nonce).also { it.prefetch() }
    }

    private fun deriveKey(password: CharArray, salt: ByteArray, iterations: Int): SecretKeySpec {
        val spec = PBEKeySpec(password, salt, iterations, 256)
        try {
            val bytes = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
            return SecretKeySpec(bytes, "AES")
        } finally {
            spec.clearPassword()
        }
    }

    private fun chunkNonce(base: ByteArray, index: Long): ByteArray =
        base.copyOf().also { n -> for (i in 0 until 8) n[NONCE_SIZE - 1 - i] = (n[NONCE_SIZE - 1 - i].toInt() xor (index shr (8 * i)).toInt()).toByte() }

    private fun aad(index: Long, last: Boolean): ByteArray = ByteBuffer.allocate(9).putLong(index).put(if (last) 1 else 0).array()

    private fun readFully(input: DataInputStream, target: ByteArray) {
        try {
            input.readFully(target)
        } catch (_: EOFException) {
            throw NotBackupException()
        }
    }

    private class ChunkedOutput(
        out: OutputStream,
        private val key: SecretKeySpec,
        private val nonce: ByteArray,
        chunkSize: Int,
    ) : FilterOutputStream(out) {
        private val buffer = ByteArray(chunkSize)
        private var filled = 0
        private var index = 0L
        private var closed = false

        override fun write(b: Int) {
            if (filled == buffer.size) flushChunk(last = false)
            buffer[filled++] = b.toByte()
        }

        override fun write(b: ByteArray, off: Int, len: Int) {
            var offset = off
            var left = len
            while (left > 0) {
                if (filled == buffer.size) flushChunk(last = false)
                val n = minOf(left, buffer.size - filled)
                System.arraycopy(b, offset, buffer, filled, n)
                filled += n
                offset += n
                left -= n
            }
        }

        override fun close() {
            if (closed) return
            closed = true
            flushChunk(last = true)
            out.close()
        }

        private fun flushChunk(last: Boolean) {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(TAG_BITS, chunkNonce(nonce, index)))
            cipher.updateAAD(aad(index, last))
            val sealed = cipher.doFinal(buffer, 0, filled)
            out.write(ByteBuffer.allocate(4).putInt(sealed.size).array())
            out.write(sealed)
            filled = 0
            index++
        }
    }

    private class ChunkedInput(
        private val input: DataInputStream,
        private val key: SecretKeySpec,
        private val nonce: ByteArray,
    ) : InputStream() {
        private var chunk = ByteArray(0)
        private var position = 0
        private var index = 0L
        private var finished = false

        /** Расшифровать первый кусок сразу — чтобы неверный пароль был виден до распаковки. */
        fun prefetch() {
            nextChunk()
        }

        override fun read(): Int {
            if (!ensure()) return -1
            return chunk[position++].toInt() and 0xFF
        }

        override fun read(b: ByteArray, off: Int, len: Int): Int {
            if (len == 0) return 0
            if (!ensure()) return -1
            val n = minOf(len, chunk.size - position)
            System.arraycopy(chunk, position, b, off, n)
            position += n
            return n
        }

        override fun close() = input.close()

        private fun ensure(): Boolean {
            while (position >= chunk.size) {
                if (finished) return false
                nextChunk()
            }
            return true
        }

        private fun nextChunk() {
            val size = try {
                input.readInt()
            } catch (_: EOFException) {
                throw IOException("Файл копии обрезан")
            }
            if (size !in 16..(CHUNK_SIZE + 16)) throw NotBackupException()
            val sealed = ByteArray(size)
            try {
                input.readFully(sealed)
            } catch (_: EOFException) {
                throw IOException("Файл копии обрезан")
            }
            // Последний ли кусок, заранее не знаем — пробуем оба варианта метки
            chunk = open(sealed, last = false) ?: open(sealed, last = true)?.also { finished = true }
                ?: if (index == 0L) throw WrongPasswordException() else throw IOException("Файл копии повреждён")
            position = 0
            index++
        }

        private fun open(sealed: ByteArray, last: Boolean): ByteArray? = try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_BITS, chunkNonce(nonce, index)))
            cipher.updateAAD(aad(index, last))
            cipher.doFinal(sealed)
        } catch (_: AEADBadTagException) {
            null
        }
    }
}

/** Пароль не подошёл (или файл подменён). */
class WrongPasswordException : IOException("Неверный пароль")

/** Это не файл резервной копии Re:Member. */
class NotBackupException : IOException("Не файл резервной копии")
