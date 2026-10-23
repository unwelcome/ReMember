package dev.chaik.remember.core.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import dev.chaik.remember.appContainer
import kotlinx.coroutines.launch

/**
 * Будильник напоминаний, а также перезагрузка телефона, смена времени или часового пояса
 * и обновление приложения: во всех случаях — разослать подошедшее и поставить следующий будильник.
 */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        val container = context.appContainer
        container.appScope.launch {
            try {
                container.reminderScheduler.check()
            } catch (e: Exception) {
                Log.w(TAG, "Не удалось проверить напоминания (${intent.action})", e)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_CHECK = "dev.chaik.remember.CHECK_REMINDERS"
        private const val TAG = "Reminders"
    }
}
