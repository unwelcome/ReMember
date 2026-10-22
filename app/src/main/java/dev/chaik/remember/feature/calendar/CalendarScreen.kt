package dev.chaik.remember.feature.calendar

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation3.runtime.NavKey
import dev.chaik.remember.R
import dev.chaik.remember.core.ui.components.PlaceholderScreen
import kotlinx.serialization.Serializable

@Serializable
data object CalendarRoute : NavKey

@Composable
fun CalendarScreen(modifier: Modifier = Modifier) {
    PlaceholderScreen(
        title = stringResource(R.string.tab_calendar),
        icon = Icons.Outlined.CalendarMonth,
        description = stringResource(R.string.placeholder_calendar),
        modifier = modifier,
    )
}
