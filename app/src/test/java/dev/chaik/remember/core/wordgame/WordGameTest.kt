package dev.chaik.remember.core.wordgame

import dev.chaik.remember.core.wordgame.LetterMark.HIT
import dev.chaik.remember.core.wordgame.LetterMark.MISS
import dev.chaik.remember.core.wordgame.LetterMark.PRESENT
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class WordGameTest {
    private val dictionary = object : WordDictionary {
        val words = setOf("crane", "apple", "speed", "abide", "erase", "eerie", "steer")
        override fun isWord(word: String) = word in words
        override fun randomAnswer(random: Random) = "crane"
    }
    private val game = WordGame(dictionary)

    @Test
    fun `буквы на месте, в другом месте и мимо`() {
        assertEquals(listOf(HIT, HIT, HIT, HIT, HIT), WordGameState.mark("crane", "crane"))
        assertEquals(listOf(HIT, MISS, MISS, MISS, HIT), WordGameState.mark("abide", "apple"))
    }

    @Test
    fun `повторы отмечаются не больше, чем их в слове`() {
        // В «abide» одна «e»: в «speed» отмечается только одна из двух
        assertEquals(listOf(MISS, MISS, PRESENT, MISS, PRESENT), WordGameState.mark("speed", "abide"))
        // Буква на месте забирает свою «e» раньше: остальные «e» в «eerie» — мимо
        assertEquals(listOf(MISS, MISS, MISS, PRESENT, HIT), WordGameState.mark("eerie", "abide"))
    }

    @Test
    fun `короткое слово и слово не из словаря не принимаются`() {
        var state = WordGameState(answer = "crane")
        "cra".forEach { state = game.type(state, it) }
        assertEquals(GuessError.TOO_SHORT, game.submit(state).second)
        "zz".forEach { state = game.type(state, it) }
        assertEquals(GuessError.NOT_A_WORD, game.submit(state).second)
        assertEquals(emptyList<String>(), game.submit(state).first.guesses)
    }

    @Test
    fun `победа и проигрыш`() {
        var state = WordGameState(answer = "crane")
        repeat(5) {
            "apple".forEach { state = game.type(state, it) }
            val (next, error) = game.submit(state)
            assertNull(error)
            state = next
        }
        assertFalse(state.finished)
        "crane".forEach { state = game.type(state, it) }
        state = game.submit(state).first
        assertTrue(state.won)

        val lost = WordGameState(answer = "crane", guesses = List(6) { "apple" })
        assertTrue(lost.lost && lost.finished)
        // После конца игры буквы не набираются
        assertEquals("", game.type(lost, 'a').input)
    }

    @Test
    fun `клавиатура — лучшее известное о букве`() {
        val state = WordGameState(answer = "crane", guesses = listOf("apple", "crane"))
        val keys = state.keyMarks()
        assertEquals(HIT, keys['a'])
        assertEquals(MISS, keys['p'])
        assertEquals(HIT, keys['e'])
    }
}
