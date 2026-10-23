package dev.chaik.remember

import android.app.Application
import android.content.Context
import android.util.Log
import dev.chaik.remember.app.AppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ReMemberApplication : Application() {
    val container by lazy { AppContainer(this) }

    /** Фоновые задачи уровня приложения — живут, пока жив процесс. */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        appScope.launch {
            // Стандартные категории трат: создать при первом запуске, обновить при смене набора
            val prefs = getSharedPreferences(PREFS, MODE_PRIVATE)
            runCatching {
                val applied = prefs.getInt(KEY_CATEGORY_DEFAULTS, 0)
                val version = container.moneyRepository.ensureDefaultCategories(applied)
                if (version != applied) prefs.edit().putInt(KEY_CATEGORY_DEFAULTS, version).apply()
            }.onFailure { Log.w(TAG, "Не удалось обновить категории", it) }
        }
        appScope.launch {
            // Не мешаем запуску: уборка — через пару секунд после старта
            delay(3_000)
            runCatching { container.photoStorage.cleanupUnreferenced() }
                .onSuccess { if (it > 0) Log.i(TAG, "Удалено фото без ссылок: $it") }
                .onFailure { Log.w(TAG, "Уборка фото не удалась", it) }
        }
    }

    private companion object {
        const val TAG = "ReMember"
        const val PREFS = "app_state"
        const val KEY_CATEGORY_DEFAULTS = "category_defaults_version"
    }
}

/** Контейнер зависимостей из любого места, где есть Context. */
val Context.appContainer: AppContainer
    get() = (applicationContext as ReMemberApplication).container
