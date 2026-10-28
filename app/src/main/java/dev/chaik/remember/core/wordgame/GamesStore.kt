package dev.chaik.remember.core.wordgame

import android.content.Context
import androidx.core.content.edit
import kotlinx.serialization.json.Json

/** Незаконченные игры раздела «Игры» — чтобы вернуться и продолжить с того же места. */
class GamesStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    var fiveLetters: WordGameState?
        get() = prefs.getString(KEY_FIVE_LETTERS, null)
            ?.let { runCatching { json.decodeFromString(WordGameState.serializer(), it) }.getOrNull() }
        set(value) = prefs.edit {
            if (value == null) remove(KEY_FIVE_LETTERS) else putString(KEY_FIVE_LETTERS, json.encodeToString(WordGameState.serializer(), value))
        }

    private companion object {
        const val PREFS = "games"
        const val KEY_FIVE_LETTERS = "five_letters"
        val json = Json { ignoreUnknownKeys = true }
    }
}
