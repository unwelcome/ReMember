package dev.chaik.remember.feature.finance

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation3.runtime.NavKey
import dev.chaik.remember.R
import dev.chaik.remember.core.ui.components.PlaceholderScreen
import dev.chaik.remember.app.navigation.Destination
import kotlinx.serialization.Serializable

@Serializable
data object FinanceRoute : NavKey

@Composable
fun FinanceScreen(
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    PlaceholderScreen(
        title = stringResource(R.string.section_finance),
        icon = Destination.Finance.icon,
        description = stringResource(R.string.section_finance_hint),
        modifier = modifier,
        onBack = onBack,
    )
}
