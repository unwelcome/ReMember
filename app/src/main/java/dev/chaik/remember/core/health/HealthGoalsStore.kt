package dev.chaik.remember.core.health

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Цели активности в SharedPreferences — три числа, личных данных нет. */
class HealthGoalsStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val state = MutableStateFlow(read())

    val goals: StateFlow<HealthGoals> = state.asStateFlow()

    fun save(goals: HealthGoals) {
        prefs.edit {
            putLong(KEY_STEPS, goals.steps)
            putInt(KEY_MINUTES, goals.activeMinutes)
            putInt(KEY_CALORIES, goals.calories)
        }
        state.value = goals
    }

    private fun read(): HealthGoals {
        val d = HealthGoals()
        return HealthGoals(
            steps = prefs.getLong(KEY_STEPS, d.steps),
            activeMinutes = prefs.getInt(KEY_MINUTES, d.activeMinutes),
            calories = prefs.getInt(KEY_CALORIES, d.calories),
        )
    }

    private companion object {
        const val PREFS = "health"
        const val KEY_STEPS = "goal_steps"
        const val KEY_MINUTES = "goal_minutes"
        const val KEY_CALORIES = "goal_calories"
    }
}
