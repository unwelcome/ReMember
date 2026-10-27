package dev.chaik.remember.feature.health

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

/** Цвета колец: шаги, активность, калории — снаружи внутрь. */
val StepsColor = Color(0xFF30D158)
val MinutesColor = Color(0xFF0A84FF)
val CaloriesColor = Color(0xFFBF5AF2)

/** Больше стольких кругов не рисуем — дальше всё равно сплошное кольцо. */
private const val MAX_LAPS = 20f

/** Насколько темнее хвост кольца, когда оно пошло на второй круг. */
private const val TAIL_DARKEN = 0.25f

/**
 * Три кольца прогресса к целям: снаружи шаги, потом активность, внутри калории.
 * [progress] — доли от цели (1 = цель выполнена). Сверх цели кольцо идёт дальше по кругу
 * поверх себя: от тёмного хвоста к яркой голове, а под головой — тень, чтобы было видно, где она.
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
            val v by animateFloatAsState(p.coerceIn(0f, MAX_LAPS), tween(durationMillis = 700), label = "ring")
            v
        } else {
            p.coerceIn(0f, MAX_LAPS)
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
            val value = values.getOrElse(i) { 0f }
            if (value < 1f) drawArc(color.copy(alpha = 0.2f), 0f, 360f, false, topLeft, arcSize, style = Stroke(width))
            if (value > 0f) drawProgressRing(color, value, topLeft, arcSize, width)
        }
    }
}

/**
 * Дуга прогресса: цвет — от тёмного хвоста к яркой голове, голова — круглый конец с тенью впереди.
 * До цели хвост в начале круга и темнеет тем сильнее, чем длиннее дуга (полный круг — как у
 * переполненного). Сверх цели закрашен весь круг, а самый тёмный кусок — сразу за головой:
 * это самая старая часть последнего круга.
 */
private fun DrawScope.drawProgressRing(color: Color, value: Float, topLeft: Offset, arcSize: Size, width: Float) {
    val center = topLeft + Offset(arcSize.width / 2, arcSize.height / 2)
    val radius = arcSize.width / 2
    val overflow = value > 1f
    val headDegrees = -90f + 360f * (if (overflow) value % 1f else value)
    val tail = lerp(color, Color.Black, TAIL_DARKEN * value.coerceAtMost(1f))

    fun pointAt(degrees: Float): Offset {
        val radians = Math.toRadians(degrees.toDouble())
        return center + Offset((radius * cos(radians)).toFloat(), (radius * sin(radians)).toFloat())
    }

    // Градиент по кругу начинается в 0° — поворачиваем так, чтобы 0° пришёлся на хвост
    val startDegrees = if (overflow) headDegrees else -90f
    val sweep = if (overflow) 360f else 360f * value
    rotate(startDegrees, pivot = center) {
        drawArc(
            brush = Brush.sweepGradient(0f to tail, sweep / 360f to color, center = center),
            startAngle = 0f,
            sweepAngle = sweep,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            // Концы — кружками отдельно: круглый край линии взял бы цвет с другого конца градиента
            style = Stroke(width),
        )
    }
    // Хвост до цели — скруглённый, своего тёмного цвета
    if (!overflow) drawCircle(tail, radius = width / 2, center = pointAt(-90f))

    // Тень чуть впереди головы — голова будто лежит поверх кольца
    val shadowShift = Math.toDegrees((width * 0.18f / radius).toDouble()).toFloat()
    drawCircle(Color.Black.copy(alpha = 0.35f), radius = width / 2, center = pointAt(headDegrees + shadowShift))
    drawCircle(Color.Black.copy(alpha = 0.2f), radius = width / 2, center = pointAt(headDegrees + shadowShift * 2))
    // Сама голова: тот же яркий цвет, что и конец градиента
    drawCircle(color, radius = width / 2, center = pointAt(headDegrees))
}
