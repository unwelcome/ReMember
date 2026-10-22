package dev.chaik.remember.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import dev.chaik.remember.core.database.entity.SleepEntryEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface SleepDao {
    @Query("SELECT * FROM sleep_entry WHERE date = :date")
    fun observe(date: LocalDate): Flow<SleepEntryEntity?>

    @Query("SELECT * FROM sleep_entry WHERE date BETWEEN :from AND :to ORDER BY date DESC")
    fun observeRange(from: LocalDate, to: LocalDate): Flow<List<SleepEntryEntity>>

    @Upsert
    suspend fun upsert(entry: SleepEntryEntity)

    @Query("DELETE FROM sleep_entry WHERE date = :date")
    suspend fun delete(date: LocalDate)
}
