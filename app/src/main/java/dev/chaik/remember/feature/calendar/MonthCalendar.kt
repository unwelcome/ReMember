package dev.chaik.remember.feature.calendar

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chaik.remember.R
import dev.chaik.remember.core.model.Event
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val CellShape = RoundedCornerShape(14.dp)
private val CellSpacing = 6.dp

/** Сколько точек событий помещается под числом. */
private const val MAX_DOTS = 4

/**
 * Большой календарь месяца в карточке: заголовок со стрелками, дни недели
 * и сетка 6×7 скруглённых квадратов. Месяцы листаются свайпом по сетке.
 * Под числом — цветные точки событий дня.
 *
 * @param monthOf какой месяц на странице пейджера
 */
@Composable
fun MonthCalendar(
    pager: PagerState,
    monthOf: (page: Int) -> YearMonth,
    events: List<Event>,
    today: LocalDate,
    selected: LocalDate,
    firstDayOfWeek: DayOfWeek,
    onSelect: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        MonthHeader(
            month = monthOf(pager.currentPage),
            onPrev = { scope.launch { pager.animateScrollToPage(pager.currentPage - 1) } },
            onNext = { scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } },
        )
        WeekdayRow(firstDayOfWeek, Modifier.padding(vertical = 6.dp))

        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val cell = (maxWidth - CellSpacing * 6) / 7
            HorizontalPager(
                state = pager,
                // Высота постоянная — 6 недель, у всех месяцев одинаково
                modifier = Modifier.height(cell * 6 + CellSpacing * 5),
                key = { it },
            ) { page ->
                val month = monthOf(page)
                val days = remember(month, firstDayOfWeek) { monthGrid(month, firstDayOfWeek) }
                val byDay = remember(events, days) { eventsByDay(events, days) }
                MonthGrid(
                    month = month,
                    days = days,
                    byDay = byDay,
                    today = today,
                    selected = selected,
                    cell = cell,
                    onSelect = onSelect,
                )
            }
        }
    }
}

@Composable
private fun MonthHeader(month: YearMonth, onPrev: () -> Unit, onNext: () -> Unit) {
    val name = remember(month) {
        month.format(DateTimeFormatter.ofPattern("LLLL", Locale.getDefault())).replaceFirstChar { it.titlecase() }
    }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.weight(1f).padding(start = 8.dp)) {
            Text(name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
                text = month.year.toString(),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp, bottom = 2.dp),
            )
        }
        IconButton(onClick = onPrev) {
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, stringResource(R.string.calendar_prev_month))
        }
        IconButton(onClick = onNext) {
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, stringResource(R.string.calendar_next_month))
        }
    }
}

@Composable
private fun WeekdayRow(firstDayOfWeek: DayOfWeek, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(CellSpacing)) {
        repeat(7) { i ->
            val day = firstDayOfWeek.plus(i.toLong())
            Text(
                text = day.getDisplayName(TextStyle.SHORT, Locale.getDefault()).replaceFirstChar { it.titlecase() },
                style = MaterialTheme.typography.labelMedium,
                color = if (day.isWeekend) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun MonthGrid(
    month: YearMonth,
    days: List<LocalDate>,
    byDay: Map<LocalDate, List<Event>>,
    today: LocalDate,
    selected: LocalDate,
    cell: Dp,
    onSelect: (LocalDate) -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    Column(verticalArrangement = Arrangement.spacedBy(CellSpacing)) {
        days.chunked(7).forEach { week ->
            Row(horizontalArrangement = Arrangement.spacedBy(CellSpacing)) {
                week.forEach { date ->
                    DayCell(
                        date = date,
                        events = byDay[date].orEmpty(),
                        inMonth = YearMonth.from(date) == month,
                        isToday = date == today,
                        isSelected = date == selected,
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                            onSelect(date)
                        },
                        modifier = Modifier.size(cell),
                    )
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    events: List<Event>,
    inMonth: Boolean,
    isToday: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    // Фон карточки — surfaceContainerLow, клетки месяца на тон светлее, чтобы квадраты читались
    val background by animateColorAsState(
        when {
            isSelected -> colors.primary
            isToday -> colors.primaryContainer
            inMonth -> colors.surfaceContainerHighest
            else -> Color.Transparent
        },
        label = "day-background",
    )
    val textColor = when {
        isSelected -> colors.onPrimary
        isToday -> colors.onPrimaryContainer
        !inMonth -> colors.outline.copy(alpha = 0.6f)
        date.dayOfWeek.isWeekend -> colors.error
        else -> colors.onSurface
    }

    Box(
        modifier = modifier
            .clip(CellShape)
            .background(background)
            .clickable(onClick = onClick),
    ) {
        Text(
            text = date.dayOfMonth.toString(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Medium,
            color = textColor,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(bottom = 6.dp),
        )
        if (events.isNotEmpty()) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 7.dp),
            ) {
                events.take(MAX_DOTS).forEach { event ->
                    Box(
                        Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(event.color.color().copy(alpha = if (inMonth) 1f else 0.5f))
                            // На выделенном дне точка того же цвета, что фон, пропала бы — обводим
                            .then(if (isSelected) Modifier.border(1.dp, colors.onPrimary, CircleShape) else Modifier),
                    )
                }
            }
        }
    }
}

private val DayOfWeek.isWeekend get() = this == DayOfWeek.SATURDAY || this == DayOfWeek.SUNDAY
