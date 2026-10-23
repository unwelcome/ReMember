package dev.chaik.remember.feature.settings.appearance

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Wallpaper
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import dev.chaik.remember.R
import dev.chaik.remember.appContainer
import dev.chaik.remember.core.settings.AccentColor
import dev.chaik.remember.core.settings.BackSwipe
import dev.chaik.remember.core.settings.LocalAppSettings
import dev.chaik.remember.core.settings.ThemeMode
import dev.chaik.remember.core.settings.TimePickerStyle
import dev.chaik.remember.core.ui.components.ScreenScaffold
import dev.chaik.remember.core.ui.theme.preview
import dev.chaik.remember.feature.settings.SettingsCard
import dev.chaik.remember.feature.settings.SettingsGroupTitle
import dev.chaik.remember.feature.settings.SettingsRadioRow
import dev.chaik.remember.feature.settings.SettingsSwitchRow
import kotlinx.serialization.Serializable

@Serializable
data object PersonalizationRoute : NavKey

/** Персонализация: тема, акцент, режим левши, свайп «назад», вид выбора времени. Меняется сразу. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PersonalizationScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val store = LocalContext.current.appContainer.settings
    val settings = LocalAppSettings.current

    ScreenScaffold(title = stringResource(R.string.settings_personalization), modifier = modifier, onBack = onBack) { padding ->
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(top = padding.calculateTopPadding() + 8.dp, bottom = padding.calculateBottomPadding() + 24.dp)
                .padding(horizontal = 16.dp),
        ) {
            SettingsGroupTitle(stringResource(R.string.appearance_theme))
            SettingsCard {
                listOf(
                    ThemeMode.SYSTEM to R.string.appearance_theme_system,
                    ThemeMode.LIGHT to R.string.appearance_theme_light,
                    ThemeMode.DARK to R.string.appearance_theme_dark,
                    ThemeMode.AMOLED to R.string.appearance_theme_amoled,
                ).forEach { (mode, label) ->
                    SettingsRadioRow(
                        label = stringResource(label),
                        hint = if (mode == ThemeMode.AMOLED) stringResource(R.string.appearance_theme_amoled_hint) else null,
                        selected = settings.theme == mode,
                        onClick = { store.update { it.copy(theme = mode) } },
                    )
                }
            }

            SettingsGroupTitle(stringResource(R.string.appearance_accent))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                AccentColor.entries.forEach { accent ->
                    AccentSwatch(accent, selected = settings.accent == accent) { store.update { it.copy(accent = accent) } }
                }
            }
            if (settings.accent == AccentColor.WALLPAPER) {
                Text(
                    text = stringResource(R.string.appearance_accent_wallpaper),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }

            SettingsGroupTitle(stringResource(R.string.appearance_hands))
            SettingsCard {
                SettingsSwitchRow(
                    title = stringResource(R.string.appearance_left_handed),
                    hint = stringResource(R.string.appearance_left_handed_hint),
                    checked = settings.leftHanded,
                    onChange = { on -> store.update { it.copy(leftHanded = on) } },
                )
            }

            SettingsGroupTitle(stringResource(R.string.appearance_back_swipe))
            SettingsCard {
                listOf(
                    BackSwipe.RIGHT_TO_LEFT to R.string.appearance_back_rtl,
                    BackSwipe.LEFT_TO_RIGHT to R.string.appearance_back_ltr,
                ).forEach { (direction, label) ->
                    SettingsRadioRow(
                        label = stringResource(label),
                        selected = settings.backSwipe == direction,
                        onClick = { store.update { it.copy(backSwipe = direction) } },
                    )
                }
            }

            SettingsGroupTitle(stringResource(R.string.appearance_time_picker))
            SettingsCard {
                listOf(
                    TimePickerStyle.DIAL to R.string.appearance_time_dial,
                    TimePickerStyle.INPUT to R.string.appearance_time_input,
                ).forEach { (style, label) ->
                    SettingsRadioRow(
                        label = stringResource(label),
                        selected = settings.timePicker == style,
                        onClick = { store.update { it.copy(timePicker = style) } },
                    )
                }
            }
        }
    }
}

/** Кружок цвета; «из обоев» — радужный с картинкой. */
@Composable
private fun AccentSwatch(accent: AccentColor, selected: Boolean, onClick: () -> Unit) {
    val color = accent.preview()
    val background = if (color != null) {
        Modifier.background(color)
    } else {
        Modifier.background(Brush.sweepGradient(listOf(Color(0xFFE5484D), Color(0xFFE0A21B), Color(0xFF3FA34D), Color(0xFF2F80ED), Color(0xFF9A5CE6), Color(0xFFE5484D))))
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .then(background)
            .then(if (selected) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape) else Modifier)
            .clickable(onClick = onClick),
    ) {
        when {
            selected -> Icon(Icons.Outlined.Check, null, tint = Color.White, modifier = Modifier.size(22.dp))
            color == null -> Icon(Icons.Outlined.Wallpaper, null, tint = Color.White, modifier = Modifier.size(20.dp))
        }
    }
}
