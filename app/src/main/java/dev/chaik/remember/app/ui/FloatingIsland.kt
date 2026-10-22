package dev.chaik.remember.app.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import dev.chaik.remember.app.navigation.TopLevelTab
import kotlin.math.roundToInt

private val ItemWidth = 60.dp
private val ItemHeight = 48.dp
private val IslandPadding = 6.dp

/** Высота островка вместе с внутренними отступами — нужна для отступа контента. */
val IslandHeight = ItemHeight + IslandPadding * 2

/**
 * Плавающая панель вкладок: только иконки. Подсветка выбранной вкладки
 * движется вместе со свайпом между страницами.
 *
 * [indicatorPosition] — дробный индекс вкладки (1.5 = между второй и третьей);
 * читается только при отрисовке, поэтому свайп не вызывает перекомпозицию.
 */
@Composable
fun FloatingIsland(
    tabs: List<TopLevelTab>,
    selected: TopLevelTab,
    indicatorPosition: () -> Float,
    onSelect: (TopLevelTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme

    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = colors.surfaceContainerHigh.copy(alpha = 0.95f),
        shadowElevation = 8.dp,
        tonalElevation = 2.dp,
    ) {
        Box(Modifier.padding(IslandPadding)) {
            Box(
                Modifier
                    .offset { IntOffset((ItemWidth.toPx() * indicatorPosition()).roundToInt(), 0) }
                    .size(ItemWidth, ItemHeight)
                    .clip(CircleShape)
                    .background(colors.secondaryContainer),
            )
            Row {
                tabs.forEach { tab ->
                    IslandItem(
                        tab = tab,
                        selected = tab == selected,
                        onClick = { onSelect(tab) },
                    )
                }
            }
        }
    }
}

@Composable
private fun IslandItem(
    tab: TopLevelTab,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val tint by animateColorAsState(
        targetValue = if (selected) colors.onSecondaryContainer else colors.onSurfaceVariant,
        label = "tint",
    )
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.1f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "scale",
    )

    Box(
        modifier = Modifier
            .size(ItemWidth, ItemHeight)
            .clip(CircleShape)
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.Tab,
                interactionSource = null,
                indication = ripple(bounded = true),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = if (selected) tab.selectedIcon else tab.icon,
            contentDescription = stringResource(tab.label),
            tint = tint,
            modifier = Modifier.graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        )
    }
}
