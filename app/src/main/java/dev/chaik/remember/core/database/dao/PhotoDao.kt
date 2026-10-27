package dev.chaik.remember.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import dev.chaik.remember.core.database.entity.PhotoEntity
import dev.chaik.remember.core.database.entity.PhotoRefEntity
import dev.chaik.remember.core.model.PhotoOwner
import java.time.Instant
import kotlinx.coroutines.flow.Flow

@Dao
interface PhotoDao {
    @Insert
    suspend fun insert(photo: PhotoEntity)

    @Query("SELECT * FROM photo WHERE id = :id")
    suspend fun get(id: String): PhotoEntity?

    @Query("SELECT * FROM photo WHERE id IN (:ids)")
    suspend fun getMany(ids: Collection<String>): List<PhotoEntity>

    @Query("DELETE FROM photo WHERE id IN (:ids)")
    suspend fun delete(ids: Collection<String>)

    /** Все фото, новые сверху — для галереи. */
    @Query("SELECT * FROM photo ORDER BY created_at DESC")
    fun observeAll(): Flow<List<PhotoEntity>>

    @Query("SELECT * FROM photo_ref")
    fun observeRefs(): Flow<List<PhotoRefEntity>>

    @Query("SELECT * FROM photo_ref WHERE photo_id = :photoId")
    suspend fun refsOf(photoId: String): List<PhotoRefEntity>

    /**
     * Фото без единой ссылки, созданные раньше [before].
     * Порог по времени нужен, чтобы не удалить фото, которое только что добавили
     * в редактор, но запись ещё не сохранилась.
     */
    @Query("SELECT * FROM photo WHERE created_at < :before AND id NOT IN (SELECT photo_id FROM photo_ref)")
    suspend fun unreferencedBefore(before: Instant): List<PhotoEntity>

    // Ссылки

    @Query("SELECT id FROM photo WHERE id IN (:ids)")
    suspend fun existingIds(ids: Collection<String>): List<String>

    @Query("DELETE FROM photo_ref WHERE owner_type = :ownerType AND owner_id = :ownerId")
    suspend fun clearRefs(ownerType: PhotoOwner, ownerId: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertRefs(refs: List<PhotoRefEntity>)

    /** Заменить ссылки владельца: ссылки на несуществующие фото пропускаются. */
    @Transaction
    suspend fun replaceRefs(ownerType: PhotoOwner, ownerId: Long, photoIds: Set<String>) {
        clearRefs(ownerType, ownerId)
        if (photoIds.isEmpty()) return
        insertRefs(existingIds(photoIds).map { PhotoRefEntity(it, ownerType, ownerId) })
    }
}
