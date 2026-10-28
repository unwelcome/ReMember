package dev.chaik.remember.core.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import dev.chaik.remember.MainActivity
import dev.chaik.remember.core.reminders.OpenTarget
import java.time.Instant

/** Что ставится на системный будильник. */
sealed class AlarmKey(val action: String, val id: Long) {
    class Ring(id: Long) : AlarmKey(AlarmReceiver.ACTION_RING, id)
    class Snooze(id: Long) : AlarmKey(AlarmReceiver.ACTION_SNOOZE_END, id)

    /** Повторный звонок усиленного будильника, если в игре долго ничего не делали. */
    data object Again : AlarmKey(AlarmReceiver.ACTION_AGAIN, 0)
    data object Timer : AlarmKey(AlarmReceiver.ACTION_TIMER, 0)
}

/**
 * Ставит будильники через `setAlarmClock`: так система будит телефон точно в срок даже в режиме
 * сна и показывает значок будильника в строке состояния.
 */
class AlarmScheduler(private val context: Context) {
    private val manager = context.getSystemService(AlarmManager::class.java)

    fun set(key: AlarmKey, at: Instant) {
        val operation = intent(key)
        if (manager.canScheduleExactAlarms()) {
            // Тап по значку будильника в шторке — страница «Время»
            val show = PendingIntent.getActivity(
                context,
                REQUEST_SHOW,
                OpenTarget.put(Intent(context, MainActivity::class.java), OpenTarget.Time),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            manager.setAlarmClock(AlarmManager.AlarmClockInfo(at.toEpochMilli(), show), operation)
        } else {
            manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at.toEpochMilli(), operation)
        }
    }

    fun cancel(key: AlarmKey) = manager.cancel(intent(key))

    private fun intent(key: AlarmKey): PendingIntent = PendingIntent.getBroadcast(
        context,
        0,
        // У каждого будильника свой адрес — иначе PendingIntent одного заменял бы другой
        Intent(context, AlarmReceiver::class.java)
            .setAction(key.action)
            .setData("remember-time://${key.action}/${key.id}".toUri())
            .putExtra(AlarmReceiver.EXTRA_ID, key.id),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private companion object {
        const val REQUEST_SHOW = 300
    }
}
