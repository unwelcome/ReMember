package dev.chaik.remember.core.data

import dev.chaik.remember.core.database.ReMemberDatabase
import dev.chaik.remember.core.database.entity.EventEntity
import dev.chaik.remember.core.model.Event
import dev.chaik.remember.core.model.EventColor
import dev.chaik.remember.core.model.Repeat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant

/**
 * События календаря. Хранятся «правилом» (дата + повтор), а не каждым разом:
 * какие события на какой день приходятся, считает экран.
 */
class EventRepository(db: ReMemberDatabase) {
    private val dao = db.eventDao()

    fun observeAll(): Flow<List<Event>> = dao.observeAll().map { list -> list.map { it.toModel() } }

    suspend fun get(id: Long): Event? = dao.get(id)?.toModel()

    /** Создаёт (id = 0) или обновляет событие. Возвращает id. */
    suspend fun save(event: Event): Long {
        val now = Instant.now()
        if (event.id == 0L) return dao.insert(event.toEntity(createdAt = now, updatedAt = now))
        val createdAt = dao.get(event.id)?.createdAt ?: now
        dao.update(event.toEntity(createdAt = createdAt, updatedAt = now))
        return event.id
    }

    suspend fun delete(id: Long) = dao.delete(id)
}

private fun EventEntity.toModel() = Event(
    id = id,
    title = title,
    description = description,
    date = date,
    time = time,
    color = EventColor.fromName(color),
    repeat = Repeat.decode(repeat),
    remindMinutes = remindMinutes,
    isBirthday = isBirthday,
    birthYear = birthYear,
)

private fun Event.toEntity(createdAt: Instant, updatedAt: Instant) = EventEntity(
    id = id,
    title = title,
    description = description,
    date = date,
    time = time,
    color = color.name,
    // День рождения всегда ежегодный — так и храним, чтобы правило читалось и без флага
    repeat = Repeat.encode(if (isBirthday) Repeat.Yearly else repeat),
    remindMinutes = remindMinutes,
    isBirthday = isBirthday,
    birthYear = birthYear?.takeIf { isBirthday },
    createdAt = createdAt,
    updatedAt = updatedAt,
)
