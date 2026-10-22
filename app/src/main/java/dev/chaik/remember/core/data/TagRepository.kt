package dev.chaik.remember.core.data

import dev.chaik.remember.core.database.ReMemberDatabase
import dev.chaik.remember.core.model.Tag
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Общие теги для записей дней и заметок. */
class TagRepository(db: ReMemberDatabase) {
    private val dao = db.tagDao()

    fun observeAll(): Flow<List<Tag>> = dao.observeAll().map { list -> list.map { it.toModel() } }

    /** Тег по имени (без учёта регистра); если такого нет — создаётся. */
    suspend fun getOrCreate(name: String): Tag = dao.getOrCreate(name.trim()).toModel()

    /** Удалить теги, которые больше нигде не используются. */
    suspend fun deleteUnused() = dao.deleteUnused()
}
