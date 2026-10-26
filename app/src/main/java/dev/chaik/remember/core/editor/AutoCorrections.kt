package dev.chaik.remember.core.editor

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

/**
 * Слово, которое клавиатура заменила сама (Т9): [corrected] стоит в тексте с позиции [start],
 * а набрано было [original]. Живёт, пока открыт редактор, — в запись не сохраняется.
 */
data class AutoCorrection(val start: Int, val corrected: String, val original: String) {
    val end: Int get() = start + corrected.length

    /** Слово на своём месте и целое: не дописано и не разрезано. */
    fun isValidIn(text: String): Boolean =
        end <= text.length &&
            text.regionMatches(start, corrected, 0, corrected.length) &&
            text.getOrNull(start - 1)?.isLetterOrDigit() != true &&
            text.getOrNull(end)?.isLetterOrDigit() != true
}

object AutoCorrections {

    /**
     * Замены после правки текста [old] → [new]: те, что до правки, остаются, после — сдвигаются,
     * задетые правкой пропадают. [pending] — замена, о которой клавиатура только что сообщила:
     * её слово как раз появилось в [new].
     */
    fun update(
        corrections: List<AutoCorrection>,
        pending: AutoCorrection?,
        old: String,
        new: TextFieldValue,
    ): List<AutoCorrection> {
        val text = new.text
        val prefix = old.commonPrefixWith(text).length
        val suffix = old.commonSuffixWith(text).length.coerceAtMost(minOf(old.length, text.length) - prefix)
        val oldEditEnd = old.length - suffix
        val delta = text.length - old.length

        val kept = corrections.mapNotNull { c ->
            when {
                c.end <= prefix -> c
                c.start >= oldEditEnd -> c.copy(start = c.start + delta)
                else -> null
            }
        }
        val added = pending?.let { locate(it, new) }
        return (kept + listOfNotNull(added))
            .filter { it.isValidIn(text) }
            .distinctBy { it.start }
            .sortedBy { it.start }
    }

    /**
     * Найти слово новой замены. Обычно клавиатура говорит, где оно ([AutoCorrection.start]);
     * если не сказала или промахнулась — ближайшее такое слово перед курсором.
     */
    private fun locate(pending: AutoCorrection, new: TextFieldValue): AutoCorrection? {
        if (pending.start >= 0 && pending.isValidIn(new.text)) return pending
        val cursor = new.selection.start
        val from = (cursor - pending.corrected.length).coerceAtLeast(0)
        val index = new.text.lastIndexOf(pending.corrected, startIndex = from)
        // Между словом и курсором — разве что пробел или знак, который клавиатура дописала следом
        if (index < 0 || cursor - (index + pending.corrected.length) > MAX_GAP) return null
        return pending.copy(start = index).takeIf { it.isValidIn(new.text) }
    }

    /** Вернуть набранное слово вместо замены; курсор — сразу за ним. */
    fun revert(value: TextFieldValue, correction: AutoCorrection): TextFieldValue {
        val text = value.text.replaceRange(correction.start, correction.end, correction.original)
        return TextFieldValue(text, TextRange(correction.start + correction.original.length))
    }

    private const val MAX_GAP = 2
}
