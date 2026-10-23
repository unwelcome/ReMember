package dev.chaik.remember.feature.tasks.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import dev.chaik.remember.R
import dev.chaik.remember.appContainer
import dev.chaik.remember.core.model.TaskPriority
import dev.chaik.remember.core.model.TaskRepeat
import dev.chaik.remember.core.ui.components.DueDatePickerDialog
import dev.chaik.remember.core.ui.components.ScreenScaffold
import dev.chaik.remember.core.ui.components.TimePickerDialog
import dev.chaik.remember.feature.notes.ConfirmDialog
import dev.chaik.remember.feature.tasks.TaskCheckbox
import dev.chaik.remember.feature.tasks.color
import dev.chaik.remember.feature.tasks.dueLabel
import dev.chaik.remember.feature.tasks.label
import kotlinx.serialization.Serializable
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.TextStyle as JavaTextStyle
import java.util.Locale

@Serializable
data class TaskEditorRoute(val taskId: Long) : NavKey

@Composable
fun TaskEditorScreen(
    route: TaskEditorRoute,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val container = LocalContext.current.appContainer
    val viewModel = viewModel { TaskEditorViewModel(container, route.taskId) }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { viewModel.flush() }

    var pickingDate by remember { mutableStateOf(false) }
    var pickingTime by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    ScreenScaffold(
        title = stringResource(R.string.tab_tasks),
        modifier = modifier,
        onBack = onBack,
        actions = {
            IconButton(onClick = { confirmDelete = true }) {
                Icon(Icons.Outlined.Delete, stringResource(R.string.notes_delete))
            }
        },
    ) { padding ->
        if (!viewModel.isLoaded) return@ScreenScaffold
        Column(
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding() + 24.dp)
                .padding(horizontal = 16.dp),
        ) {
            TitleRow(viewModel)
            DueSection(viewModel, onPickDate = { pickingDate = true }, onPickTime = { pickingTime = true })
            PrioritySection(viewModel)
            RepeatSection(viewModel)
            SubtasksSection(viewModel)
            OutlinedTextField(
                value = viewModel.notes,
                onValueChange = { viewModel.notes = it },
                label = { Text(stringResource(R.string.tasks_notes)) },
                minLines = 3,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    if (pickingDate) {
        DueDatePickerDialog(
            initial = viewModel.dueDate,
            onPick = {
                viewModel.dueDate = it
                pickingDate = false
            },
            onClear = {
                viewModel.dueDate = null
                viewModel.dueTime = null
                pickingDate = false
            },
            onDismiss = { pickingDate = false },
        )
    }
    if (pickingTime) {
        TimePickerDialog(
            initial = viewModel.dueTime ?: LocalTime.of(9, 0),
            onPick = {
                viewModel.dueTime = it
                pickingTime = false
            },
            onClear = {
                viewModel.dueTime = null
                pickingTime = false
            },
            onDismiss = { pickingTime = false },
        )
    }
    if (confirmDelete) {
        ConfirmDialog(
            title = stringResource(R.string.tasks_delete_title),
            text = stringResource(R.string.notes_delete_text),
            confirmLabel = stringResource(R.string.notes_delete),
            onConfirm = { viewModel.delete(onBack) },
            onDismiss = { confirmDelete = false },
        )
    }
}

@Composable
private fun TitleRow(viewModel: TaskEditorViewModel) {
    val style = MaterialTheme.typography.titleLarge.copy(
        color = if (viewModel.done) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
        fontWeight = FontWeight.Bold,
        textDecoration = if (viewModel.done) TextDecoration.LineThrough else null,
    )
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
        TaskCheckbox(done = viewModel.done, priority = viewModel.priority, onToggle = viewModel::toggleDone, size = 28.dp)
        PlainField(
            value = viewModel.title,
            onChange = { viewModel.title = it },
            hint = stringResource(R.string.tasks_title_hint),
            style = style,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun DueSection(viewModel: TaskEditorViewModel, onPickDate: () -> Unit, onPickTime: () -> Unit) {
    Section(stringResource(R.string.tasks_due)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val date = viewModel.dueDate
            FilterChip(
                selected = date != null,
                onClick = onPickDate,
                label = { Text(if (date != null) dueLabel(date, null) else stringResource(R.string.tasks_no_due)) },
                leadingIcon = { Icon(Icons.Outlined.CalendarMonth, null, Modifier.size(18.dp)) },
            )
            if (date != null) {
                val time = viewModel.dueTime
                FilterChip(
                    selected = time != null,
                    onClick = onPickTime,
                    label = { Text(time?.toString() ?: stringResource(R.string.tasks_no_time)) },
                    leadingIcon = { Icon(Icons.Outlined.Schedule, null, Modifier.size(18.dp)) },
                )
            }
        }
    }
}

@Composable
private fun PrioritySection(viewModel: TaskEditorViewModel) {
    Section(stringResource(R.string.tasks_priority)) {
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            TaskPriority.entries.forEachIndexed { i, priority ->
                SegmentedButton(
                    selected = viewModel.priority == priority,
                    onClick = { viewModel.priority = priority },
                    shape = SegmentedButtonDefaults.itemShape(i, TaskPriority.entries.size),
                    icon = {
                        // requiredSize: слот иконки растягивает по высоте — кружок становился овалом
                        Box(
                            Modifier
                                .requiredSize(10.dp)
                                .clip(CircleShape)
                                .background(priority.color()),
                        )
                    },
                    label = { Text(priority.label(), style = MaterialTheme.typography.labelSmall, maxLines = 1) },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun RepeatSection(viewModel: TaskEditorViewModel) {
    var expanded by remember { mutableStateOf(false) }
    val options = listOf(
        TaskRepeat.None to R.string.tasks_repeat_none,
        TaskRepeat.Daily to R.string.tasks_repeat_daily,
        TaskRepeat.Weekly to R.string.tasks_repeat_weekly,
        TaskRepeat.Monthly to R.string.tasks_repeat_monthly,
        TaskRepeat.Yearly to R.string.tasks_repeat_yearly,
    )
    val repeat = viewModel.repeat
    val currentLabel = when (repeat) {
        is TaskRepeat.Weekdays -> stringResource(R.string.tasks_repeat_weekdays)
        else -> stringResource(options.first { it.first == repeat }.second)
    }

    Section(stringResource(R.string.tasks_repeat)) {
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
            OutlinedTextField(
                value = currentLabel,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { (option, label) ->
                    DropdownMenuItem(
                        text = { Text(stringResource(label)) },
                        onClick = {
                            viewModel.repeat = option
                            expanded = false
                        },
                    )
                }
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.tasks_repeat_weekdays)) },
                    onClick = {
                        // По умолчанию — день недели срока (или сегодняшний)
                        val day = (viewModel.dueDate ?: LocalDate.now()).dayOfWeek
                        viewModel.repeat = TaskRepeat.Weekdays(setOf(day))
                        expanded = false
                    },
                )
            }
        }
        if (repeat is TaskRepeat.Weekdays) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 8.dp)) {
                DayOfWeek.entries.forEach { day ->
                    val selected = day in repeat.days
                    FilterChip(
                        selected = selected,
                        onClick = {
                            val days = if (selected) repeat.days - day else repeat.days + day
                            // Совсем без дней повтор не имеет смысла — последний день не снимается
                            if (days.isNotEmpty()) viewModel.repeat = TaskRepeat.Weekdays(days)
                        },
                        label = { Text(day.getDisplayName(JavaTextStyle.SHORT, Locale.getDefault()).replaceFirstChar { it.titlecase() }) },
                    )
                }
            }
        }
    }
}

@Composable
private fun SubtasksSection(viewModel: TaskEditorViewModel) {
    Section(stringResource(R.string.tasks_subtasks)) {
        viewModel.subtasks.forEach { subtask ->
            val focus = remember { FocusRequester() }
            LaunchedEffect(viewModel.focusSubtaskKey) {
                if (viewModel.focusSubtaskKey == subtask.key) {
                    focus.requestFocus()
                    viewModel.focusSubtaskKey = null
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                TaskCheckbox(
                    done = subtask.done,
                    priority = TaskPriority.NONE,
                    onToggle = { subtask.done = !subtask.done },
                    size = 20.dp,
                )
                PlainField(
                    value = subtask.title,
                    onChange = { subtask.title = it },
                    hint = stringResource(R.string.tasks_subtask_hint),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = if (subtask.done) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                        textDecoration = if (subtask.done) TextDecoration.LineThrough else null,
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focus),
                )
                IconButton(onClick = { viewModel.removeSubtask(subtask) }) {
                    Icon(Icons.Outlined.Close, stringResource(R.string.tasks_remove_subtask), tint = MaterialTheme.colorScheme.outline)
                }
            }
        }
        TextButton(onClick = viewModel::addSubtask) {
            Icon(Icons.Outlined.Add, null, Modifier.size(18.dp))
            Text(stringResource(R.string.tasks_add_subtask), modifier = Modifier.padding(start = 4.dp))
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 6.dp),
        )
        content()
    }
}

/** Поле без рамки с подсказкой. */
@Composable
private fun PlainField(
    value: String,
    onChange: (String) -> Unit,
    hint: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
) {
    BasicTextField(
        value = value,
        onValueChange = onChange,
        textStyle = style,
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        modifier = modifier.padding(vertical = 8.dp),
        decorationBox = { inner ->
            Box {
                if (value.isEmpty()) Text(hint, style = style.copy(color = MaterialTheme.colorScheme.outline, textDecoration = null))
                inner()
            }
        },
    )
}
