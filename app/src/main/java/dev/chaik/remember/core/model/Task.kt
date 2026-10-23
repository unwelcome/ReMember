package dev.chaik.remember.core.model

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

/** Задача из списка дел. */
data class Task(
    val id: Long = 0,
    val title: String = "",
    val notes: String = "",
    /** Срок; null — без срока. */
    val dueDate: LocalDate? = null,
    /** Время в день срока; null — весь день. Без [dueDate] не используется. */
    val dueTime: LocalTime? = null,
    val priority: TaskPriority = TaskPriority.NONE,
    val repeat: TaskRepeat = TaskRepeat.None,
    val done: Boolean = false,
    val doneAt: Instant? = null,
    val subtasks: List<Subtask> = emptyList(),
    val createdAt: Instant? = null,
)

data class Subtask(
    val id: Long = 0,
    val title: String,
    val done: Boolean = false,
)

/** Приоритет: важные стоят выше в своей группе, цвет — на кружке-галочке. */
enum class TaskPriority(val level: Int) {
    NONE(0),
    LOW(1),
    MEDIUM(2),
    HIGH(3),
    ;

    companion object {
        fun fromLevel(level: Int): TaskPriority = entries.firstOrNull { it.level == level } ?: NONE
    }
}

/**
 * Повтор задачи. Выполнили повторяющуюся — она не уходит в «Выполнено»,
 * а переезжает на следующий срок ([next]).
 */
sealed interface TaskRepeat {
    data object None : TaskRepeat
    data object Daily : TaskRepeat
    data object Weekly : TaskRepeat
    data object Monthly : TaskRepeat
    data object Yearly : TaskRepeat

    /** По выбранным дням недели, например пн-ср-пт. */
    data class Weekdays(val days: Set<DayOfWeek>) : TaskRepeat

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
        fun encode(repeat: TaskRepeat): String = when (repeat) {
            None -> ""
            Daily -> "D"
            Weekly -> "W"
            Monthly -> "M"
            Yearly -> "Y"
            is Weekdays -> "WD:" + repeat.days.map { it.value }.sorted().joinToString(",")
        }

        fun decode(value: String): TaskRepeat = when {
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
