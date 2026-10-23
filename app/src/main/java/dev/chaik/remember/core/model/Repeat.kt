package dev.chaik.remember.core.model

import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Повтор задачи или события. Выполнили повторяющуюся задачу — она не уходит в «Выполнено»,
 * а переезжает на следующий срок ([next]); событие показывается во все дни повтора ([occursOn]).
 */
sealed interface Repeat {
    data object None : Repeat
    data object Daily : Repeat
    data object Weekly : Repeat
    data object Monthly : Repeat
    data object Yearly : Repeat

    /** По выбранным дням недели, например пн-ср-пт. */
    data class Weekdays(val days: Set<DayOfWeek>) : Repeat

    /**
     * Следующий срок после выполнения: первый повтор строго после [due],
     * но не раньше завтрашнего дня — иначе просроченная ежедневная задача
     * после выполнения снова оказалась бы «на сегодня».
     */
    fun next(due: LocalDate?, today: LocalDate): LocalDate? {
        if (this == None) return null
        var date = after(due ?: today)
        while (!date.isAfter(today)) date = after(date)
        return date
    }

    /**
     * Приходится ли повтор, начатый [start], на [date]. Ежемесячный с 31-го в коротком месяце —
     * в последний день месяца, ежегодный с 29 февраля в невисокосный год — 28-го.
     */
    fun occursOn(start: LocalDate, date: LocalDate): Boolean {
        if (date.isBefore(start)) return false
        return when (this) {
            None -> date == start
            Daily -> true
            Weekly -> date.dayOfWeek == start.dayOfWeek
            Monthly -> date.dayOfMonth == minOf(start.dayOfMonth, date.lengthOfMonth())
            Yearly -> date.month == start.month && date.dayOfMonth == minOf(start.dayOfMonth, date.lengthOfMonth())
            is Weekdays -> date.dayOfWeek in days.ifEmpty { setOf(start.dayOfWeek) }
        }
    }

    private fun after(date: LocalDate): LocalDate = when (this) {
        None -> date
        Daily -> date.plusDays(1)
        Weekly -> date.plusWeeks(1)
        Monthly -> date.plusMonths(1)
        Yearly -> date.plusYears(1)
        is Weekdays -> {
            val days = days.ifEmpty { setOf(date.dayOfWeek) }
            generateSequence(date.plusDays(1)) { it.plusDays(1) }.first { it.dayOfWeek in days }
        }
    }

    companion object {
        /** Как хранится в базе: «», «D», «W», «M», «Y», «WD:1,3,5» (номера дней ISO: 1 — пн). */
        fun encode(repeat: Repeat): String = when (repeat) {
            None -> ""
            Daily -> "D"
            Weekly -> "W"
            Monthly -> "M"
            Yearly -> "Y"
            is Weekdays -> "WD:" + repeat.days.map { it.value }.sorted().joinToString(",")
        }

        fun decode(value: String): Repeat = when {
            value == "D" -> Daily
            value == "W" -> Weekly
            value == "M" -> Monthly
            value == "Y" -> Yearly
            value.startsWith("WD:") -> Weekdays(
                value.removePrefix("WD:").split(',').mapNotNull { it.toIntOrNull()?.takeIf { d -> d in 1..7 } }
                    .map(DayOfWeek::of).toSet(),
            )
            else -> None
        }
    }
}
