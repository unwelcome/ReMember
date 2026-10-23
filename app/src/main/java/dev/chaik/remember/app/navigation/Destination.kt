package dev.chaik.remember.app.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.outlined.ViewAgenda
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation3.runtime.NavKey
import dev.chaik.remember.R
import dev.chaik.remember.core.settings.AppSettings
import dev.chaik.remember.feature.calendar.CalendarRoute
import dev.chaik.remember.feature.feed.FeedRoute
import dev.chaik.remember.feature.finance.FinanceRoute
import dev.chaik.remember.feature.gym.GymRoute
import dev.chaik.remember.feature.health.HealthRoute
import dev.chaik.remember.feature.more.MoreRoute
import dev.chaik.remember.feature.notes.NotesRoute
import dev.chaik.remember.feature.search.SearchRoute
import dev.chaik.remember.feature.settings.SettingsRoute
import dev.chaik.remember.feature.stats.StatsRoute
import dev.chaik.remember.feature.tasks.TasksRoute
import dev.chaik.remember.feature.today.TodayRoute

/**
 * Разделы приложения. Любой (кроме «Ещё» и «Настроек») можно поставить вкладкой островка
 * или оставить плиткой на странице «Ещё» — см. Настройки → Страницы.
 * Имя константы хранится в настройках, поэтому переименовывать константы нельзя.
 */
enum class Destination(
    @param:StringRes val label: Int,
    @param:StringRes val hint: Int,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
    val root: NavKey,
) {
    Today(R.string.tab_today, R.string.section_today_hint, Icons.Outlined.WbSunny, Icons.Filled.WbSunny, TodayRoute),
    Notes(R.string.tab_notes, R.string.section_notes_hint, Icons.Outlined.Description, Icons.Filled.Description, NotesRoute),
    Tasks(R.string.tab_tasks, R.string.section_tasks_hint, Icons.Outlined.TaskAlt, Icons.Filled.TaskAlt, TasksRoute),
    Calendar(R.string.tab_calendar, R.string.section_calendar_hint, Icons.Outlined.CalendarMonth, Icons.Filled.CalendarMonth, CalendarRoute),
    Feed(R.string.section_feed, R.string.section_feed_hint, Icons.Outlined.ViewAgenda, Icons.Filled.ViewAgenda, FeedRoute),
    Search(R.string.section_search, R.string.section_search_hint, Icons.Outlined.Search, Icons.Filled.Search, SearchRoute),
    Stats(R.string.section_stats, R.string.section_stats_hint, Icons.Outlined.Insights, Icons.Filled.Insights, StatsRoute),
    Finance(R.string.section_finance, R.string.section_finance_hint, Icons.Outlined.AccountBalanceWallet, Icons.Filled.AccountBalanceWallet, FinanceRoute),
    Health(R.string.section_health, R.string.section_health_hint, Icons.Outlined.MonitorHeart, Icons.Filled.MonitorHeart, HealthRoute),
    Gym(R.string.section_gym, R.string.section_gym_hint, Icons.Outlined.FitnessCenter, Icons.Filled.FitnessCenter, GymRoute),

    /** Всегда последняя плитка «Ещё», во вкладки не ставится. */
    Settings(R.string.section_settings, R.string.section_settings_hint, Icons.Outlined.Settings, Icons.Filled.Settings, SettingsRoute),

    /** Всегда последняя вкладка островка. */
    More(R.string.tab_more, R.string.tab_more, Icons.Outlined.Menu, Icons.Filled.Menu, MoreRoute),
    ;

    /** Можно ли поставить раздел вкладкой. */
    val movable: Boolean get() = this != Settings && this != More

    companion object {
        /** Вкладок островка кроме «Ещё»: от 2 до 4 (всего с «Ещё» — от 3 до 5). */
        const val MIN_TABS = 2
        const val MAX_TABS = 4

        val DefaultTabs = listOf(Today, Notes, Tasks, Calendar)

        /** Порядок плиток «Ещё» по умолчанию; разделы из островка в «Ещё» не показываются. */
        val DefaultMoreOrder = listOf(Feed, Search, Stats, Finance, Health, Gym, Today, Notes, Tasks, Calendar)
    }
}

/** Вкладки островка по настройкам, «Ещё» — последней. Неверные настройки — по умолчанию. */
fun AppSettings.islandTabs(): List<Destination> {
    val chosen = islandTabs.mapNotNull { name -> Destination.entries.firstOrNull { it.name == name } }
        .filter { it.movable }
        .distinct()
    val tabs = if (chosen.size in Destination.MIN_TABS..Destination.MAX_TABS) chosen else Destination.DefaultTabs
    return tabs + Destination.More
}

/** Плитки «Ещё»: все разделы, которых нет в островке, в выбранном порядке; «Настройки» — последней. */
fun AppSettings.moreTiles(): List<Destination> {
    val island = islandTabs()
    val saved = moreOrder.mapNotNull { name -> Destination.entries.firstOrNull { it.name == name } }
    // Разделы, которых нет в сохранённом порядке (появились позже), — в конец, как по умолчанию
    val order = (saved + Destination.DefaultMoreOrder).distinct().filter { it.movable }
    return order.filter { it !in island } + Destination.Settings
}
