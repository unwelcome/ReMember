package dev.chaik.remember.core.security

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * PIN хранится только в виде хэша PBKDF2 с солью — сам PIN нигде не записан.
 * Формат строки: `v1:<итерации>:<соль base64>:<хэш base64>`.
 */
object PinHasher {
    private const val VERSION = "v1"
    // Немного итераций: у PIN из 4 цифр всего 10 000 вариантов, перебор и так останавливает
    // пауза после ошибок, а 120 000 итераций давали полсекунды ожидания после ввода
    private const val ITERATIONS = 20_000
    private const val SALT_BYTES = 16
    private const val HASH_BITS = 256
    private const val ALGORITHM = "PBKDF2WithHmacSHA256"

    /** [iterations] меняют только тесты — чтобы получить «старый» хэш. */
    fun hash(pin: String, iterations: Int = ITERATIONS): String {
        val salt = ByteArray(SALT_BYTES).also { SecureRandom().nextBytes(it) }
        return encode(iterations, salt, derive(pin, salt, iterations))
    }

    /** Совпадает ли [pin] с сохранённым хэшем. Не мгновенно — вызывать не в главном потоке. */
    fun verify(pin: String, stored: String): Boolean {
        val parts = stored.split(':')
        if (parts.size != 4 || parts[0] != VERSION) return false
        val iterations = parts[1].toIntOrNull() ?: return false
        val salt = Base64.getDecoder().decode(parts[2])
        val expected = Base64.getDecoder().decode(parts[3])
        // Сравнение за постоянное время — не подсказывает, сколько символов совпало
        return MessageDigest.isEqual(derive(pin, salt, iterations), expected)
    }

    /** Хэш сделан со старыми параметрами — после верного ввода его стоит пересчитать. */
    fun needsRehash(stored: String): Boolean {
        val parts = stored.split(':')
        return parts.size != 4 || parts[0] != VERSION || parts[1].toIntOrNull() != ITERATIONS
    }

    private fun derive(pin: String, salt: ByteArray, iterations: Int): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, iterations, HASH_BITS)
        try {
            return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    private fun encode(iterations: Int, salt: ByteArray, hash: ByteArray): String {
        val encoder = Base64.getEncoder()
        return "$VERSION:$iterations:${encoder.encodeToString(salt)}:${encoder.encodeToString(hash)}"
    }
}
