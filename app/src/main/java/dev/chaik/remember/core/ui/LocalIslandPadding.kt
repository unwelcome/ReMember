package dev.chaik.remember.core.ui

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Отступ снизу, который нужен контенту экрана, чтобы последние элементы
 * не прятались под плавающим островком (включает системную панель навигации).
 */
val LocalIslandPadding = staticCompositionLocalOf<Dp> { 0.dp }
