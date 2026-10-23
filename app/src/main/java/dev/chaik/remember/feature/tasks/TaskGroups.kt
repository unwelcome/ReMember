package dev.chaik.remember.feature.tasks

import dev.chaik.remember.core.model.Task
import java.time.LocalDate
import java.time.LocalTime

/** Группы списка задач — в этом порядке на экране: «без срока» сразу после сегодняшних. */
enum class TaskGroup {
    Overdue,
    Today,
    NoDate,
    Tomorrow,
    Later,
    Done,
}

/**
 * Разложить задачи по группам. Внутри группы: важные выше, потом по сроку и времени,
 * потом старые раньше новых. Выполненные — последние выполненные сверху.
 */
fun groupTasks(tasks: List<Task>, today: LocalDate): Map<TaskGroup, List<Task>> {
    val tomorrow = today.plusDays(1)
    val active = compareByDescending<Task> { it.priority.level }
        .thenBy { it.dueDate }
        .thenBy { it.dueTime ?: LocalTime.MAX }
        .thenBy { it.createdAt }

    return tasks
        .groupBy { task ->
            val due = task.dueDate
            when {
                task.done -> TaskGroup.Done
                due == null -> TaskGroup.NoDate
                due.isBefore(today) -> TaskGroup.Overdue
                due == today -> TaskGroup.Today
                due == tomorrow -> TaskGroup.Tomorrow
                else -> TaskGroup.Later
            }
        }
        .mapValues { (group, list) ->
            if (group == TaskGroup.Done) list.sortedByDescending { it.doneAt } else list.sortedWith(active)
        }
        .toSortedMap()
}
