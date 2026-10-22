package dev.chaik.remember.app.navigation

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import dev.chaik.remember.feature.calendar.CalendarRoute
import dev.chaik.remember.feature.calendar.CalendarScreen
import dev.chaik.remember.feature.feed.FeedRoute
import dev.chaik.remember.feature.feed.FeedScreen
import dev.chaik.remember.feature.finance.FinanceRoute
import dev.chaik.remember.feature.finance.FinanceScreen
import dev.chaik.remember.feature.gym.GymRoute
import dev.chaik.remember.feature.gym.GymScreen
import dev.chaik.remember.feature.health.HealthRoute
import dev.chaik.remember.feature.health.HealthScreen
import dev.chaik.remember.feature.more.MoreRoute
import dev.chaik.remember.feature.more.MoreScreen
import dev.chaik.remember.feature.notes.NotesRoute
import dev.chaik.remember.feature.notes.NotesScreen
import dev.chaik.remember.feature.search.SearchRoute
import dev.chaik.remember.feature.search.SearchScreen
import dev.chaik.remember.feature.settings.SettingsRoute
import dev.chaik.remember.feature.settings.SettingsScreen
import dev.chaik.remember.feature.stats.StatsRoute
import dev.chaik.remember.feature.stats.StatsScreen
import dev.chaik.remember.feature.tasks.TasksRoute
import dev.chaik.remember.feature.tasks.TasksScreen
import dev.chaik.remember.feature.today.TodayRoute
import dev.chaik.remember.feature.today.TodayScreen

/** Связывает маршруты с экранами. Новый экран = новая строка здесь. */
fun appEntryProvider(backStack: NavBackStack<NavKey>): (NavKey) -> NavEntry<NavKey> {
    val back: () -> Unit = { backStack.removeLastOrNull() }

    return entryProvider {
        // Корни вкладок
        entry<TodayRoute> { TodayScreen() }
        entry<NotesRoute> { NotesScreen() }
        entry<TasksRoute> { TasksScreen() }
        entry<CalendarRoute> { CalendarScreen() }
        entry<MoreRoute> { MoreScreen(onOpen = { backStack.add(it) }) }

        // Разделы из «Ещё»
        entry<FeedRoute> { FeedScreen(onBack = back) }
        entry<SearchRoute> { SearchScreen(onBack = back) }
        entry<StatsRoute> { StatsScreen(onBack = back) }
        entry<FinanceRoute> { FinanceScreen(onBack = back) }
        entry<HealthRoute> { HealthScreen(onBack = back) }
        entry<GymRoute> { GymScreen(onBack = back) }
        entry<SettingsRoute> { SettingsScreen(onBack = back) }
    }
}
