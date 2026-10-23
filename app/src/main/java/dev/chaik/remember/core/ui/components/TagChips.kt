package dev.chaik.remember.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import dev.chaik.remember.R
import dev.chaik.remember.core.model.Tag

/**
 * Теги записи: чипы с крестиком и кнопка «+ Тег», открывающая выбор тегов.
 * Используется и в записи дня, и в заметках.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TagChips(
    tags: List<Tag>,
    allTags: List<Tag>,
    onRemove: (Tag) -> Unit,
    onToggle: (Tag) -> Unit,
    onCreate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var dialogOpen by remember { mutableStateOf(false) }

    FlowRow(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        tags.forEach { tag ->
            InputChip(
                selected = false,
                onClick = { onRemove(tag) },
                label = { Text(tag.name) },
                trailingIcon = {
                    Icon(Icons.Outlined.Close, stringResource(R.string.tags_remove, tag.name), Modifier.size(16.dp))
                },
            )
        }
        AssistChip(
            onClick = { dialogOpen = true },
            label = { Text(stringResource(R.string.tags_add)) },
            leadingIcon = { Icon(Icons.Outlined.Add, null, Modifier.size(18.dp)) },
        )
    }

    if (dialogOpen) {
        TagPickerDialog(
            allTags = allTags,
            selected = tags,
            onToggle = onToggle,
            onCreate = onCreate,
            onDismiss = { dialogOpen = false },
        )
    }
}

/** Выбор тегов: существующие — переключателями, новый — полем ввода. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagPickerDialog(
    allTags: List<Tag>,
    selected: List<Tag>,
    onToggle: (Tag) -> Unit,
    onCreate: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    val create = {
        onCreate(name)
        name = ""
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.tags_dialog_title)) },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.tags_new_hint)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { create() }),
                    trailingIcon = {
                        TextButton(onClick = create, enabled = name.isNotBlank()) {
                            Text(stringResource(R.string.tags_create))
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    stringResource(R.string.tags_dialog_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                // Подсказки: подходящие по введённому тексту
                val query = name.trim().lowercase()
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    allTags.filter { query.isEmpty() || it.name.lowercase().contains(query) }.forEach { tag ->
                        val isSelected = selected.any { it.id == tag.id }
                        // Отмеченный тег должен заметно отличаться: галочка и цвет
                        FilterChip(
                            selected = isSelected,
                            onClick = { onToggle(tag) },
                            label = { Text(tag.name) },
                            leadingIcon = if (isSelected) {
                                { Icon(Icons.Outlined.Check, null, Modifier.size(18.dp)) }
                            } else {
                                null
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_ok)) }
        },
    )
}
