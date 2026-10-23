package dev.chaik.remember

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import dev.chaik.remember.app.AppContainer
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch

class ReMemberApplication : Application() {
    val container by lazy { AppContainer(this) }

    @OptIn(FlowPreview::class)
    override fun onCreate() {
        super.onCreate()
        // Всё приложение ушло в фон (а не просто открылся выбор фото внутри него) — запоминаем когда
        ProcessLifecycleOwner.get().lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onStop(owner: LifecycleOwner) = container.appLock.onBackground()
            },
        )
        container.appScope.launch {
            // Открыть зашифрованную базу заранее, пока на экране замок: SQLCipher при открытии
            // специально долго вычисляет ключ (~1–2 с), и после PIN запись дня появлялась с задержкой
            runCatching { container.database.openHelper.writableDatabase }
                .onFailure { Log.w(TAG, "Не удалось открыть базу", it) }
        }
        container.appScope.launch {
            // Стандартные категории трат: создать при первом запуске, обновить при смене набора
            val prefs = getSharedPreferences(PREFS, MODE_PRIVATE)
            runCatching {
                val applied = prefs.getInt(KEY_CATEGORY_DEFAULTS, 0)
                val version = container.moneyRepository.ensureDefaultCategories(applied)
                if (version != applied) prefs.edit().putInt(KEY_CATEGORY_DEFAULTS, version).apply()
            }.onFailure { Log.w(TAG, "Не удалось обновить категории", it) }
        }
        container.appScope.launch {
            // Расписание напоминаний пересчитывается при любом изменении событий, задач, настроек
            // и идущей тренировки
            // (и сразу при запуске). Пока приложение не запущено, его ведут будильник и ReminderReceiver
            combine(
                container.eventRepository.observeAll(),
                container.taskRepository.observeAll(),
                container.reminderSettings.settings,
                container.workoutRepository.observeActive(),
            ) { _, _, _, _ -> }
                .debounce(RESCHEDULE_DEBOUNCE_MS)
                .collect {
                    runCatching { container.reminderScheduler.check() }
                        .onFailure { Log.w(TAG, "Не удалось пересчитать напоминания", it) }
                }
        }
        container.appScope.launch {
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
        const val RESCHEDULE_DEBOUNCE_MS = 1_000L
    }
}

/** Контейнер зависимостей из любого места, где есть Context. */
val Context.appContainer: AppContainer
    get() = (applicationContext as ReMemberApplication).container
