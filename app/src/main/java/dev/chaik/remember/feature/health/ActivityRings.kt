package dev.chaik.remember.feature.health

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Цвета колец: шаги, активность, калории — снаружи внутрь. */
val StepsColor = Color(0xFF30D158)
val MinutesColor = Color(0xFF0A84FF)
val CaloriesColor = Color(0xFFBF5AF2)

/**
 * Три кольца прогресса к целям: снаружи шаги, потом активность, внутри калории.
 * [progress] — доли от цели (1 = цель выполнена; больше единицы кольцо не рисуется).
 */
@Composable
fun ActivityRings(
    progress: List<Float>,
    modifier: Modifier = Modifier,
    stroke: Dp = 22.dp,
    gap: Dp = 6.dp,
    animate: Boolean = true,
) {
    val colors = listOf(StepsColor, MinutesColor, CaloriesColor)
    val values = progress.map { p ->
        if (animate) {
            val v by animateFloatAsState(p.coerceIn(0f, 1f), tween(durationMillis = 700), label = "ring")
            v
        } else {
            p.coerceIn(0f, 1f)
        }
    }
    Canvas(modifier) {
        val width = stroke.toPx()
        val step = width + gap.toPx()
        colors.forEachIndexed { i, color ->
            val inset = width / 2 + step * i
            val topLeft = Offset(inset, inset)
            val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
            if (arcSize.width <= 0f) return@forEachIndexed
            drawArc(color.copy(alpha = 0.2f), 0f, 360f, false, topLeft, arcSize, style = Stroke(width))
            val sweep = 360f * values.getOrElse(i) { 0f }
            if (sweep > 0f) {
                drawArc(color, -90f, sweep, false, topLeft, arcSize, style = Stroke(width, cap = StrokeCap.Round))
            }
        }
    }
}
