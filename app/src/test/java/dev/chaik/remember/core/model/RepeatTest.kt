package dev.chaik.remember.core.model

import dev.chaik.remember.feature.tasks.TaskGroup
import dev.chaik.remember.feature.tasks.groupTasks
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek.FRIDAY
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.WEDNESDAY
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

class RepeatTest {
    // Пятница
    private val today = LocalDate.of(2026, 10, 23)

    @Test
    fun `ежедневная — на завтра, даже если была просрочена`() {
        assertEquals(today.plusDays(1), Repeat.Daily.next(today, today))
        assertEquals(today.plusDays(1), Repeat.Daily.next(today.minusDays(5), today))
    }

    @Test
    fun `еженедельная, ежемесячная, ежегодная`() {
        assertEquals(today.plusWeeks(1), Repeat.Weekly.next(today, today))
        // Выполнили заранее — следующий раз через неделю после срока
        assertEquals(today.plusDays(10), Repeat.Weekly.next(today.plusDays(3), today))
        assertEquals(LocalDate.of(2026, 11, 30), Repeat.Monthly.next(LocalDate.of(2026, 10, 31), LocalDate.of(2026, 10, 31)))
        assertEquals(today.plusYears(1), Repeat.Yearly.next(today, today))
    }

    @Test
    fun `по дням недели`() {
        val gym = Repeat.Weekdays(setOf(MONDAY, WEDNESDAY, FRIDAY))
        // Пятница → понедельник
        assertEquals(LocalDate.of(2026, 10, 26), gym.next(today, today))
        // Без срока — ближайший подходящий день после сегодня
        assertEquals(LocalDate.of(2026, 10, 26), gym.next(null, today))
    }

    @Test
    fun `без повтора следующего срока нет`() {
        assertNull(Repeat.None.next(today, today))
    }

    @Test
    fun `повтор сохраняется и читается из базы`() {
        listOf(
            Repeat.None, Repeat.Daily, Repeat.Weekly, Repeat.Monthly, Repeat.Yearly,
            Repeat.Weekdays(setOf(FRIDAY, MONDAY)),
        ).forEach { assertEquals(it, Repeat.decode(Repeat.encode(it))) }
        assertEquals("WD:1,5", Repeat.encode(Repeat.Weekdays(setOf(FRIDAY, MONDAY))))
        assertEquals(Repeat.None, Repeat.decode("мусор"))
    }

    @Test
    fun `группы и порядок внутри`() {
        val t = { id: Long, due: LocalDate?, priority: TaskPriority, time: LocalTime? ->
            Task(id = id, title = "$id", dueDate = due, dueTime = time, priority = priority, createdAt = Instant.ofEpochSecond(id))
        }
        val groups = groupTasks(
            listOf(
                t(1, today.minusDays(1), TaskPriority.NONE, null),
                t(2, today, TaskPriority.NONE, LocalTime.of(18, 0)),
                t(3, today, TaskPriority.HIGH, null),
                t(4, today, TaskPriority.NONE, LocalTime.of(9, 0)),
                t(5, today.plusDays(1), TaskPriority.NONE, null),
                t(6, today.plusDays(9), TaskPriority.NONE, null),
                t(7, null, TaskPriority.LOW, null),
                Task(id = 8, title = "8", done = true, doneAt = Instant.ofEpochSecond(1)),
                Task(id = 9, title = "9", done = true, doneAt = Instant.ofEpochSecond(2)),
            ),
            today,
        )
        assertEquals(listOf(TaskGroup.Overdue, TaskGroup.Today, TaskGroup.NoDate, TaskGroup.Tomorrow, TaskGroup.Later, TaskGroup.Done), groups.keys.toList())
        // Сначала важная, потом по времени
        assertEquals(listOf(3L, 4L, 2L), groups.getValue(TaskGroup.Today).map { it.id })
        assertEquals(listOf(9L, 8L), groups.getValue(TaskGroup.Done).map { it.id })
    }
}
