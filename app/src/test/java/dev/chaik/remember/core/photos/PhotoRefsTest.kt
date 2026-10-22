package dev.chaik.remember.core.photos

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PhotoRefsTest {
    private val a = "3f2b8c1e-0d4a-4b6f-9e21-7c5a0b9d1e42"
    private val b = "a1b2c3d4-e5f6-4a7b-8c9d-0e1f2a3b4c5d"

    @Test
    fun `находит все фото в тексте`() {
        val text = """
            Утром гулял.
            ![](${PhotoRefs.uri(a)})
            Вечером кино.
            ![](${PhotoRefs.uri(b)}?w=50)
        """.trimIndent()

        assertEquals(setOf(a, b), PhotoRefs.extract(text))
    }

    @Test
    fun `повторная ссылка считается один раз`() {
        val text = "![](photo:$a) и снова ![](photo:$a)"
        assertEquals(setOf(a), PhotoRefs.extract(text))
    }

    @Test
    fun `текст без фото и мусорные ссылки`() {
        assertTrue(PhotoRefs.extract("просто текст").isEmpty())
        assertTrue(PhotoRefs.extract("photo:не-id и photo:1234").isEmpty())
    }
}
