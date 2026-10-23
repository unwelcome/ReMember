package dev.chaik.remember.feature.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.chaik.remember.core.data.EventRepository
import dev.chaik.remember.core.model.Event
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/** Календарь: все события в памяти, раскладка по дням — на экране. */
class CalendarViewModel(events: EventRepository) : ViewModel() {

    /** null — ещё грузится. */
    val events: StateFlow<List<Event>?> = events.observeAll().stateIn(viewModelScope, SharingStarted.Lazily, null)
}
