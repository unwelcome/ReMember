package dev.chaik.remember.feature.time.ring

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.HourglassBottom
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.chaik.remember.R
import dev.chaik.remember.appContainer
import dev.chaik.remember.core.alarm.RingKind
import dev.chaik.remember.core.alarm.RingSession
import dev.chaik.remember.core.ui.theme.ReMemberTheme
import dev.chaik.remember.core.wordgame.WordGameState
import dev.chaik.remember.feature.games.fiveletters.WordGameMode
import dev.chaik.remember.feature.games.fiveletters.WordGameScreen
import kotlinx.coroutines.delay
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Звонок будильника или таймера — поверх экрана блокировки, без PIN приложения: отсюда
 * доступны только кнопки звонка и игра, остальное приложение не открыть.
 *
 * С [EXTRA_PRACTICE] — просто попробовать игру (из настроек будильника), без звонка.
 */
class AlarmActivity : ComponentActivity() {
    private var practice = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        enableEdgeToEdge()
        practice = intent.getBooleanExtra(EXTRA_PRACTICE, false)
        val practice = practice
        setContent {
            ReMemberTheme {
                Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxSize()) {
                    if (practice) PracticeGame(onClose = ::finish) else AlarmScreen(onClose = ::finish)
                }
            }
        }
    }

    /** Пока пробовали игру, зазвонил настоящий будильник (или наоборот) — экран открывается заново. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.getBooleanExtra(EXTRA_PRACTICE, false) != practice) {
            setIntent(intent)
            recreate()
        }
    }

    companion object {
        private const val EXTRA_PRACTICE = "practice"

        /** Попробовать игру усиленного будильника. */
        fun practice(context: Context) = context.startActivity(
            Intent(context, AlarmActivity::class.java)
                .putExtra(EXTRA_PRACTICE, true)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK),
        )
    }
}

@Composable
private fun AlarmScreen(onClose: () -> Unit) {
    val container = LocalContext.current.appContainer
    val controller = container.alarmController
    val session by container.timeStore.session.collectAsStateWithLifecycle()
    val current = session
    // Звонок выключили (здесь или из шторки) — закрываемся
    LaunchedEffect(current == null) { if (current == null) onClose() }
    if (current == null) return
    // «Назад» не выключает звонок и не выходит из игры — только кнопки
    BackHandler {}

    val game = current.game
    when {
        current.ringing -> RingingContent(current, onDismiss = controller::dismiss, onSnooze = controller::snooze)
        game != null -> Box(Modifier.fillMaxSize().safeDrawingPadding()) {
            WordGameScreen(state = game, onChange = controller::updateGame, onDone = controller::finish, mode = WordGameMode.ALARM)
            if (!game.finished) {
                AgainCountdown(current.againAt, Modifier.align(Alignment.TopEnd).padding(top = 12.dp, end = 16.dp))
            }
        }
        // Звонок замолк по времени, игры ещё нет — будильник скоро повторится
        else -> RingingContent(current, onDismiss = controller::dismiss, onSnooze = null)
    }
}

@Composable
private fun PracticeGame(onClose: () -> Unit) {
    val dictionary = LocalContext.current.appContainer.wordDictionary
    var game by remember { mutableStateOf(WordGameState(answer = dictionary.randomAnswer())) }
    BackHandler(onBack = onClose)
    WordGameScreen(
        state = game,
        onChange = { game = it },
        onDone = onClose,
        mode = WordGameMode.PRACTICE,
        modifier = Modifier.safeDrawingPadding(),
    )
}

/**
 * Сколько осталось до повторного звонка — видно только последнюю минуту. Каждые 10 секунд
 * цифры краснеют сильнее, последние 10 секунд — совсем красные. Любой ход отодвигает звонок,
 * и отсчёт пропадает.
 */
@Composable
private fun AgainCountdown(againAt: Long?, modifier: Modifier) {
    if (againAt == null) return
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(againAt) {
        while (true) {
            now = System.currentTimeMillis()
            delay(250)
        }
    }
    val left = ((againAt - now + 999) / 1000).coerceAtLeast(0)
    if (left > COUNTDOWN_SECONDS || left == 0L) return
    // 60…51 — обычный цвет, дальше на шаг краснее каждые 10 секунд, с 10 и меньше — красный
    val redness = ((COUNTDOWN_SECONDS - left) / 10 + if (left <= 10) 1 else 0).coerceAtMost(5) / 5f
    val color by animateColorAsState(lerp(MaterialTheme.colorScheme.onSurface, CountdownRed, redness), label = "countdown")
    Text(
        text = "%d:%02d".format(left / 60, left % 60),
        fontSize = 22.sp,
        fontWeight = FontWeight.SemiBold,
        color = color,
        modifier = modifier,
    )
}

private const val COUNTDOWN_SECONDS = 60L
private val CountdownRed = Color(0xFFFF3B30)

/** Звонит: текущее время, подпись и кнопки. */
@Composable
private fun RingingContent(session: RingSession, onDismiss: () -> Unit, onSnooze: (() -> Unit)?) {
    var now by remember { mutableStateOf(LocalTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = LocalTime.now()
            delay(1_000)
        }
    }
    val colors = MaterialTheme.colorScheme
    val timer = session.kind == RingKind.TIMER
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(24.dp),
    ) {
        Icon(
            imageVector = when {
                timer -> Icons.Outlined.HourglassBottom
                session.hard -> Icons.Outlined.Extension
                else -> Icons.Outlined.Alarm
            },
            contentDescription = null,
            tint = colors.primary,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = now.format(DateTimeFormatter.ofPattern("HH:mm")),
            fontSize = 88.sp,
            fontWeight = FontWeight.Light,
            color = colors.onSurface,
        )
        val title = when {
            timer -> stringResource(R.string.time_timer_done)
            session.again -> stringResource(R.string.time_ring_again)
            else -> session.alarm?.label?.takeIf { it.isNotBlank() } ?: stringResource(R.string.time_alarm)
        }
        Text(title, style = MaterialTheme.typography.titleLarge, color = colors.onSurface, textAlign = TextAlign.Center)
        if (session.hard) {
            Text(
                text = stringResource(if (session.game != null) R.string.time_hard_continue_hint else R.string.time_hard_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        Spacer(Modifier.height(48.dp))
        Button(
            onClick = onDismiss,
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
        ) {
            Text(
                text = stringResource(
                    when {
                        timer -> R.string.time_stop
                        session.hard && session.game != null -> R.string.time_continue_game
                        session.hard -> R.string.time_dismiss_and_play
                        else -> R.string.time_dismiss
                    },
                ),
                style = MaterialTheme.typography.titleMedium,
            )
        }
        val alarm = session.alarm
        if (onSnooze != null && session.canSnooze && alarm != null) {
            OutlinedButton(
                onClick = onSnooze,
                modifier = Modifier
                    .padding(top = 12.dp)
                    .fillMaxWidth()
                    .height(56.dp),
            ) {
                Text(stringResource(R.string.time_snooze_for, alarm.snoozeMinutes), style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}
