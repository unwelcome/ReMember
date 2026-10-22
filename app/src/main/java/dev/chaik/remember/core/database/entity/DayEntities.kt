package dev.chaik.remember.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDate

/** Запись «как прошёл день» — одна на дату. */
@Entity(tableName = "day_entry")
data class DayEntryEntity(
    @PrimaryKey val date: LocalDate,
    /** Настроение, null — не указано. Шкалу выберем на этапе 4. */
    val mood: Int?,
    /** Markdown со ссылками на фото вида `photo:<id>`. */
    val text: String,
    @ColumnInfo(name = "created_at") val createdAt: Instant,
    @ColumnInfo(name = "updated_at") val updatedAt: Instant,
)

/** Сон: привязан к дате пробуждения. */
@Entity(tableName = "sleep_entry")
data class SleepEntryEntity(
    @PrimaryKey val date: LocalDate,
    @ColumnInfo(name = "bed_time") val bedTime: Instant?,
    @ColumnInfo(name = "wake_time") val wakeTime: Instant?,
    val quality: Int?,
    /** Что приснилось. */
    val dream: String,
    @ColumnInfo(name = "updated_at") val updatedAt: Instant,
)
