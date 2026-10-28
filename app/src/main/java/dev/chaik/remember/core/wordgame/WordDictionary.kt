package dev.chaik.remember.core.wordgame

import android.content.Context
import kotlin.random.Random

/** Слова для игры. [answers] — что загадывается, [guesses] — какие попытки принимаются. */
interface WordDictionary {
    fun isWord(word: String): Boolean
    fun randomAnswer(random: Random = Random.Default): String
}

/**
 * Словари из assets/words: допустимые попытки — все 5-буквенные слова словаря ENABLE
 * (общественное достояние), загадываются — только частые слова оттуда же. Читаются один раз, при первой игре.
 */
class AssetWordDictionary(private val context: Context) : WordDictionary {
    private val answers: List<String> by lazy { read("words/answers.txt") }
    private val guesses: Set<String> by lazy { read("words/guesses.txt").toHashSet() + answers }

    override fun isWord(word: String): Boolean = word.lowercase() in guesses

    override fun randomAnswer(random: Random): String = answers.random(random)

    private fun read(path: String): List<String> =
        context.assets.open(path).bufferedReader().useLines { lines -> lines.map(String::trim).filter { it.isNotEmpty() }.toList() }
}
