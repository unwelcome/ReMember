package dev.chaik.remember.feature.calendar

import dev.chaik.remember.core.model.Event
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

/** Клеток в сетке месяца: всегда 6 недель — чтобы при листании высота не прыгала. */
const val GRID_DAYS = 42

/** Дни сетки [month]: с начала недели, где 1-е число, включая хвосты соседних месяцев. */
fun monthGrid(month: YearMonth, firstDayOfWeek: DayOfWeek): List<LocalDate> {
    val first = month.atDay(1)
    val offset = (first.dayOfWeek.value - firstDayOfWeek.value + 7) % 7
    val start = first.minusDays(offset.toLong())
    return List(GRID_DAYS) { start.plusDays(it.toLong()) }
}

/** События дня по времени, при одинаковом времени — по названию. */
fun eventsOn(events: List<Event>, day: LocalDate): List<Event> =
    events.filter { it.occursOn(day) }.sortedWith(compareBy<Event> { it.time }.thenBy { it.title.lowercase() })

/** События по дням [days]; дни без событий не попадают в ответ. */
fun eventsByDay(events: List<Event>, days: List<LocalDate>): Map<LocalDate, List<Event>> =
    days.associateWith { eventsOn(events, it) }.filterValues { it.isNotEmpty() }
