package dev.chaik.remember.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import dev.chaik.remember.core.database.entity.DayEntryEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface DayEntryDao {
    @Query("SELECT * FROM day_entry WHERE date = :date")
    fun observe(date: LocalDate): Flow<DayEntryEntity?>

    @Query("SELECT * FROM day_entry WHERE date = :date")
    suspend fun get(date: LocalDate): DayEntryEntity?

    /** Записи за период, новые сверху. */
    @Query("SELECT * FROM day_entry WHERE date BETWEEN :from AND :to ORDER BY date DESC")
    fun observeRange(from: LocalDate, to: LocalDate): Flow<List<DayEntryEntity>>

    // Upsert, а не REPLACE: REPLACE удаляет строку и каскадом сносит её теги
    @Upsert
    suspend fun upsert(entry: DayEntryEntity)

    @Query("DELETE FROM day_entry WHERE date = :date")
    suspend fun delete(date: LocalDate)
}
