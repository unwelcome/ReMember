package dev.chaik.remember.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp

/**
 * Ползунок приложения: полоска и точки шагов — как в Material 3, а ручка — круглая
 * (вместо вертикальной палочки). Все ползунки приложения — через него.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    val colors = SliderDefaults.colors()
    Slider(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        onValueChangeFinished = onValueChangeFinished,
        colors = colors,
        interactionSource = interaction,
        steps = steps,
        valueRange = valueRange,
        thumb = {
            Box(
                Modifier
                    .size(ThumbSize)
                    .shadow(2.dp, CircleShape)
                    .background(MaterialTheme.colorScheme.primary, CircleShape),
            )
        },
        // Без выреза в полоске вокруг ручки: круг лежит поверх полоски
        track = { state -> SliderDefaults.Track(sliderState = state, colors = colors, thumbTrackGapSize = 0.dp) },
    )
}

private val ThumbSize = 22.dp
