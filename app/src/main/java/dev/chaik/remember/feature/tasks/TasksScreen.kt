package dev.chaik.remember.feature.tasks

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation3.runtime.NavKey
import dev.chaik.remember.R
import dev.chaik.remember.core.ui.components.PlaceholderScreen
import kotlinx.serialization.Serializable

@Serializable
data object TasksRoute : NavKey

@Composable
fun TasksScreen(modifier: Modifier = Modifier) {
    PlaceholderScreen(
        title = stringResource(R.string.tab_tasks),
        icon = Icons.Outlined.TaskAlt,
        description = stringResource(R.string.placeholder_tasks),
        modifier = modifier,
    )
}
