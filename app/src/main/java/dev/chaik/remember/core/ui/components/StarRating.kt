package dev.chaik.remember.core.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.StarHalf
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chaik.remember.core.model.Rating

/** Цвет заполненных звёзд. */
val StarColor = Color(0xFFFFC107)

/** Сколько надо тапнуть по последней звезде при максимуме, чтобы открыть секрет. */
private const val SECRET_TAPS = 3
private const val SECRET_TAP_WINDOW_MS = 700L

/**
 * Оценка звёздами. Тап по левой половине звезды — половинка, по правой — целая.
 *
 * Значение — в половинках звезды ([Rating]): 19 из [stars] = 10 → 9.5 звезды.
 * Если [secret] = true: при максимуме три быстрых тапа по последней звезде
 * дают [Rating.SECRET] (11 из 10) — звёзды переливаются радугой.
 */
@Composable
fun StarRating(
    halves: Int?,
    onChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    stars: Int = Rating.DAY_STARS,
    allowHalf: Boolean = true,
    secret: Boolean = false,
    starSize: Dp = 28.dp,
) {
    val haptics = LocalHapticFeedback.current
    val max = stars * 2
    val value = halves ?: 0
    val isSecret = secret && Rating.isSecret(halves)
    val current by rememberUpdatedState(value)

    // Счётчик тапов по последней звезде для секрета
    var secretTaps by remember { mutableIntStateOf(0) }
    var lastSecretTap by remember { mutableLongStateOf(0L) }

    // Радуга для 11 из 10: оттенок плывёт по кругу
    val hueShift by rememberInfiniteTransition(label = "rainbow").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(2_400, easing = LinearEasing), RepeatMode.Restart),
        label = "hue",
    )
    val pulse by animateFloatAsState(
        targetValue = if (isSecret) 1.15f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioHighBouncy, stiffness = Spring.StiffnessLow),
        label = "pulse",
    )

    fun onTap(index: Int, leftHalf: Boolean, time: Long) {
        val tapped = if (allowHalf && leftHalf) index * 2 + 1 else index * 2 + 2
        val isLast = index == stars - 1
        if (secret && isLast && current >= max) {
            secretTaps = if (time - lastSecretTap <= SECRET_TAP_WINDOW_MS) secretTaps + 1 else 1
            lastSecretTap = time
            if (secretTaps >= SECRET_TAPS && current == max) {
                secretTaps = 0
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onChange(Rating.SECRET)
                return
            }
        } else {
            // Тапы считаются только когда 10 из 10 уже стоит
            secretTaps = 0
        }
        haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
        onChange(tapped)
    }

    Row(modifier, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        repeat(stars) { index ->
            val filled = value - index * 2 // 2 — целая звезда, 1 — половинка
            val icon = when {
                isSecret || filled >= 2 -> Icons.Filled.Star
                filled == 1 -> Icons.AutoMirrored.Filled.StarHalf
                else -> Icons.Outlined.StarBorder
            }
            val tint = when {
                isSecret -> Color.hsv((hueShift + index * 36f) % 360f, 0.75f, 1f)
                filled >= 1 -> StarColor
                else -> MaterialTheme.colorScheme.outline
            }
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier
                    .size(starSize)
                    .graphicsLayer {
                        scaleX = pulse
                        scaleY = pulse
                    }
                    .pointerInput(index) {
                        detectTapGestures { offset ->
                            onTap(index, leftHalf = offset.x < size.width / 2, time = System.currentTimeMillis())
                        }
                    },
            )
        }
    }
}
