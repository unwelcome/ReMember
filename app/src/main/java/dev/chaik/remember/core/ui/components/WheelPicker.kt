package dev.chaik.remember.core.ui.components

import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlin.math.abs

/**
 * Барабан чисел [range]: листается пальцем и останавливается ровно на числе.
 * Выбранное — по центру, крупнее; соседние бледнеют. [onChange] — при каждом новом числе по центру.
 */
@Composable
fun WheelPicker(
    value: Int,
    range: IntRange,
    onChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    itemHeight: Dp = 56.dp,
    visible: Int = 3,
) {
    val count = range.last - range.first + 1
    val state = rememberLazyListState(initialFirstVisibleItemIndex = (value - range.first).coerceIn(0, count - 1))
    val itemPx = with(LocalDensity.current) { itemHeight.toPx() }
    // Какое число сейчас по центру: сверху отступ в (visible / 2) строк, поэтому центр — первая видимая
    val centered by remember {
        derivedStateOf {
            val index = state.firstVisibleItemIndex + if (state.firstVisibleItemScrollOffset > itemPx / 2) 1 else 0
            index.coerceIn(0, count - 1)
        }
    }
    val haptics = LocalHapticFeedback.current
    val change by rememberUpdatedState(onChange)
    LaunchedEffect(state) {
        snapshotFlow { centered }.distinctUntilChanged().collect {
            haptics.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
            change(range.first + it)
        }
    }
    // Значение поменяли снаружи (сброс таймера) — прокрутить к нему
    LaunchedEffect(value) {
        val index = (value - range.first).coerceIn(0, count - 1)
        if (index != centered && !state.isScrollInProgress) state.scrollToItem(index)
    }

    Box(modifier.height(itemHeight * visible), contentAlignment = Alignment.Center) {
        LazyColumn(
            state = state,
            flingBehavior = rememberSnapFlingBehavior(state),
            contentPadding = PaddingValues(vertical = itemHeight * (visible / 2)),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth(),
        ) {
            items(count) { index ->
                Box(contentAlignment = Alignment.Center, modifier = Modifier.height(itemHeight).fillMaxWidth()) {
                    // Чем дальше от центра, тем бледнее и мельче
                    val distance = remember(index) {
                        derivedStateOf {
                            val info = state.layoutInfo.visibleItemsInfo.firstOrNull { it.index == index } ?: return@derivedStateOf 1f
                            // Отступы сверху и снизу входят в окно списка: его начало — отрицательное
                            val center = (state.layoutInfo.viewportStartOffset + state.layoutInfo.viewportEndOffset) / 2f
                            (abs(info.offset + info.size / 2f - center) / itemPx).coerceIn(0f, 1f)
                        }
                    }
                    Text(
                        text = "%02d".format(range.first + index),
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Light,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.graphicsLayer {
                            val d = distance.value
                            alpha = 1f - 0.7f * d
                            scaleX = 1f - 0.25f * d
                            scaleY = 1f - 0.25f * d
                        },
                    )
                }
            }
        }
    }
}
