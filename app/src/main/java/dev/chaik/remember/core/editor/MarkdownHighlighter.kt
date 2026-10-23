package dev.chaik.remember.core.editor

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.em

/**
 * Подсветка Markdown прямо в поле ввода: символы разметки остаются видны
 * (бледными), а текст между ними становится жирным, курсивным и т. д.
 * Текст не меняется, только оформление — поэтому позиции курсора совпадают.
 */
class MarkdownHighlighter(
    private val markerColor: Color,
    private val accentColor: Color,
) : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText =
        TransformedText(highlight(text.text), OffsetMapping.Identity)

    fun highlight(text: String): AnnotatedString = buildAnnotatedString {
        append(text)
        val marker = SpanStyle(color = markerColor)

        // Построчно: заголовки и списки
        var lineStart = 0
        for (line in text.split('\n')) {
            HeadingPrefix.find(line)?.let { match ->
                val size = if (match.groupValues[1].length == 1) 1.4.em else 1.2.em
                addStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = size), lineStart, lineStart + line.length)
                addStyle(marker, lineStart, lineStart + match.value.length)
            }
            ListPrefix.find(line)?.let { match ->
                val from = lineStart + match.groupValues[1].length
                addStyle(SpanStyle(color = accentColor, fontWeight = FontWeight.Bold), from, lineStart + match.value.length)
                if (match.groupValues[2].startsWith("- [x", ignoreCase = true)) {
                    // Выполненный пункт — зачёркнут
                    addStyle(
                        SpanStyle(textDecoration = TextDecoration.LineThrough, color = markerColor),
                        lineStart + match.value.length,
                        lineStart + line.length,
                    )
                }
            }
            lineStart += line.length + 1
        }

        // Внутри строк: жирный, курсив, зачёркнутый
        inline(text, Bold, SpanStyle(fontWeight = FontWeight.Bold), marker, markerLength = 2)
        inline(text, Strike, SpanStyle(textDecoration = TextDecoration.LineThrough), marker, markerLength = 2)
        inline(text, Italic, SpanStyle(fontStyle = FontStyle.Italic), marker, markerLength = 1)
    }

    private fun AnnotatedString.Builder.inline(
        text: String,
        pattern: Regex,
        style: SpanStyle,
        marker: SpanStyle,
        markerLength: Int,
    ) {
        for (match in pattern.findAll(text)) {
            val range = match.groups[1]!!.range
            addStyle(style, range.first, range.last + 1)
            addStyle(marker, range.first, range.first + markerLength)
            addStyle(marker, range.last + 1 - markerLength, range.last + 1)
        }
    }

    // equals/hashCode: иначе поле пересчитывает разметку при каждой перекомпозиции
    override fun equals(other: Any?) =
        other is MarkdownHighlighter && other.markerColor == markerColor && other.accentColor == accentColor

    override fun hashCode() = 31 * markerColor.hashCode() + accentColor.hashCode()

    private companion object {
        val Bold = Regex("""(\*\*[^*\n]+\*\*)""")
        val Strike = Regex("""(~~[^~\n]+~~)""")
        // _курсив_ или *курсив*, но не часть **жирного**
        val Italic = Regex("""(?<![*\w])(_[^_\n]+_|\*[^*\n]+\*)(?![*\w])""")
    }
}
