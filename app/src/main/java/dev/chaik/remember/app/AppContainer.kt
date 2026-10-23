package dev.chaik.remember.app

import android.content.Context
import dev.chaik.remember.core.data.DayRepository
import dev.chaik.remember.core.data.EventRepository
import dev.chaik.remember.core.data.MoneyRepository
import dev.chaik.remember.core.data.NoteRepository
import dev.chaik.remember.core.data.SleepRepository
import dev.chaik.remember.core.data.TagRepository
import dev.chaik.remember.core.data.TaskRepository
import dev.chaik.remember.core.data.WorkoutRepository
import dev.chaik.remember.core.database.DatabaseFactory
import dev.chaik.remember.core.database.ReMemberDatabase
import dev.chaik.remember.core.health.HealthConnectSource
import dev.chaik.remember.core.health.HealthGoalsStore
import dev.chaik.remember.core.photos.PhotoStorage
import dev.chaik.remember.core.reminders.Notifier
import dev.chaik.remember.core.reminders.OpenTarget
import dev.chaik.remember.core.reminders.ReminderScheduler
import dev.chaik.remember.core.reminders.ReminderSettingsStore
import dev.chaik.remember.core.security.AppLock
import dev.chaik.remember.core.security.DatabaseKeyManager
import dev.chaik.remember.core.security.PrefsLockStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Все общие объекты приложения в одном месте (ручной DI).
 * Создаются лениво — при первом обращении, а не при запуске.
 */
class AppContainer(context: Context) {
    val appContext: Context = context.applicationContext

    /**
     * Фоновые задачи уровня приложения — живут, пока жив процесс.
     * Сюда же — сохранения, которые должны доделаться, даже если экран уже закрыт.
     */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val database: ReMemberDatabase by lazy {
        DatabaseFactory.create(appContext, DatabaseKeyManager(appContext))
    }

    val photoStorage by lazy { PhotoStorage(appContext, database.photoDao()) }

    val appLock by lazy { AppLock(PrefsLockStore(appContext)) }

    val dayRepository by lazy { DayRepository(database) }
    val sleepRepository by lazy { SleepRepository(database) }
    val moneyRepository by lazy { MoneyRepository(database) }
    val noteRepository by lazy { NoteRepository(database) }
    val tagRepository by lazy { TagRepository(database) }
    val taskRepository by lazy { TaskRepository(database) }
    val eventRepository by lazy { EventRepository(database) }
    val workoutRepository by lazy { WorkoutRepository(database) }

    val health by lazy { HealthConnectSource(appContext) }
    val healthGoals by lazy { HealthGoalsStore(appContext) }

    val reminderSettings by lazy { ReminderSettingsStore(appContext) }
    val notifier by lazy { Notifier(appContext) }
    val reminderScheduler by lazy {
        ReminderScheduler(
            appContext,
            reminderSettings,
            notifier,
            dayRepository,
            sleepRepository,
            eventRepository,
            taskRepository,
            workoutRepository,
        )
    }

    /** Куда перейти по тапу на уведомление; экран, который это выполнил, обнуляет запрос. */
    val openRequests = MutableStateFlow<OpenTarget?>(null)
}
