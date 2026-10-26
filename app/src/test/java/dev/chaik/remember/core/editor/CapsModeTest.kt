package dev.chaik.remember.core.editor

import android.text.TextUtils.CAP_MODE_SENTENCES
import org.junit.Assert.assertEquals
import org.junit.Test

class CapsModeTest {
    private fun caps(before: String) = capsMode(before, CAP_MODE_SENTENCES) == CAP_MODE_SENTENCES

    @Test
    fun startOfTextAndLine() {
        assertEquals(true, caps(""))
        assertEquals(true, caps("Первая строка\n"))
    }

    @Test
    fun afterSentenceEnd() {
        assertEquals(true, caps("Был в зале. "))
        assertEquals(true, caps("Правда?! "))
        assertEquals(true, caps("Сказал «хватит». "))
        assertEquals(true, caps("(Потом.) "))
        assertEquals(false, caps("Был в зале. Потом"))
        assertEquals(false, caps("Был в зале, "))
        assertEquals(false, caps("Был в зале."))
    }

    @Test
    fun listItemsAndHeadings() {
        assertEquals(true, caps("- "))
        assertEquals(true, caps("Список:\n- "))
        assertEquals(true, caps("  - "))
        assertEquals(true, caps("* "))
        assertEquals(true, caps("12. "))
        assertEquals(true, caps("- [ ] "))
        assertEquals(true, caps("- [x] "))
        assertEquals(true, caps("## "))
        assertEquals(false, caps("- молоко, "))
        assertEquals(false, caps("-"))
    }
}
