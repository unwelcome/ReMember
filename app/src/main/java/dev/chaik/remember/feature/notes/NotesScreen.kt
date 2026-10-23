package dev.chaik.remember.feature.notes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import dev.chaik.remember.R
import dev.chaik.remember.appContainer
import dev.chaik.remember.core.model.Note
import dev.chaik.remember.core.ui.components.ScreenScaffold
import kotlinx.serialization.Serializable
import java.io.File

@Serializable
data object NotesRoute : NavKey

/**
 * Заметки: папки и теги-фильтры сверху, карточки в две колонки
 * (закреплённые — отдельным блоком), «+» — новая заметка в открытой папке.
 */
@Composable
fun NotesScreen(
    onOpen: (noteId: Long) -> Unit,
    onCreate: (folderId: Long?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val container = LocalContext.current.appContainer
    val viewModel = viewModel { NotesViewModel(container.noteRepository) }
    val folders by viewModel.folders.collectAsStateWithLifecycle()
    val tags by viewModel.usedTags.collectAsStateWithLifecycle()
    val notes by viewModel.visibleNotes.collectAsStateWithLifecycle()

    var moving by remember { mutableStateOf<Note?>(null) }
    var deleting by remember { mutableStateOf<Note?>(null) }

    ScreenScaffold(title = stringResource(R.string.tab_notes), modifier = modifier) { padding ->
        Box(Modifier.fillMaxSize()) {
            Column(Modifier.padding(top = padding.calculateTopPadding())) {
                // Блок фильтров: с отступами сверху и снизу и тонкой чертой — отделён от прокручиваемых карточек
                Column(Modifier.padding(top = 8.dp, bottom = 10.dp)) {
                    NoteFolderChips(
                        folders = folders,
                        selected = viewModel.folderFilter,
                        onSelect = viewModel::selectFolder,
                        onCreate = viewModel::createFolder,
                        onRename = viewModel::renameFolder,
                        onDelete = viewModel::deleteFolder,
                    )
                    if (tags.isNotEmpty()) {
                        NoteTagFilter(
                            tags = tags,
                            selected = viewModel.tagFilter,
                            onToggle = viewModel::toggleTagFilter,
                            onClear = viewModel::clearTagFilter,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                val list = notes
                when {
                    list == null -> Unit // ещё грузится
                    list.isEmpty() -> EmptyNotes(Modifier.fillMaxWidth().padding(32.dp))
                    else -> NotesGrid(
                        notes = list,
                        // В «Все» на карточке видно, в какой папке заметка
                        folderNames = if (viewModel.folderFilter == FolderFilter.All) {
                            folders.associate { it.id to it.name }
                        } else {
                            emptyMap()
                        },
                        thumbnail = container.photoStorage::thumbnailFile,
                        bottomPadding = padding.calculateBottomPadding(),
                        onOpen = onOpen,
                        onTogglePin = viewModel::togglePinned,
                        onMove = { moving = it },
                        onDelete = { deleting = it },
                    )
                }
            }

            FloatingActionButton(
                onClick = { onCreate(viewModel.currentFolderId) },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 20.dp, bottom = padding.calculateBottomPadding()),
            ) {
                Icon(Icons.Outlined.Add, stringResource(R.string.notes_new))
            }
        }
    }

    moving?.let { note ->
        FolderPickerDialog(
            folders = folders,
            current = note.folderId,
            onPick = {
                viewModel.moveToFolder(note, it)
                moving = null
            },
            onDismiss = { moving = null },
        )
    }
    deleting?.let { note ->
        ConfirmDialog(
            title = stringResource(R.string.notes_delete_title),
            text = stringResource(R.string.notes_delete_text),
            confirmLabel = stringResource(R.string.notes_delete),
            onConfirm = { viewModel.delete(note) },
            onDismiss = { deleting = null },
        )
    }
}

@Composable
private fun NotesGrid(
    notes: List<Note>,
    folderNames: Map<Long, String>,
    thumbnail: (String) -> File,
    bottomPadding: Dp,
    onOpen: (Long) -> Unit,
    onTogglePin: (Note) -> Unit,
    onMove: (Note) -> Unit,
    onDelete: (Note) -> Unit,
) {
    val (pinned, others) = notes.partition { it.pinned }

    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(2),
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = bottomPadding + 88.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalItemSpacing = 8.dp,
        modifier = Modifier.fillMaxSize(),
    ) {
        if (pinned.isNotEmpty()) {
            item(span = StaggeredGridItemSpan.FullLine, key = "pinnedHeader") {
                SectionTitle(stringResource(R.string.notes_pinned))
            }
            // Ключи разные для блоков: при закреплении карточка не «переезжает», а появляется заново —
            // с одним ключом staggered-сетка оставляла блок «Закреплённые» пустым до перерисовки
            items(pinned, key = { "pinned-${it.id}" }) { note ->
                NoteCard(note, note.folderId?.let(folderNames::get), thumbnail, { onOpen(note.id) }, { onTogglePin(note) }, { onMove(note) }, { onDelete(note) })
            }
            if (others.isNotEmpty()) {
                item(span = StaggeredGridItemSpan.FullLine, key = "othersHeader") {
                    SectionTitle(stringResource(R.string.notes_others))
                }
            }
        }
        items(others, key = { "other-${it.id}" }) { note ->
            NoteCard(note, note.folderId?.let(folderNames::get), thumbnail, { onOpen(note.id) }, { onTogglePin(note) }, { onMove(note) }, { onDelete(note) })
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 4.dp, top = 4.dp),
    )
}

@Composable
private fun EmptyNotes(modifier: Modifier = Modifier) {
    Text(
        text = stringResource(R.string.notes_empty),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.outline,
        textAlign = TextAlign.Center,
        modifier = modifier,
    )
}
