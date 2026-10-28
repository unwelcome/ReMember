package dev.chaik.remember.core.alarm

import android.content.Context
import dev.chaik.remember.core.wordgame.WordDictionary
import dev.chaik.remember.core.wordgame.WordGameState
import java.time.Instant
import java.time.ZonedDateTime

/**
 * Всё, что происходит с будильниками и таймером: постановка, звонок, «выключить», «отложить»
 * и игра усиленного будильника.
 *
 * Усиленный будильник: «выключить» гасит звук и открывает игру. Пока игра не пройдена (или не
 * проиграна), стоит запасной звонок через [Alarm.againMinutes] после последнего действия в игре —
 * если человек снова уснул, будильник зазвонит опять и продолжит ту же игру.
 */
class AlarmController(
    private val context: Context,
    private val store: TimeStore,
    private val scheduler: AlarmScheduler,
    private val dictionary: WordDictionary,
    private val notifications: TimeNotifications,
) {

    // ---- Будильники ----

    fun save(alarm: Alarm) {
        store.saveAlarm(alarm)
        schedule(alarm)
    }

    fun delete(id: Long) {
        store.deleteAlarm(id)
        scheduler.cancel(AlarmKey.Ring(id))
        scheduler.cancel(AlarmKey.Snooze(id))
    }

    fun setEnabled(id: Long, enabled: Boolean) {
        val alarm = store.alarm(id) ?: return
        save(alarm.copy(enabled = enabled))
        // Выключили — отложенный звонок тоже не нужен
        if (!enabled) scheduler.cancel(AlarmKey.Snooze(id))
    }

    /** После перезагрузки, смены времени и при запуске: поставить всё заново. */
    fun rescheduleAll() {
        store.alarms.value.forEach(::schedule)
        val timer = store.timer.value
        timer.endsAt?.let { endsAt ->
            if (endsAt > System.currentTimeMillis()) scheduler.set(AlarmKey.Timer, Instant.ofEpochMilli(endsAt)) else onTimerEnd()
        }
        // Телефон перезагрузили посреди звонка или игры усиленного будильника — звонок скоро повторится
        val session = store.session.value
        when {
            session == null -> Unit
            session.hard -> scheduler.set(AlarmKey.Again, Instant.now().plusSeconds(60))
            session.ringing && !RingService.running -> store.setSession(null)
        }
        notifications.refreshLive()
    }

    private fun schedule(alarm: Alarm) {
        if (alarm.enabled) {
            scheduler.set(AlarmKey.Ring(alarm.id), alarm.nextRing(ZonedDateTime.now()).toInstant())
        } else {
            scheduler.cancel(AlarmKey.Ring(alarm.id))
        }
    }

    // ---- Звонок ----

    fun onAlarm(id: Long) {
        val alarm = store.alarm(id)?.takeIf { it.enabled } ?: return
        // Однократный будильник после звонка выключается, повторяющийся — ставится на следующий раз
        if (alarm.days.isEmpty()) store.saveAlarm(alarm.copy(enabled = false)) else schedule(alarm)
        ring(alarm)
    }

    fun onSnoozeEnd(id: Long) {
        val alarm = store.alarm(id) ?: return
        ring(alarm)
    }

    /** Повторный звонок: в игре усиленного будильника долго ничего не делали. */
    fun onAgain() {
        val session = store.session.value?.takeIf { it.hard } ?: return
        store.setSession(session.copy(ringing = true, again = true))
        RingService.start(context)
    }

    fun onTimerEnd() {
        store.setTimer(store.timer.value.copy(endsAt = null, leftMs = null))
        notifications.refreshLive()
        // Таймер не перебивает игру усиленного будильника
        if (store.session.value?.hard == true) return
        store.setSession(RingSession(RingKind.TIMER))
        RingService.start(context)
    }

    private fun ring(alarm: Alarm) {
        // Идёт игра усиленного будильника — другой будильник её не сбрасывает, а продолжает
        val game = store.session.value?.takeIf { it.hard }?.game
        val keep = game != null
        store.setSession(
            RingSession(
                kind = RingKind.ALARM,
                alarm = if (keep) store.session.value?.alarm else alarm,
                again = keep,
                game = game,
            ),
        )
        RingService.start(context)
    }

    /**
     * «Выключить». Усиленный будильник при этом только замолкает и открывает игру,
     * обычный и таймер — выключаются совсем.
     */
    fun dismiss() {
        val session = store.session.value ?: return
        if (session.hard) {
            val game = session.game ?: WordGameState(answer = dictionary.randomAnswer())
            store.setSession(session.copy(ringing = false, game = game))
            RingService.stop(context)
            armAgain()
        } else {
            finish()
        }
    }

    fun snooze() {
        val session = store.session.value?.takeIf { it.canSnooze } ?: return
        val alarm = session.alarm ?: return
        scheduler.set(AlarmKey.Snooze(alarm.id), Instant.now().plusSeconds(alarm.snoozeMinutes * 60L))
        finish()
    }

    /** Ход в игре: сохранить и отодвинуть повторный звонок. */
    fun updateGame(game: WordGameState) {
        store.updateSession { it.copy(game = game) }
        if (game.finished) scheduler.cancel(AlarmKey.Again) else armAgain()
    }

    /** Игра пройдена или проиграна, экран закрыт — будильник выключен. */
    fun finish() {
        store.setSession(null)
        scheduler.cancel(AlarmKey.Again)
        RingService.stop(context)
    }

    /** Звонит слишком долго: обычный будильник замолкает, усиленный — повторит позже. */
    fun onRingTimeout() {
        val session = store.session.value ?: return
        if (session.hard) {
            store.setSession(session.copy(ringing = false))
            RingService.stop(context)
            armAgain()
        } else {
            finish()
        }
    }

    /**
     * Отодвинуть повторный звонок: после каждого действия в игре — снова полный срок.
     * Момент запоминается в звонке — по нему игра показывает отсчёт перед повтором.
     */
    private fun armAgain() {
        val minutes = store.session.value?.alarm?.againMinutes ?: DEFAULT_AGAIN_MINUTES
        val at = Instant.now().plusSeconds(minutes * 60L)
        store.updateSession { it.copy(againAt = at.toEpochMilli()) }
        scheduler.set(AlarmKey.Again, at)
    }

    // ---- Таймер ----

    fun startTimer(durationMs: Long) {
        val endsAt = System.currentTimeMillis() + durationMs
        store.setTimer(TimerState(durationMs = durationMs, endsAt = endsAt))
        scheduler.set(AlarmKey.Timer, Instant.ofEpochMilli(endsAt))
        notifications.refreshLive()
    }

    fun pauseTimer() {
        val timer = store.timer.value.takeIf { it.running } ?: return
        store.setTimer(timer.copy(endsAt = null, leftMs = timer.left(System.currentTimeMillis())))
        scheduler.cancel(AlarmKey.Timer)
        notifications.refreshLive()
    }

    fun resumeTimer() {
        val timer = store.timer.value.takeIf { it.paused } ?: return
        val endsAt = System.currentTimeMillis() + (timer.leftMs ?: 0)
        store.setTimer(timer.copy(endsAt = endsAt, leftMs = null))
        scheduler.set(AlarmKey.Timer, Instant.ofEpochMilli(endsAt))
        notifications.refreshLive()
    }

    /** +1 минута к идущему или стоящему на паузе таймеру. */
    fun addMinute() {
        val timer = store.timer.value
        when {
            timer.running -> {
                val endsAt = (timer.endsAt ?: return) + 60_000
                store.setTimer(timer.copy(endsAt = endsAt))
                scheduler.set(AlarmKey.Timer, Instant.ofEpochMilli(endsAt))
            }
            timer.paused -> store.setTimer(timer.copy(leftMs = (timer.leftMs ?: 0) + 60_000))
            else -> return
        }
        notifications.refreshLive()
    }

    fun resetTimer() {
        store.setTimer(TimerState(durationMs = store.timer.value.durationMs))
        scheduler.cancel(AlarmKey.Timer)
        notifications.refreshLive()
    }

    fun setTimerDuration(durationMs: Long) {
        val timer = store.timer.value.takeIf { it.idle } ?: return
        store.setTimer(timer.copy(durationMs = durationMs))
    }

    // ---- Секундомер ----

    fun startStopwatch() {
        val sw = store.stopwatch.value.takeIf { !it.running } ?: return
        store.setStopwatch(sw.copy(startedAt = System.currentTimeMillis()))
        notifications.refreshLive()
    }

    fun pauseStopwatch() {
        val sw = store.stopwatch.value.takeIf { it.running } ?: return
        store.setStopwatch(StopwatchState(accumulatedMs = sw.elapsed(System.currentTimeMillis()), laps = sw.laps))
        notifications.refreshLive()
    }

    fun lap() {
        val sw = store.stopwatch.value.takeIf { it.running } ?: return
        store.setStopwatch(sw.copy(laps = sw.laps + sw.elapsed(System.currentTimeMillis())))
    }

    fun resetStopwatch() {
        store.setStopwatch(StopwatchState())
        notifications.refreshLive()
    }

    companion object {
        /** Через сколько минут бездействия в игре будильник звонит снова, если у него не задано своё. */
        private const val DEFAULT_AGAIN_MINUTES = 5
    }
}
