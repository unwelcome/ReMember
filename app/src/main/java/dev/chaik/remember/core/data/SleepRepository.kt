package dev.chaik.remember.core.data

import dev.chaik.remember.core.database.ReMemberDatabase
import dev.chaik.remember.core.model.SleepEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate

/** Сон и сны. */
class SleepRepository(db: ReMemberDatabase) {
    private val dao = db.sleepDao()

    fun observe(date: LocalDate): Flow<SleepEntry?> = dao.observe(date).map { it?.toModel() }

    fun observeRange(from: LocalDate, to: LocalDate): Flow<List<SleepEntry>> =
        dao.observeRange(from, to).map { list -> list.map { it.toModel() } }

    suspend fun save(entry: SleepEntry) = dao.upsert(entry.toEntity(updatedAt = Instant.now()))

    suspend fun delete(date: LocalDate) = dao.delete(date)
}
