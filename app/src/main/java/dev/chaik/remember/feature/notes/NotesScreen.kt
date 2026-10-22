package dev.chaik.remember.feature.notes

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation3.runtime.NavKey
import dev.chaik.remember.R
import dev.chaik.remember.core.ui.components.PlaceholderScreen
import kotlinx.serialization.Serializable

@Serializable
data object NotesRoute : NavKey

@Composable
fun NotesScreen(modifier: Modifier = Modifier) {
    PlaceholderScreen(
        title = stringResource(R.string.tab_notes),
        icon = Icons.Outlined.Description,
        description = stringResource(R.string.placeholder_notes),
        modifier = modifier,
    )
}
