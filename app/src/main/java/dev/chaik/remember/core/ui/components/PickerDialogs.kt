package dev.chaik.remember.core.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import dev.chaik.remember.R
import dev.chaik.remember.core.settings.LocalAppSettings
import dev.chaik.remember.core.settings.TimePickerStyle
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

/**
 * Часы для выбора времени (24 часа).
 * [onClear] — убрать время (кнопка слева; null — без неё), [clearLabel] — её подпись.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    initial: LocalTime,
    onPick: (LocalTime) -> Unit,
    onClear: (() -> Unit)?,
    onDismiss: () -> Unit,
    clearLabel: String = stringResource(R.string.action_remove),
) {
    val state = rememberTimePickerState(initialHour = initial.hour, initialMinute = initial.minute, is24Hour = true)
    AlertDialog(
        onDismissRequest = onDismiss,
        text = {
            if (LocalAppSettings.current.timePicker == TimePickerStyle.INPUT) TimeInput(state = state) else TimePicker(state = state)
        },
        confirmButton = {
            TextButton(onClick = { onPick(LocalTime.of(state.hour, state.minute)) }) {
                Text(stringResource(R.string.action_ok))
            }
        },
        dismissButton = onClear?.let { clear ->
            { TextButton(onClick = clear) { Text(clearLabel) } }
        },
    )
}

/** Календарь для выбора срока — любые даты, включая будущие. [onClear] — убрать дату (null — без кнопки). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DueDatePickerDialog(
    initial: LocalDate?,
    onPick: (LocalDate) -> Unit,
    onClear: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    // DatePicker работает с полночью по UTC
    val state = rememberDatePickerState(
        initialSelectedDateMillis = (initial ?: LocalDate.now()).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli(),
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    state.selectedDateMillis?.let { onPick(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) }
                        ?: onDismiss()
                },
            ) { Text(stringResource(R.string.action_ok)) }
        },
        dismissButton = onClear?.let { clear ->
            { TextButton(onClick = clear) { Text(stringResource(R.string.action_remove)) } }
        },
    ) {
        DatePicker(state = state, title = null)
    }
}
