package dev.chaik.remember.core.editor

import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import dev.chaik.remember.core.settings.AppSettings
import dev.chaik.remember.core.settings.UnderlineStyle

/** Как рисовать линию под заменой Т9 — из настроек «Персонализации». */
data class CorrectionLine(val style: UnderlineStyle, val widthDp: Float, val color: Color)

/** Линия по настройкам; без линии — null. */
@Composable
fun correctionLine(settings: AppSettings): CorrectionLine? {
    if (settings.t9Line == UnderlineStyle.NONE) return null
    return CorrectionLine(
        style = settings.t9Line,
        widthDp = settings.t9LineWidth / 10f,
        color = MaterialTheme.colorScheme.primary.copy(alpha = settings.t9LineAlpha / 100f),
    )
}

/** Линия под словом [start]…[end] (слово может переноситься на следующую строку). */
internal fun DrawScope.drawCorrectionUnderline(layout: TextLayoutResult, start: Int, end: Int, line: CorrectionLine) {
    val stroke = line.widthDp.dp.toPx()
    val gap = 2.dp.toPx()
    val effect = when (line.style) {
        UnderlineStyle.DASHED -> PathEffect.dashPathEffect(floatArrayOf(stroke * 3, stroke * 2.5f))
        // Почти нулевой штрих с круглым концом — точка
        UnderlineStyle.DOTTED -> PathEffect.dashPathEffect(floatArrayOf(0.01f, stroke * 2.2f))
        else -> null
    }
    for (row in layout.getLineForOffset(start)..layout.getLineForOffset(end - 1)) {
        val from = maxOf(start, layout.getLineStart(row))
        val to = minOf(end, layout.getLineEnd(row, visibleEnd = true))
        if (from >= to) continue
        val left = layout.getBoundingBox(from).left
        val right = layout.getBoundingBox(to - 1).right
        val y = layout.getLineBaseline(row) + gap + stroke / 2
        drawLine(
            color = line.color,
            start = Offset(minOf(left, right), y),
            end = Offset(maxOf(left, right), y),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
            pathEffect = effect,
        )
    }
}

/** Образец для настроек: текст, где [word] подчёркнуто так же, как в редакторе. */
@Composable
fun CorrectionUnderlinePreview(text: String, word: String, line: CorrectionLine?, style: TextStyle, modifier: Modifier = Modifier) {
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    val start = text.indexOf(word)
    BasicText(
        text = text,
        style = style,
        onTextLayout = { layout = it },
        modifier = modifier.drawBehind {
            val result = layout ?: return@drawBehind
            if (line != null && start >= 0) drawCorrectionUnderline(result, start, start + word.length, line)
        },
    )
}
