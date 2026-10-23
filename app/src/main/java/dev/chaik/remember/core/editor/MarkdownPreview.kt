package dev.chaik.remember.core.editor

/** Короткий текст для карточек и списков: без разметки и без строк с фото. */
object MarkdownPreview {
    private val photoLine = Regex("""(?m)^!\[]\(photo:[^)]*\)(?: !\[]\(photo:[^)]*\))*$""")
    private val linePrefix = Regex("""(?m)^\s*(#{1,3} |- \[[ xX]] |[-*] |\d+\. )""")
    private val inlineMarkers = Regex("""\*\*|~~|(?<![\w*])[_*](?=\S)|(?<=\S)[_*](?![\w*])""")
    private val blankLines = Regex("""\n{2,}""")

    /** Plain-текст: без фото, заголовков, маркеров списков и выделения; не длиннее [maxChars]. */
    fun plain(markdown: String, maxChars: Int = 300): String {
        val text = markdown
            .replace(photoLine, "")
            .replace(linePrefix, "")
            .replace(inlineMarkers, "")
            .replace(blankLines, "\n")
            .trim()
        return if (text.length <= maxChars) text else text.take(maxChars).trimEnd() + "…"
    }
}
