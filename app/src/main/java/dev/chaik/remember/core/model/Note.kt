package dev.chaik.remember.core.model

import java.time.Instant

data class Note(
    val id: Long = 0,
    val title: String = "",
    /** Markdown; фото вставляются как `photo:<id>` (см. PhotoRefs). */
    val text: String = "",
    val folderId: Long? = null,
    val pinned: Boolean = false,
    val tags: List<Tag> = emptyList(),
    val createdAt: Instant? = null,
    val updatedAt: Instant? = null,
)

data class NoteFolder(
    val id: Long = 0,
    val name: String,
    val sortOrder: Int = 0,
)
