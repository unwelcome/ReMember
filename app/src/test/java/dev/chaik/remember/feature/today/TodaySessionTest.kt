package dev.chaik.remember.feature.today

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalTime

class TodaySessionTest {
    @Test
    fun `утром — сон, в остальное время — день`() {
        assertEquals(TodayMode.Sleep, TodaySession.defaultFor(LocalTime.of(4, 0)))
        assertEquals(TodayMode.Sleep, TodaySession.defaultFor(LocalTime.of(11, 59)))
        assertEquals(TodayMode.Day, TodaySession.defaultFor(LocalTime.of(12, 0)))
        assertEquals(TodayMode.Day, TodaySession.defaultFor(LocalTime.of(2, 30)))
    }

    @Test
    fun `выбранный режим держится, пока не было долгой паузы`() {
        var time = LocalTime.of(8, 0)
        val session = TodaySession { time }
        assertEquals(TodayMode.Sleep, session.mode)

        session.mode = TodayMode.Money
        // Короткая отлучка — режим тот же
        session.onBackground(now = 0)
        session.onForeground(now = 5 * 60_000)
        assertEquals(TodayMode.Money, session.mode)

        // Вернулись днём через несколько часов — новое открытие, режим по времени
        time = LocalTime.of(14, 0)
        session.onBackground(now = 1_000_000)
        session.onForeground(now = 1_000_000 + TodaySession.NEW_OPENING_MILLIS)
        assertEquals(TodayMode.Day, session.mode)
    }
}
