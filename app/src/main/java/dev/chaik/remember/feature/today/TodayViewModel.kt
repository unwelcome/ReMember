package dev.chaik.remember.feature.today

import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.chaik.remember.core.data.DayRepository
import dev.chaik.remember.core.editor.EditorState
import dev.chaik.remember.core.model.DayEntry
import dev.chaik.remember.core.photos.PhotoStorage
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.time.LocalDate

/**
 * Запись дня: загрузка, автосохранение, добавление фото.
 *
 * Сохраняется сама — через [AUTOSAVE_DELAY_MS] после последнего изменения
 * и сразу, когда приложение уходит в фон ([flush]).
 */
class TodayViewModel(
    private val days: DayRepository,
    private val photos: PhotoStorage,
    val date: LocalDate = LocalDate.now(),
) : ViewModel() {

    val editor = EditorState()

    var isLoaded by mutableStateOf(false)
        private set

    var isImporting by mutableStateOf(false)
        private set

    /** Сколько фото не удалось добавить в последний раз (для сообщения). */
    var failedImports by mutableStateOf(0)
        private set

    private var entry: DayEntry? = null
    private var savedMarkdown = ""
    private val saveLock = Mutex()

    init {
        viewModelScope.launch {
            entry = days.observe(date).first()
            savedMarkdown = entry?.text.orEmpty()
            editor.load(savedMarkdown)
            editor.applyPhotoSizes(photos.photos(editor.photoIds))
            isLoaded = true
            autosave()
        }
    }

    @OptIn(FlowPreview::class)
    private suspend fun autosave() {
        snapshotFlow { editor.markdown }
            .debounce(AUTOSAVE_DELAY_MS)
            .collect { save(it) }
    }

    /** Сохранить сейчас, не дожидаясь паузы в наборе. */
    fun flush() {
        viewModelScope.launch {
            // Даже если экран закрывается прямо сейчас — дописать до конца
            withContext(NonCancellable) { save(editor.markdown) }
        }
    }

    fun addPhotos(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            isImporting = true
            val imported = uris.mapNotNull { uri ->
                runCatching { photos.import(uri) }
                    .onFailure { Log.w(TAG, "Не удалось добавить фото $uri", it) }
                    .getOrNull()
            }
            failedImports = uris.size - imported.size
            editor.insertPhotos(imported)
            isImporting = false
        }
    }

    fun consumeImportError() {
        failedImports = 0
    }

    private suspend fun save(markdown: String): Unit = saveLock.withLock {
        if (!isLoaded || markdown == savedMarkdown) return
        // Пустую запись не создаём: открыть «Сегодня» и ничего не написать — не запись
        val base = entry ?: if (markdown.isBlank()) return else DayEntry(date = date)
        val updated = base.copy(text = markdown)
        days.save(updated)
        entry = updated
        savedMarkdown = markdown
    }

    private companion object {
        const val TAG = "TodayViewModel"
        const val AUTOSAVE_DELAY_MS = 700L
    }
}
