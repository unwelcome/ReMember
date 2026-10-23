package dev.chaik.remember.core.model

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
    val repeat: Repeat = Repeat.None,
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
