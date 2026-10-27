package dev.chaik.remember.core.ui.components

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs

/** Сколько протянуть, чтобы сменился день. */
private val DaySwipeThreshold = 56.dp

/**
 * Свайп вбок по дню (шапка «Сегодня», кольца «Здоровья»): влево — следующий день, вправо — предыдущий.
 * Содержимое едет за пальцем и, сменив день, въезжает с другой стороны. В будущее нельзя —
 * в ту сторону только туго тянется. Жест забирает движение себе, поэтому вкладки под ним не перелистываются.
 */
@Composable
fun daySwipeModifier(canGoForward: Boolean, onSwipe: (days: Long) -> Unit): Modifier {
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val threshold = with(LocalDensity.current) { DaySwipeThreshold.toPx() }
    val canGo by rememberUpdatedState(canGoForward)
    val swipe by rememberUpdatedState(onSwipe)
    var offset by remember { mutableFloatStateOf(0f) }
    var settle by remember { mutableStateOf<Job?>(null) }

    return Modifier
        .pointerInput(Unit) {
            suspend fun animateTo(target: Float, spec: AnimationSpec<Float>) =
                animate(offset, target, animationSpec = spec) { value, _ -> offset = value }

            detectHorizontalDragGestures(
                onDragStart = { settle?.cancel() },
                onDragEnd = {
                    val days = when {
                        offset <= -threshold && canGo -> 1L
                        offset >= threshold -> -1L
                        else -> 0L
                    }
                    settle = scope.launch {
                        if (days == 0L) {
                            animateTo(0f, spring())
                            return@launch
                        }
                        val out = size.width / 3f * if (days > 0) -1 else 1
                        animateTo(out, tween(110))
                        haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                        swipe(days)
                        offset = -out
                        animateTo(0f, spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow))
                    }
                },
                onDragCancel = { settle = scope.launch { animateTo(0f, spring()) } },
            ) { change, dragAmount ->
                change.consume()
                val resist = if (offset + dragAmount < 0 && !canGo) 0.25f else 1f
                offset += dragAmount * resist
            }
        }
        .graphicsLayer {
            translationX = offset
            alpha = 1f - (abs(offset) / size.width * 1.5f).coerceIn(0f, 0.7f)
        }
}
