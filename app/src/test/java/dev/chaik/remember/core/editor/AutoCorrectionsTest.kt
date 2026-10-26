package dev.chaik.remember.core.editor

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import org.junit.Assert.assertEquals
import org.junit.Test

class AutoCorrectionsTest {
    /** «|» — курсор; без него курсор в конце. */
    private fun v(text: String): TextFieldValue {
        val clean = text.replace("|", "")
        return TextFieldValue(clean, TextRange(text.indexOf('|').takeIf { it >= 0 } ?: clean.length))
    }

    private val milk = AutoCorrection(start = 6, corrected = "молоко", original = "малако")

    @Test
    fun `новая замена встаёт на своё место`() {
        val result = AutoCorrections.update(emptyList(), milk, "Купил малако|", v("Купил молоко |"))
        assertEquals(listOf(milk), result)
    }

    @Test
    fun `без позиции от клавиатуры ищем слово перед курсором`() {
        val unknown = milk.copy(start = -1)
        assertEquals(listOf(milk), AutoCorrections.update(emptyList(), unknown, "Купил малако", v("Купил молоко |")))
        // Слово далеко от курсора — не наше
        assertEquals(emptyList<AutoCorrection>(), AutoCorrections.update(emptyList(), unknown, "Купил", v("Купил молоко и хлеб|")))
    }

    @Test
    fun `правки до слова сдвигают его, после — не трогают`() {
        val before = AutoCorrections.update(listOf(milk), null, "Купил молоко", v("Я купил молоко"))
        assertEquals(listOf(milk.copy(start = 8)), before)
        val after = AutoCorrections.update(listOf(milk), null, "Купил молоко", v("Купил молоко и хлеб"))
        assertEquals(listOf(milk), after)
    }

    @Test
    fun `правка самого слова убирает замену`() {
        assertEquals(emptyList<AutoCorrection>(), AutoCorrections.update(listOf(milk), null, "Купил молоко", v("Купил молок")))
        // Дописали букву вплотную — слово уже другое
        assertEquals(emptyList<AutoCorrection>(), AutoCorrections.update(listOf(milk), null, "Купил молоко", v("Купил молокос")))
    }

    @Test
    fun `вернуть набранное слово`() {
        val reverted = AutoCorrections.revert(v("Купил молоко и хлеб"), milk)
        assertEquals("Купил малако и хлеб", reverted.text)
        assertEquals(TextRange(12), reverted.selection)
    }
}
