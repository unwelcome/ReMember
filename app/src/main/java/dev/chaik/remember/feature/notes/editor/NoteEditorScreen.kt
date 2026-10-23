package dev.chaik.remember.feature.notes.editor

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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.DriveFileMove
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
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
import dev.chaik.remember.core.ui.components.ScreenScaffold
import dev.chaik.remember.core.ui.components.TagChips
import dev.chaik.remember.feature.notes.ConfirmDialog
import dev.chaik.remember.feature.notes.FolderPickerDialog
import kotlinx.serialization.Serializable

/** Заметка: [noteId] = 0 — новая, в папке [folderId]. */
@Serializable
data class NoteEditorRoute(val noteId: Long = 0, val folderId: Long? = null) : NavKey

private const val MAX_PHOTOS_PER_PICK = 10

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NoteEditorScreen(
    route: NoteEditorRoute,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val container = context.appContainer
    val viewModel = viewModel { NoteEditorViewModel(container, route.noteId, route.folderId) }
    val editor = viewModel.editor
    val allTags by viewModel.allTags.collectAsStateWithLifecycle()
    val folders by viewModel.folders.collectAsStateWithLifecycle()

    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { viewModel.flush() }

    val pickPhotos = rememberLauncherForActivityResult(PickMultipleVisualMedia(MAX_PHOTOS_PER_PICK)) {
        viewModel.addPhotos(it)
    }
    val launchPicker = { pickPhotos.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly)) }
    val failedImport = stringResource(R.string.editor_photo_import_failed)
    LaunchedEffect(viewModel.failedImports) {
        if (viewModel.failedImports > 0) {
            Toast.makeText(context, failedImport, Toast.LENGTH_SHORT).show()
            viewModel.consumeImportError()
        }
    }

    var openedPhotos by remember { mutableStateOf<Pair<List<String>, Int>?>(null) }
    var pickingFolder by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val keyboardVisible = WindowInsets.isImeVisible
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    // В шапке — папка заметки, чтобы было видно, где она лежит
    val folderName = folders.firstOrNull { it.id == viewModel.folderId }?.name
        ?: stringResource(R.string.notes_no_folder)

    ScreenScaffold(
        title = folderName,
        modifier = modifier,
        onBack = onBack,
        actions = {
            IconButton(onClick = viewModel::togglePinned) {
                Icon(
                    imageVector = if (viewModel.pinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                    contentDescription = stringResource(if (viewModel.pinned) R.string.notes_unpin else R.string.notes_pin),
                    tint = if (viewModel.pinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = { pickingFolder = true }) {
                Icon(Icons.AutoMirrored.Outlined.DriveFileMove, stringResource(R.string.notes_move_to_folder))
            }
            IconButton(onClick = { confirmDelete = true }) {
                Icon(Icons.Outlined.Delete, stringResource(R.string.notes_delete))
            }
        },
    ) { padding ->
        val bottomSpace = if (keyboardVisible) EditorToolbarHeight + 24.dp else padding.calculateBottomPadding() + 96.dp

        Box(Modifier.fillMaxSize()) {
            if (viewModel.isLoaded) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .imePadding()
                        .verticalScroll(rememberScrollState())
                        .padding(top = padding.calculateTopPadding())
                        .padding(horizontal = 20.dp),
                ) {
                    TitleField(
                        value = viewModel.title,
                        onChange = { viewModel.title = it },
                        modifier = Modifier.padding(top = 8.dp),
                    )
                    TagChips(
                        tags = viewModel.tags,
                        allTags = allTags,
                        onRemove = viewModel::removeTag,
                        onToggle = viewModel::toggleTag,
                        onCreate = viewModel::addTag,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                    HorizontalDivider(Modifier.padding(top = 12.dp, bottom = 16.dp))
                    RichEditor(
                        state = editor,
                        photoFile = container.photoStorage::file,
                        onOpenPhotos = { ids, index -> openedPhotos = ids to index },
                        placeholder = stringResource(R.string.notes_text_hint),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    // Пустое место под текстом: тап ставит курсор в конец заметки
                    Spacer(
                        Modifier
                            .fillMaxWidth()
                            .height(bottomSpace)
                            .clickable(interactionSource = null, indication = null) { editor.focusEnd() },
                    )
                }
            }

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
                visible = keyboardVisible && editor.hasFocus,
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

    if (pickingFolder) {
        FolderPickerDialog(
            folders = folders,
            current = viewModel.folderId,
            onPick = {
                viewModel.moveToFolder(it)
                pickingFolder = false
            },
            onDismiss = { pickingFolder = false },
        )
    }
    if (confirmDelete) {
        ConfirmDialog(
            title = stringResource(R.string.notes_delete_title),
            text = stringResource(R.string.notes_delete_text),
            confirmLabel = stringResource(R.string.notes_delete),
            onConfirm = { viewModel.delete(onBack) },
            onDismiss = { confirmDelete = false },
        )
    }
    openedPhotos?.let { (ids, index) ->
        PhotoViewer(
            files = ids.map(container.photoStorage::file),
            initialIndex = index,
            onDismiss = { openedPhotos = null },
        )
    }
}

/** Крупное поле заголовка без рамки. */
@Composable
private fun TitleField(value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    val style = MaterialTheme.typography.headlineSmall.copy(
        color = MaterialTheme.colorScheme.onSurface,
        fontWeight = FontWeight.Bold,
    )
    BasicTextField(
        value = value,
        onValueChange = onChange,
        textStyle = style,
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next),
        modifier = modifier.fillMaxWidth(),
        decorationBox = { inner ->
            Box {
                if (value.isEmpty()) {
                    Text(stringResource(R.string.notes_title_hint), style = style.copy(color = MaterialTheme.colorScheme.outline))
                }
                inner()
            }
        },
    )
}
