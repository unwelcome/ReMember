package dev.chaik.remember.core.editor

import dev.chaik.remember.core.editor.DocumentBlock.Photo
import dev.chaik.remember.core.editor.DocumentBlock.Text
import org.junit.Assert.assertEquals
import org.junit.Test

class EditorDocumentTest {
    private val p1 = "3f2b8c1e-0d4a-4b6f-9e21-7c5a0b9d1e42"
    private val p2 = "a1b2c3d4-e5f6-4a7b-8c9d-0e1f2a3b4c5d"

    private fun roundTrip(vararg blocks: DocumentBlock) {
        val list = blocks.toList()
        assertEquals(list, EditorDocument.parse(EditorDocument.serialize(list)))
    }

    @Test
    fun `обычный текст без фото`() {
        assertEquals(listOf(Text("Привет\nмир")), EditorDocument.parse("Привет\nмир"))
        roundTrip(Text(""))
        roundTrip(Text("Привет\n\nмир\n"))
    }

    @Test
    fun `фото между абзацами`() {
        val markdown = "Утром гулял.\n![](photo:$p1)\nВечером кино."
        val blocks = listOf(Text("Утром гулял."), Photo(p1), Text("Вечером кино."))

        assertEquals(blocks, EditorDocument.parse(markdown))
        assertEquals(markdown, EditorDocument.serialize(blocks))
    }

    @Test
    fun `ширина фото`() {
        assertEquals(
            listOf(Text("a"), Photo(p1, 50), Text("b")),
            EditorDocument.parse("a\n![](photo:$p1?w=50)\nb"),
        )
        // Слишком маленькая ширина поднимается до минимума
        assertEquals(Photo(p1, DocumentBlock.MIN_WIDTH), EditorDocument.parse("![](photo:$p1?w=5)")[1])
    }

    @Test
    fun `крайние случаи сохраняются без потерь`() {
        roundTrip(Text(""), Photo(p1), Text(""))
        roundTrip(Text(""), Photo(p1), Text(""), Photo(p2, 75), Text(""))
        roundTrip(Text("до\n"), Photo(p1), Text("\nпосле"))
        roundTrip(Text("\n\n"), Photo(p1, 30), Text("\n\n"))
    }

    @Test
    fun `нормализация склеивает тексты и окружает фото текстом`() {
        assertEquals(
            listOf(Text(""), Photo(p1), Text(""), Photo(p2), Text("a\nb")),
            EditorDocument.normalize(listOf(Photo(p1), Photo(p2), Text("a"), Text("b"))),
        )
    }
}
