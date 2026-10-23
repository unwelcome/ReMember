package dev.chaik.remember.core.editor

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import org.junit.Assert.assertEquals
import org.junit.Test

class MarkdownFormattingTest {
    /** Текст с курсором `|` или выделением `«…»` (не `[]` — они есть в разметке чек-листов). */
    private fun v(spec: String): TextFieldValue {
        val start = spec.indexOf('«')
        if (start >= 0) {
            val end = spec.indexOf('»') - 1
            return TextFieldValue(spec.replace("«", "").replace("»", ""), TextRange(start, end))
        }
        val cursor = spec.indexOf('|')
        return TextFieldValue(spec.replace("|", ""), TextRange(cursor))
    }

    private fun assertValue(expected: String, actual: TextFieldValue) = assertEquals(v(expected), actual)

    @Test
    fun `жирный оборачивает и снимает`() {
        assertValue("очень **«важно»** сегодня", MarkdownFormatting.toggleWrap(v("очень «важно» сегодня"), "**"))
        assertValue("очень «важно» сегодня", MarkdownFormatting.toggleWrap(v("очень **«важно»** сегодня"), "**"))
        assertValue("текст **|**", MarkdownFormatting.toggleWrap(v("текст |"), "**"))
    }

    @Test
    fun `список включается для всех выделенных строк и выключается`() {
        val on = MarkdownFormatting.toggleLineStyle(v("«хлеб\nмолоко»"), LineStyle.Bullet)
        assertEquals("- хлеб\n- молоко", on.text)
        assertEquals("хлеб\nмолоко", MarkdownFormatting.toggleLineStyle(on, LineStyle.Bullet).text)
    }

    @Test
    fun `нумерация по порядку и замена другого списка`() {
        assertEquals("1. a\n2. b", MarkdownFormatting.toggleLineStyle(v("«- a\n- b»"), LineStyle.Numbered).text)
        assertValue("- [ ] купить|", MarkdownFormatting.toggleLineStyle(v("купить|"), LineStyle.Checkbox))
    }

    @Test
    fun `заголовок по кругу`() {
        val h1 = MarkdownFormatting.cycleHeading(v("Итоги|"))
        assertValue("# Итоги|", h1)
        val h2 = MarkdownFormatting.cycleHeading(h1)
        assertValue("## Итоги|", h2)
        assertValue("Итоги|", MarkdownFormatting.cycleHeading(h2))
    }

    @Test
    fun `заголовок меняет только строку курсора`() {
        assertValue("утро\n# де|нь\nвечер", MarkdownFormatting.cycleHeading(v("утро\nде|нь\nвечер")))
    }

    @Test
    fun `Enter продолжает список`() {
        assertValue("- хлеб\n- |", MarkdownFormatting.continueList(v("- хлеб|"), v("- хлеб\n|")))
        assertValue("1. раз\n2. |", MarkdownFormatting.continueList(v("1. раз|"), v("1. раз\n|")))
        assertValue(
            "- [x] сделано\n- [ ] |",
            MarkdownFormatting.continueList(v("- [x] сделано|"), v("- [x] сделано\n|")),
        )
    }

    @Test
    fun `Enter на пустом пункте заканчивает список`() {
        assertValue("- хлеб\n|", MarkdownFormatting.continueList(v("- хлеб\n- |"), v("- хлеб\n- \n|")))
    }

    @Test
    fun `обычный ввод не трогается`() {
        val new = v("привет\n|")
        assertEquals(new, MarkdownFormatting.continueList(v("привет|"), new))
        val typed = v("- хлеба|")
        assertEquals(typed, MarkdownFormatting.continueList(v("- хлеб|"), typed))
    }
}
