package dev.chaik.remember.core.editor

import org.junit.Assert.assertEquals
import org.junit.Test

class MarkdownPreviewTest {
    private val photo = "3f2b8c1e-0d4a-4b6f-9e21-7c5a0b9d1e42"

    @Test
    fun `убирает разметку и фото`() {
        val markdown = """
            # План
            ![](photo:$photo?w=50) ![](photo:$photo)

            Купить **хлеб** и _молоко_, ~~сыр~~ не надо.
            - яйца
            1. масло
            - [x] чай
        """.trimIndent()

        assertEquals(
            "План\nКупить хлеб и молоко, сыр не надо.\nяйца\nмасло\nчай",
            MarkdownPreview.plain(markdown),
        )
    }

    @Test
    fun `обычные подчёркивания в словах не трогает`() {
        assertEquals("snake_case и 2*3", MarkdownPreview.plain("snake_case и 2*3"))
    }

    @Test
    fun `обрезает длинный текст`() {
        assertEquals("абвгд…", MarkdownPreview.plain("абвгдеёжз", maxChars = 5))
    }
}
