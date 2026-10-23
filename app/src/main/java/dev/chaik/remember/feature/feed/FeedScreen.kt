package dev.chaik.remember.feature.feed

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation3.runtime.NavKey
import dev.chaik.remember.R
import dev.chaik.remember.core.ui.components.PlaceholderScreen
import dev.chaik.remember.app.navigation.Destination
import kotlinx.serialization.Serializable

@Serializable
data object FeedRoute : NavKey

@Composable
fun FeedScreen(
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    PlaceholderScreen(
        title = stringResource(R.string.section_feed),
        icon = Destination.Feed.icon,
        description = stringResource(R.string.section_feed_hint),
        modifier = modifier,
        onBack = onBack,
    )
}
