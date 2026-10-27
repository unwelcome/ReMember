package dev.chaik.remember.core.health

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.util.Log
import androidx.core.content.edit
import dev.chaik.remember.R
import dev.chaik.remember.core.reminders.Message
import dev.chaik.remember.core.reminders.Notifier
import dev.chaik.remember.core.reminders.NotificationType
import dev.chaik.remember.core.reminders.OpenTarget
import dev.chaik.remember.core.reminders.QuietHours
import dev.chaik.remember.core.reminders.QuietMode
import dev.chaik.remember.core.reminders.ReminderReceiver
import dev.chaik.remember.core.reminders.ReminderSettingsStore
import dev.chaik.remember.core.settings.AppSettingsStore
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.text.NumberFormat
import java.time.LocalDate
import java.time.LocalTime

/** Что делать при очередной проверке шагов. */
enum class StepsGoalAction {
    /** Ничего: цель не достигнута, уже поздравили или уведомления выключены. */
    NONE,

    /** Прислать уведомление. */
    NOTIFY,

    /** Прислать без звука (тихие часы, режим «без звука»). */
    NOTIFY_SILENT,

    /** Тихие часы, режим «пропустить»: не присылать, но и больше не напоминать сегодня. */
    SKIP,
}

object StepsGoalRules {
    /**
     * Решение по шагам за сегодня. Тихие часы — как у напоминаний: «отложить» значит подождать
     * (следующая проверка после тихих часов пришлёт), «без звука» — прислать тихо, «пропустить» — не присылать.
     */
    fun decide(
        steps: Long,
        goal: Long,
        alreadyNotified: Boolean,
        notificationsEnabled: Boolean,
        quiet: QuietHours,
        now: LocalTime,
    ): StepsGoalAction = when {
        !notificationsEnabled || alreadyNotified || goal <= 0 || steps < goal -> StepsGoalAction.NONE
        !quiet.contains(now) -> StepsGoalAction.NOTIFY
        else -> when (quiet.mode) {
            QuietMode.POSTPONE -> StepsGoalAction.NONE
            QuietMode.SILENT -> StepsGoalAction.NOTIFY_SILENT
            QuietMode.SKIP -> StepsGoalAction.SKIP
        }
    }
}

/**
 * Уведомление «цель по шагам выполнена». Android не сообщает о новых шагах сам, поэтому
 * раз в [AppSettings.stepsGoalCheckMinutes][dev.chaik.remember.core.settings.AppSettings.stepsGoalCheckMinutes]
 * минут приложение просыпается и смотрит шаги за сегодня в Health Connect (нужно разрешение
 * на чтение в фоне). Поздравляет один раз в день.
 */
class StepsGoalWatcher(
    private val context: Context,
    private val settings: AppSettingsStore,
    private val reminders: ReminderSettingsStore,
    private val goals: HealthGoalsStore,
    private val health: HealthConnectSource,
    private val notifier: Notifier,
) {
    private val alarms = context.getSystemService(AlarmManager::class.java)
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val lock = Mutex()

    /** Поставить следующую проверку по настройке или снять, если выключено. */
    fun schedule() {
        val minutes = settings.settings.value.stepsGoalCheckMinutes
        val intent = alarmIntent()
        if (minutes <= 0) {
            alarms.cancel(intent)
            return
        }
        // Неточный будильник: Android сам сдвигает его ради батареи — для поздравления это не важно
        alarms.setAndAllowWhileIdle(
            AlarmManager.ELAPSED_REALTIME_WAKEUP,
            SystemClock.elapsedRealtime() + minutes * 60_000L,
            intent,
        )
    }

    suspend fun check() = lock.withLock {
        try {
            if (settings.settings.value.stepsGoalCheckMinutes <= 0) return@withLock
            val today = LocalDate.now()
            val alreadyNotified = prefs.getLong(KEY_NOTIFIED_DAY, -1) == today.toEpochDay()
            if (alreadyNotified || health.availability != HealthAvailability.AVAILABLE) return@withLock

            val goal = goals.goals.value.steps
            val steps = health.stepsToday()
            val reminderSettings = reminders.settings.value
            val action = StepsGoalRules.decide(
                steps = steps,
                goal = goal,
                alreadyNotified = false,
                notificationsEnabled = reminderSettings.enabled,
                quiet = reminderSettings.quiet,
                now = LocalTime.now(),
            )
            Log.i(TAG, "Шаги: $steps из $goal → $action")
            if (action == StepsGoalAction.NONE) return@withLock
            if (action != StepsGoalAction.SKIP) {
                notifier.ensureChannels(reminderSettings.sound)
                val number = NumberFormat.getIntegerInstance()
                notifier.post(
                    Message(
                        id = ID_STEPS,
                        type = NotificationType.HEALTH,
                        title = context.getString(R.string.notify_steps_title),
                        text = context.getString(R.string.notify_steps_text, number.format(steps), number.format(goal)),
                        target = OpenTarget.Health,
                        silent = action == StepsGoalAction.NOTIFY_SILENT,
                    ),
                    reminderSettings.sound,
                )
            }
            prefs.edit { putLong(KEY_NOTIFIED_DAY, today.toEpochDay()) }
        } catch (e: Exception) {
            // Нет разрешения на чтение в фоне или Health Connect недоступен — попробуем в следующий раз
            Log.w(TAG, "Не удалось проверить шаги", e)
        } finally {
            schedule()
        }
    }

    private fun alarmIntent(): PendingIntent = PendingIntent.getBroadcast(
        context,
        REQUEST_ALARM,
        Intent(context, ReminderReceiver::class.java).setAction(ReminderReceiver.ACTION_STEPS),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private companion object {
        const val TAG = "StepsGoal"
        const val PREFS = "steps_goal"
        const val KEY_NOTIFIED_DAY = "notified_day"
        const val ID_STEPS = 5
        const val REQUEST_ALARM = 1
    }
}
