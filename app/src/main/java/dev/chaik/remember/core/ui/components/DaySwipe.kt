package dev.chaik.remember.core.ui.components

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
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

/** Насколько сейчас сдвинуто то, что едет за пальцем при свайпе дня. */
@Stable
class DaySwipeState internal constructor() {
    var offset by mutableFloatStateOf(0f)
        internal set
}

/**
 * Свайп вбок по дню: влево — следующий день, вправо — предыдущий. Жест ловит [Modifier] из
 * [rememberDaySwipe], а едет за пальцем то, на что надет [daySwipeOffset] — можно ловить свайп
 * по всему блоку, а двигать только дату. Сменив день, сдвинутое въезжает с другой стороны.
 * В будущее нельзя — в ту сторону только туго тянется. Жест забирает движение себе,
 * поэтому вкладки под ним не перелистываются.
 */
@Composable
fun rememberDaySwipe(canGoForward: Boolean, onSwipe: (days: Long) -> Unit): Pair<DaySwipeState, Modifier> {
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val threshold = with(LocalDensity.current) { DaySwipeThreshold.toPx() }
    val canGo by rememberUpdatedState(canGoForward)
    val swipe by rememberUpdatedState(onSwipe)
    val state = remember { DaySwipeState() }
    var settle by remember { mutableStateOf<Job?>(null) }

    val gesture = Modifier.pointerInput(Unit) {
        suspend fun animateTo(target: Float, spec: AnimationSpec<Float>) =
            animate(state.offset, target, animationSpec = spec) { value, _ -> state.offset = value }

        detectHorizontalDragGestures(
            onDragStart = { settle?.cancel() },
            onDragEnd = {
                val days = when {
                    state.offset <= -threshold && canGo -> 1L
                    state.offset >= threshold -> -1L
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
                    state.offset = -out
                    animateTo(0f, spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow))
                }
            },
            onDragCancel = { settle = scope.launch { animateTo(0f, spring()) } },
        ) { change, dragAmount ->
            change.consume()
            val resist = if (state.offset + dragAmount < 0 && !canGo) 0.25f else 1f
            state.offset += dragAmount * resist
        }
    }
    return state to gesture
}

/** Сдвиг за пальцем при свайпе дня: едет вбок и бледнеет. */
fun Modifier.daySwipeOffset(state: DaySwipeState): Modifier = graphicsLayer {
    translationX = state.offset
    alpha = 1f - (abs(state.offset) / size.width * 1.5f).coerceIn(0f, 0.7f)
}

/** Свайп дня, когда ловит и едет одно и то же (шапка «Сегодня»). */
@Composable
fun daySwipeModifier(canGoForward: Boolean, onSwipe: (days: Long) -> Unit): Modifier {
    val (state, gesture) = rememberDaySwipe(canGoForward, onSwipe)
    return gesture.daySwipeOffset(state)
}
