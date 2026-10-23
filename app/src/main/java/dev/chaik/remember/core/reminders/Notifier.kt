package dev.chaik.remember.core.reminders

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import androidx.annotation.StringRes
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import dev.chaik.remember.MainActivity
import dev.chaik.remember.R

/** Виды уведомлений — у каждого свой канал, их можно отдельно выключить в настройках Android. */
enum class NotificationType(val id: String, @param:StringRes val title: Int) {
    DAY("day", R.string.channel_day),
    EVENTS("events", R.string.channel_events),
    TASKS("tasks", R.string.channel_tasks),
    SUMMARY("summary", R.string.channel_summary),
}

/** Готовое к показу уведомление. */
data class Message(
    /** Одно и то же напоминание заменяет прежнее, а не копится. */
    val id: Int,
    val type: NotificationType,
    val title: String,
    val text: String,
    val target: OpenTarget,
    /** Строки развёрнутого вида (сводка). */
    val lines: List<String> = emptyList(),
    val silent: Boolean = false,
)

/** Каналы и показ уведомлений. */
class Notifier(private val context: Context) {
    private val manager = context.getSystemService(NotificationManager::class.java)

    val canPost: Boolean
        get() = NotificationManagerCompat.from(context).areNotificationsEnabled() &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    /**
     * Создать каналы под [sound]. Звук и вибрацию канала после создания меняет только пользователь
     * в настройках Android, поэтому для другого режима заводим каналы с другим id, а старые удаляем.
     */
    fun ensureChannels(sound: SoundMode) {
        manager.createNotificationChannelGroup(NotificationChannelGroup(GROUP, context.getString(R.string.app_name)))
        val wanted = NotificationType.entries.map { channelId(it, sound) }.toSet() + QUIET_CHANNEL
        manager.notificationChannels
            .filter { it.id !in wanted }
            .forEach { manager.deleteNotificationChannel(it.id) }

        NotificationType.entries.forEach { type ->
            val importance = if (sound == SoundMode.SILENT) NotificationManager.IMPORTANCE_LOW else NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(channelId(type, sound), context.getString(type.title), importance).apply {
                group = GROUP
                if (sound.sound) {
                    setSound(
                        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                        AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION).build(),
                    )
                } else {
                    setSound(null, null)
                }
                enableVibration(sound.vibration)
                if (!sound.vibration) vibrationPattern = longArrayOf(0)
            }
            manager.createNotificationChannel(channel)
        }
        manager.createNotificationChannel(
            NotificationChannel(QUIET_CHANNEL, context.getString(R.string.channel_quiet), NotificationManager.IMPORTANCE_LOW).apply {
                group = GROUP
                setSound(null, null)
                enableVibration(false)
            },
        )
    }

    fun post(message: Message, sound: SoundMode) {
        if (!canPost) return
        val intent = OpenTarget.put(
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            message.target,
        )
        val tap = PendingIntent.getActivity(
            context,
            message.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val builder = NotificationCompat.Builder(context, if (message.silent) QUIET_CHANNEL else channelId(message.type, sound))
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(context.getColor(R.color.notification_accent))
            .setContentTitle(message.title)
            .setContentText(message.text)
            .setContentIntent(tap)
            .setAutoCancel(true)
            .setCategory(if (message.type == NotificationType.EVENTS) NotificationCompat.CATEGORY_EVENT else NotificationCompat.CATEGORY_REMINDER)
        if (message.lines.isNotEmpty()) {
            builder.setStyle(NotificationCompat.InboxStyle().also { style -> message.lines.forEach(style::addLine) })
        } else {
            builder.setStyle(NotificationCompat.BigTextStyle().bigText(message.text))
        }
        try {
            NotificationManagerCompat.from(context).notify(message.id, builder.build())
        } catch (_: SecurityException) {
            // Разрешение отозвали между проверкой и показом — просто пропускаем
        }
    }

    private fun channelId(type: NotificationType, sound: SoundMode) = "${type.id}_${sound.key}"

    private companion object {
        const val GROUP = "remember"
        const val QUIET_CHANNEL = "quiet"
    }
}
