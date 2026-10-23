package dev.chaik.remember.core.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import dev.chaik.remember.R

/**
 * Значок приложения внутри интерфейса (экран PIN, «О приложении»): передний слой
 * адаптивной иконки на её фоне, в скруглённом квадрате.
 */
@Composable
fun AppIcon(size: Dp, modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.3f))
            .background(colorResource(R.color.ic_launcher_background)),
    ) {
        // Слой адаптивной иконки на 50% больше видимой части — так призма заполняет квадрат как на рабочем столе
        Image(painterResource(R.mipmap.ic_launcher_foreground), contentDescription = null, modifier = Modifier.size(size * 1.5f))
    }
}
