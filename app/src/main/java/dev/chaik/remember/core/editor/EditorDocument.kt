package dev.chaik.remember.core.editor

/**
 * Документ редактора — чередование текста и фото:
 * `текст, фото, текст, фото, …, текст`. Текстовый блок может быть пустым,
 * поэтому между двумя фото всегда есть место, куда поставить курсор.
 *
 * В базе документ хранится как Markdown. Фото — отдельная строка
 * `![](photo:<id>)` или `![](photo:<id>?w=50)`, где `w` — ширина в процентах.
 */
sealed interface DocumentBlock {
    data class Text(val text: String) : DocumentBlock
    data class Photo(val photoId: String, val widthPercent: Int = FULL_WIDTH) : DocumentBlock

    companion object {
        const val FULL_WIDTH = 100
        const val MIN_WIDTH = 30
    }
}

object EditorDocument {
    private val photoLine = Regex(
        """(?m)^!\[]\(photo:([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})(?:\?w=(\d{1,3}))?\)$""",
    )

    /** Markdown → блоки. Результат всегда начинается и заканчивается текстом. */
    fun parse(markdown: String): List<DocumentBlock> {
        val blocks = mutableListOf<DocumentBlock>()
        var cursor = 0
        for (match in photoLine.findAll(markdown)) {
            blocks += DocumentBlock.Text(markdown.substring(cursor, match.range.first).dropTrailingNewline())
            val width = match.groupValues[2].toIntOrNull()
                ?.coerceIn(DocumentBlock.MIN_WIDTH, DocumentBlock.FULL_WIDTH)
                ?: DocumentBlock.FULL_WIDTH
            blocks += DocumentBlock.Photo(match.groupValues[1], width)
            cursor = match.range.last + 1
            // Отрезаем перевод строки после фото — он принадлежит разметке, а не тексту
            if (markdown.startsWith("\n", cursor)) cursor++
        }
        blocks += DocumentBlock.Text(markdown.substring(cursor))
        return blocks
    }

    /** Блоки → Markdown. Обратная к [parse]: parse(serialize(x)) == x. */
    fun serialize(blocks: List<DocumentBlock>): String = buildString {
        blocks.forEachIndexed { index, block ->
            when (block) {
                is DocumentBlock.Text -> append(block.text)
                is DocumentBlock.Photo -> {
                    // Фото всегда на своей строке
                    if (index > 0) append('\n')
                    append("![](photo:").append(block.photoId)
                    if (block.widthPercent != DocumentBlock.FULL_WIDTH) append("?w=").append(block.widthPercent)
                    append(')')
                    if (index < blocks.lastIndex) append('\n')
                }
            }
        }
    }

    /**
     * Приводит список к виду «текст, фото, текст, …, текст»:
     * склеивает соседние тексты и добавляет пустые тексты вокруг фото.
     */
    fun normalize(blocks: List<DocumentBlock>): List<DocumentBlock> {
        val result = mutableListOf<DocumentBlock>()
        for (block in blocks) {
            val last = result.lastOrNull()
            when {
                block is DocumentBlock.Text && last is DocumentBlock.Text ->
                    result[result.lastIndex] = DocumentBlock.Text(joinParagraphs(last.text, block.text))
                block is DocumentBlock.Photo && last !is DocumentBlock.Text ->
                    result += listOf(DocumentBlock.Text(""), block)
                else -> result += block
            }
        }
        if (result.lastOrNull() !is DocumentBlock.Text) result += DocumentBlock.Text("")
        return result
    }

    /** Склеить два куска текста, сохранив границу абзаца. */
    fun joinParagraphs(first: String, second: String): String = when {
        first.isEmpty() -> second
        second.isEmpty() -> first
        else -> first + "\n" + second
    }

    /** Перевод строки перед фото тоже принадлежит разметке. */
    private fun String.dropTrailingNewline(): String = removeSuffix("\n")
}
