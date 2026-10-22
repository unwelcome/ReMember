package dev.chaik.remember.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import dev.chaik.remember.core.database.entity.NoteEntity
import dev.chaik.remember.core.database.entity.NoteFolderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    // Заметки: закреплённые сверху, дальше — недавно изменённые

    @Query("SELECT * FROM note ORDER BY pinned DESC, updated_at DESC")
    fun observeAll(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM note WHERE folder_id IS :folderId ORDER BY pinned DESC, updated_at DESC")
    fun observeInFolder(folderId: Long?): Flow<List<NoteEntity>>

    @Query("SELECT * FROM note WHERE id = :id")
    fun observe(id: Long): Flow<NoteEntity?>

    @Query("SELECT * FROM note WHERE id = :id")
    suspend fun get(id: Long): NoteEntity?

    @Insert
    suspend fun insert(note: NoteEntity): Long

    @Update
    suspend fun update(note: NoteEntity)

    @Query("DELETE FROM note WHERE id = :id")
    suspend fun delete(id: Long)

    // Папки

    @Query("SELECT * FROM note_folder ORDER BY sort_order, name")
    fun observeFolders(): Flow<List<NoteFolderEntity>>

    @Insert
    suspend fun insertFolder(folder: NoteFolderEntity): Long

    @Query("UPDATE note_folder SET name = :name, sort_order = :sortOrder WHERE id = :id")
    suspend fun updateFolder(id: Long, name: String, sortOrder: Int)

    @Query("DELETE FROM note_folder WHERE id = :id")
    suspend fun deleteFolder(id: Long)
}
