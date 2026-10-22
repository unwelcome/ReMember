package dev.chaik.remember.app.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation3.runtime.NavKey
import dev.chaik.remember.R
import dev.chaik.remember.feature.calendar.CalendarRoute
import dev.chaik.remember.feature.more.MoreRoute
import dev.chaik.remember.feature.notes.NotesRoute
import dev.chaik.remember.feature.tasks.TasksRoute
import dev.chaik.remember.feature.today.TodayRoute

/** Вкладки островка. У каждой свой стек экранов, [root] — его корень. */
enum class TopLevelTab(
    @param:StringRes val label: Int,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
    val root: NavKey,
) {
    Today(R.string.tab_today, Icons.Outlined.WbSunny, Icons.Filled.WbSunny, TodayRoute),
    Notes(R.string.tab_notes, Icons.Outlined.Description, Icons.Filled.Description, NotesRoute),
    Tasks(R.string.tab_tasks, Icons.Outlined.TaskAlt, Icons.Filled.TaskAlt, TasksRoute),
    Calendar(R.string.tab_calendar, Icons.Outlined.CalendarMonth, Icons.Filled.CalendarMonth, CalendarRoute),
    More(R.string.tab_more, Icons.Outlined.Menu, Icons.Filled.Menu, MoreRoute),
}
