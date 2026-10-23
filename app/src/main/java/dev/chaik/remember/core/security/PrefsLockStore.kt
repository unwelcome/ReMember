package dev.chaik.remember.core.security

import android.content.Context
import androidx.core.content.edit

/** Состояние блокировки в SharedPreferences (как и всё приложение, исключено из бэкапов). */
class PrefsLockStore(context: Context) : LockStore {
    private val prefs = context.getSharedPreferences("security", Context.MODE_PRIVATE)

    override var pinHash: String?
        get() = prefs.getString(KEY_PIN_HASH, null)
        set(value) = prefs.edit { putString(KEY_PIN_HASH, value) }

    override var failedAttempts: Int
        get() = prefs.getInt(KEY_FAILED, 0)
        set(value) = prefs.edit { putInt(KEY_FAILED, value) }

    override var lockoutUntil: Long
        get() = prefs.getLong(KEY_LOCKOUT, 0)
        set(value) = prefs.edit { putLong(KEY_LOCKOUT, value) }

    override var backgroundSince: Long
        get() = prefs.getLong(KEY_BACKGROUND, 0)
        set(value) = prefs.edit { putLong(KEY_BACKGROUND, value) }

    private companion object {
        const val KEY_PIN_HASH = "pin_hash"
        const val KEY_FAILED = "failed_attempts"
        const val KEY_LOCKOUT = "lockout_until"
        const val KEY_BACKGROUND = "background_since"
    }
}
