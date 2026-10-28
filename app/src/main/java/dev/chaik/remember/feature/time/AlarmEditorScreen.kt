package dev.chaik.remember.feature.time

import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.FilledTonalButton
import dev.chaik.remember.core.ui.components.AppSlider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.pluralStringResource
import dev.chaik.remember.core.alarm.AlarmSoundPreview
import kotlin.math.roundToInt
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation3.runtime.NavKey
import dev.chaik.remember.R
import dev.chaik.remember.appContainer
import dev.chaik.remember.core.alarm.Alarm
import dev.chaik.remember.core.settings.LocalAppSettings
import dev.chaik.remember.core.settings.weekDays
import dev.chaik.remember.core.ui.components.ScreenScaffold
import dev.chaik.remember.core.ui.components.TimePickerDialog
import dev.chaik.remember.feature.settings.SettingsCard
import dev.chaik.remember.feature.settings.SettingsDropdown
import dev.chaik.remember.feature.settings.SettingsGroupTitle
import dev.chaik.remember.feature.settings.SettingsSwitchRow
import dev.chaik.remember.feature.time.ring.AlarmActivity
import kotlinx.serialization.Serializable
import java.time.LocalTime

/** Будильник; [id] = 0 — новый. */
@Serializable
data class AlarmEditorRoute(val id: Long) : NavKey

/**
 * Настройки одного будильника. Сохраняются при выходе — галочкой, стрелкой, свайпом или «назад»;
 * изменённый будильник при этом включается.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AlarmEditorScreen(route: AlarmEditorRoute, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val container = context.appContainer
    val store = container.timeStore
    // Новому будильнику номер даём сразу: сохранение при выходе может случиться и дважды
    // (например, экран пересоздали при повороте) — второе просто обновит тот же будильник
    val initial = remember(route.id) {
        store.alarm(route.id) ?: LocalTime.now().plusHours(1).let { Alarm(id = store.newAlarmId(), hour = it.hour, minute = 0) }
    }
    var alarm by rememberSaveable(stateSaver = AlarmSaver) { mutableStateOf(initial) }
    var pickingTime by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    var deleted by remember { mutableStateOf(false) }

    val latest by rememberUpdatedState(alarm)
    DisposableEffect(Unit) {
        onDispose {
            if (deleted) return@onDispose
            val saved = store.alarm(latest.id)
            // Новый — сохраняем всегда, старый — только если что-то поменяли
            if (saved == null || saved != latest) container.alarmController.save(latest.copy(enabled = true))
        }
    }

    // Подпись: «назад» прячет клавиатуру — вместе с ней уходит и курсор; тап мимо поля — тоже
    val focusManager = LocalFocusManager.current
    val keyboardVisible = WindowInsets.isImeVisible
    LaunchedEffect(keyboardVisible) { if (!keyboardVisible) focusManager.clearFocus() }

    val preview = remember { AlarmSoundPreview(context) }
    var previewing by remember { mutableStateOf(false) }
    DisposableEffect(preview) {
        preview.onStopped = { previewing = false }
        onDispose { preview.stop() }
    }

    // Мелодия из системного списка
    val systemPicker = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        @Suppress("DEPRECATION")
        val uri = result.data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI) ?: return@rememberLauncherForActivityResult
        val title = RingtoneManager.getRingtone(context, uri)?.getTitle(context)
        alarm = alarm.copy(ringtone = uri.toString(), ringtoneTitle = title)
    }
    // Своя мелодия из файлов: доступ к файлу сохраняется, чтобы будильник мог его прочитать и через месяц
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        val name = context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
        alarm = alarm.copy(ringtone = uri.toString(), ringtoneTitle = name?.substringBeforeLast('.'))
    }

    ScreenScaffold(
        title = stringResource(if (route.id == 0L) R.string.time_new_alarm else R.string.time_edit_alarm),
        modifier = modifier,
        onBack = onBack,
        actions = {
            if (route.id != 0L) {
                IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Outlined.Delete, stringResource(R.string.time_delete_alarm), tint = MaterialTheme.colorScheme.error) }
            }
            IconButton(onClick = onBack) { Icon(Icons.Outlined.Check, stringResource(R.string.time_save)) }
        },
    ) { padding ->
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) { detectTapGestures { focusManager.clearFocus() } }
                .verticalScroll(rememberScrollState())
                .padding(top = padding.calculateTopPadding() + 8.dp, bottom = padding.calculateBottomPadding() + 24.dp)
                .padding(horizontal = 16.dp),
        ) {
            // Время — крупно, тап открывает часы
            Text(
                text = "%02d:%02d".format(alarm.hour, alarm.minute),
                fontSize = 72.sp,
                fontWeight = FontWeight.Light,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { pickingTime = true }
                    .padding(horizontal = 16.dp),
            )

            SettingsGroupTitle(stringResource(R.string.time_repeat))
            SettingsCard {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    weekDays(LocalAppSettings.current.firstDayOfWeek).forEach { day ->
                        val selected = day.value in alarm.days
                        DayToggle(dayName(day), selected) {
                            alarm = alarm.copy(days = if (selected) alarm.days - day.value else alarm.days + day.value)
                        }
                    }
                }
                Text(
                    text = daysText(alarm.days),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                )
            }

            OutlinedTextField(
                value = alarm.label,
                onValueChange = { alarm = alarm.copy(label = it.take(60)) },
                label = { Text(stringResource(R.string.time_label)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            SettingsGroupTitle(stringResource(R.string.time_sound))
            SettingsCard {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Text(stringResource(R.string.time_ringtone), style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = alarm.ringtoneTitle ?: stringResource(R.string.time_ringtone_default),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                        OutlinedButton(onClick = {
                            systemPicker.launch(
                                Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
                                    .putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                                    .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                                    .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, false)
                                    .putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, alarm.ringtone?.let(Uri::parse)),
                            )
                        }) { Text(stringResource(R.string.time_ringtone_system)) }
                        OutlinedButton(onClick = { filePicker.launch(arrayOf("audio/*")) }) { Text(stringResource(R.string.time_ringtone_file)) }
                    }
                    if (alarm.ringtone != null) {
                        TextButton(onClick = { alarm = alarm.copy(ringtone = null, ringtoneTitle = null) }) {
                            Text(stringResource(R.string.time_ringtone_reset))
                        }
                    }
                }
                VolumeCheck(
                    preview = preview,
                    playing = previewing,
                    onToggle = {
                        if (previewing) {
                            preview.stop()
                        } else {
                            preview.play(alarm.ringtone)
                            previewing = preview.playing
                        }
                    },
                )
                SettingsSwitchRow(stringResource(R.string.time_vibrate), alarm.vibrate, { alarm = alarm.copy(vibrate = it) })
                SettingsSwitchRow(
                    title = stringResource(R.string.time_gradual),
                    hint = stringResource(R.string.time_gradual_hint, Alarm.GRADUAL_SECONDS),
                    checked = alarm.gradual,
                    onChange = { alarm = alarm.copy(gradual = it) },
                )
            }

            SettingsGroupTitle(stringResource(R.string.time_wake_up))
            SettingsCard {
                SettingsSwitchRow(
                    title = stringResource(R.string.time_snooze),
                    hint = stringResource(R.string.time_snooze_hint),
                    checked = alarm.snooze,
                    onChange = { alarm = alarm.copy(snooze = it) },
                )
                if (alarm.snooze) {
                    Box(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                        SettingsDropdown(
                            selected = alarm.snoozeMinutes,
                            options = Alarm.SnoozeOptions.map { it to stringResource(R.string.time_snooze_minutes, it) },
                            onSelect = { alarm = alarm.copy(snoozeMinutes = it) },
                        )
                    }
                }
                SettingsSwitchRow(
                    title = stringResource(R.string.time_hard),
                    hint = stringResource(R.string.time_hard_setting_hint),
                    checked = alarm.hard,
                    onChange = { alarm = alarm.copy(hard = it) },
                )
                if (alarm.hard) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 12.dp),
                    ) {
                        Text(stringResource(R.string.time_again_after), style = MaterialTheme.typography.bodyMedium)
                        SettingsDropdown(
                            selected = alarm.againMinutes,
                            options = Alarm.AgainOptions.map { it to pluralStringResource(R.plurals.time_again_minutes, it, it) },
                            onSelect = { alarm = alarm.copy(againMinutes = it) },
                        )
                    }
                    OutlinedButton(
                        onClick = { AlarmActivity.practice(context) },
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                    ) {
                        Icon(Icons.Outlined.Extension, null, modifier = Modifier.size(18.dp))
                        Text(stringResource(R.string.time_try_game), modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        }
    }

    if (pickingTime) {
        TimePickerDialog(
            initial = alarm.time,
            onPick = {
                alarm = alarm.copy(hour = it.hour, minute = it.minute)
                pickingTime = false
            },
            onClear = null,
            onDismiss = { pickingTime = false },
        )
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.time_delete_alarm_q)) },
            confirmButton = {
                TextButton(onClick = {
                    deleted = true
                    container.alarmController.delete(alarm.id)
                    confirmDelete = false
                    onBack()
                }) { Text(stringResource(R.string.time_delete)) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

/** Проверка громкости: мелодия звучит 10 секунд, ползунок — громкость будильника телефона. */
@Composable
private fun VolumeCheck(preview: AlarmSoundPreview, playing: Boolean, onToggle: () -> Unit) {
    var volume by remember { mutableFloatStateOf(preview.volume.toFloat()) }
    Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.AutoMirrored.Outlined.VolumeUp, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            AppSlider(
                value = volume,
                onValueChange = {
                    volume = it
                    preview.volume = it.roundToInt()
                },
                valueRange = preview.minVolume.toFloat()..preview.maxVolume.toFloat(),
                steps = (preview.maxVolume - preview.minVolume - 1).coerceAtLeast(0),
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
            )
        }
        FilledTonalButton(onClick = onToggle) {
            Icon(if (playing) Icons.Outlined.Stop else Icons.Outlined.PlayArrow, null, modifier = Modifier.size(18.dp))
            Text(
                text = stringResource(if (playing) R.string.time_volume_stop else R.string.time_volume_check),
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

@Composable
private fun DayToggle(label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(if (selected) colors.primary else colors.surfaceContainerHighest)
            .clickable(onClick = onClick),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.Bold else null,
            color = if (selected) colors.onPrimary else colors.onSurfaceVariant,
        )
    }
}

/** Будильник в rememberSaveable — через JSON, он и так сериализуемый. */
private val AlarmSaver = androidx.compose.runtime.saveable.Saver<Alarm, String>(
    save = { kotlinx.serialization.json.Json.encodeToString(Alarm.serializer(), it) },
    restore = { kotlinx.serialization.json.Json.decodeFromString(Alarm.serializer(), it) },
)
