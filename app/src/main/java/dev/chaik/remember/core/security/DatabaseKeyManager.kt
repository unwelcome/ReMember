package dev.chaik.remember.core.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import java.io.File
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Пароль для шифрования базы (SQLCipher).
 *
 * Сам пароль — 32 случайных байта. Он хранится в файле зашифрованным ключом
 * из Android Keystore: ключ живёт в защищённом железе телефона и не извлекается,
 * поэтому файл без этого телефона бесполезен.
 *
 * Важно: если ключ в Keystore пропадёт (например, сброс телефона), базу
 * прочитать будет нельзя — на этот случай нужен собственный бэкап (экспорт в ZIP).
 */
class DatabaseKeyManager(
    context: Context,
    // Параметры нужны только тестам, чтобы не трогать настоящий ключ
    private val keyAlias: String = KEY_ALIAS,
    keyFileName: String = KEY_FILE,
) {
    private val file = AtomicFile(File(context.noBackupFilesDir, keyFileName))

    /** Возвращает пароль базы; при первом запуске создаёт его. */
    @Synchronized
    fun passphrase(): ByteArray {
        if (file.baseFile.exists()) {
            // Файл есть, но не расшифровывается — ни в коем случае не создаём новый пароль:
            // это сделало бы существующую базу нечитаемой. Пусть ошибка будет видна.
            return decrypt(file.readFully())
        }
        val passphrase = ByteArray(PASSPHRASE_SIZE).also { SecureRandom().nextBytes(it) }
        val stream = file.startWrite()
        try {
            stream.write(encrypt(passphrase))
            file.finishWrite(stream)
        } catch (e: Exception) {
            file.failWrite(stream)
            throw e
        }
        return passphrase
    }

    /**
     * Заменить пароль базы — при восстановлении из копии. Пароль перешифровывается
     * ключом чипа этого телефона.
     */
    @Synchronized
    fun replace(passphrase: ByteArray) {
        val stream = file.startWrite()
        try {
            stream.write(encrypt(passphrase))
            file.finishWrite(stream)
        } catch (e: Exception) {
            file.failWrite(stream)
            throw e
        }
    }

    private fun encrypt(plain: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, keystoreKey())
        // Формат файла: [12 байт IV][шифротекст + тег GCM]
        return cipher.iv + cipher.doFinal(plain)
    }

    private fun decrypt(stored: ByteArray): ByteArray {
        val iv = stored.copyOfRange(0, IV_SIZE)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, keystoreKey(), GCMParameterSpec(TAG_BITS, iv))
        return cipher.doFinal(stored, IV_SIZE, stored.size - IV_SIZE)
    }

    private fun keystoreKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        (keyStore.getKey(keyAlias, null) as? SecretKey)?.let { return it }

        val spec = KeyGenParameterSpec.Builder(
            keyAlias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE)
            .apply { init(spec) }
            .generateKey()
    }

    private companion object {
        const val KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "remember_database_key"
        const val KEY_FILE = "database.key"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val PASSPHRASE_SIZE = 32
        const val IV_SIZE = 12
        const val TAG_BITS = 128
    }
}
