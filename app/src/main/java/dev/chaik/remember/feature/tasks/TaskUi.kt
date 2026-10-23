package dev.chaik.remember.feature.tasks

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chaik.remember.R
import dev.chaik.remember.core.model.TaskPriority
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/** Цвет приоритета — на кружке-галочке; у «без приоритета» — обычный контур. */
@Composable
fun TaskPriority.color(): Color = when (this) {
    TaskPriority.NONE -> MaterialTheme.colorScheme.outline
    TaskPriority.LOW -> Color(0xFF42A5F5)
    TaskPriority.MEDIUM -> Color(0xFFFFA726)
    TaskPriority.HIGH -> Color(0xFFEF5350)
}

@Composable
fun TaskPriority.label(): String = stringResource(
    when (this) {
        TaskPriority.NONE -> R.string.tasks_priority_none
        TaskPriority.LOW -> R.string.tasks_priority_low
        TaskPriority.MEDIUM -> R.string.tasks_priority_medium
        TaskPriority.HIGH -> R.string.tasks_priority_high
    },
)

/** Кружок-галочка цвета приоритета; выполненная — залитая с галочкой. */
@Composable
fun TaskCheckbox(
    done: Boolean,
    priority: TaskPriority,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
) {
    val haptics = LocalHapticFeedback.current
    val color = priority.color()
    val fill by animateColorAsState(if (done) color else Color.Transparent, label = "checkFill")
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size + 16.dp) // палец попадает легче, чем в сам кружок
            .clip(CircleShape)
            .clickable {
                haptics.performHapticFeedback(if (done) HapticFeedbackType.SegmentTick else HapticFeedbackType.Confirm)
                onToggle()
            },
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(fill)
                .border(2.dp, color, CircleShape),
        ) {
            if (done) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = stringResource(R.string.tasks_mark_undone),
                    tint = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.size(size * 0.7f),
                )
            }
        }
    }
}

/** «Сегодня, 18:00», «Завтра», «Вчера», «пн, 27 окт.», «27 окт. 2027 г.». */
@Composable
fun dueLabel(date: LocalDate, time: LocalTime?, today: LocalDate = LocalDate.now()): String {
    val day = when (date) {
        today -> stringResource(R.string.tasks_today)
        today.plusDays(1) -> stringResource(R.string.tasks_tomorrow)
        today.minusDays(1) -> stringResource(R.string.tasks_yesterday)
        else -> date.format(
            DateTimeFormatter.ofPattern(if (date.year == today.year) "EE, d MMM" else "d MMM yyyy"),
        )
    }
    return if (time != null) "$day, ${time.format(DateTimeFormatter.ofPattern("H:mm"))}" else day
}
