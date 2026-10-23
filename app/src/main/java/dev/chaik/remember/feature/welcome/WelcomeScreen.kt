package dev.chaik.remember.feature.welcome

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.chaik.remember.R
import java.time.LocalTime

/**
 * Приветствие при запуске (после PIN, если он есть): «Доброе утро» и ник, пока главная
 * страница достраивается под ним. Показывается один раз за запуск, отключается в Настройках → Страницы.
 */
@Composable
fun WelcomeScreen(nickname: String, modifier: Modifier = Modifier) {
    val greeting = remember { greetingFor(LocalTime.now()) }
    Surface(color = MaterialTheme.colorScheme.surface, modifier = modifier.fillMaxSize()) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
        ) {
            Text(
                text = stringResource(greeting),
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
            )
            if (nickname.isNotBlank()) {
                Text(
                    text = nickname,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

/** Утро 5–11, день 12–16, вечер 17–22, ночь — остальное. */
private fun greetingFor(time: LocalTime): Int = when (time.hour) {
    in 5..11 -> R.string.welcome_morning
    in 12..16 -> R.string.welcome_day
    in 17..22 -> R.string.welcome_evening
    else -> R.string.welcome_night
}
