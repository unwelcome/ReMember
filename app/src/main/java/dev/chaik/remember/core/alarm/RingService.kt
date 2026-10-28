package dev.chaik.remember.core.alarm

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.VibratorManager
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import dev.chaik.remember.appContainer

/**
 * Звонок будильника или таймера: мелодия на громкости будильника (громкость нарастает, если
 * так задано), вибрация и уведомление на весь экран. Работает, пока звонок не выключат,
 * не отложат или он не прозвонит [RING_TIMEOUT_MS].
 */
class RingService : Service() {
    private var player: MediaPlayer? = null
    private var focus: AudioFocusRequest? = null
    private val handler = Handler(Looper.getMainLooper())
    private val timeout = Runnable { appContainer.alarmController.onRingTimeout() }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val container = appContainer
        val session = container.timeStore.session.value
        // startForegroundService обязывает показать уведомление — даже если звонить уже нечего
        val notification = container.timeNotifications.ring(session ?: RingSession(RingKind.ALARM))
        ServiceCompat.startForeground(this, TimeNotifications.ID_RING, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        if (session == null || !session.ringing) {
            stopSelf()
            return START_NOT_STICKY
        }
        running = true
        startRinging(session)
        return START_NOT_STICKY
    }

    private fun startRinging(session: RingSession) {
        stopRinging()
        val alarm = session.alarm
        val audio = getSystemService(AudioManager::class.java)
        focus = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT).setAudioAttributes(AlarmAudioAttributes).build()
            .also { audio.requestAudioFocus(it) }
        player = playAlarmSound(this, alarm?.ringtone, wakeLock = true)
        if (alarm?.gradual == true) rampVolume(0)

        if (alarm?.vibrate != false) {
            getSystemService(VibratorManager::class.java)?.defaultVibrator?.vibrate(
                VibrationEffect.createWaveform(longArrayOf(0, 800, 700), 0),
                VibrationAttributes.createForUsage(VibrationAttributes.USAGE_ALARM),
            )
        }
        handler.postDelayed(timeout, RING_TIMEOUT_MS)
    }

    /** Плавное нарастание: от 5 % до полной громкости за [Alarm.GRADUAL_SECONDS] секунд. */
    private fun rampVolume(step: Int) {
        val steps = Alarm.GRADUAL_SECONDS * 2
        val volume = (0.05f + 0.95f * step / steps).coerceAtMost(1f)
        player?.setVolume(volume, volume)
        if (step < steps) handler.postDelayed({ rampVolume(step + 1) }, 500)
    }

    private fun stopRinging() {
        handler.removeCallbacksAndMessages(null)
        player?.runCatching { stop(); release() }
        player = null
        focus?.let { getSystemService(AudioManager::class.java).abandonAudioFocusRequest(it) }
        focus = null
        getSystemService(VibratorManager::class.java)?.defaultVibrator?.cancel()
    }

    override fun onDestroy() {
        stopRinging()
        running = false
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    companion object {
        /** Дольше звонок не длится: обычный будильник замолкает, усиленный — повторит позже. */
        const val RING_TIMEOUT_MS = 10 * 60_000L

        /** Звонит ли сейчас (в этом процессе). */
        @Volatile
        var running = false
            private set

        fun start(context: Context) = ContextCompat.startForegroundService(context, Intent(context, RingService::class.java))

        fun stop(context: Context) {
            context.stopService(Intent(context, RingService::class.java))
        }
    }
}
