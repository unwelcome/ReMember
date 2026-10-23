package dev.chaik.remember.core.ui.theme

import androidx.compose.ui.graphics.Color
import dev.chaik.remember.core.model.Rating

// Цвет оценки дня — для календаря, а дальше и для heatmap-ов.
// 0–3.5 — красный, 4–6.5 — жёлтый, 7–9.5 — зелёный, 10 — циан, 11 — розово-фиолетовый.

val RatingBad = Color(0xFFEF5350)
val RatingOkay = Color(0xFFFFCA28)
val RatingGood = Color(0xFF66BB6A)
val RatingPerfect = Color(0xFF26C6DA)
val RatingSecret = Color(0xFFE040FB)

/** Цвет оценки дня в половинках звезды ([Rating]). */
fun ratingColor(halves: Int): Color = when {
    Rating.isSecret(halves) -> RatingSecret
    halves >= 20 -> RatingPerfect
    halves >= 14 -> RatingGood
    halves >= 8 -> RatingOkay
    else -> RatingBad
}
