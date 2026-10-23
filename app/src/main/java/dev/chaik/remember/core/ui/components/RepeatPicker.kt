package dev.chaik.remember.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.chaik.remember.R
import dev.chaik.remember.core.model.Repeat
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

private val RepeatOptions = listOf(
    Repeat.None to R.string.repeat_none,
    Repeat.Daily to R.string.repeat_daily,
    Repeat.Weekly to R.string.repeat_weekly,
    Repeat.Monthly to R.string.repeat_monthly,
    Repeat.Yearly to R.string.repeat_yearly,
)

/** Подпись повтора: «Каждый месяц», «По дням недели»… */
@Composable
fun Repeat.label(): String = when (this) {
    is Repeat.Weekdays -> stringResource(R.string.repeat_weekdays)
    else -> stringResource(RepeatOptions.first { it.first == this }.second)
}

/**
 * Выбор повтора: список вариантов, а для «по дням недели» — дни чипами.
 * [defaultDay] — какой день отметить, когда выбрали «по дням недели».
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RepeatPicker(
    repeat: Repeat,
    onChange: (Repeat) -> Unit,
    defaultDay: () -> DayOfWeek,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier) {
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
            OutlinedTextField(
                value = repeat.label(),
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                RepeatOptions.forEach { (option, label) ->
                    DropdownMenuItem(
                        text = { Text(stringResource(label)) },
                        onClick = {
                            onChange(option)
                            expanded = false
                        },
                    )
                }
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.repeat_weekdays)) },
                    onClick = {
                        onChange(Repeat.Weekdays(setOf(defaultDay())))
                        expanded = false
                    },
                )
            }
        }
        if (repeat is Repeat.Weekdays) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 8.dp)) {
                DayOfWeek.entries.forEach { day ->
                    val selected = day in repeat.days
                    FilterChip(
                        selected = selected,
                        onClick = {
                            val days = if (selected) repeat.days - day else repeat.days + day
                            // Совсем без дней повтор не имеет смысла — последний день не снимается
                            if (days.isNotEmpty()) onChange(Repeat.Weekdays(days))
                        },
                        label = { Text(day.getDisplayName(TextStyle.SHORT, Locale.getDefault()).replaceFirstChar { it.titlecase() }) },
                    )
                }
            }
        }
    }
}
