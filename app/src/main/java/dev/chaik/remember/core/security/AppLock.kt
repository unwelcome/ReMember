package dev.chaik.remember.core.security

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/** Где хранится всё, что нужно блокировке (SharedPreferences или, в тестах, память). */
interface LockStore {
    var pinHash: String?
    var failedAttempts: Int

    /** До какого момента (мс) ввод PIN запрещён после череды ошибок; 0 — не запрещён. */
    var lockoutUntil: Long

    /** Когда приложение ушло в фон (мс); 0 — с тех пор не уходило. */
    var backgroundSince: Long
}

/** Чем закончилась попытка ввести PIN. */
sealed interface PinResult {
    data object Unlocked : PinResult
    data class Wrong(val attemptsLeft: Int) : PinResult
    data class LockedOut(val untilMillis: Long) : PinResult
}

/**
 * Блокировка приложения.
 *
 * - при каждом запуске приложение заблокировано;
 * - ушли в фон больше чем на [timeoutMillis] — при возвращении снова блокировка;
 * - [MAX_ATTEMPTS] неверных PIN подряд — пауза [LOCKOUT_MILLIS].
 *
 * Настройки (таймаут, свой PIN, отпечаток) появятся на этапе настроек; пока PIN по умолчанию — [DEFAULT_PIN].
 */
class AppLock(
    private val store: LockStore,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    val timeoutMillis: Long = DEFAULT_TIMEOUT_MILLIS
    val pinLength: Int = DEFAULT_PIN.length
    val biometricEnabled: Boolean = true

    private val _locked = MutableStateFlow(true)

    /** Заблокировано ли сейчас. При запуске процесса — всегда да. */
    val locked: StateFlow<Boolean> = _locked.asStateFlow()

    /** Приложение ушло в фон. */
    fun onBackground() {
        if (!_locked.value) store.backgroundSince = clock()
    }

    /** Приложение вернулось: если пробыло в фоне дольше таймаута — блокируем. */
    fun onForeground() {
        val since = store.backgroundSince
        store.backgroundSince = 0
        if (since > 0 && clock() - since >= timeoutMillis) _locked.value = true
    }

    /** Если ввод PIN сейчас запрещён — до какого момента; иначе null. */
    fun lockoutUntil(): Long? = store.lockoutUntil.takeIf { it > clock() }

    suspend fun unlockWithPin(pin: String): PinResult {
        lockoutUntil()?.let { return PinResult.LockedOut(it) }
        val hash = store.pinHash ?: PinHasher.hash(DEFAULT_PIN).also { store.pinHash = it }
        val correct = withContext(Dispatchers.Default) { PinHasher.verify(pin, hash) }
        if (correct) {
            unlock()
            if (PinHasher.needsRehash(hash)) {
                withContext(Dispatchers.Default) { store.pinHash = PinHasher.hash(pin) }
            }
            return PinResult.Unlocked
        }
        val failed = store.failedAttempts + 1
        if (failed >= MAX_ATTEMPTS) {
            store.failedAttempts = 0
            store.lockoutUntil = clock() + LOCKOUT_MILLIS
            return PinResult.LockedOut(store.lockoutUntil)
        }
        store.failedAttempts = failed
        return PinResult.Wrong(MAX_ATTEMPTS - failed)
    }

    /** Отпечаток подтверждён системой. */
    fun unlockWithBiometric() = unlock()

    private fun unlock() {
        store.failedAttempts = 0
        store.lockoutUntil = 0
        store.backgroundSince = 0
        _locked.value = false
    }

    companion object {
        const val DEFAULT_PIN = "1111"
        const val DEFAULT_TIMEOUT_MILLIS = 10 * 60 * 1000L
        const val MAX_ATTEMPTS = 5
        const val LOCKOUT_MILLIS = 30_000L
    }
}
