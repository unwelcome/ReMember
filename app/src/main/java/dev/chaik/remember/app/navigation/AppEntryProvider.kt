package dev.chaik.remember.app.navigation

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import dev.chaik.remember.feature.calendar.CalendarRoute
import dev.chaik.remember.feature.calendar.CalendarScreen
import dev.chaik.remember.feature.calendar.editor.EventEditorRoute
import dev.chaik.remember.feature.calendar.editor.EventEditorScreen
import dev.chaik.remember.feature.feed.FeedRoute
import dev.chaik.remember.feature.feed.FeedScreen
import dev.chaik.remember.feature.finance.FinanceRoute
import dev.chaik.remember.feature.finance.FinanceScreen
import dev.chaik.remember.feature.gym.GymRoute
import dev.chaik.remember.feature.gym.GymScreen
import dev.chaik.remember.feature.gym.workout.WorkoutRoute
import dev.chaik.remember.feature.gym.workout.WorkoutScreen
import dev.chaik.remember.feature.health.HealthRoute
import dev.chaik.remember.feature.health.HealthScreen
import dev.chaik.remember.feature.more.MoreRoute
import dev.chaik.remember.feature.more.MoreScreen
import dev.chaik.remember.feature.notes.NotesRoute
import dev.chaik.remember.feature.notes.NotesScreen
import dev.chaik.remember.feature.notes.editor.NoteEditorRoute
import dev.chaik.remember.feature.notes.editor.NoteEditorScreen
import dev.chaik.remember.feature.search.SearchRoute
import dev.chaik.remember.feature.search.SearchScreen
import dev.chaik.remember.feature.settings.SettingsRoute
import dev.chaik.remember.feature.settings.SettingsScreen
import dev.chaik.remember.feature.settings.appearance.PersonalizationRoute
import dev.chaik.remember.feature.settings.appearance.PersonalizationScreen
import dev.chaik.remember.feature.settings.pages.PagesRoute
import dev.chaik.remember.feature.settings.pages.PagesScreen
import dev.chaik.remember.feature.settings.profile.ProfileRoute
import dev.chaik.remember.feature.settings.profile.ProfileScreen
import dev.chaik.remember.feature.settings.reminders.RemindersRoute
import dev.chaik.remember.feature.settings.reminders.RemindersScreen
import dev.chaik.remember.feature.stats.StatsRoute
import dev.chaik.remember.feature.stats.StatsScreen
import dev.chaik.remember.feature.tasks.TasksRoute
import dev.chaik.remember.feature.tasks.TasksScreen
import dev.chaik.remember.feature.tasks.editor.TaskEditorRoute
import dev.chaik.remember.feature.tasks.editor.TaskEditorScreen
import dev.chaik.remember.feature.today.TodayRoute
import dev.chaik.remember.feature.today.TodayScreen

/** Связывает маршруты с экранами. Новый экран = новая строка здесь. */
fun appEntryProvider(backStack: NavBackStack<NavKey>): (NavKey) -> NavEntry<NavKey> {
    val back: () -> Unit = { backStack.removeLastOrNull() }

    /** «Назад» для раздела: у корня вкладки его нет, внутри «Ещё» — есть. */
    fun backFor(key: NavKey): (() -> Unit)? = if (backStack.firstOrNull() == key) null else back

    return entryProvider {
        // Корни вкладок
        entry<TodayRoute> { TodayScreen(onBack = backFor(it)) }
        entry<NotesRoute> {
            NotesScreen(
                onBack = backFor(it),
                onOpen = { backStack.add(NoteEditorRoute(noteId = it)) },
                onCreate = { backStack.add(NoteEditorRoute(folderId = it)) },
            )
        }
        entry<TasksRoute> { TasksScreen(onBack = backFor(it), onOpen = { backStack.add(TaskEditorRoute(it)) }) }
        entry<CalendarRoute> {
            CalendarScreen(
                onBack = backFor(it),
                onOpen = { backStack.add(EventEditorRoute(eventId = it)) },
                onCreate = { backStack.add(EventEditorRoute(date = it.toEpochDay())) },
            )
        }
        entry<MoreRoute> { MoreScreen(onOpen = { backStack.add(it) }) }

        // Вложенные страницы вкладок
        entry<NoteEditorRoute> { NoteEditorScreen(route = it, onBack = back) }
        entry<TaskEditorRoute> { TaskEditorScreen(route = it, onBack = back) }
        entry<EventEditorRoute> { EventEditorScreen(route = it, onBack = back) }

        // Разделы из «Ещё»
        entry<FeedRoute> { FeedScreen(onBack = backFor(it)) }
        entry<SearchRoute> { SearchScreen(onBack = backFor(it)) }
        entry<StatsRoute> { StatsScreen(onBack = backFor(it)) }
        entry<FinanceRoute> { FinanceScreen(onBack = backFor(it)) }
        entry<HealthRoute> { HealthScreen(onBack = backFor(it)) }
        entry<GymRoute> { GymScreen(onBack = backFor(it), onOpen = { backStack.add(WorkoutRoute(it)) }) }
        entry<WorkoutRoute> { WorkoutScreen(route = it, onBack = back) }
        entry<SettingsRoute> { SettingsScreen(onBack = back, onOpen = { backStack.add(it) }) }
        entry<RemindersRoute> { RemindersScreen(onBack = back) }
        entry<ProfileRoute> { ProfileScreen(onBack = back) }
        entry<PagesRoute> { PagesScreen(onBack = back) }
        entry<PersonalizationRoute> { PersonalizationScreen(onBack = back) }
    }
}
