package dev.chaik.remember.feature.health

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation3.runtime.NavKey
import dev.chaik.remember.R
import dev.chaik.remember.core.ui.components.PlaceholderScreen
import dev.chaik.remember.feature.more.MoreSection
import kotlinx.serialization.Serializable

@Serializable
data object HealthRoute : NavKey

@Composable
fun HealthScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PlaceholderScreen(
        title = stringResource(R.string.section_health),
        icon = MoreSection.Health.icon,
        description = stringResource(R.string.section_health_hint),
        modifier = modifier,
        onBack = onBack,
    )
}
