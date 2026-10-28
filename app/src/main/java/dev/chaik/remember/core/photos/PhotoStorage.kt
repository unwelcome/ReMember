package dev.chaik.remember.core.photos

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.util.Size
import androidx.core.graphics.scale
import dev.chaik.remember.core.database.dao.PhotoDao
import dev.chaik.remember.core.database.entity.PhotoEntity
import dev.chaik.remember.core.model.Photo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.time.Instant
import java.util.UUID
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.toJavaDuration

/**
 * Хранилище фото во внутренней памяти приложения.
 *
 * При добавлении фото копируется к себе (оригинал в галерее можно удалить),
 * уменьшается до [MAX_SIDE] по длинной стороне и сохраняется в WebP.
 * Рядом лежит миниатюра [THUMB_SIDE] — её грузят списки и лента, чтобы не тормозить.
 *
 * Папка приложения недоступна другим приложениям и исключена из бэкапа.
 */
class PhotoStorage(
    private val context: Context,
    private val photoDao: PhotoDao,
) {
    private val photosDir = File(context.filesDir, "photos")
    private val thumbsDir = File(photosDir, "thumbs")

    fun file(photoId: String): File = File(photosDir, "$photoId.webp")

    fun thumbnailFile(photoId: String): File = File(thumbsDir, "$photoId.webp")

    /** Сведения о фото (размеры) по id; отсутствующих в ответе нет. */
    suspend fun photos(ids: Collection<String>): Map<String, Photo> =
        if (ids.isEmpty()) emptyMap() else photoDao.getMany(ids).associate { it.id to Photo(it.id, it.width, it.height) }

    /** Скопировать фото (из галереи, камеры и т. п.) к себе. */
    suspend fun import(source: Uri): Photo = withContext(Dispatchers.IO) {
        val bitmap = decodeScaled(source)
        try {
            val id = UUID.randomUUID().toString()
            thumbsDir.mkdirs()
            writeWebp(bitmap, file(id), PHOTO_QUALITY)
            bitmap.fitInto(THUMB_SIDE).let { thumb ->
                writeWebp(thumb, thumbnailFile(id), THUMB_QUALITY)
                if (thumb !== bitmap) thumb.recycle()
            }

            val entity = PhotoEntity(
                id = id,
                width = bitmap.width,
                height = bitmap.height,
                sizeBytes = file(id).length(),
                createdAt = Instant.now(),
            )
            try {
                photoDao.insert(entity)
            } catch (e: Exception) {
                deleteFiles(id)
                throw e
            }
            Photo(id, entity.width, entity.height)
        } finally {
            bitmap.recycle()
        }
    }

    /**
     * Удалить фото, на которые больше не ссылается ни одна запись.
     * [olderThan] — запас, чтобы не тронуть только что добавленное, ещё не сохранённое фото:
     * запись сохраняется меньше чем через секунду после правки, так что минут хватает с лихвой.
     * Возвращает количество удалённых.
     */
    suspend fun cleanupUnreferenced(olderThan: Duration = UNUSED_GRACE): Int = withContext(Dispatchers.IO) {
        val orphans = photoDao.unreferencedBefore(Instant.now() - olderThan.toJavaDuration())
        if (orphans.isEmpty()) return@withContext 0
        photoDao.delete(orphans.map { it.id })
        orphans.forEach { deleteFiles(it.id) }
        orphans.size
    }

    /** Удалить фото совсем: строку в базе (ссылки уйдут каскадом) и файлы. */
    suspend fun delete(photoId: String) = withContext(Dispatchers.IO) {
        photoDao.delete(listOf(photoId))
        deleteFiles(photoId)
    }

    /**
     * Скопировать фото в общую галерею телефона (Pictures/ReMember) в JPEG — его открывает
     * любое приложение. Разрешения не нужны: своё приложение пишет туда свободно.
     */
    suspend fun exportToGallery(photoId: String): Boolean = withContext(Dispatchers.IO) {
        val bitmap = BitmapFactory.decodeFile(file(photoId).path) ?: return@withContext false
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "ReMember_${photoId.take(8)}.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/ReMember")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return@withContext false
        try {
            val written = resolver.openOutputStream(uri)?.use { bitmap.compress(Bitmap.CompressFormat.JPEG, EXPORT_QUALITY, it) } == true
            if (!written) {
                resolver.delete(uri, null, null)
                return@withContext false
            }
            resolver.update(uri, ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }, null, null)
            true
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            false
        } finally {
            bitmap.recycle()
        }
    }

    private fun deleteFiles(photoId: String) {
        file(photoId).delete()
        thumbnailFile(photoId).delete()
    }

    /** Декодирует сразу в уменьшенном размере — полноразмерный снимок в память не грузится. */
    private fun decodeScaled(source: Uri): Bitmap {
        val imageSource = ImageDecoder.createSource(context.contentResolver, source)
        return ImageDecoder.decodeBitmap(imageSource) { decoder, info, _ ->
            // Поворот по EXIF ImageDecoder применяет сам
            val target = info.size.fitInto(MAX_SIDE)
            decoder.setTargetSize(target.width, target.height)
            // Программный битмап: аппаратный нельзя сжать в файл
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        }
    }

    private fun writeWebp(bitmap: Bitmap, target: File, quality: Int) {
        // Пишем во временный файл и переименовываем: недописанный файл не останется
        val temp = File(target.parentFile, "${target.name}.tmp")
        temp.outputStream().use { out ->
            check(bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, quality, out)) { "Не удалось сохранить фото" }
        }
        check(temp.renameTo(target)) { "Не удалось сохранить фото" }
    }

    companion object {
        /** Столько живёт фото, которое ни в одной записи не стоит, — потом удаляется само. */
        val UNUSED_GRACE: Duration = 10.minutes

        private const val MAX_SIDE = 2560
        private const val THUMB_SIDE = 480
        private const val PHOTO_QUALITY = 85
        private const val THUMB_QUALITY = 75
        private const val EXPORT_QUALITY = 95
    }
}

/** Размер, вписанный в квадрат [maxSide]; маленькие картинки не увеличиваются. */
internal fun Size.fitInto(maxSide: Int): Size {
    val longest = max(width, height)
    if (longest <= maxSide) return this
    val ratio = maxSide.toFloat() / longest
    return Size((width * ratio).roundToInt().coerceAtLeast(1), (height * ratio).roundToInt().coerceAtLeast(1))
}

private fun Bitmap.fitInto(maxSide: Int): Bitmap {
    val target = Size(width, height).fitInto(maxSide)
    return if (target.width == width && target.height == height) this else scale(target.width, target.height)
}
