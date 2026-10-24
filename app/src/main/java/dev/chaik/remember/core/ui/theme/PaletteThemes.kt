package dev.chaik.remember.core.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color

/*
 * Готовые темы-палитры — в цветах известных тем редакторов кода (не сами темы, а заготовки
 * в их духе). У такой темы свои фон, карточки, текст и акценты; выбранный акцентный цвет
 * заменяет только основной (см. withAccent в Theme.kt). Новая тема = ещё одна функция здесь и строка в ThemeMode.
 */

/** В духе One Dark (Atom): графитовый фон, синий акцент, фиолетовый и зелёный — вторичные. */
fun oneDarkScheme(): ColorScheme = darkColorScheme(
    primary = Color(0xFF61AFEF),
    onPrimary = Color(0xFF1B2028),
    primaryContainer = Color(0xFF2B4560),
    onPrimaryContainer = Color(0xFFD3E8FB),
    secondary = Color(0xFFC678DD),
    onSecondary = Color(0xFF2A1E30),
    secondaryContainer = Color(0xFF433653),
    onSecondaryContainer = Color(0xFFF0DDF7),
    tertiary = Color(0xFF98C379),
    onTertiary = Color(0xFF1E2A15),
    tertiaryContainer = Color(0xFF34452A),
    onTertiaryContainer = Color(0xFFDDF0CF),
    background = Color(0xFF282C34),
    onBackground = Color(0xFFD7DAE0),
    surface = Color(0xFF282C34),
    onSurface = Color(0xFFD7DAE0),
    surfaceVariant = Color(0xFF3E4451),
    onSurfaceVariant = Color(0xFFABB2BF),
    surfaceTint = Color(0xFF61AFEF),
    surfaceContainerLowest = Color(0xFF1F2329),
    surfaceContainerLow = Color(0xFF2C313A),
    surfaceContainer = Color(0xFF30353F),
    surfaceContainerHigh = Color(0xFF353B45),
    surfaceContainerHighest = Color(0xFF3E4451),
    surfaceDim = Color(0xFF21252B),
    surfaceBright = Color(0xFF3E4451),
    inverseSurface = Color(0xFFD7DAE0),
    inverseOnSurface = Color(0xFF282C34),
    inversePrimary = Color(0xFF2F6FA5),
    outline = Color(0xFF5C6370),
    outlineVariant = Color(0xFF3E4451),
    error = Color(0xFFE06C75),
    onError = Color(0xFF2B1418),
    errorContainer = Color(0xFF4E2A30),
    onErrorContainer = Color(0xFFF6C4C8),
)

/** В духе Night Owl: глубокий тёмно-синий фон, бирюзовый акцент, голубой и сиреневый — вторичные. */
fun nightOwlScheme(): ColorScheme = darkColorScheme(
    primary = Color(0xFF7FDBCA),
    onPrimary = Color(0xFF00241F),
    primaryContainer = Color(0xFF134F4A),
    onPrimaryContainer = Color(0xFFC8F5EC),
    secondary = Color(0xFF82AAFF),
    onSecondary = Color(0xFF0B1F45),
    secondaryContainer = Color(0xFF1D3B53),
    onSecondaryContainer = Color(0xFFD6E4FF),
    tertiary = Color(0xFFC792EA),
    onTertiary = Color(0xFF2B1640),
    tertiaryContainer = Color(0xFF3B2D57),
    onTertiaryContainer = Color(0xFFEBD9FB),
    background = Color(0xFF011627),
    onBackground = Color(0xFFD6DEEB),
    surface = Color(0xFF011627),
    onSurface = Color(0xFFD6DEEB),
    surfaceVariant = Color(0xFF1D3B53),
    onSurfaceVariant = Color(0xFF9FB2C8),
    surfaceTint = Color(0xFF7FDBCA),
    surfaceContainerLowest = Color(0xFF010E1A),
    surfaceContainerLow = Color(0xFF0B2942),
    surfaceContainer = Color(0xFF0E2E4A),
    surfaceContainerHigh = Color(0xFF13344F),
    surfaceContainerHighest = Color(0xFF1D3B53),
    surfaceDim = Color(0xFF01111D),
    surfaceBright = Color(0xFF1D3B53),
    inverseSurface = Color(0xFFD6DEEB),
    inverseOnSurface = Color(0xFF011627),
    inversePrimary = Color(0xFF1F8A7A),
    outline = Color(0xFF637777),
    outlineVariant = Color(0xFF1D3B53),
    error = Color(0xFFEF5350),
    onError = Color(0xFF2D0A0A),
    errorContainer = Color(0xFF4A1B1E),
    onErrorContainer = Color(0xFFFFB4B0),
)
