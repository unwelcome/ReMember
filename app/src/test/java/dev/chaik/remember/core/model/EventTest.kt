package dev.chaik.remember.core.model

import dev.chaik.remember.feature.calendar.GRID_DAYS
import dev.chaik.remember.feature.calendar.eventsByDay
import dev.chaik.remember.feature.calendar.eventsOn
import dev.chaik.remember.feature.calendar.monthGrid
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek.FRIDAY
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.TUESDAY
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth

class EventTest {
    private fun day(y: Int, m: Int, d: Int) = LocalDate.of(y, m, d)

    @Test
    fun `разовое — только в свой день`() {
        val event = Event(date = day(2026, 10, 23))
        assertTrue(event.occursOn(day(2026, 10, 23)))
        assertFalse(event.occursOn(day(2026, 10, 24)))
    }

    @Test
    fun `повторы не начинаются раньше первого раза`() {
        val weekly = Event(date = day(2026, 10, 23), repeat = Repeat.Weekly)
        assertFalse(weekly.occursOn(day(2026, 10, 16)))
        assertTrue(weekly.occursOn(day(2026, 10, 30)))
        assertFalse(weekly.occursOn(day(2026, 10, 31)))
        assertTrue(Event(date = day(2026, 10, 23), repeat = Repeat.Daily).occursOn(day(2027, 3, 1)))
    }

    @Test
    fun `ежемесячное с 31-го — в последний день короткого месяца`() {
        val rent = Event(date = day(2026, 1, 31), repeat = Repeat.Monthly)
        assertTrue(rent.occursOn(day(2026, 2, 28)))
        assertTrue(rent.occursOn(day(2026, 4, 30)))
        assertFalse(rent.occursOn(day(2026, 4, 29)))
        assertTrue(rent.occursOn(day(2026, 5, 31)))
    }

    @Test
    fun `по дням недели`() {
        val gym = Event(date = day(2026, 10, 19), repeat = Repeat.Weekdays(setOf(MONDAY, FRIDAY)))
        assertTrue(gym.occursOn(day(2026, 10, 23)))
        assertFalse(gym.occursOn(day(2026, 10, 20)))
        assertEquals(TUESDAY, day(2026, 10, 20).dayOfWeek)
    }

    @Test
    fun `день рождения — каждый год, и в прошлые годы тоже`() {
        val birthday = Event(title = "Иван Петров", date = day(2026, 3, 10), isBirthday = true, birthYear = 1995)
        assertTrue(birthday.occursOn(day(2027, 3, 10)))
        assertTrue(birthday.occursOn(day(2020, 3, 10)))
        assertFalse(birthday.occursOn(day(1994, 3, 10)))
        assertFalse(birthday.occursOn(day(2027, 3, 11)))
        assertEquals(31, birthday.ageOn(day(2026, 3, 10)))
        assertNull(birthday.copy(birthYear = null).ageOn(day(2026, 3, 10)))
    }

    @Test
    fun `день рождения 29 февраля — 28-го в невисокосный год`() {
        val birthday = Event(date = day(2024, 2, 29), isBirthday = true)
        assertTrue(birthday.occursOn(day(2027, 2, 28)))
        assertTrue(birthday.occursOn(day(2028, 2, 29)))
        assertFalse(birthday.occursOn(day(2028, 2, 28)))
        assertTrue(birthday.occursOn(day(2023, 2, 28)))
    }

    @Test
    fun `сетка месяца — 6 недель с понедельника`() {
        val grid = monthGrid(YearMonth.of(2026, 10), MONDAY)
        assertEquals(GRID_DAYS, grid.size)
        // 1 октября 2026 — четверг, сетка начинается с понедельника 28 сентября
        assertEquals(day(2026, 9, 28), grid.first())
        assertEquals(day(2026, 11, 8), grid.last())
    }

    @Test
    fun `события дня по времени и раскладка по дням`() {
        val late = Event(id = 1, title = "б", date = day(2026, 10, 23), time = LocalTime.of(20, 0))
        val early = Event(id = 2, title = "а", date = day(2026, 10, 23), time = LocalTime.of(9, 0))
        val daily = Event(id = 3, title = "в", date = day(2026, 10, 22), repeat = Repeat.Daily)
        assertEquals(listOf(3L, 2L, 1L), eventsOn(listOf(late, early, daily), day(2026, 10, 23)).map { it.id })

        val byDay = eventsByDay(listOf(late, early), monthGrid(YearMonth.of(2026, 10), MONDAY))
        assertEquals(setOf(day(2026, 10, 23)), byDay.keys)
    }
}
