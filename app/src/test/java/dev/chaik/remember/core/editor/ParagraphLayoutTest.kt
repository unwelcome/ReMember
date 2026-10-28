package dev.chaik.remember.core.editor

import org.junit.Assert.assertEquals
import org.junit.Test

class ParagraphLayoutTest {
    @Test
    fun `обычные строки — отдельные абзацы с красной строкой, пустая строка — пустой абзац`() {
        //          0123 4567 8 9
        val text = "Раз\nДва\n\nТри"
        assertEquals(
            listOf(
                TextBlock(0 until 3, true),
                TextBlock(4 until 7, true),
                TextBlock(8 until 8, true, blank = true),
                TextBlock(9 until 12, true),
            ),
            ParagraphLayout.blocks(text),
        )
    }

    @Test
    fun `после Enter в конце текста курсор — в пустом абзаце, а не в зазоре`() {
        // «Раз\n»: на экране «Раз·\n·» — пустая строка тоже со своим невидимым символом
        val text = "Раз\n"
        val ends = ParagraphLayout.ends(text, ParagraphLayout.blocks(text))
        assertEquals(listOf(3, 4), ends)
        val mapping = ParagraphEndMapping(ends)
        assertEquals(5, mapping.originalToTransformed(4)) // перед невидимым символом пустой строки
        assertEquals(4, mapping.transformedToOriginal(6))
    }

    @Test
    fun `список — один абзац без красной строки, заголовок — без красной строки`() {
        val text = "# Список\n- хлеб\n- сыр\nПотом домой"
        assertEquals(
            listOf(
                TextBlock(0 until 8, false),
                TextBlock(9 until 21, false), // «- хлеб\n- сыр» целиком: между пунктами зазора нет
                TextBlock(22 until 33, true),
            ),
            ParagraphLayout.blocks(text),
        )
    }

    @Test
    fun `невидимый символ — в конце абзаца перед переводом строки, кроме последнего`() {
        val text = "Раз\nДва\n\nТри"
        assertEquals(listOf(3, 7, 8), ParagraphLayout.ends(text, ParagraphLayout.blocks(text)))
    }

    @Test
    fun `конец абзаца остаётся в абзаце, зазор курсор не получает`() {
        // Текст «Раз\nДва»; на экране «Раз·\nДва» (· — невидимый символ на позиции 3)
        val mapping = ParagraphEndMapping(listOf(3))
        assertEquals(3, mapping.originalToTransformed(3)) // конец «Раз» — перед невидимым символом
        assertEquals(5, mapping.originalToTransformed(4)) // начало «Два»
        assertEquals(4, mapping.charToTransformed(3)) // сам перевод строки
        assertEquals(3, mapping.transformedToOriginal(3))
        assertEquals(3, mapping.transformedToOriginal(4)) // тап в зазор — конец «Раз»
        assertEquals(4, mapping.transformedToOriginal(5))
        assertEquals(7, mapping.transformedToOriginal(8))
    }
}
