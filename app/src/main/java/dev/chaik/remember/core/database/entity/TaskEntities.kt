package dev.chaik.remember.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

@Entity(tableName = "task", indices = [Index("done"), Index("due_date")])
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val notes: String,
    @ColumnInfo(name = "due_date") val dueDate: LocalDate?,
    @ColumnInfo(name = "due_time") val dueTime: LocalTime?,
    /** 0 — без приоритета, 1 — низкий, 2 — средний, 3 — высокий. */
    val priority: Int,
    /** Повтор, см. Repeat.encode: «», «D», «W», «M», «Y», «WD:1,3,5». */
    val repeat: String,
    val done: Boolean,
    @ColumnInfo(name = "done_at") val doneAt: Instant?,
    @ColumnInfo(name = "created_at") val createdAt: Instant,
    @ColumnInfo(name = "updated_at") val updatedAt: Instant,
)

@Entity(
    tableName = "subtask",
    foreignKeys = [
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["task_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("task_id")],
)
data class SubtaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "task_id") val taskId: Long,
    val title: String,
    val done: Boolean,
    @ColumnInfo(name = "sort_order") val sortOrder: Int,
)
