package dev.chaik.remember.feature.today.day

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.chaik.remember.R
import dev.chaik.remember.core.model.Rating
import dev.chaik.remember.core.model.Tag
import dev.chaik.remember.core.ui.components.StarRating
import dev.chaik.remember.core.ui.components.StarColor

/** Оценка дня с подписью «9.5 / 10» и кнопкой сброса. */
@Composable
fun DayRating(controller: DayController, modifier: Modifier = Modifier) {
    val mood = controller.mood
    Column(modifier) {
        // Высота строки постоянная: появление «Очистить» не должно сдвигать звёзды под пальцем
        Row(Modifier.height(32.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.day_rating),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = when {
                    mood == null -> stringResource(R.string.day_rating_empty)
                    Rating.isSecret(mood) -> stringResource(R.string.day_rating_secret)
                    else -> stringResource(R.string.day_rating_value, Rating.format(mood))
                },
                style = MaterialTheme.typography.titleSmall,
                fontWeight = if (mood != null) FontWeight.Bold else null,
                color = if (mood != null) StarColor else MaterialTheme.colorScheme.outline,
            )
            if (mood != null) {
                Text(
                    text = stringResource(R.string.action_clear),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = controller::clearMood)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
        }
        StarRating(
            halves = mood,
            onChange = controller::setMood,
            secret = true,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

/** Теги дня: чипы с крестиком и кнопка «+ Тег». */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DayTags(controller: DayController, modifier: Modifier = Modifier) {
    var dialogOpen by remember { mutableStateOf(false) }

    FlowRow(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        controller.tags.forEach { tag ->
            InputChip(
                selected = false,
                onClick = { controller.removeTag(tag) },
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
        val allTags by controller.allTags.collectAsStateWithLifecycle()
        TagPickerDialog(
            allTags = allTags,
            selected = controller.tags,
            onToggle = controller::toggleTag,
            onCreate = controller::addTag,
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
