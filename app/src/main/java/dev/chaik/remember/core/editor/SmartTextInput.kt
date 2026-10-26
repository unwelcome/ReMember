package dev.chaik.remember.core.editor

import android.text.TextUtils
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputConnectionWrapper
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.InterceptPlatformTextInput
import androidx.compose.ui.platform.PlatformTextInputInterceptor
import androidx.compose.ui.platform.PlatformTextInputMethodRequest
import androidx.compose.ui.platform.PlatformTextInputSession

/**
 * Связь поля записи с клавиатурой: Enter по порядку с остальными правками и подсказка
 * о заглавной букве в начале пункта списка — для клавиатур, которые о ней спрашивают
 * (Samsung не спрашивает и решает сама — у неё после «- » и «# » строчная).
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun SmartTextInput(content: @Composable () -> Unit) {
    InterceptPlatformTextInput(SmartTextInputInterceptor, content)
}

private object SmartTextInputInterceptor : PlatformTextInputInterceptor {
    override suspend fun interceptStartInputMethod(
        request: PlatformTextInputMethodRequest,
        nextHandler: PlatformTextInputSession,
    ): Nothing {
        nextHandler.startInputMethod(
            object : PlatformTextInputMethodRequest {
                override fun createInputConnection(outAttributes: EditorInfo): InputConnection =
                    SmartInputConnection(request.createInputConnection(outAttributes))
            },
        )
    }
}

private class SmartInputConnection(target: InputConnection) : InputConnectionWrapper(target, false) {
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
