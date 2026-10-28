package dev.chaik.remember.feature.games.fiveletters

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.automirrored.outlined.KeyboardReturn
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.chaik.remember.R
import dev.chaik.remember.appContainer
import dev.chaik.remember.core.wordgame.GuessError
import dev.chaik.remember.core.wordgame.LetterMark
import dev.chaik.remember.core.wordgame.WordGame
import dev.chaik.remember.core.wordgame.WordGameState
import kotlinx.coroutines.delay

/** Цвета клеток — одинаковые в любой теме, чтобы подсказки читались сразу, спросонья. */
private val HitColor = Color(0xFF2E9E6B)
private val PresentColor = Color(0xFFD99A1E)

private const val KeyEnter = '\n'
private const val KeyErase = '\b'
private val KeyRows = listOf("qwertyuiop", "asdfghjkl", "${KeyEnter}zxcvbnm$KeyErase")

/** Где идёт игра — от этого зависят подписи и кнопка после конца игры. */
enum class WordGameMode {
    /** Усиленный будильник: победа или проигрыш выключают его. */
    ALARM,

    /** «Попробовать игру» из настроек будильника. */
    PRACTICE,

    /** Раздел «Игры»: после конца — следующее слово. Заголовок — у страницы. */
    FREE,
}

/**
 * «Пять букв»: поле 6×5 и своя клавиатура (системная на экране блокировки не нужна).
 * Один экран для будильника и раздела «Игры» — всё, что меняется в игре, меняется и там, и там.
 * [onChange] — каждое изменение (набор, попытка), [onDone] — кнопка после конца игры
 * («Готово» или, в разделе «Игры», «Следующее слово»).
 */
@Composable
fun WordGameScreen(
    state: WordGameState,
    onChange: (WordGameState) -> Unit,
    onDone: () -> Unit,
    mode: WordGameMode,
    modifier: Modifier = Modifier,
) {
    val dictionary = LocalContext.current.appContainer.wordDictionary
    val game = remember(dictionary) { WordGame(dictionary) }
    val haptics = LocalHapticFeedback.current
    var error by remember { mutableStateOf<GuessError?>(null) }
    var shakes by remember { mutableIntStateOf(0) }
    LaunchedEffect(error, shakes) {
        if (error != null) {
            delay(1_800)
            error = null
        }
    }

    fun press(key: Char) {
        haptics.performHapticFeedback(HapticFeedbackType.KeyboardTap)
        when (key) {
            KeyEnter -> {
                val (next, problem) = game.submit(state)
                if (problem != null) {
                    error = problem
                    shakes++
                } else {
                    onChange(next)
                }
            }
            KeyErase -> onChange(game.erase(state))
            else -> onChange(game.type(state, key))
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        if (mode != WordGameMode.FREE) {
            Text(
                text = stringResource(R.string.game_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        // Итог игры — сверху, вместо подсказки
        if (state.finished) {
            Result(state, mode, Modifier.padding(top = 4.dp, bottom = 12.dp))
        } else {
            Text(
                text = stringResource(if (mode == WordGameMode.PRACTICE) R.string.game_practice_hint else R.string.game_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 2.dp, bottom = 12.dp),
            )
        }

        Board(state, shakes, Modifier.weight(1f))

        // Сообщение о неверной попытке — между полем и клавиатурой
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().height(40.dp)) {
            when {
                state.finished -> Unit
                error != null -> Text(
                    text = stringResource(if (error == GuessError.TOO_SHORT) R.string.game_too_short else R.string.game_not_a_word),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }

        if (state.finished) {
            // Внизу — только большая кнопка
            Button(
                onClick = onDone,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .height(64.dp),
            ) {
                Text(
                    text = stringResource(if (mode == WordGameMode.FREE) R.string.game_next else R.string.game_done),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        } else {
            Keyboard(state.keyMarks(), onKey = ::press)
        }
    }
}

@Composable
private fun Board(state: WordGameState, shakes: Int, modifier: Modifier) {
    // Неверное слово — строка вздрагивает
    val shake = remember { Animatable(0f) }
    LaunchedEffect(shakes) {
        if (shakes == 0) return@LaunchedEffect
        for (x in listOf(-14f, 12f, -8f, 6f, -3f, 0f)) shake.animateTo(x, tween(45))
    }
    // Клетка — сколько влезает и по ширине, и по высоте (не больше 64 dp)
    BoxWithConstraints(contentAlignment = Alignment.Center, modifier = modifier.fillMaxWidth()) {
        val gap = 6.dp
        val byWidth = (maxWidth - gap * (WordGameState.WORD_LENGTH - 1)) / WordGameState.WORD_LENGTH
        val byHeight = (maxHeight - gap * (WordGameState.MAX_ATTEMPTS - 1)) / WordGameState.MAX_ATTEMPTS
        val tile = minOf(byWidth, byHeight, 64.dp)
        Column(verticalArrangement = Arrangement.spacedBy(gap), horizontalAlignment = Alignment.CenterHorizontally) {
            repeat(WordGameState.MAX_ATTEMPTS) { row ->
                val guess = state.guesses.getOrNull(row)
                val current = row == state.guesses.size && !state.finished
                val letters = guess ?: if (current) state.input else ""
                val marks = guess?.let { WordGameState.mark(it, state.answer) }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(gap),
                    modifier = Modifier.graphicsLayer { if (current) translationX = shake.value * density },
                ) {
                    repeat(WordGameState.WORD_LENGTH) { i ->
                        Tile(letters.getOrNull(i), marks?.get(i), Modifier.size(tile))
                    }
                }
            }
        }
    }
}

@Composable
private fun Tile(letter: Char?, mark: LetterMark?, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    val background by animateColorAsState(
        when (mark) {
            LetterMark.HIT -> HitColor
            LetterMark.PRESENT -> PresentColor
            LetterMark.MISS -> colors.surfaceContainerHighest
            null -> colors.surface
        },
        label = "tile",
    )
    val shape = RoundedCornerShape(12.dp)
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(shape)
            .background(background)
            .then(
                if (mark == null) Modifier.border(2.dp, if (letter != null) colors.outline else colors.outlineVariant, shape) else Modifier,
            ),
    ) {
        if (letter != null) {
            Text(
                text = letter.uppercase(),
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = when (mark) {
                    LetterMark.HIT, LetterMark.PRESENT -> Color.White
                    LetterMark.MISS -> colors.onSurfaceVariant
                    null -> colors.onSurface
                },
            )
        }
    }
}

@Composable
private fun Keyboard(marks: Map<Char, LetterMark>, onKey: (Char) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        KeyRows.forEachIndexed { index, row ->
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.fillMaxWidth()) {
                // Средний ряд короче — по краям отступ в полклавиши
                if (index == 1) Spacer(Modifier.weight(0.5f))
                row.forEach { key -> Key(key, marks[key], onKey) }
                if (index == 1) Spacer(Modifier.weight(0.5f))
            }
        }
    }
}

@Composable
private fun RowScope.Key(key: Char, mark: LetterMark?, onKey: (Char) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val wide = key == KeyEnter || key == KeyErase
    val background = when (mark) {
        LetterMark.HIT -> HitColor
        LetterMark.PRESENT -> PresentColor
        LetterMark.MISS -> colors.surfaceContainerLowest
        null -> colors.surfaceContainerHighest
    }
    val content = when (mark) {
        LetterMark.HIT, LetterMark.PRESENT -> Color.White
        LetterMark.MISS -> colors.outline
        null -> colors.onSurface
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .weight(if (wide) 1.5f else 1f)
            .height(54.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (key == KeyEnter) colors.primary else background)
            .clickable { onKey(key) },
    ) {
        when (key) {
            KeyEnter -> Icon(Icons.AutoMirrored.Outlined.KeyboardReturn, stringResource(R.string.game_enter), tint = colors.onPrimary)
            KeyErase -> Icon(Icons.AutoMirrored.Outlined.Backspace, stringResource(R.string.game_erase), tint = colors.onSurface)
            else -> Text(key.uppercase(), fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = content)
        }
    }
}

@Composable
private fun Result(state: WordGameState, mode: WordGameMode, modifier: Modifier) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Text(
            text = when {
                !state.won -> stringResource(R.string.game_lost, state.answer.uppercase())
                mode == WordGameMode.ALARM -> stringResource(R.string.game_won)
                else -> stringResource(R.string.game_won_plain)
            },
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        if (mode == WordGameMode.ALARM) {
            Text(
                text = stringResource(R.string.game_alarm_off),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
