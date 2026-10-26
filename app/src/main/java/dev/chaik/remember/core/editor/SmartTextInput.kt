package dev.chaik.remember.core.editor

import android.text.Spanned
import android.text.TextUtils
import android.text.style.SuggestionSpan
import android.view.KeyEvent
import android.view.inputmethod.CorrectionInfo
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputConnectionWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.InterceptPlatformTextInput
import androidx.compose.ui.platform.PlatformTextInputInterceptor
import androidx.compose.ui.platform.PlatformTextInputMethodRequest
import androidx.compose.ui.platform.PlatformTextInputSession

/**
 * Связь поля записи с клавиатурой: Enter по порядку с остальными правками, сообщения
 * об автозаменах Т9 ([onAutoCorrect]) и подсказка о заглавной букве в начале пункта списка —
 * для клавиатур, которые о ней спрашивают (Samsung не спрашивает и решает сама).
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun SmartTextInput(onAutoCorrect: (AutoCorrection) -> Unit, content: @Composable () -> Unit) {
    val callback by rememberUpdatedState(onAutoCorrect)
    val interceptor = remember { SmartTextInputInterceptor { callback(it) } }
    InterceptPlatformTextInput(interceptor, content)
}

private class SmartTextInputInterceptor(private val onAutoCorrect: (AutoCorrection) -> Unit) : PlatformTextInputInterceptor {
    override suspend fun interceptStartInputMethod(
        request: PlatformTextInputMethodRequest,
        nextHandler: PlatformTextInputSession,
    ): Nothing {
        nextHandler.startInputMethod(
            object : PlatformTextInputMethodRequest {
                override fun createInputConnection(outAttributes: EditorInfo): InputConnection =
                    SmartInputConnection(request.createInputConnection(outAttributes), onAutoCorrect)
            },
        )
    }
}

private class SmartInputConnection(
    target: InputConnection,
    private val onAutoCorrect: (AutoCorrection) -> Unit,
) : InputConnectionWrapper(target, false) {
    /** Последнее «где и что заменено» — клавиатура присылает его прямо перед самим словом. */
    private var lastCorrection: CorrectionInfo? = null

    override fun commitCorrection(correctionInfo: CorrectionInfo?): Boolean {
        lastCorrection = correctionInfo
        return super.commitCorrection(correctionInfo)
    }

    // Автозамену клавиатура присылает словом с пометкой: в подсказках — то, что было набрано
    override fun commitText(text: CharSequence?, newCursorPosition: Int): Boolean {
        if (text is Spanned && text.isNotEmpty()) {
            val corrected = text.toString()
            val original = text.getSpans(0, text.length, SuggestionSpan::class.java)
                .filter { it.flags and CORRECTION_FLAGS != 0 }
                .firstNotNullOfOrNull { span -> span.suggestions.firstOrNull { it.isNotBlank() && it != corrected } }
            if (original != null) {
                val info = lastCorrection?.takeIf { it.newText?.toString() == corrected }
                onAutoCorrect(AutoCorrection(start = info?.offset ?: -1, corrected = corrected, original = original))
            }
        }
        lastCorrection = null
        return super.commitText(text, newCursorPosition)
    }
    override fun getCursorCapsMode(reqModes: Int): Int {
        val before = getTextBeforeCursor(CAPS_LOOKBEHIND, 0) ?: return super.getCursorCapsMode(reqModes)
        return capsMode(before, reqModes)
    }

    // Enter Compose применяет сразу, а слово, присланное перед ним в той же пачке правок, — только
    // в конце пачки. Тогда перенос встаёт раньше слова, и исправление Т9 уезжает на новую строку.
    // Отдаём Enter обычным текстом — он встаёт в очередь вслед за словом.
    override fun sendKeyEvent(event: KeyEvent): Boolean {
        val enter = event.keyCode == KeyEvent.KEYCODE_ENTER || event.keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER
        if (enter && event.metaState == 0) {
            if (event.action == KeyEvent.ACTION_DOWN) {
                // Недописанное слово оставляем как есть, иначе перенос встанет вместо него
                super.finishComposingText()
                super.commitText("\n", 1)
            }
            return true
        }
        return super.sendKeyEvent(event)
    }

    private companion object {
        const val CAPS_LOOKBEHIND = 200
        const val CORRECTION_FLAGS = SuggestionSpan.FLAG_EASY_CORRECT or SuggestionSpan.FLAG_AUTO_CORRECTION
    }
}

/**
 * Какие из запрошенных режимов заглавных ([TextUtils.CAP_MODE_SENTENCES] и др.) включить для
 * текста перед курсором. Начало предложения — начало строки, пустой пункт списка, заголовок
 * или пробел после «.», «!», «?», «…» (в том числе за закрывающей кавычкой или скобкой).
 */
internal fun capsMode(before: CharSequence, reqModes: Int): Int {
    var result = reqModes and TextUtils.CAP_MODE_CHARACTERS
    val line = before.substring(before.lastIndexOf('\n') + 1)
    val marker = ListPrefix.find(line) ?: HeadingPrefix.find(line)
    val body = if (marker != null) line.substring(marker.range.last + 1) else line
    val wordStart = body.isEmpty() || body.last().isWhitespace()
    if (wordStart) {
        result = result or (reqModes and TextUtils.CAP_MODE_WORDS)
        val sentence = body.trimEnd().trimEnd(*SentenceClosers)
        if (sentence.isEmpty() || sentence.last() in SentenceEnds) {
            result = result or (reqModes and TextUtils.CAP_MODE_SENTENCES)
        }
    }
    return result
}

private const val SentenceEnds = ".!?…"
private val SentenceClosers = charArrayOf('"', '\'', '»', '”', ')', ']')
