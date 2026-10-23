package dev.chaik.remember.feature.today

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.chaik.remember.app.AppContainer
import dev.chaik.remember.feature.today.day.DayController
import dev.chaik.remember.feature.today.money.MoneyController
import dev.chaik.remember.feature.today.sleep.SleepController
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.time.LocalDate

/**
 * Страница «Сегодня»: выбранный день и режим (день / сон / траты).
 * Каждый режим — свой контроллер со своим сохранением.
 */
class TodayViewModel(container: AppContainer) : ViewModel() {

    var date by mutableStateOf(LocalDate.now())
        private set

    var mode by mutableStateOf(TodayMode.Day)
        private set

    val day = DayController(viewModelScope, container.dayRepository, container.tagRepository, container.photoStorage)
    val sleep = SleepController(viewModelScope, container.sleepRepository)
    val money = MoneyController(viewModelScope, container.moneyRepository)

    val isToday: Boolean get() = date == LocalDate.now()

    // Смена дня: сначала сохранить старый, потом загрузить новый — строго по очереди
    private val switchLock = Mutex()

    init {
        load(date)
    }

    fun selectDate(date: LocalDate) {
        if (date == this.date) return
        this.date = date
        load(date)
    }

    fun nextMode() {
        mode = mode.next()
    }

    /** Сохранить всё сейчас (приложение уходит в фон). */
    fun flush() {
        viewModelScope.launch {
            withContext(NonCancellable) {
                day.flushNow()
                sleep.flushNow()
            }
        }
    }

    private fun load(date: LocalDate) {
        money.setDate(date)
        viewModelScope.launch {
            switchLock.withLock {
                day.load(date)
                sleep.load(date)
            }
        }
    }
}
