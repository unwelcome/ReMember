package dev.chaik.remember.feature.more

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.ViewAgenda
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation3.runtime.NavKey
import dev.chaik.remember.R
import dev.chaik.remember.feature.feed.FeedRoute
import dev.chaik.remember.feature.finance.FinanceRoute
import dev.chaik.remember.feature.gym.GymRoute
import dev.chaik.remember.feature.health.HealthRoute
import dev.chaik.remember.feature.search.SearchRoute
import dev.chaik.remember.feature.settings.SettingsRoute
import dev.chaik.remember.feature.stats.StatsRoute

/** Разделы, которые открываются со страницы «Ещё». Порядок = порядок плиток. */
enum class MoreSection(
    @param:StringRes val title: Int,
    @param:StringRes val hint: Int,
    val icon: ImageVector,
    val route: NavKey,
) {
    Feed(R.string.section_feed, R.string.section_feed_hint, Icons.Outlined.ViewAgenda, FeedRoute),
    Search(R.string.section_search, R.string.section_search_hint, Icons.Outlined.Search, SearchRoute),
    Stats(R.string.section_stats, R.string.section_stats_hint, Icons.Outlined.Insights, StatsRoute),
    Finance(R.string.section_finance, R.string.section_finance_hint, Icons.Outlined.AccountBalanceWallet, FinanceRoute),
    Health(R.string.section_health, R.string.section_health_hint, Icons.Outlined.MonitorHeart, HealthRoute),
    Gym(R.string.section_gym, R.string.section_gym_hint, Icons.Outlined.FitnessCenter, GymRoute),
    Settings(R.string.section_settings, R.string.section_settings_hint, Icons.Outlined.Settings, SettingsRoute),
}
