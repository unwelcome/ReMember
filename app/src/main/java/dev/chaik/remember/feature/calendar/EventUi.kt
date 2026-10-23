package dev.chaik.remember.feature.calendar

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import dev.chaik.remember.R
import dev.chaik.remember.core.model.EventColor
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

fun EventColor.color(): Color = Color(argb)

/** Готовые варианты «Предупредить за», в минутах. null — не предупреждать. */
val RemindPresets: List<Int?> = listOf(null, 0, 15, 30, 60, 120, 1440, 2880, 10080)

const val MINUTES_IN_HOUR = 60
const val MINUTES_IN_DAY = 1440

/** «Не предупреждать», «В момент события», «За 2 часа», «За 3 дня»… */
@Composable
fun remindLabel(minutes: Int?): String = when {
    minutes == null -> stringResource(R.string.event_remind_none)
    minutes == 0 -> stringResource(R.string.event_remind_at_time)
    minutes % MINUTES_IN_DAY == 0 -> (minutes / MINUTES_IN_DAY).let { pluralStringResource(R.plurals.remind_days, it, it) }
    minutes % MINUTES_IN_HOUR == 0 -> (minutes / MINUTES_IN_HOUR).let { pluralStringResource(R.plurals.remind_hours, it, it) }
    else -> pluralStringResource(R.plurals.remind_minutes, minutes, minutes)
}

/** «31 год», «2 года», «5 лет». */
@Composable
fun yearsLabel(years: Int): String = pluralStringResource(R.plurals.years, years, years)

/** «23 октября 2026»; [withYear] = false — «23 октября» (для дней рождения). */
fun formatDay(date: LocalDate, withYear: Boolean = true): String =
    date.format(DateTimeFormatter.ofPattern(if (withYear) "d MMMM yyyy" else "d MMMM", Locale.getDefault()))
