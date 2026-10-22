package dev.chaik.remember.core.database

import androidx.room.TypeConverter
import java.time.Instant
import java.time.LocalDate

/** Как хранить в базе типы, которых SQLite не знает. */
class Converters {
    // Дата — номер дня от 1970-01-01: компактно, сортируется и сравнивается как число
    @TypeConverter
    fun localDateToLong(date: LocalDate?): Long? = date?.toEpochDay()

    @TypeConverter
    fun longToLocalDate(value: Long?): LocalDate? = value?.let(LocalDate::ofEpochDay)

    // Момент времени — миллисекунды от 1970-01-01 UTC
    @TypeConverter
    fun instantToLong(instant: Instant?): Long? = instant?.toEpochMilli()

    @TypeConverter
    fun longToInstant(value: Long?): Instant? = value?.let(Instant::ofEpochMilli)
}
