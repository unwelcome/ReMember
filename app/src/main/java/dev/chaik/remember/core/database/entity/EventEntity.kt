package dev.chaik.remember.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

@Entity(tableName = "event")
data class EventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String,
    val date: LocalDate,
    val time: LocalTime,
    /** Имя константы EventColor. */
    val color: String,
    /** Повтор, см. Repeat.encode. */
    val repeat: String,
    @ColumnInfo(name = "remind_minutes") val remindMinutes: Int?,
    @ColumnInfo(name = "is_birthday") val isBirthday: Boolean,
    @ColumnInfo(name = "birth_year") val birthYear: Int?,
    @ColumnInfo(name = "created_at") val createdAt: Instant,
    @ColumnInfo(name = "updated_at") val updatedAt: Instant,
)
