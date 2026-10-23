package dev.chaik.remember.core.ui.format

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MoneyFormatTest {
    @Test
    fun `формат суммы`() {
        assertEquals("450 ₽", MoneyFormat.format(45_000, "RUB"))
        assertEquals("1 234,50 ₽", MoneyFormat.format(123_450, "RUB"))
        assertEquals("−99 ₽", MoneyFormat.format(9_900, "RUB", withSign = true))
        assertEquals("+0,05 ₽", MoneyFormat.format(5, "RUB", withSign = true, sign = '+'))
        // Знак — по коду валюты, незнакомый код пишется как есть
        assertEquals("450 $", MoneyFormat.format(45_000, "USD"))
        assertEquals("450 CHF", MoneyFormat.format(45_000, "CHF"))
    }

    @Test
    fun `разбор ввода`() {
        assertEquals(45_000L, MoneyFormat.parse("450"))
        assertEquals(45_050L, MoneyFormat.parse("450,5"))
        assertEquals(123_450L, MoneyFormat.parse("1 234.50"))
        assertEquals(1L, MoneyFormat.parse("0,005")) // округление до копейки
        assertNull(MoneyFormat.parse(""))
        assertNull(MoneyFormat.parse("0"))
        assertNull(MoneyFormat.parse("-5"))
        assertNull(MoneyFormat.parse("абв"))
    }

    @Test
    fun `сумма для поля ввода`() {
        assertEquals("450", MoneyFormat.toInput(45_000))
        assertEquals("450,50", MoneyFormat.toInput(45_050))
    }
}
