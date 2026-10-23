package dev.chaik.remember.feature.tasks

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.chaik.remember.core.model.Task
import dev.chaik.remember.core.model.TaskRepeat
import java.time.LocalDate

/**
 * Строка задачи: кружок-галочка цвета приоритета, название и мелкие пометки —
 * срок (красный, если просрочен), повтор, подзадачи «1/4», есть ли заметка.
 */
@Composable
fun TaskRow(
    task: Task,
    today: LocalDate,
    onToggle: () -> Unit,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
            .padding(start = 8.dp, end = 16.dp, top = 2.dp, bottom = 2.dp),
    ) {
        TaskCheckbox(done = task.done, priority = task.priority, onToggle = onToggle)
        Column(Modifier.weight(1f).padding(vertical = 8.dp)) {
            Text(
                text = task.title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (task.done) muted else MaterialTheme.colorScheme.onSurface,
                textDecoration = if (task.done) TextDecoration.LineThrough else null,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            val due = task.dueDate
            val hasMeta = due != null || task.repeat != TaskRepeat.None || task.subtasks.isNotEmpty() || task.notes.isNotBlank()
            if (hasMeta && !task.done) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 2.dp),
                ) {
                    if (due != null) {
                        val overdue = due.isBefore(today)
                        Text(
                            text = dueLabel(due, task.dueTime, today),
                            style = MaterialTheme.typography.labelMedium,
                            color = if (overdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        )
                    }
                    if (task.repeat != TaskRepeat.None) Meta(Icons.Outlined.Repeat, null, muted)
                    if (task.subtasks.isNotEmpty()) {
                        Meta(Icons.Outlined.Checklist, "${task.subtasks.count { it.done }}/${task.subtasks.size}", muted)
                    }
                    if (task.notes.isNotBlank()) Meta(Icons.AutoMirrored.Outlined.Notes, null, muted)
                }
            }
        }
    }
}

@Composable
private fun Meta(icon: ImageVector, text: String?, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
        if (text != null) {
            Text(text, style = MaterialTheme.typography.labelMedium, color = color, modifier = Modifier.padding(start = 2.dp))
        }
    }
}
