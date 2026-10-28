package dev.chaik.remember.core.editor

import dev.chaik.remember.core.model.Photo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EditorStateFocusTest {
    private val photo = Photo("3f2b8c1e-0d4a-4b6f-9e21-7c5a0b9d1e42", 400, 300)

    @Test
    fun `редактор создаётся с текстом и без курсора`() {
        val state = EditorState("Привет")
        assertFalse(state.hasFocus)
        assertEquals("Привет", state.markdown)
    }

    @Test
    fun `после вставки фото новое поле без фокуса не гасит курсор`() {
        val state = EditorState("Утро")
        val first = state.blocks.first() as EditorState.TextBlock
        state.onTextFocusChanged(first, true)

        val caret = state.insertPhotos(listOf(photo))!!
        // Новое поле под фото при появлении сообщает «без фокуса» — курсор всё ещё в старом блоке
        state.onTextFocusChanged(caret, false)
        assertTrue(state.hasFocus)

        // Курсор переехал под фото: старый блок потерял фокус, новый получил
        state.onTextFocusChanged(caret, true)
        state.onTextFocusChanged(first, false)
        assertTrue(state.hasFocus)

        // «Назад» убрал курсор
        state.onTextFocusChanged(caret, false)
        assertFalse(state.hasFocus)
    }
}
