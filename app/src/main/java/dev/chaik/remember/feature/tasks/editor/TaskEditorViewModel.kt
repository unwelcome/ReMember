package dev.chaik.remember.feature.tasks.editor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.chaik.remember.app.AppContainer
import dev.chaik.remember.core.model.Subtask
import dev.chaik.remember.core.model.Task
import dev.chaik.remember.core.model.TaskPriority
import dev.chaik.remember.core.model.Repeat
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

/** Подзадача в редакторе: свой ключ для списка, поля меняются на месте. */
class EditableSubtask(val key: Long, title: String, done: Boolean) {
    var title by mutableStateOf(title)
    var done by mutableStateOf(done)
}

/**
 * Страница задачи. Сохраняется сама — через [AUTOSAVE_DELAY_MS] после изменения
 * и при закрытии экрана (в фоне приложения, даже если экран уже исчез).
 */
class TaskEditorViewModel(
    private val container: AppContainer,
    private val taskId: Long,
) : ViewModel() {
    private val tasks = container.taskRepository

    var title by mutableStateOf("")
    var notes by mutableStateOf("")
    var dueDate by mutableStateOf<LocalDate?>(null)
    var dueTime by mutableStateOf<LocalTime?>(null)
    var priority by mutableStateOf(TaskPriority.NONE)
    var repeat by mutableStateOf<Repeat>(Repeat.None)
    var done by mutableStateOf(false)
        private set
    val subtasks = mutableStateListOf<EditableSubtask>()

    var isLoaded by mutableStateOf(false)
        private set

    /** Ключ подзадачи, в которую надо поставить курсор (только что добавленная). */
    var focusSubtaskKey by mutableStateOf<Long?>(null)

    private var doneAt: Instant? = null
    private var nextKey = 0L
    private var deleted = false
    private var saved: Task? = null
    private val lock = Mutex()

    init {
        viewModelScope.launch {
            load()
            isLoaded = true
            autosave()
        }
    }

    @OptIn(FlowPreview::class)
    private suspend fun autosave() {
        snapshotFlow { snapshot() }
            .drop(1)
            .debounce(AUTOSAVE_DELAY_MS)
            .collect { save(it) }
    }

    fun addSubtask() {
        val subtask = EditableSubtask(nextKey++, "", false)
        subtasks += subtask
        focusSubtaskKey = subtask.key
    }

    fun removeSubtask(subtask: EditableSubtask) {
        subtasks.remove(subtask)
    }

    /** Выполнить / вернуть. Повторяющаяся задача переезжает на следующий срок — поля обновятся. */
    fun toggleDone() {
        viewModelScope.launch {
            val current = snapshot()
            save(current)
            lock.withLock { tasks.setDone(current, !current.done) }
            load()
        }
    }

    fun delete(onDone: () -> Unit) {
        deleted = true
        container.appScope.launch { tasks.delete(taskId) }
        onDone()
    }

    fun flush() {
        if (!isLoaded) return
        val snapshot = snapshot()
        container.appScope.launch { save(snapshot) }
    }

    override fun onCleared() = flush()

    private suspend fun load() = lock.withLock {
        val task = tasks.get(taskId) ?: return@withLock
        title = task.title
        notes = task.notes
        dueDate = task.dueDate
        dueTime = task.dueTime
        priority = task.priority
        repeat = task.repeat
        done = task.done
        doneAt = task.doneAt
        subtasks.clear()
        task.subtasks.forEach { subtasks += EditableSubtask(nextKey++, it.title, it.done) }
        saved = snapshot()
    }

    private fun snapshot() = Task(
        id = taskId,
        title = title,
        notes = notes,
        dueDate = dueDate,
        dueTime = dueTime.takeIf { dueDate != null },
        priority = priority,
        repeat = repeat,
        done = done,
        doneAt = doneAt,
        subtasks = subtasks.map { Subtask(title = it.title, done = it.done) },
    )

    private suspend fun save(task: Task) = lock.withLock {
        if (deleted || !isLoaded || task == saved) return@withLock
        // Пустые подзадачи (добавили и ничего не вписали) не храним
        tasks.save(task.copy(title = task.title.trim(), subtasks = task.subtasks.filter { it.title.isNotBlank() }))
        saved = task
    }

    private companion object {
        const val AUTOSAVE_DELAY_MS = 500L
    }
}
