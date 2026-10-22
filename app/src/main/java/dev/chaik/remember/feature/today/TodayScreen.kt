package dev.chaik.remember.feature.today

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation3.runtime.NavKey
import dev.chaik.remember.R
import dev.chaik.remember.core.ui.components.PlaceholderScreen
import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Serializable
data object TodayRoute : NavKey

@Composable
fun TodayScreen(modifier: Modifier = Modifier) {
    // «Пятница, 23 октября» — на языке телефона
    val title = remember {
        LocalDate.now()
            .format(DateTimeFormatter.ofPattern("EEEE, d MMMM"))
            .replaceFirstChar { it.titlecase() }
    }
    PlaceholderScreen(
        title = title,
        icon = Icons.Outlined.WbSunny,
        description = stringResource(R.string.placeholder_today),
        modifier = modifier,
    )
}
