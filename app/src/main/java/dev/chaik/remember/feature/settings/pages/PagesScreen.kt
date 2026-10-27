package dev.chaik.remember.feature.settings.pages

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import dev.chaik.remember.R
import dev.chaik.remember.app.navigation.Destination
import dev.chaik.remember.app.navigation.islandTabs
import dev.chaik.remember.app.navigation.moreTiles
import dev.chaik.remember.app.ui.FloatingIsland
import dev.chaik.remember.appContainer
import dev.chaik.remember.core.settings.LocalAppSettings
import dev.chaik.remember.core.settings.WELCOME_MAX
import dev.chaik.remember.core.settings.WELCOME_MIN
import dev.chaik.remember.core.settings.WELCOME_STEP
import dev.chaik.remember.core.ui.components.ScreenScaffold
import dev.chaik.remember.feature.settings.SettingsCard
import dev.chaik.remember.feature.settings.SettingsGroupTitle
import dev.chaik.remember.feature.settings.SettingsSwitchRow
import kotlinx.serialization.Serializable
import kotlin.math.roundToInt

@Serializable
data object PagesRoute : NavKey

/**
 * Страницы: какие разделы стоят вкладками островка (от 2 до 4, «Ещё» — всегда последней)
 * и в каком порядке плитки на странице «Ещё». Меняется сразу — островок над страницей тоже.
 */
@Composable
fun PagesScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val store = LocalContext.current.appContainer.settings
    val settings = LocalAppSettings.current
    val island = settings.islandTabs()
    val tabs = island - Destination.More
    val tiles = settings.moreTiles() - Destination.Settings
    var adding by remember { mutableStateOf(false) }

    fun saveTabs(list: List<Destination>) = store.update { it.copy(islandTabs = list.map(Destination::name)) }

    // Полный порядок «Ещё» хранится для всех разделов: так убранная вкладка встаёт на своё место
    fun saveTiles(visible: List<Destination>) = store.update { s ->
        val full = (s.moreOrder.mapNotNull { n -> Destination.entries.firstOrNull { it.name == n } } + Destination.DefaultMoreOrder)
            .distinct()
            .filter { it.movable }
        s.copy(moreOrder = (visible + (full - visible.toSet())).map(Destination::name))
    }

    ScreenScaffold(title = stringResource(R.string.settings_pages), modifier = modifier, onBack = onBack) { padding ->
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(top = padding.calculateTopPadding() + 8.dp, bottom = padding.calculateBottomPadding() + 24.dp)
                .padding(horizontal = 16.dp),
        ) {
            // Как будет выглядеть островок
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                FloatingIsland(tabs = island, selected = island.first(), indicatorPosition = { 0f }, onSelect = {})
            }

            SettingsGroupTitle(stringResource(R.string.pages_island))
            SettingsCard {
                tabs.forEachIndexed { i, tab ->
                    OrderRow(
                        destination = tab,
                        onUp = if (i > 0) ({ saveTabs(tabs.swap(i, i - 1)) }) else null,
                        onDown = if (i < tabs.lastIndex) ({ saveTabs(tabs.swap(i, i + 1)) }) else null,
                        // Меньше двух вкладок (трёх с «Ещё») — нельзя
                        onRemove = if (tabs.size > Destination.MIN_TABS) ({ saveTabs(tabs - tab) }) else null,
                    )
                }
                FixedRow(Destination.More)
                if (tabs.size < Destination.MAX_TABS) {
                    TextButton(onClick = { adding = true }, modifier = Modifier.padding(start = 4.dp)) {
                        Icon(Icons.Outlined.Add, null, Modifier.size(18.dp))
                        Text(stringResource(R.string.pages_add_tab), modifier = Modifier.padding(start = 6.dp))
                    }
                }
            }
            Hint(stringResource(R.string.pages_island_hint))

            SettingsGroupTitle(stringResource(R.string.pages_more_tiles))
            SettingsCard {
                tiles.forEachIndexed { i, tile ->
                    OrderRow(
                        destination = tile,
                        onUp = if (i > 0) ({ saveTiles(tiles.swap(i, i - 1)) }) else null,
                        onDown = if (i < tiles.lastIndex) ({ saveTiles(tiles.swap(i, i + 1)) }) else null,
                        onRemove = null,
                    )
                }
                FixedRow(Destination.Settings)
            }

            SettingsGroupTitle(stringResource(R.string.pages_start))
            SettingsCard {
                SettingsSwitchRow(
                    title = stringResource(R.string.pages_welcome),
                    hint = stringResource(R.string.pages_welcome_hint),
                    checked = settings.welcome,
                    onChange = { on -> store.update { it.copy(welcome = on) } },
                )
                if (settings.welcome) {
                    // Ползунок двигается плавно, а сохраняется значение по отпусканию
                    var millis by remember(settings.welcomeMillis) { mutableFloatStateOf(settings.welcomeMillis.toFloat()) }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
                    ) {
                        Text(stringResource(R.string.pages_welcome_duration), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        Text(
                            text = stringResource(R.string.pages_welcome_seconds, millis.roundToInt() / 1000f),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Slider(
                        value = millis,
                        onValueChange = { millis = it },
                        onValueChangeFinished = { store.update { it.copy(welcomeMillis = millis.roundToInt()) } },
                        valueRange = WELCOME_MIN.toFloat()..WELCOME_MAX.toFloat(),
                        steps = (WELCOME_MAX - WELCOME_MIN) / WELCOME_STEP - 1,
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
                    )
                }
            }

            // Настройки самой страницы «Сегодня» — отдельным разделом с её названием
            SettingsGroupTitle(stringResource(R.string.tab_today))
            SettingsCard {
                SettingsSwitchRow(
                    title = stringResource(R.string.pages_today_auto),
                    hint = stringResource(R.string.pages_today_auto_hint),
                    checked = settings.todayAutoMode,
                    onChange = { on -> store.update { it.copy(todayAutoMode = on) } },
                )
            }

            TextButton(
                onClick = { store.update { it.copy(islandTabs = emptyList(), moreOrder = emptyList()) } },
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) {
                Text(stringResource(R.string.pages_reset))
            }
        }
    }

    if (adding) {
        AlertDialog(
            onDismissRequest = { adding = false },
            title = { Text(stringResource(R.string.pages_add_title)) },
            text = {
                Column {
                    tiles.forEach { candidate ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    saveTabs(tabs + candidate)
                                    adding = false
                                }
                                .padding(vertical = 12.dp, horizontal = 4.dp),
                        ) {
                            Icon(candidate.icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(stringResource(candidate.label), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 16.dp))
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { adding = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

/** Раздел со стрелками «выше / ниже» и, если можно, крестиком «убрать из островка». */
@Composable
private fun OrderRow(destination: Destination, onUp: (() -> Unit)?, onDown: (() -> Unit)?, onRemove: (() -> Unit)?) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 16.dp, end = 4.dp)) {
        Icon(destination.icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = stringResource(destination.label),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp),
        )
        IconButton(onClick = { onUp?.invoke() }, enabled = onUp != null) {
            Icon(Icons.Outlined.KeyboardArrowUp, stringResource(R.string.pages_move_up))
        }
        IconButton(onClick = { onDown?.invoke() }, enabled = onDown != null) {
            Icon(Icons.Outlined.KeyboardArrowDown, stringResource(R.string.pages_move_down))
        }
        if (onRemove != null) {
            IconButton(onClick = onRemove) { Icon(Icons.Outlined.Close, stringResource(R.string.pages_remove)) }
        }
    }
}

/** Раздел, место которого не меняется: «Ещё» в островке, «Настройки» в плитках. */
@Composable
private fun FixedRow(destination: Destination) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
    ) {
        Icon(destination.icon, null, tint = MaterialTheme.colorScheme.outline)
        Text(
            text = stringResource(destination.label),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp),
        )
        Icon(Icons.Outlined.Lock, stringResource(R.string.pages_fixed), tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun Hint(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 4.dp),
    )
}

private fun <T> List<T>.swap(a: Int, b: Int): List<T> = toMutableList().also { it[a] = this[b]; it[b] = this[a] }
