package dev.chaik.remember.core.network

import org.junit.Assert.assertEquals
import org.junit.Test

class WhitelistCheckTest {
    private fun ok(host: String) = SiteResult(host, 80)
    private fun fail(host: String) = SiteResult(host, null)
    private val allowedOk = listOf(ok("ya.ru"), ok("vk.com"), ok("mail.ru"))

    @Test
    fun `белые списки — свои открываются, остальные нет`() {
        assertEquals(WhitelistVerdict.WHITELIST, WhitelistCheck.verdict(allowedOk, listOf(fail("google.com"), fail("github.com"))))
        // Даже если один «свой» сайт лёг
        assertEquals(WhitelistVerdict.WHITELIST, WhitelistCheck.verdict(listOf(ok("ya.ru"), fail("vk.com")), listOf(fail("google.com"))))
    }

    @Test
    fun `ограничений нет и интернета нет`() {
        assertEquals(WhitelistVerdict.OPEN, WhitelistCheck.verdict(allowedOk, listOf(ok("google.com"), ok("github.com"), fail("amazon.com"))))
        assertEquals(WhitelistVerdict.NO_INTERNET, WhitelistCheck.verdict(listOf(fail("ya.ru")), listOf(fail("google.com"))))
    }

    @Test
    fun `неоднозначно и не с чем сравнить`() {
        assertEquals(WhitelistVerdict.PARTIAL, WhitelistCheck.verdict(allowedOk, listOf(ok("google.com"), fail("github.com"), fail("amazon.com"))))
        assertEquals(WhitelistVerdict.UNKNOWN, WhitelistCheck.verdict(allowedOk, emptyList()))
    }

    @Test
    fun `адрес сайта из ввода`() {
        assertEquals("github.com", WhitelistCheck.normalizeHost(" https://GitHub.com/login?x=1 "))
        assertEquals("сбербанк.рф", WhitelistCheck.normalizeHost("сбербанк.рф"))
        assertEquals(null, WhitelistCheck.normalizeHost("не адрес"))
        assertEquals(null, WhitelistCheck.normalizeHost("localhost"))
    }
}
