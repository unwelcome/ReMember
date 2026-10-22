package dev.chaik.remember.feature.search

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation3.runtime.NavKey
import dev.chaik.remember.R
import dev.chaik.remember.core.ui.components.PlaceholderScreen
import dev.chaik.remember.feature.more.MoreSection
import kotlinx.serialization.Serializable

@Serializable
data object SearchRoute : NavKey

@Composable
fun SearchScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PlaceholderScreen(
        title = stringResource(R.string.section_search),
        icon = MoreSection.Search.icon,
        description = stringResource(R.string.section_search_hint),
        modifier = modifier,
        onBack = onBack,
    )
}
