package dev.chaik.remember.core.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Velocity

/**
 * Горизонтальная лента, докрученная до конца, не передаёт остаток жеста наверх.
 * Иначе после упора ленты начинают листаться вкладки островка.
 */
fun Modifier.keepHorizontalScroll(): Modifier = nestedScroll(KeepHorizontalScroll)

private object KeepHorizontalScroll : NestedScrollConnection {
    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset =
        Offset(available.x, 0f)

    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity =
        Velocity(available.x, 0f)
}
