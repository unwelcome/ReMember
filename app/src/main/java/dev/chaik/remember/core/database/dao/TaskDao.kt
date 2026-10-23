package dev.chaik.remember.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import dev.chaik.remember.core.database.entity.SubtaskEntity
import dev.chaik.remember.core.database.entity.TaskEntity
import kotlinx.coroutines.flow.Flow
import java.time.Instant

@Dao
interface TaskDao {
    @Query("SELECT * FROM task")
    fun observeAll(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM subtask ORDER BY task_id, sort_order")
    fun observeAllSubtasks(): Flow<List<SubtaskEntity>>

    @Query("SELECT * FROM task WHERE id = :id")
    suspend fun get(id: Long): TaskEntity?

    @Query("SELECT * FROM subtask WHERE task_id = :taskId ORDER BY sort_order")
    suspend fun subtasks(taskId: Long): List<SubtaskEntity>

    @Insert
    suspend fun insert(task: TaskEntity): Long

    @Update
    suspend fun update(task: TaskEntity)

    @Query("DELETE FROM task WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM task WHERE done = 1")
    suspend fun deleteDone()

    @Query("UPDATE task SET done = :done, done_at = :doneAt WHERE id = :id")
    suspend fun setDone(id: Long, done: Boolean, doneAt: Instant?)

    @Query("UPDATE subtask SET done = :done WHERE id = :id")
    suspend fun setSubtaskDone(id: Long, done: Boolean)

    @Query("UPDATE subtask SET done = 0 WHERE task_id = :taskId")
    suspend fun resetSubtasks(taskId: Long)

    @Query("DELETE FROM subtask WHERE task_id = :taskId")
    suspend fun clearSubtasks(taskId: Long)

    @Insert
    suspend fun insertSubtasks(subtasks: List<SubtaskEntity>)

    @Transaction
    suspend fun replaceSubtasks(taskId: Long, subtasks: List<SubtaskEntity>) {
        clearSubtasks(taskId)
        if (subtasks.isNotEmpty()) insertSubtasks(subtasks)
    }
}
