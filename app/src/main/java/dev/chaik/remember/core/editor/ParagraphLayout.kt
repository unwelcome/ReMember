package dev.chaik.remember.core.editor

import androidx.compose.ui.text.input.OffsetMapping

/** Кусок текста, который на экране — отдельный абзац. [blank] — пустая строка. */
internal data class TextBlock(val range: IntRange, val indented: Boolean, val blank: Boolean = false)

/**
 * Абзацы текста для экрана. Обычная строка — абзац с красной строкой; пункты одного списка
 * подряд — один абзац (между ними зазора нет); заголовок — абзац без красной строки;
 * пустая строка — пустой абзац обычной высоты (на нём может стоять курсор).
 * Переводы строк между абзацами в абзацы не входят — на экране они и дают зазор.
 */
internal object ParagraphLayout {

    fun blocks(text: String): List<TextBlock> {
        val blocks = mutableListOf<TextBlock>()
        var start = 0
        var listStart = -1
        var listEnd = -1
        fun closeList() {
            if (listStart >= 0) blocks += TextBlock(listStart until listEnd, indented = false)
            listStart = -1
        }
        for (line in text.split('\n')) {
            val end = start + line.length
            when {
                line.isBlank() -> {
                    closeList()
                    // С красной строкой — курсор стоит там, где появится первая буква
                    blocks += TextBlock(start until end, indented = true, blank = true)
                }
                ListPrefix.find(line) != null -> {
                    if (listStart < 0) listStart = start
                    listEnd = end
                }
                else -> {
                    closeList()
                    blocks += TextBlock(start until end, indented = HeadingPrefix.find(line) == null)
                }
            }
            start = end + 1
        }
        closeList()
        return blocks
    }

    /**
     * Позиции, перед которыми на экране невидимый символ: конец каждого абзаца перед переводом
     * строки (иначе позиция «конец абзаца» уже относится к зазору) и конец каждой пустой строки
     * (иначе у пустого абзаца нет ни одного символа и на экране его нет).
     */
    fun ends(text: String, blocks: List<TextBlock>): List<Int> =
        blocks.filter { hasMark(text, it) }.map { it.range.last + 1 }

    fun hasMark(text: String, block: TextBlock): Boolean = block.blank || block.range.last + 1 < text.length
}

/**
 * Позиции «текст ↔ экран», когда на экране перед каждой из позиций [ends] вставлено
 * по одному невидимому символу (он — часть абзаца перед ним).
 */
internal class ParagraphEndMapping(val ends: List<Int>) : OffsetMapping {
    /** Позиция курсора: конец абзаца остаётся перед невидимым символом. */
    override fun originalToTransformed(offset: Int): Int = offset + ends.count { it < offset }

    /** Символ текста с номером [index] — где он на экране. */
    fun charToTransformed(index: Int): Int = index + ends.count { it <= index }

    override fun transformedToOriginal(offset: Int): Int {
        // Невидимый символ номер k стоит на экране в позиции ends[k] + k
        val before = ends.withIndex().count { (k, end) -> end + k < offset }
        return offset - before
    }
}
