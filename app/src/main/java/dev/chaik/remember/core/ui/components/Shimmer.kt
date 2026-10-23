package dev.chaik.remember.core.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush

private const val SHIMMER_MILLIS = 1_200

/**
 * Заглушка «грузится»: тон карточки и бегущий по диагонали блик.
 * Для скелетов карточек и места под ещё не загруженное фото.
 */
fun Modifier.shimmer(): Modifier = composed {
    val base = MaterialTheme.colorScheme.surfaceContainerHigh
    val highlight = MaterialTheme.colorScheme.surfaceContainerHighest
    val progress by rememberInfiniteTransition(label = "shimmer").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(SHIMMER_MILLIS, easing = LinearEasing), RepeatMode.Restart),
        label = "shimmerProgress",
    )
    background(base).drawWithContent {
        drawContent()
        // Блик шире самого элемента: проходит от левого верхнего угла за правый нижний
        val span = size.width + size.height
        val start = -span + 2 * span * progress
        drawRect(
            Brush.linearGradient(
                colors = listOf(base, highlight, base),
                start = Offset(start, start * size.height / span),
                end = Offset(start + span / 2, (start + span / 2) * size.height / span),
            ),
        )
    }
}
