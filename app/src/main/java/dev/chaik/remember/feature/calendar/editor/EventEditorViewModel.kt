package dev.chaik.remember.feature.calendar.editor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.chaik.remember.app.AppContainer
import dev.chaik.remember.core.model.Event
import dev.chaik.remember.core.model.EventColor
import dev.chaik.remember.core.model.Repeat
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate

/**
 * Страница события. Сохраняется сама — через [AUTOSAVE_DELAY_MS] после изменения
 * и при закрытии экрана. Новое событие появляется в базе, когда у него есть название.
 *
 * @param eventId 0 — новое событие
 * @param newDate день нового события
 */
class EventEditorViewModel(
    private val container: AppContainer,
    eventId: Long,
    newDate: LocalDate,
) : ViewModel() {
    private val events = container.eventRepository

    var title by mutableStateOf("")
    var description by mutableStateOf("")
    var date by mutableStateOf(newDate)
    var time by mutableStateOf(Event.DEFAULT_TIME)
    var color by mutableStateOf(EventColor.DEFAULT)
    var repeat by mutableStateOf<Repeat>(Repeat.None)
    var remindMinutes by mutableStateOf<Int?>(null)
    var isBirthday by mutableStateOf(false)
        private set

    /** Год рождения как введён: проверяется при сохранении. */
    var birthYear by mutableStateOf("")

    var isLoaded by mutableStateOf(false)
        private set

    val isNew = eventId == 0L

    private var id = eventId
    private var deleted = false
    private var saved: Event? = null
    private val lock = Mutex()

    init {
        viewModelScope.launch {
            if (eventId != 0L) {
                events.get(eventId)?.let { event ->
                    title = event.title
                    description = event.description
                    date = event.date
                    time = event.time
                    color = event.color
                    repeat = event.repeat
                    remindMinutes = event.remindMinutes
                    isBirthday = event.isBirthday
                    birthYear = event.birthYear?.toString().orEmpty()
                }
            }
            saved = snapshot()
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

    /** День рождения по умолчанию красный, но цвет потом можно сменить. */
    fun switchBirthday(on: Boolean) {
        isBirthday = on
        color = when {
            on -> EventColor.BIRTHDAY
            color == EventColor.BIRTHDAY -> EventColor.DEFAULT
            else -> color
        }
    }

    /** Год рождения, если введён правдоподобный. */
    fun validBirthYear(): Int? = birthYear.toIntOrNull()?.takeIf { it in MIN_BIRTH_YEAR..LocalDate.now().year }

    fun delete(onDone: () -> Unit) {
        deleted = true
        val eventId = id
        container.appScope.launch { if (eventId != 0L) events.delete(eventId) }
        onDone()
    }

    fun flush() {
        if (!isLoaded) return
        val snapshot = snapshot()
        container.appScope.launch { save(snapshot) }
    }

    override fun onCleared() = flush()

    // Без id: после первого сохранения у события появляется id, а сравнивать нужно только поля
    private fun snapshot() = Event(
        title = title,
        description = description,
        date = date,
        time = time,
        color = color,
        repeat = if (isBirthday) Repeat.Yearly else repeat,
        remindMinutes = remindMinutes,
        isBirthday = isBirthday,
        birthYear = if (isBirthday) validBirthYear() else null,
    )

    private suspend fun save(event: Event) = lock.withLock {
        if (deleted || !isLoaded || event == saved) return@withLock
        // Пустое новое событие не создаём
        if (id == 0L && event.title.isBlank()) return@withLock
        id = events.save(event.copy(id = id, title = event.title.trim(), description = event.description.trim()))
        saved = event
    }

    private companion object {
        const val AUTOSAVE_DELAY_MS = 500L
        const val MIN_BIRTH_YEAR = 1900
    }
}
