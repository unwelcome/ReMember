package dev.chaik.remember.core.photos

/**
 * Ссылки на фото внутри Markdown-текста.
 *
 * Фото вставляется в текст как картинка Markdown с адресом `photo:<id>`,
 * например `![](photo:3f2b…)`. Параметры (размер и т. п.) редактор
 * добавит после id — на поиск ссылок это не влияет.
 */
object PhotoRefs {
    private const val SCHEME = "photo:"
    private val pattern = Regex("""photo:([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})""")

    /** Адрес фото для вставки в текст. */
    fun uri(photoId: String): String = SCHEME + photoId

    /** Все id фото, на которые ссылается текст. */
    fun extract(markdown: String): Set<String> =
        pattern.findAll(markdown).mapTo(LinkedHashSet()) { it.groupValues[1] }
}
