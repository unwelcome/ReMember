package dev.chaik.remember.core.backup

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import kotlin.random.Random

class BackupCryptoTest {
    private val password = "секрет-пароль".toCharArray()

    /** Быстрые параметры для тестов: мало итераций и маленькие куски, чтобы их было много. */
    private fun seal(data: ByteArray, pass: CharArray = password): ByteArray {
        val out = ByteArrayOutputStream()
        BackupCrypto.encrypt(out, pass, iterations = 1_000, chunkSize = 1_000).use { it.write(data) }
        return out.toByteArray()
    }

    private fun open(sealed: ByteArray, pass: CharArray = password): ByteArray =
        BackupCrypto.decrypt(ByteArrayInputStream(sealed), pass).use { it.readBytes() }

    @Test
    fun `данные возвращаются как были — и пустые, и на несколько кусков`() {
        listOf(0, 1, 999, 1_000, 1_001, 25_000).forEach { size ->
            val data = Random(size).nextBytes(size)
            assertArrayEquals(data, open(seal(data)))
        }
    }

    @Test
    fun `чужой пароль — сразу понятная ошибка`() {
        val sealed = seal(Random(1).nextBytes(5_000))
        assertThrows(WrongPasswordException::class.java) { open(sealed, "другой-пароль".toCharArray()) }
    }

    @Test
    fun `обрезанный или испорченный файл не принимается за целый`() {
        val sealed = seal(Random(2).nextBytes(5_000))
        // Без последнего куска
        assertThrows(IOException::class.java) { open(sealed.copyOf(sealed.size - 500)) }
        // Испорчен байт в середине
        val broken = sealed.copyOf().also { it[sealed.size / 2] = (it[sealed.size / 2] + 1).toByte() }
        assertThrows(IOException::class.java) { open(broken) }
    }

    @Test
    fun `не файл копии`() {
        assertThrows(NotBackupException::class.java) { open("просто текст".toByteArray()) }
    }
}
