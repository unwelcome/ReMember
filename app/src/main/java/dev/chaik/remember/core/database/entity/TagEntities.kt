package dev.chaik.remember.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

/**
 * Тег. Имя уникально без учёта регистра: «Работа» и «работа» — один тег.
 *
 * Сравниваем по [key], а не через COLLATE NOCASE: NOCASE в SQLite
 * понимает регистр только у латиницы, кириллицу он считает разными буквами.
 */
@Entity(tableName = "tag", indices = [Index(value = ["key"], unique = true)])
data class TagEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Как тег написали в первый раз — так он и показывается. */
    val name: String,
    /** Имя в нижнем регистре — для поиска и уникальности. */
    val key: String = tagKey(name),
)

fun tagKey(name: String): String = name.trim().lowercase()

@Entity(
    tableName = "day_entry_tag",
    primaryKeys = ["date", "tag_id"],
    foreignKeys = [
        ForeignKey(
            entity = DayEntryEntity::class,
            parentColumns = ["date"],
            childColumns = ["date"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = TagEntity::class,
            parentColumns = ["id"],
            childColumns = ["tag_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("tag_id")],
)
data class DayEntryTagEntity(
    val date: LocalDate,
    @ColumnInfo(name = "tag_id") val tagId: Long,
)

@Entity(
    tableName = "note_tag",
    primaryKeys = ["note_id", "tag_id"],
    foreignKeys = [
        ForeignKey(
            entity = NoteEntity::class,
            parentColumns = ["id"],
            childColumns = ["note_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = TagEntity::class,
            parentColumns = ["id"],
            childColumns = ["tag_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("tag_id")],
)
data class NoteTagEntity(
    @ColumnInfo(name = "note_id") val noteId: Long,
    @ColumnInfo(name = "tag_id") val tagId: Long,
)
