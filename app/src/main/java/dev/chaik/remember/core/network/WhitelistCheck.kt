package dev.chaik.remember.core.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** Ответил ли сайт; [ms] — за сколько (null — не ответил). */
data class SiteResult(val host: String, val ms: Long?) {
    val reachable: Boolean get() = ms != null
}

/** Итог проверки. */
enum class WhitelistVerdict {
    /** Не открылось ничего — интернета нет. */
    NO_INTERNET,

    /** Сайты из белого списка открываются, остальные — нет: похоже, включены белые списки. */
    WHITELIST,

    /** Открывается большинство «обычных» сайтов — ограничений не видно. */
    OPEN,

    /** Открывается не всё, но и на белые списки не похоже (блокировки отдельных сайтов, медленная сеть). */
    PARTIAL,

    /** Сравнивать не с чем — в одном из списков нет сайтов. */
    UNKNOWN,
}

/**
 * Проверка белых списков: открываются ли сайты, которые обычно в белом списке
 * (отечественные сервисы), и те, которых в нём обычно нет. Сайт «открылся», если ответил
 * хоть чем-то (даже ошибкой 403/404): важно, что соединение проходит, а не что на странице.
 */
object WhitelistCheck {

    /** Все сайты сразу; [onResult] — как только ответил (или не ответил) очередной. */
    suspend fun check(hosts: List<String>, onResult: (SiteResult) -> Unit = {}): List<SiteResult> = withContext(Dispatchers.IO) {
        coroutineScope { hosts.map { host -> async { checkSite(host).also(onResult) } }.awaitAll() }
    }

    private fun checkSite(host: String): SiteResult {
        val connection = try {
            URL("https://$host/").openConnection() as HttpURLConnection
        } catch (_: Exception) {
            return SiteResult(host, null)
        }
        connection.connectTimeout = TIMEOUT_MS
        connection.readTimeout = TIMEOUT_MS
        connection.instanceFollowRedirects = false
        connection.useCaches = false
        val start = System.nanoTime()
        return try {
            connection.responseCode
            SiteResult(host, (System.nanoTime() - start) / 1_000_000)
        } catch (_: IOException) {
            SiteResult(host, null)
        } finally {
            connection.disconnect()
        }
    }

    /** [allowed] — сайты «обычно в белом списке», [others] — «обычно нет». */
    fun verdict(allowed: List<SiteResult>, others: List<SiteResult>): WhitelistVerdict {
        if (allowed.isEmpty() || others.isEmpty()) return WhitelistVerdict.UNKNOWN
        val allowedOk = allowed.count { it.reachable }
        val othersOk = others.count { it.reachable }
        return when {
            allowedOk == 0 && othersOk == 0 -> WhitelistVerdict.NO_INTERNET
            othersOk == 0 -> WhitelistVerdict.WHITELIST
            othersOk * 2 >= others.size && allowedOk * 2 >= allowed.size -> WhitelistVerdict.OPEN
            else -> WhitelistVerdict.PARTIAL
        }
    }

    /** Адрес сайта из того, что ввели: без схемы, пути и пробелов. null — не похоже на адрес. */
    fun normalizeHost(input: String): String? {
        val host = input.trim().lowercase()
            .removePrefix("https://").removePrefix("http://")
            .substringBefore('/').substringBefore('?')
        return host.takeIf { it.matches(HOST) }
    }

    private val HOST = Regex("""^[\p{L}\d-]+(\.[\p{L}\d-]+)+$""")
    private const val TIMEOUT_MS = 5_000
}
