package dev.chaik.remember.core.ui

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chaik.remember.core.settings.LocalAppSettings

/** Угол главной кнопки экрана: справа внизу, а в режиме левши — слева. */
@Composable
fun primaryCorner(): Alignment = if (LocalAppSettings.current.leftHanded) Alignment.BottomStart else Alignment.BottomEnd

/** Противоположный угол — для второй кнопки (фото на «Сегодня»). */
@Composable
fun secondaryCorner(): Alignment = if (LocalAppSettings.current.leftHanded) Alignment.BottomEnd else Alignment.BottomStart

/** Кнопка в углу главной руки с отступом [side] от края и [bottom] снизу. */
@Composable
fun BoxScope.primaryCornerModifier(bottom: Dp, side: Dp = 20.dp): Modifier {
    val left = LocalAppSettings.current.leftHanded
    return Modifier
        .align(if (left) Alignment.BottomStart else Alignment.BottomEnd)
        .padding(start = if (left) side else 0.dp, end = if (left) 0.dp else side, bottom = bottom)
}
