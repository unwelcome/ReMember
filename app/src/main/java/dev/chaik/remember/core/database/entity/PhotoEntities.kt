package dev.chaik.remember.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import dev.chaik.remember.core.model.PhotoOwner
import java.time.Instant

/** Фото, сохранённое во внутренней памяти приложения (файлы см. PhotoStorage). */
@Entity(tableName = "photo")
data class PhotoEntity(
    @PrimaryKey val id: String,
    val width: Int,
    val height: Int,
    @ColumnInfo(name = "size_bytes") val sizeBytes: Long,
    @ColumnInfo(name = "created_at") val createdAt: Instant,
)

/**
 * Какая запись ссылается на фото. Пересобирается при каждом сохранении текста;
 * фото без единой ссылки удаляются при очистке.
 */
@Entity(
    tableName = "photo_ref",
    primaryKeys = ["photo_id", "owner_type", "owner_id"],
    foreignKeys = [
        ForeignKey(
            entity = PhotoEntity::class,
            parentColumns = ["id"],
            childColumns = ["photo_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class PhotoRefEntity(
    @ColumnInfo(name = "photo_id") val photoId: String,
    @ColumnInfo(name = "owner_type") val ownerType: PhotoOwner,
    /** Для записи дня — номер дня (epochDay), для заметки — её id. */
    @ColumnInfo(name = "owner_id") val ownerId: Long,
)
