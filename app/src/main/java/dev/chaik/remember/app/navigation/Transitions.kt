package dev.chaik.remember.app.navigation

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.ui.unit.IntOffset

// Переходы между страницами внутри вкладки.
// Открытие: новая страница въезжает справа, старая чуть сдвигается влево.
// Возврат (кнопкой, свайпом справа налево или системным жестом): верхняя страница
// уезжает влево, открывая предыдущую — при свайпе она движется за пальцем.
//
// Только сдвиги, без прозрачности: если жест прервать на середине,
// прозрачность могла «залипнуть» на странице до следующего перехода.

private val slideSpec = tween<IntOffset>(durationMillis = 300, easing = FastOutSlowInEasing)

fun pushTransition(): ContentTransform =
    slideInHorizontally(slideSpec) { it } togetherWith
        slideOutHorizontally(slideSpec) { -it / 4 }

fun popTransition(): ContentTransform =
    (slideInHorizontally(slideSpec) { it / 4 } togetherWith
        slideOutHorizontally(slideSpec) { -it })
        .apply { targetContentZIndex = -1f } // уходящая страница рисуется поверх
