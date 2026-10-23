package dev.chaik.remember.core.data

import androidx.room.withTransaction
import dev.chaik.remember.core.database.ReMemberDatabase
import dev.chaik.remember.core.database.entity.SubtaskEntity
import dev.chaik.remember.core.database.entity.TaskEntity
import dev.chaik.remember.core.model.Subtask
import dev.chaik.remember.core.model.Task
import dev.chaik.remember.core.model.TaskPriority
import dev.chaik.remember.core.model.TaskRepeat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.Instant
import java.time.LocalDate

/** Задачи и подзадачи. */
class TaskRepository(private val db: ReMemberDatabase) {
    private val dao = db.taskDao()

    /** Все задачи с подзадачами (группировка и порядок — на экране). */
    fun observeAll(): Flow<List<Task>> =
        combine(dao.observeAll(), dao.observeAllSubtasks()) { tasks, subtasks ->
            val byTask = subtasks.groupBy({ it.taskId }, { it.toModel() })
            tasks.map { it.toModel(byTask[it.id].orEmpty()) }
        }

    suspend fun get(id: Long): Task? = dao.get(id)?.toModel(dao.subtasks(id).map { it.toModel() })

    /** Создаёт задачу (id = 0) или обновляет — вместе с подзадачами. Возвращает id. */
    suspend fun save(task: Task): Long = db.withTransaction {
        val now = Instant.now()
        val id = if (task.id == 0L) {
            dao.insert(task.toEntity(createdAt = now, updatedAt = now))
        } else {
            val createdAt = dao.get(task.id)?.createdAt ?: now
            dao.update(task.toEntity(createdAt = createdAt, updatedAt = now))
            task.id
        }
        dao.replaceSubtasks(id, task.subtasks.mapIndexed { i, s -> SubtaskEntity(0, id, s.title, s.done, i) })
        id
    }

    /**
     * Отметить выполненной или вернуть. Повторяющаяся задача при выполнении не уходит
     * в «Выполнено»: срок переезжает на следующий раз, подзадачи снова не отмечены.
     */
    suspend fun setDone(task: Task, done: Boolean, today: LocalDate = LocalDate.now()) = db.withTransaction {
        val next = if (done) task.repeat.next(task.dueDate, today) else null
        if (next != null) {
            val entity = dao.get(task.id) ?: return@withTransaction
            dao.update(entity.copy(dueDate = next, done = false, doneAt = null, updatedAt = Instant.now()))
            dao.resetSubtasks(task.id)
        } else {
            dao.setDone(task.id, done, if (done) Instant.now() else null)
        }
    }

    suspend fun setSubtaskDone(subtask: Subtask, done: Boolean) = dao.setSubtaskDone(subtask.id, done)

    suspend fun delete(id: Long) = dao.delete(id) // подзадачи удалятся каскадом

    suspend fun deleteDone() = dao.deleteDone()
}

private fun TaskEntity.toModel(subtasks: List<Subtask>) = Task(
    id = id,
    title = title,
    notes = notes,
    dueDate = dueDate,
    dueTime = dueTime,
    priority = TaskPriority.fromLevel(priority),
    repeat = TaskRepeat.decode(repeat),
    done = done,
    doneAt = doneAt,
    subtasks = subtasks,
    createdAt = createdAt,
)

private fun Task.toEntity(createdAt: Instant, updatedAt: Instant) = TaskEntity(
    id = id,
    title = title,
    notes = notes,
    dueDate = dueDate,
    dueTime = dueTime?.takeIf { dueDate != null },
    priority = priority.level,
    repeat = TaskRepeat.encode(repeat),
    done = done,
    doneAt = doneAt,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

private fun SubtaskEntity.toModel() = Subtask(id = id, title = title, done = done)
