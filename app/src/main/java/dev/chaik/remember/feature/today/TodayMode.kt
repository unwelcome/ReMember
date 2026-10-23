package dev.chaik.remember.feature.today

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.ui.graphics.vector.ImageVector
import dev.chaik.remember.R

/** Режимы страницы «Сегодня»; кнопка слева снизу переключает их по кругу. */
enum class TodayMode(@param:StringRes val label: Int, val icon: ImageVector) {
    Day(R.string.today_mode_day, Icons.Outlined.EditNote),
    Sleep(R.string.today_mode_sleep, Icons.Outlined.Bedtime),
    Money(R.string.today_mode_money, Icons.Outlined.Payments),
    ;

    fun next(): TodayMode = entries[(ordinal + 1) % entries.size]
}
