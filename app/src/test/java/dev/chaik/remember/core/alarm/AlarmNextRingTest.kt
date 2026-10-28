package dev.chaik.remember.core.alarm

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

class AlarmNextRingTest {
    private val zone = ZoneId.of("Europe/Moscow")

    // Среда, 28 октября 2026, 21:00
    private val now = ZonedDateTime.of(LocalDateTime.of(2026, 10, 28, 21, 0), zone)

    private fun at(day: Int, hour: Int, minute: Int) = ZonedDateTime.of(LocalDateTime.of(2026, 10, day, hour, minute), zone)

    @Test
    fun `однократный — сегодня, если время ещё не прошло, иначе завтра`() {
        assertEquals(at(28, 22, 30), Alarm(1, 22, 30).nextRing(now))
        assertEquals(at(29, 7, 0), Alarm(1, 7, 0).nextRing(now))
        // Ровно сейчас — уже прошло
        assertEquals(at(29, 21, 0), Alarm(1, 21, 0).nextRing(now))
    }

    @Test
    fun `по дням недели — ближайший подходящий день`() {
        // Будни: сейчас среда вечер → четверг утром
        assertEquals(at(29, 7, 0), Alarm(1, 7, 0, days = (1..5).toSet()).nextRing(now))
        // Только суббота
        assertEquals(at(31, 9, 0), Alarm(1, 9, 0, days = setOf(6)).nextRing(now))
        // Только среда, время прошло → через неделю
        assertEquals(ZonedDateTime.of(LocalDateTime.of(2026, 11, 4, 7, 0), zone), Alarm(1, 7, 0, days = setOf(3)).nextRing(now))
    }

    @Test
    fun `таймер и секундомер считают время`() {
        assertEquals(30_000L, TimerState(durationMs = 60_000, endsAt = 1_030_000).left(1_000_000))
        assertEquals(0L, TimerState(durationMs = 60_000, endsAt = 1_000_000).left(2_000_000))
        assertEquals(12_000L, TimerState(durationMs = 60_000, leftMs = 12_000).left(5_000_000))
        assertEquals(15_000L, StopwatchState(startedAt = 1_000_000, accumulatedMs = 5_000).elapsed(1_010_000))
    }
}
