package dev.chaik.remember.core.data

import androidx.room.withTransaction
import dev.chaik.remember.core.database.ReMemberDatabase
import dev.chaik.remember.core.database.entity.NoteFolderEntity
import dev.chaik.remember.core.model.Note
import dev.chaik.remember.core.model.NoteFolder
import dev.chaik.remember.core.model.PhotoOwner
import dev.chaik.remember.core.photos.PhotoRefs
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.Instant

/** Заметки и папки. */
class NoteRepository(private val db: ReMemberDatabase) {
    private val noteDao = db.noteDao()
    private val tagDao = db.tagDao()
    private val photoDao = db.photoDao()

    /** Все заметки (без тегов): закреплённые сверху, дальше — недавно изменённые. */
    fun observeAll(): Flow<List<Note>> = noteDao.observeAll().map { list -> list.map { it.toModel() } }

    /** Заметки папки; null — заметки без папки. */
    fun observeInFolder(folderId: Long?): Flow<List<Note>> =
        noteDao.observeInFolder(folderId).map { list -> list.map { it.toModel() } }

    /** Заметка вместе с тегами; null — удалена. */
    fun observe(id: Long): Flow<Note?> =
        combine(noteDao.observe(id), tagDao.observeForNote(id)) { note, tags -> note?.toModel(tags) }

    /** Создаёт заметку (id = 0) или обновляет существующую. Возвращает id. */
    suspend fun save(note: Note): Long = db.withTransaction {
        val now = Instant.now()
        val id = if (note.id == 0L) {
            noteDao.insert(note.toEntity(createdAt = now, updatedAt = now))
        } else {
            val createdAt = noteDao.get(note.id)?.createdAt ?: now
            noteDao.update(note.toEntity(createdAt = createdAt, updatedAt = now))
            note.id
        }
        tagDao.setNoteTags(id, note.tags.map { it.id })
        photoDao.replaceRefs(PhotoOwner.NOTE, id, PhotoRefs.extract(note.text))
        id
    }

    suspend fun delete(id: Long) = db.withTransaction {
        noteDao.delete(id) // теги удалятся каскадом
        photoDao.clearRefs(PhotoOwner.NOTE, id)
    }

    // Папки

    fun observeFolders(): Flow<List<NoteFolder>> =
        noteDao.observeFolders().map { list -> list.map { it.toModel() } }

    /** Создаёт папку (id = 0) или переименовывает/двигает существующую. Возвращает id. */
    suspend fun saveFolder(folder: NoteFolder): Long =
        if (folder.id == 0L) {
            noteDao.insertFolder(NoteFolderEntity(name = folder.name, sortOrder = folder.sortOrder, createdAt = Instant.now()))
        } else {
            noteDao.updateFolder(folder.id, folder.name, folder.sortOrder)
            folder.id
        }

    /** Удаляет папку; её заметки остаются без папки. */
    suspend fun deleteFolder(id: Long) = noteDao.deleteFolder(id)
}
