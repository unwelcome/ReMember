package dev.chaik.remember.feature.gym

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.chaik.remember.app.AppContainer
import dev.chaik.remember.core.data.ImportPreview
import dev.chaik.remember.core.data.WorkoutImport
import dev.chaik.remember.core.data.WorkoutImportException
import dev.chaik.remember.core.model.WorkoutSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/** Ход импорта тренировок из файла. */
sealed interface ImportState {
    data object Reading : ImportState
    data class Ready(val preview: ImportPreview) : ImportState
    data class Failed(val message: String) : ImportState
    data class Done(val count: Int) : ImportState
}

/** Список тренировок, их запуск и импорт. */
class GymViewModel(private val container: AppContainer) : ViewModel() {
    private val workouts = container.workoutRepository

    /** По хронологии; null — ещё грузится. */
    val summaries: StateFlow<List<WorkoutSummary>?> =
        workouts.observeSummaries().stateIn(viewModelScope, SharingStarted.Lazily, null)

    /** Прошлый вес — подставить в форму «до тренировки». */
    suspend fun lastBodyWeight(): Double? = workouts.lastBodyWeight()

    /** Начать с временем [startedAt] (момент нажатия, а не закрытия формы веса). */
    fun start(startedAt: LocalDateTime, weight: Double?, onStarted: (Long) -> Unit) {
        viewModelScope.launch { onStarted(workouts.start(startedAt, weight)) }
    }

    var importState by mutableStateOf<ImportState?>(null)
        private set

    /** Прочитать и разобрать файл, показать, что получится. */
    fun readImport(uri: Uri) {
        importState = ImportState.Reading
        viewModelScope.launch {
            importState = try {
                val text = withContext(Dispatchers.IO) {
                    container.appContext.contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() }
                } ?: throw WorkoutImportException("Файл не открылся")
                val parsed = withContext(Dispatchers.Default) { WorkoutImport.parse(text) }
                ImportState.Ready(workouts.previewImport(parsed))
            } catch (e: WorkoutImportException) {
                ImportState.Failed(e.message.orEmpty())
            } catch (e: IOException) {
                ImportState.Failed(e.message.orEmpty())
            }
        }
    }

    fun confirmImport() {
        val ready = importState as? ImportState.Ready ?: return
        importState = ImportState.Reading
        container.appScope.launch {
            val count = workouts.import(ready.preview.workouts)
            importState = ImportState.Done(count)
        }
    }

    fun dismissImport() {
        importState = null
    }

    fun delete(ids: Set<Long>) {
        // В фоне приложения — чтобы удаление доделалось, даже если экран сразу закрыли
        container.appScope.launch { ids.forEach { workouts.delete(it) } }
    }

    fun createPast(date: LocalDate, start: LocalTime?, end: LocalTime?, onCreated: (Long) -> Unit) {
        viewModelScope.launch { onCreated(workouts.createPast(date, start, end)) }
    }
}
