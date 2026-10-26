package dev.chaik.remember.feature.today

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickMultipleVisualMedia
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import dev.chaik.remember.R
import dev.chaik.remember.appContainer
import dev.chaik.remember.core.editor.EditorToolbar
import dev.chaik.remember.core.editor.EditorToolbarHeight
import dev.chaik.remember.core.editor.PhotoViewer
import dev.chaik.remember.core.editor.RichEditor
import dev.chaik.remember.core.reminders.OpenTarget
import dev.chaik.remember.core.ui.components.MasterButton
import dev.chaik.remember.core.ui.components.MasterButtonItem
import dev.chaik.remember.core.ui.components.ScreenScaffold
import dev.chaik.remember.core.ui.secondaryCorner
import dev.chaik.remember.feature.today.calendar.DayPickerPage
import dev.chaik.remember.feature.today.day.DayController
import dev.chaik.remember.feature.today.day.DayRating
import dev.chaik.remember.feature.today.day.DayTags
import dev.chaik.remember.feature.today.money.MoneyContent
import dev.chaik.remember.feature.today.sleep.SleepContent
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable
data object TodayRoute : NavKey

/** Сколько фото можно выбрать за раз. */
private const val MAX_PHOTOS_PER_PICK = 10

/** Место под кнопками внизу, чтобы последняя строка не пряталась за ними. */
private val FabSpace = 96.dp

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TodayScreen(modifier: Modifier = Modifier, onBack: (() -> Unit)? = null) {
    val context = LocalContext.current
    val container = context.appContainer
    val viewModel = viewModel { TodayViewModel(container) }
    val day = viewModel.day
    val haptics = LocalHapticFeedback.current

    // Открыли из уведомления «Как прошёл день?» / «Как спалось?» — сегодняшний день в нужном режиме
    val openRequest by container.openRequests.collectAsStateWithLifecycle()
    LaunchedEffect(openRequest) {
        val mode = when (openRequest) {
            OpenTarget.Day -> TodayMode.Day
            OpenTarget.Sleep -> TodayMode.Sleep
            else -> return@LaunchedEffect
        }
        viewModel.selectDate(LocalDate.now())
        viewModel.selectMode(mode)
        container.openRequests.value = null
    }

    // Уходим в фон — сохраняем сразу, не дожидаясь паузы в наборе
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { viewModel.flush() }

    val pickPhotos = rememberLauncherForActivityResult(PickMultipleVisualMedia(MAX_PHOTOS_PER_PICK)) { uris ->
        day.addPhotos(uris)
    }
    val launchPicker = {
        // Выбор фото — системное окно: блокировка по возвращении не нужна
        container.appLock.allowExternal()
        pickPhotos.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly))
    }

    val failedImport = stringResource(R.string.editor_photo_import_failed)
    LaunchedEffect(day.failedImports) {
        if (day.failedImports > 0) {
            Toast.makeText(context, failedImport, Toast.LENGTH_SHORT).show()
            day.consumeImportError()
        }
    }

    var openedPhotos by remember { mutableStateOf<OpenedPhotos?>(null) }
    var pickingDate by remember { mutableStateOf(false) }
    val keyboardVisible = WindowInsets.isImeVisible
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val mode = viewModel.mode

    // «Пятница, 23 октября» — на языке телефона
    val title = remember(viewModel.date) {
        viewModel.date.format(DateTimeFormatter.ofPattern("EEEE, d MMMM")).replaceFirstChar { it.titlecase() }
    }

    ScreenScaffold(
        title = title,
        modifier = modifier,
        onBack = onBack,
        onTitleClick = { pickingDate = true },
        titleModifier = daySwipeModifier(canGoForward = !viewModel.isToday) { days ->
            focusManager.clearFocus()
            viewModel.selectDate(viewModel.date.plusDays(days))
        },
        actions = {
            if (!viewModel.isToday) {
                IconButton(onClick = { viewModel.selectDate(LocalDate.now()) }) {
                    Icon(Icons.Outlined.Today, stringResource(R.string.today_go_today))
                }
            }
        },
    ) { padding ->
        val bottomSpace = if (keyboardVisible) EditorToolbarHeight + 24.dp else padding.calculateBottomPadding() + FabSpace

        Box(Modifier.fillMaxSize()) {
            AnimatedContent(
                targetState = mode,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "todayMode",
            ) { shown ->
                Column(
                    Modifier
                        .fillMaxSize()
                        .imePadding()
                        .verticalScroll(rememberScrollState())
                        .padding(top = padding.calculateTopPadding())
                        .padding(horizontal = 20.dp),
                ) {
                    when (shown) {
                        TodayMode.Day -> DayModeContent(
                            day = day,
                            photoFile = container.photoStorage::file,
                            onOpenPhotos = { ids, index -> openedPhotos = OpenedPhotos(ids, index) },
                            bottomSpace = bottomSpace,
                        )
                        TodayMode.Sleep -> {
                            if (viewModel.sleep.isLoaded) SleepContent(viewModel.sleep, Modifier.padding(top = 8.dp))
                            Spacer(Modifier.height(bottomSpace))
                        }
                        TodayMode.Money -> {
                            MoneyContent(viewModel.money, Modifier.padding(top = 8.dp))
                            Spacer(Modifier.height(bottomSpace))
                        }
                    }
                }
            }

            val buttonsPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = padding.calculateBottomPadding())
            PhotoButton(
                visible = !keyboardVisible && mode == TodayMode.Day,
                isImporting = day.isImporting,
                onClick = { if (!day.isImporting) launchPicker() },
                padding = buttonsPadding,
            )
            MasterButton(
                icon = mode.icon,
                contentDescription = stringResource(R.string.today_switch_mode, stringResource(mode.label)),
                // По дуге слева направо-вверх: Траты (180°) → Сон → День (90°)
                items = listOf(TodayMode.Money, TodayMode.Sleep, TodayMode.Day).map { item ->
                    // Текущий раздел — тёмный и неактивный
                    MasterButtonItem(item, item.icon, stringResource(item.label), enabled = item != mode)
                },
                onSelect = {
                    focusManager.clearFocus()
                    viewModel.selectMode(it)
                },
                visible = !keyboardVisible,
                padding = buttonsPadding,
            )

            // Панель форматирования — только когда печатаем в записи дня
            AnimatedVisibility(
                visible = keyboardVisible && mode == TodayMode.Day && day.editor.hasFocus,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .imePadding(),
            ) {
                EditorToolbar(
                    state = day.editor,
                    onAddPhoto = launchPicker,
                    onHideKeyboard = {
                        keyboard?.hide()
                        focusManager.clearFocus()
                    },
                )
            }
        }
    }

    if (pickingDate) {
        val marks by viewModel.calendarMarks.collectAsStateWithLifecycle()
        DayPickerPage(
            selected = viewModel.date,
            marks = marks,
            firstMonth = viewModel.calendarStart,
            onPick = {
                viewModel.selectDate(it)
                pickingDate = false
            },
            onDismiss = { pickingDate = false },
        )
    }

    openedPhotos?.let { opened ->
        PhotoViewer(
            files = opened.ids.map(container.photoStorage::file),
            initialIndex = opened.index,
            onDismiss = { openedPhotos = null },
        )
    }
}

/** Режим «День»: оценка, теги и запись с фото. */
@Composable
private fun DayModeContent(
    day: DayController,
    photoFile: (String) -> File,
    onOpenPhotos: (List<String>, Int) -> Unit,
    bottomSpace: Dp,
) {
    if (!day.isLoaded) return
    DayRating(day, Modifier.padding(top = 4.dp))
    DayTags(day, Modifier.padding(top = 8.dp))
    // Черта отделяет «шапку» дня (оценка, теги) от самой записи
    HorizontalDivider(Modifier.padding(top = 12.dp, bottom = 16.dp))
    RichEditor(
        state = day.editor,
        photoFile = photoFile,
        onOpenPhotos = onOpenPhotos,
        placeholder = stringResource(R.string.today_placeholder),
        modifier = Modifier.fillMaxWidth(),
    )
    // Пустое место под текстом: тап ставит курсор в конец записи
    Spacer(
        Modifier
            .fillMaxWidth()
            .height(bottomSpace)
            .clickable(interactionSource = null, indication = null) { day.editor.focusEnd() },
    )
}

/** Кнопка «добавить фото» слева снизу — только в режиме «День». */
@Composable
private fun BoxScope.PhotoButton(
    visible: Boolean,
    isImporting: Boolean,
    onClick: () -> Unit,
    padding: PaddingValues,
) {
    AnimatedVisibility(
        visible = visible,
        enter = scaleIn() + fadeIn(),
        exit = scaleOut() + fadeOut(),
        // Напротив мастеркнопки: у левши — справа
        modifier = Modifier
            .align(secondaryCorner())
            .padding(padding),
    ) {
        FloatingActionButton(onClick = onClick) {
            if (isImporting) {
                CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
            } else {
                Icon(Icons.Outlined.AddPhotoAlternate, stringResource(R.string.editor_add_photo))
            }
        }
    }
}

/** Сколько протянуть заголовок, чтобы сменился день. */
private val DaySwipeThreshold = 56.dp

/**
 * Свайп по шапке с датой: влево — следующий день, вправо — предыдущий. Заголовок едет за пальцем
 * и, сменив день, въезжает с другой стороны. В будущее нельзя — в ту сторону только туго тянется.
 * Жест забирает движение себе, поэтому вкладки под ним не перелистываются.
 */
@Composable
private fun daySwipeModifier(canGoForward: Boolean, onSwipe: (days: Long) -> Unit): Modifier {
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val threshold = with(LocalDensity.current) { DaySwipeThreshold.toPx() }
    val canGo by rememberUpdatedState(canGoForward)
    val swipe by rememberUpdatedState(onSwipe)
    var offset by remember { mutableFloatStateOf(0f) }
    var settle by remember { mutableStateOf<Job?>(null) }

    return Modifier
        .pointerInput(Unit) {
            suspend fun animateTo(target: Float, spec: AnimationSpec<Float>) =
                animate(offset, target, animationSpec = spec) { value, _ -> offset = value }

            detectHorizontalDragGestures(
                onDragStart = { settle?.cancel() },
                onDragEnd = {
                    val days = when {
                        offset <= -threshold && canGo -> 1L
                        offset >= threshold -> -1L
                        else -> 0L
                    }
                    settle = scope.launch {
                        if (days == 0L) {
                            animateTo(0f, spring())
                            return@launch
                        }
                        val out = size.width / 3f * if (days > 0) -1 else 1
                        animateTo(out, tween(110))
                        haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                        swipe(days)
                        offset = -out
                        animateTo(0f, spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow))
                    }
                },
                onDragCancel = { settle = scope.launch { animateTo(0f, spring()) } },
            ) { change, dragAmount ->
                change.consume()
                val resist = if (offset + dragAmount < 0 && !canGo) 0.25f else 1f
                offset += dragAmount * resist
            }
        }
        .graphicsLayer {
            translationX = offset
            alpha = 1f - (abs(offset) / size.width * 1.5f).coerceIn(0f, 0.7f)
        }
}

/** Какие фото открыты на весь экран и с какого начать. */
private data class OpenedPhotos(val ids: List<String>, val index: Int)
