package dev.chaik.remember.core.sleep

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/** Событие из истории телефона: экран погас или телефон разблокировали. */
data class PhoneEvent(val at: Instant, val type: Type) {
    enum class Type { SCREEN_OFF, UNLOCK }
}

/** Похоже, спал с [bed] до [wake]. */
data class SleepGuess(val bed: Instant, val wake: Instant)

/**
 * Сон по истории телефона: самый длинный ночной перерыв, когда телефоном не пользовались.
 * Отбой — экран погас после последнего использования, подъём — первая разблокировка после
 * перерыва. Экран, загоревшийся сам (уведомление), без разблокировки — не использование,
 * поэтому перерыв он не прерывает. Ночью «глянуть время» (до [NIGHT_GLANCE]) посреди сна
 * сон тоже не прерывает: такие перерывы склеиваются в один.
 */
object SleepGuesser {
    /** Короче — не сон, а просто отложенный телефон. */
    val MIN_SLEEP: Duration = Duration.ofHours(3)

    /** Столько можно посидеть в телефоне ночью, не «просыпаясь» для расчёта. */
    val NIGHT_GLANCE: Duration = Duration.ofMinutes(15)

    /** Склеиваем, только если до этого уже спал столько — иначе вечерняя пауза «отложил на полчаса» стала бы началом сна. */
    private val ASLEEP_BEFORE_GLANCE: Duration = Duration.ofHours(1)

    /** Сон, который закончился утром [date]: ищем с 18:00 накануне до 14:00 этого дня. */
    fun window(date: LocalDate, zone: ZoneId): Pair<Instant, Instant> =
        date.minusDays(1).atTime(EVENING).atZone(zone).toInstant() to date.atTime(AFTERNOON).atZone(zone).toInstant()

    fun guess(events: List<PhoneEvent>, date: LocalDate, zone: ZoneId): SleepGuess? {
        // Лечь позже 6 утра — уже не ночной сон, а дневной
        val latestBed = date.atTime(LATEST_BED).atZone(zone).toInstant()
        return mergeGlances(idleGaps(events, date, zone))
            .filter { it.length >= MIN_SLEEP && !it.bed.isAfter(latestBed) }
            .maxByOrNull { it.length }
    }

    /** Перерывы «экран погас после использования → следующая разблокировка». */
    private fun idleGaps(events: List<PhoneEvent>, date: LocalDate, zone: ZoneId): List<SleepGuess> {
        val (from, to) = window(date, zone)
        val gaps = mutableListOf<SleepGuess>()
        var inUse = false
        var idleSince: Instant? = null
        for (event in events.filter { it.at in from..to }.sortedBy { it.at }) {
            when (event.type) {
                PhoneEvent.Type.UNLOCK -> {
                    val start = idleSince
                    if (!inUse && start != null) gaps += SleepGuess(start, event.at)
                    inUse = true
                    idleSince = null
                }
                PhoneEvent.Type.SCREEN_OFF -> if (inUse) {
                    inUse = false
                    idleSince = event.at
                }
            }
        }
        return gaps
    }

    private fun mergeGlances(gaps: List<SleepGuess>): List<SleepGuess> {
        val merged = mutableListOf<SleepGuess>()
        for (gap in gaps) {
            val last = merged.lastOrNull()
            if (last != null && last.length >= ASLEEP_BEFORE_GLANCE && Duration.between(last.wake, gap.bed) <= NIGHT_GLANCE) {
                merged[merged.lastIndex] = SleepGuess(last.bed, gap.wake)
            } else {
                merged += gap
            }
        }
        return merged
    }

    private val SleepGuess.length: Duration get() = Duration.between(bed, wake)

    private val EVENING = LocalTime.of(18, 0)
    private val AFTERNOON = LocalTime.of(14, 0)
    private val LATEST_BED = LocalTime.of(6, 0)
}
