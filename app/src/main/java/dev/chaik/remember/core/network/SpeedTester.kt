package dev.chaik.remember.core.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.Collections
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.coroutineContext
import kotlin.random.Random

/** Сервер замера: Cloudflare или свой (файл для скачивания и, по желанию, адрес для отдачи). */
sealed interface SpeedServer {
    val host: String

    data object Cloudflare : SpeedServer {
        override val host = "speed.cloudflare.com"
    }

    data class Custom(val downloadUrl: String, val uploadUrl: String?) : SpeedServer {
        override val host: String get() = runCatching { URL(downloadUrl).host }.getOrDefault(downloadUrl)
    }
}

enum class SpeedPhase { PING, DOWNLOAD, UPLOAD }

/**
 * Сколько длится каждая фаза (загрузка и отдача) и сколько байт на неё можно потратить
 * (null — без ограничения; ограничение — для мобильного трафика).
 */
data class SpeedOptions(val durationMs: Long, val byteLimit: Long? = null)

/** Итог замера. [uploadMbps] — null, если у своего сервера не задан адрес отдачи. */
data class SpeedResult(val pingMs: Double, val jitterMs: Double, val downloadMbps: Double, val uploadMbps: Double?)

/**
 * Замер скорости как у speedtest: пинг серией коротких запросов, затем скачивание и отдача
 * в несколько потоков по [SpeedOptions.durationMs] каждое. Первые [WARMUP_MS] не считаются —
 * пока соединения разгоняются, скорость занижена.
 */
class SpeedTester {

    /**
     * [onProgress] — фаза, текущее значение (пинг в мс, скорость в Мбит/с) и сколько фазы
     * пройдено (0…1): по времени или по лимиту трафика — что наступит раньше.
     */
    suspend fun run(server: SpeedServer, options: SpeedOptions, onProgress: (SpeedPhase, Double, Float) -> Unit): SpeedResult =
        withContext(Dispatchers.IO) {
            val (ping, jitter) = ping(server, onProgress)
            val download = measure(SpeedPhase.DOWNLOAD, DOWNLOAD_STREAMS, options, onProgress) { total, live -> downloadLoop(server, total, live) }
            val uploadUrl = uploadUrl(server)
            val upload = uploadUrl?.let { url ->
                measure(SpeedPhase.UPLOAD, UPLOAD_STREAMS, options, onProgress) { total, live -> uploadLoop(url, total, live) }
            }
            SpeedResult(ping, jitter, download, upload)
        }

    /** Пинг — медиана времени ответа, джиттер — средний разброс между соседними замерами. */
    private suspend fun ping(server: SpeedServer, onProgress: (SpeedPhase, Double, Float) -> Unit): Pair<Double, Double> {
        // Первый запрос заодно устанавливает соединение (DNS, TLS) — его не считаем
        pingOnce(server)
        val times = mutableListOf<Double>()
        repeat(PING_COUNT) {
            coroutineContext.ensureActive()
            times += pingOnce(server)
            onProgress(SpeedPhase.PING, times.sorted()[times.size / 2], times.size.toFloat() / PING_COUNT)
        }
        val median = times.sorted()[times.size / 2]
        val jitter = times.zipWithNext { a, b -> kotlin.math.abs(a - b) }.average().takeIf { !it.isNaN() } ?: 0.0
        return median to jitter
    }

    private fun pingOnce(server: SpeedServer): Double {
        val connection = open(
            when (server) {
                SpeedServer.Cloudflare -> "https://${server.host}/__down?bytes=0"
                is SpeedServer.Custom -> server.downloadUrl
            },
        )
        if (server is SpeedServer.Custom) connection.requestMethod = "HEAD"
        val start = System.nanoTime()
        try {
            connection.responseCode
            if (server is SpeedServer.Cloudflare) connection.inputStream.use { it.readBytes() }
            return (System.nanoTime() - start) / 1_000_000.0
        } finally {
            connection.disconnect()
        }
    }

    /**
     * Общий ход замера: [streams] потоков гонят данные, счётчик байт растёт; раз в [TICK_MS]
     * сообщаем текущую скорость. Через заданное время или по лимиту трафика всё останавливается.
     */
    private suspend fun measure(
        phase: SpeedPhase,
        streams: Int,
        options: SpeedOptions,
        onProgress: (SpeedPhase, Double, Float) -> Unit,
        worker: suspend (total: AtomicLong, live: MutableSet<HttpURLConnection>) -> Unit,
    ): Double = coroutineScope {
        val total = AtomicLong()
        val live = Collections.synchronizedSet(mutableSetOf<HttpURLConnection>())
        val error = AtomicReference<Throwable?>(null)
        val start = System.nanoTime()
        val workers = List(streams) {
            launch(Dispatchers.IO) {
                try {
                    worker(total, live)
                } catch (e: IOException) {
                    error.compareAndSet(null, e)
                }
            }
        }

        try {
            var warmBytes = 0L
            var warmAt = 0L
            var lastBytes = 0L
            var lastAt = start
            while (true) {
                delay(TICK_MS)
                val now = System.nanoTime()
                val bytes = total.get()
                val elapsedMs = (now - start) / 1_000_000
                val byTime = elapsedMs.toFloat() / options.durationMs
                val byBytes = options.byteLimit?.let { bytes.toFloat() / it } ?: 0f
                onProgress(phase, mbps(bytes - lastBytes, now - lastAt), maxOf(byTime, byBytes).coerceAtMost(1f))
                lastBytes = bytes
                lastAt = now
                if (warmAt == 0L && elapsedMs >= WARMUP_MS) {
                    warmBytes = bytes
                    warmAt = now
                }
                // Все потоки упали, не передав ни байта, — сети нет или сервер не отвечает
                if (bytes == 0L && workers.all { it.isCompleted } && error.get() != null) throw error.get()!!
                if (elapsedMs >= options.durationMs || byBytes >= 1f) break
            }
            if (total.get() == 0L) throw error.get() ?: IOException("Сервер не передал данных")
            // Лимит трафика кончился ещё до разгона — считаем по всему времени
            if (warmAt == 0L) mbps(total.get(), System.nanoTime() - start) else mbps(total.get() - warmBytes, System.nanoTime() - warmAt)
        } finally {
            // Чтение и запись блокирующие — рвём соединения, чтобы потоки сразу закончили
            // (и при остановке замера тоже)
            workers.forEach { it.cancel() }
            synchronized(live) { live.toList() }.forEach { it.disconnect() }
        }
    }

    private suspend fun downloadLoop(server: SpeedServer, total: AtomicLong, live: MutableSet<HttpURLConnection>) {
        val url = when (server) {
            SpeedServer.Cloudflare -> "https://${server.host}/__down?bytes=$DOWNLOAD_CHUNK"
            is SpeedServer.Custom -> server.downloadUrl
        }
        val buffer = ByteArray(BUFFER)
        while (coroutineContext.isActive) {
            val connection = open(url)
            live += connection
            try {
                connection.inputStream.use { input ->
                    while (coroutineContext.isActive) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        total.addAndGet(read.toLong())
                    }
                }
            } finally {
                live -= connection
                connection.disconnect()
            }
        }
    }

    private suspend fun uploadLoop(url: String, total: AtomicLong, live: MutableSet<HttpURLConnection>) {
        val buffer = Random.nextBytes(BUFFER)
        while (coroutineContext.isActive) {
            val connection = open(url).apply {
                requestMethod = "POST"
                doOutput = true
                setFixedLengthStreamingMode(UPLOAD_CHUNK)
                setRequestProperty("Content-Type", "application/octet-stream")
            }
            live += connection
            try {
                connection.outputStream.use { output ->
                    var sent = 0L
                    while (sent < UPLOAD_CHUNK && coroutineContext.isActive) {
                        val size = minOf(BUFFER.toLong(), UPLOAD_CHUNK - sent).toInt()
                        output.write(buffer, 0, size)
                        sent += size
                        total.addAndGet(size.toLong())
                    }
                }
                connection.responseCode
            } finally {
                live -= connection
                connection.disconnect()
            }
        }
    }

    private fun uploadUrl(server: SpeedServer): String? = when (server) {
        SpeedServer.Cloudflare -> "https://${server.host}/__up"
        is SpeedServer.Custom -> server.uploadUrl?.takeIf { it.isNotBlank() }
    }

    private fun open(url: String): HttpURLConnection = (URL(url).openConnection() as HttpURLConnection).apply {
        connectTimeout = TIMEOUT_MS
        readTimeout = TIMEOUT_MS
        useCaches = false
        setRequestProperty("Cache-Control", "no-cache")
    }

    private fun mbps(bytes: Long, nanos: Long): Double = if (nanos <= 0) 0.0 else bytes * 8.0 / (nanos / 1e9) / 1e6

    private companion object {
        const val PING_COUNT = 10
        const val WARMUP_MS = 1_500L
        const val TICK_MS = 250L
        const val DOWNLOAD_STREAMS = 4
        const val UPLOAD_STREAMS = 3
        const val DOWNLOAD_CHUNK = 25_000_000L
        const val UPLOAD_CHUNK = 4_000_000L
        const val BUFFER = 64 * 1024
        const val TIMEOUT_MS = 8_000
    }
}
