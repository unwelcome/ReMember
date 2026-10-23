package dev.chaik.remember.feature.today

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.time.LocalTime

/**
 * Режим страницы «Сегодня» (день / сон / траты) — живёт в приложении, а не в экране:
 * вкладка, от которой ушли далеко, пересоздаётся, и выбранный режим раньше терялся.
 *
 * При открытии приложения (запуск или возвращение после долгой паузы) режим выбирается
 * по времени: утром — сон, в остальное время — день.
 */
class TodaySession(private val clock: () -> LocalTime = LocalTime::now) {
    var mode by mutableStateOf(defaultFor(clock()))

    private var leftAt = 0L

    /** Приложение ушло в фон. */
    fun onBackground(now: Long = System.currentTimeMillis()) {
        leftAt = now
    }

    /** Вернулись: после долгой паузы это новое «открытие» — режим снова по времени суток. */
    fun onForeground(now: Long = System.currentTimeMillis()) {
        if (leftAt > 0 && now - leftAt >= NEW_OPENING_MILLIS) mode = defaultFor(clock())
        leftAt = 0
    }

    companion object {
        /** Пауза, после которой возвращение считается новым открытием приложения. */
        const val NEW_OPENING_MILLIS = 30 * 60_000L

        /** Утро — с 4:00 до 11:59: время записать сон. */
        fun defaultFor(time: LocalTime): TodayMode = if (time.hour in 4..11) TodayMode.Sleep else TodayMode.Day
    }
}
