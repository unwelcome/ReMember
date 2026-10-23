package dev.chaik.remember.core.settings

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.DayOfWeek

/** Настройки в SharedPreferences: читаются мгновенно при запуске, до открытия базы. */
class AppSettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val state = MutableStateFlow(read())

    val settings: StateFlow<AppSettings> = state.asStateFlow()

    fun update(transform: (AppSettings) -> AppSettings) {
        val next = transform(state.value)
        if (next == state.value) return
        write(next)
        state.value = next
    }

    private fun read(): AppSettings {
        val d = AppSettings()
        return AppSettings(
            nickname = prefs.getString("nickname", d.nickname).orEmpty(),
            gender = enumOr(prefs.getString("gender", null), d.gender),
            currency = prefs.getString("currency", d.currency) ?: d.currency,
            firstDayOfWeek = prefs.getInt("first_day", 0).takeIf { it in 1..7 }?.let(DayOfWeek::of) ?: d.firstDayOfWeek,
            theme = enumOr(prefs.getString("theme", null), d.theme),
            accent = enumOr(prefs.getString("accent", null), d.accent),
            leftHanded = prefs.getBoolean("left_handed", d.leftHanded),
            backSwipe = enumOr(prefs.getString("back_swipe", null), d.backSwipe),
            timePicker = enumOr(prefs.getString("time_picker", null), d.timePicker),
            islandTabs = list("island_tabs"),
            moreOrder = list("more_order"),
        )
    }

    private fun write(s: AppSettings) = prefs.edit {
        putString("nickname", s.nickname)
        putString("gender", s.gender.name)
        putString("currency", s.currency)
        putInt("first_day", s.firstDayOfWeek.value)
        putString("theme", s.theme.name)
        putString("accent", s.accent.name)
        putBoolean("left_handed", s.leftHanded)
        putString("back_swipe", s.backSwipe.name)
        putString("time_picker", s.timePicker.name)
        putString("island_tabs", s.islandTabs.joinToString(","))
        putString("more_order", s.moreOrder.joinToString(","))
    }

    private fun list(key: String): List<String> =
        prefs.getString(key, null).orEmpty().split(',').filter { it.isNotBlank() }

    private inline fun <reified E : Enum<E>> enumOr(name: String?, default: E): E =
        enumValues<E>().firstOrNull { it.name == name } ?: default

    private companion object {
        const val PREFS = "settings"
    }
}
