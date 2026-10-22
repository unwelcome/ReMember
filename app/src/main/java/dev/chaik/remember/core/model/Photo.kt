package dev.chaik.remember.core.model

/** Сохранённое фото. Файлы — через PhotoStorage.file / thumbnailFile. */
data class Photo(
    val id: String,
    val width: Int,
    val height: Int,
)

/** Чей текст ссылается на фото. */
enum class PhotoOwner { DAY_ENTRY, NOTE }
