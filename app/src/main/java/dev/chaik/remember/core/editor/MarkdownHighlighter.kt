package dev.chaik.remember.core.editor

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/**
 * Подсветка Markdown прямо в поле ввода: символы разметки остаются видны
 * (бледными), а текст между ними становится жирным, курсивным и т. д.
 *
 * Ещё — вид абзацев: у обычного абзаца красная строка, а между абзацами — зазор.
 * Своего «интервала между абзацами» у текста в Compose нет, поэтому зазор — это отдельный абзац
 * из одной строки нужной высоты. Им становится перевод строки между абзацами, только на экране
 * он заменён невидимым символом: сам перевод строки Compose дорисовал бы ещё пустой строкой
 * своей высоты, и зазор не уменьшался бы до нуля. Абзацы и без него идут каждый с новой строки.
 *
 * Только позиция «конец абзаца» (перед этим переводом строки) по правилам Compose уже относится
 * к зазору — курсор вставал бы в него. Поэтому на экране в конце абзаца есть невидимый символ
 * [EndMark]: конец абзаца — перед ним. Такой же символ — в пустой строке: так она обычный абзац
 * нормальной высоты, а не низкий зазор. Текст при этом не меняется, а позиции на экране сдвинуты —
 * переводить их через [offsetMapping].
 */
class MarkdownHighlighter(
    private val markerColor: Color,
    private val accentColor: Color,
    /** Красная строка, десятые доли em; 0 — без неё. */
    private val indentTenths: Int = 12,
    /** Расстояние между абзацами, sp. */
    private val gapSp: Int = 18,
) : VisualTransformation {

    private val redLine = PlainParagraph.copy(textIndent = TextIndent(firstLine = (indentTenths / 10f).em))

    // Зазор — абзац из одной строки такой высоты. Совсем нулевой высоты строка быть не может.
    // Без обрезки: у строки из одной строки она съела бы всю добавленную высоту
    private val gapParagraph = ParagraphStyle(
        lineHeight = gapSp.toFloat().coerceAtLeast(0.1f).sp,
        lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None),
    )

    override fun filter(text: AnnotatedString): TransformedText {
        val source = text.text
        val blocks = ParagraphLayout.blocks(source)
        val mapping = ParagraphEndMapping(ParagraphLayout.ends(source, blocks))
        // Переводы строк между абзацами (и вокруг пустых строк) — это зазоры
        val gaps = mutableListOf<Int>()
        var inside = 0
        source.forEachIndexed { i, char ->
            while (inside < blocks.size && blocks[inside].range.last < i) inside++
            val inBlock = inside < blocks.size && i in blocks[inside].range
            if (char == '\n' && !inBlock) gaps += i
        }
        val highlighted = highlight(source)
        val chars = source.toCharArray().also { for (i in gaps) it[i] = EndMark }
        val styled = AnnotatedString(chars.concatToString(), highlighted.spanStyles)
        val shown = buildAnnotatedString {
            var from = 0
            for (end in mapping.ends) {
                append(styled.subSequence(from, end))
                append(EndMark)
                from = end
            }
            append(styled.subSequence(from, source.length))

            for (block in blocks) {
                // Красная строка — у обычных абзацев; список и заголовок — без неё.
                // Невидимый символ в конце — тоже часть абзаца
                val start = mapping.originalToTransformed(block.range.first)
                val end = mapping.originalToTransformed(block.range.last + 1) + if (ParagraphLayout.hasMark(source, block)) 1 else 0
                addStyle(if (block.indented) redLine else PlainParagraph, start, end)
            }
            for (i in gaps) {
                val at = mapping.charToTransformed(i)
                addStyle(GapStyle, at, at + 1)
                addStyle(gapParagraph, at, at + 1)
            }
        }
        return TransformedText(shown, mapping)
    }

    /** Как позиции в тексте [text] сдвинуты на экране (невидимые символы в конце абзацев). */
    fun offsetMapping(text: String): OffsetMapping =
        ParagraphEndMapping(ParagraphLayout.ends(text, ParagraphLayout.blocks(text)))

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
        other is MarkdownHighlighter && other.markerColor == markerColor && other.accentColor == accentColor &&
            other.indentTenths == indentTenths && other.gapSp == gapSp

    override fun hashCode() = ((31 * markerColor.hashCode() + accentColor.hashCode()) * 31 + indentTenths) * 31 + gapSp

    private companion object {
        /** Шрифт зазора — крошечный, чтобы высоту задавал только [gapParagraph]. */
        val GapStyle = SpanStyle(fontSize = 0.02.em)
        /**
         * Абзац текста. Межстрочный отступ на краях абзаца не обрезаем: иначе при нулевом
         * расстоянии абзацы стояли бы теснее, чем строки внутри абзаца.
         */
        val PlainParagraph = ParagraphStyle(
            lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Proportional, LineHeightStyle.Trim.None),
        )

        /** Невидимый символ нулевой ширины в конце абзаца. */
        const val EndMark = '​'

        val Bold = Regex("""(\*\*[^*\n]+\*\*)""")
        val Strike = Regex("""(~~[^~\n]+~~)""")
        // _курсив_ или *курсив*, но не часть **жирного**
        val Italic = Regex("""(?<![*\w])(_[^_\n]+_|\*[^*\n]+\*)(?![*\w])""")
    }
}
