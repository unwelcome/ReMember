package dev.chaik.remember.feature.today

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickMultipleVisualMedia
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import dev.chaik.remember.R
import dev.chaik.remember.appContainer
import dev.chaik.remember.core.editor.EditorState
import dev.chaik.remember.core.editor.EditorToolbar
import dev.chaik.remember.core.editor.EditorToolbarHeight
import dev.chaik.remember.core.editor.PhotoViewer
import dev.chaik.remember.core.editor.RichEditor
import dev.chaik.remember.core.ui.components.ScreenScaffold
import kotlinx.serialization.Serializable
import java.time.format.DateTimeFormatter

@Serializable
data object TodayRoute : NavKey

/** Сколько фото можно выбрать за раз. */
private const val MAX_PHOTOS_PER_PICK = 10

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TodayScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val container = context.appContainer
    val viewModel = viewModel { TodayViewModel(container.dayRepository, container.photoStorage) }
    val editor = viewModel.editor

    // Уходим в фон — сохраняем сразу, не дожидаясь паузы в наборе
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { viewModel.flush() }

    val pickPhotos = rememberLauncherForActivityResult(PickMultipleVisualMedia(MAX_PHOTOS_PER_PICK)) { uris ->
        viewModel.addPhotos(uris)
    }
    val launchPicker = { pickPhotos.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly)) }

    val failedImport = stringResource(R.string.editor_photo_import_failed)
    LaunchedEffect(viewModel.failedImports) {
        if (viewModel.failedImports > 0) {
            Toast.makeText(context, failedImport, Toast.LENGTH_SHORT).show()
            viewModel.consumeImportError()
        }
    }

    var openedPhoto by remember { mutableStateOf<String?>(null) }
    val keyboardVisible = WindowInsets.isImeVisible
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    // «Пятница, 23 октября» — на языке телефона
    val title = remember(viewModel.date) {
        viewModel.date.format(DateTimeFormatter.ofPattern("EEEE, d MMMM")).replaceFirstChar { it.titlecase() }
    }

    ScreenScaffold(title = title, modifier = modifier) { padding ->
        Box(Modifier.fillMaxSize()) {
            Column(
                Modifier
                    .fillMaxSize()
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(top = padding.calculateTopPadding())
                    .padding(horizontal = 20.dp),
            ) {
                if (viewModel.isLoaded) {
                    RichEditor(
                        state = editor,
                        photoFile = container.photoStorage::file,
                        onOpenPhoto = { openedPhoto = it },
                        placeholder = stringResource(R.string.today_placeholder),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                // Пустое место под текстом: тап ставит курсор в конец записи
                Spacer(
                    Modifier
                        .fillMaxWidth()
                        .height(if (keyboardVisible) EditorToolbarHeight + 24.dp else padding.calculateBottomPadding() + 96.dp)
                        .clickable(interactionSource = null, indication = null) { editor.focusEnd() },
                )
            }

            // Кнопка фото — справа снизу, над островком; с клавиатурой фото добавляется с панели
            AnimatedVisibility(
                visible = !keyboardVisible,
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 20.dp, bottom = padding.calculateBottomPadding()),
            ) {
                FloatingActionButton(onClick = { if (!viewModel.isImporting) launchPicker() }) {
                    if (viewModel.isImporting) {
                        CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Outlined.AddPhotoAlternate, stringResource(R.string.editor_add_photo))
                    }
                }
            }

            AnimatedVisibility(
                visible = keyboardVisible,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .imePadding(),
            ) {
                EditorToolbar(
                    state = editor,
                    onAddPhoto = launchPicker,
                    onHideKeyboard = {
                        keyboard?.hide()
                        focusManager.clearFocus()
                    },
                )
            }
        }
    }

    openedPhoto?.let { id ->
        PhotoViewer(file = container.photoStorage.file(id), onDismiss = { openedPhoto = null })
    }
}

/** Курсор в конец последнего текстового блока. */
private fun EditorState.focusEnd() {
    selectPhoto(null)
    val last = blocks.lastOrNull() as? EditorState.TextBlock ?: return
    onTextChange(last, last.value.copy(selection = TextRange(last.value.text.length)))
    last.focusRequester.requestFocus()
}
