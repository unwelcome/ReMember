package dev.chaik.remember.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import dev.chaik.remember.core.database.entity.EventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EventDao {
    @Query("SELECT * FROM event")
    fun observeAll(): Flow<List<EventEntity>>

    @Query("SELECT * FROM event WHERE id = :id")
    suspend fun get(id: Long): EventEntity?

    @Insert
    suspend fun insert(event: EventEntity): Long

    @Update
    suspend fun update(event: EventEntity)

    @Query("DELETE FROM event WHERE id = :id")
    suspend fun delete(id: Long)
}
