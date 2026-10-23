package dev.chaik.remember.core.model

import java.time.Instant
import java.time.LocalDate

/** Запись «как прошёл день». */
data class DayEntry(
    val date: LocalDate,
    /** Оценка дня в половинках звезды (см. Rating): 19 = 9.5 из 10; null — не указана. */
    val mood: Int? = null,
    /** Markdown; фото вставляются как `photo:<id>` (см. PhotoRefs). */
    val text: String = "",
    val tags: List<Tag> = emptyList(),
    val createdAt: Instant? = null,
    val updatedAt: Instant? = null,
)

/** Сон, привязан к дате пробуждения. */
data class SleepEntry(
    val date: LocalDate,
    val bedTime: Instant? = null,
    val wakeTime: Instant? = null,
    /** Качество сна в половинках звезды из 5 (см. Rating); null — не указано. */
    val quality: Int? = null,
    val dream: String = "",
)
