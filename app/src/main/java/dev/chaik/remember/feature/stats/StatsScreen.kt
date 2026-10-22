package dev.chaik.remember.feature.stats

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation3.runtime.NavKey
import dev.chaik.remember.R
import dev.chaik.remember.core.ui.components.PlaceholderScreen
import dev.chaik.remember.feature.more.MoreSection
import kotlinx.serialization.Serializable

@Serializable
data object StatsRoute : NavKey

@Composable
fun StatsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PlaceholderScreen(
        title = stringResource(R.string.section_stats),
        icon = MoreSection.Stats.icon,
        description = stringResource(R.string.section_stats_hint),
        modifier = modifier,
        onBack = onBack,
    )
}
