package dev.chaik.remember.feature.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Cake
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import dev.chaik.remember.R
import dev.chaik.remember.appContainer
import dev.chaik.remember.core.model.Event
import dev.chaik.remember.core.model.Repeat
import dev.chaik.remember.core.reminders.OpenTarget
import dev.chaik.remember.core.ui.components.ScreenScaffold
import dev.chaik.remember.core.ui.components.label
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.WeekFields
import java.util.Locale

@Serializable
data object CalendarRoute : NavKey

/** Сколько месяцев можно пролистать в обе стороны от текущего — по 50 лет. */
private const val MONTH_PAGES = 1201
private const val CURRENT_PAGE = MONTH_PAGES / 2

/**
 * Календарь событий: большая сетка месяца, под ней — события выбранного дня
 * и кнопка «Добавить событие». Сразу выбран сегодняшний день.
 */
@Composable
fun CalendarScreen(
    onOpen: (eventId: Long) -> Unit,
    onCreate: (date: LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val container = LocalContext.current.appContainer
    val viewModel = viewModel { CalendarViewModel(container.eventRepository) }
    val events by viewModel.events.collectAsStateWithLifecycle()

    val today = LocalDate.now()
    val currentMonth = remember { YearMonth.from(today) }
    val firstDayOfWeek = remember { WeekFields.of(Locale.getDefault()).firstDayOfWeek }
    var selectedDay by rememberSaveable { mutableLongStateOf(today.toEpochDay()) }
    val selected = LocalDate.ofEpochDay(selectedDay)

    val pager = rememberPagerState(initialPage = CURRENT_PAGE) { MONTH_PAGES }
    val monthOf = { page: Int -> currentMonth.plusMonths((page - CURRENT_PAGE).toLong()) }
    val pageOf = { month: YearMonth -> CURRENT_PAGE + ChronoUnit.MONTHS.between(currentMonth, month).toInt() }
    val scope = rememberCoroutineScope()

    // Пролистали на другой месяц — выбираем в нём сегодня или 1-е число,
    // чтобы список под календарём не показывал день из другого месяца
    LaunchedEffect(pager) {
        snapshotFlow { pager.settledPage }.collect { page ->
            val month = monthOf(page)
            if (YearMonth.from(LocalDate.ofEpochDay(selectedDay)) != month) {
                selectedDay = (if (month == YearMonth.from(today)) today else month.atDay(1)).toEpochDay()
            }
        }
    }

    val select: (LocalDate) -> Unit = { date ->
        selectedDay = date.toEpochDay()
        // Тап по дню соседнего месяца — переезжаем на его месяц
        val page = pageOf(YearMonth.from(date))
        if (page != pager.currentPage) scope.launch { pager.animateScrollToPage(page) }
    }

    // Открыли из уведомления о событии или из сводки — показываем его день
    val openRequest by container.openRequests.collectAsStateWithLifecycle()
    LaunchedEffect(openRequest) {
        val target = openRequest as? OpenTarget.Calendar ?: return@LaunchedEffect
        select(target.date)
        container.openRequests.value = null
    }

    val dayEvents = remember(events, selected) { events?.let { eventsOn(it, selected) } }

    ScreenScaffold(
        title = stringResource(R.string.tab_calendar),
        modifier = modifier,
        actions = {
            if (selected != today || pager.currentPage != CURRENT_PAGE) {
                IconButton(onClick = { select(today) }) {
                    Icon(Icons.Outlined.Today, stringResource(R.string.calendar_today))
                }
            }
        },
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding() + 24.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            item(key = "month") {
                MonthCalendar(
                    pager = pager,
                    monthOf = monthOf,
                    events = events.orEmpty(),
                    today = today,
                    selected = selected,
                    firstDayOfWeek = firstDayOfWeek,
                    onSelect = select,
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
            }
            item(key = "day") { DayHeader(selected, today) }
            when {
                dayEvents == null -> Unit
                dayEvents.isEmpty() -> item(key = "empty") {
                    Text(
                        text = stringResource(R.string.calendar_no_events),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                            .animateItem(),
                    )
                }
                else -> items(dayEvents, key = { it.id }) { event ->
                    EventRow(
                        event = event,
                        day = selected,
                        onClick = { onOpen(event.id) },
                        modifier = Modifier.animateItem(),
                    )
                }
            }
            item(key = "add") {
                FilledTonalButton(
                    onClick = { onCreate(selected) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .height(52.dp)
                        .animateItem(),
                ) {
                    Icon(Icons.Outlined.Add, null, Modifier.size(20.dp))
                    Text(stringResource(R.string.calendar_add_event), modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
    }
}

/** «Пятница, 23 октября» или «Сегодня, 23 октября». */
@Composable
private fun DayHeader(day: LocalDate, today: LocalDate) {
    val locale = Locale.getDefault()
    val text = if (day == today) {
        stringResource(R.string.calendar_today_day, day.format(DateTimeFormatter.ofPattern("d MMMM", locale)))
    } else {
        val pattern = if (day.year == today.year) "EEEE, d MMMM" else "EEEE, d MMMM yyyy"
        day.format(DateTimeFormatter.ofPattern(pattern, locale)).replaceFirstChar { it.titlecase() }
    }
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 8.dp),
    )
}

/** Событие в списке дня: цветная полоска, название, время и повтор, описание. */
@Composable
private fun EventRow(event: Event, day: LocalDate, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val color = event.color.color()
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val meta = buildList {
        add(event.time.toString())
        if (event.isBirthday) {
            event.ageOn(day)?.let { add(stringResource(R.string.event_turns_on_day, yearsLabel(it))) }
        } else if (event.repeat != Repeat.None) {
            add(event.repeat.label())
        }
    }.joinToString(" · ")

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .clickable(onClick = onClick)
            .padding(12.dp),
    ) {
        Box(
            Modifier
                .width(5.dp)
                .height(40.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(color),
        )
        Column(Modifier.weight(1f).padding(start = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (event.isBirthday) {
                    Icon(Icons.Outlined.Cake, null, tint = color, modifier = Modifier.size(18.dp).padding(end = 2.dp))
                }
                Text(
                    text = event.title.ifBlank { stringResource(R.string.event_untitled) },
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = if (event.isBirthday) 4.dp else 0.dp),
                )
            }
            Text(meta, style = MaterialTheme.typography.bodySmall, color = muted)
            if (event.description.isNotBlank()) {
                Text(
                    text = event.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = muted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
