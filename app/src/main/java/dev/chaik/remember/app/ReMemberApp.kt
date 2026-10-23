package dev.chaik.remember.app

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import dev.chaik.remember.app.navigation.TopLevelTab
import dev.chaik.remember.app.navigation.appEntryProvider
import dev.chaik.remember.app.navigation.popTransition
import dev.chaik.remember.app.navigation.pushTransition
import dev.chaik.remember.app.ui.FloatingIsland
import dev.chaik.remember.app.ui.IslandHeight
import dev.chaik.remember.core.ui.LocalIslandPadding
import dev.chaik.remember.core.ui.components.SwipeBackHost
import dev.chaik.remember.core.ui.theme.ReMemberTheme
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

private val IslandBottomMargin = 12.dp

@Composable
fun ReMemberApp() {
    ReMemberTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            MainScaffold()
        }
    }
}

/**
 * Вкладки островка, листаются свайпом влево/вправо.
 *
 * У каждой вкладки свой стек экранов: например, «Ещё → Настройки» остаётся открытым,
 * пока вы на других вкладках. Пока во вкладке открыта вложенная страница,
 * свайп вправо работает как «назад», а не как переход на соседнюю вкладку.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MainScaffold() {
    val tabs = TopLevelTab.entries
    val pagerState = rememberPagerState { tabs.size }
    val backStacks = tabs.associateWith { rememberNavBackStack(it.root) }
    val currentStack = backStacks.getValue(tabs[pagerState.currentPage])
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current

    fun goTo(tab: TopLevelTab) {
        scope.launch {
            pagerState.animateScrollToPage(
                page = tab.ordinal,
                animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow),
            )
        }
    }

    // Лёгкая вибрация при смене вкладки — и по тапу, и по свайпу
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.drop(1).collect {
            haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
        }
    }

    // «Назад» на корне любой вкладки, кроме «Сегодня», возвращает на «Сегодня»
    BackHandler(enabled = pagerState.currentPage != TopLevelTab.Today.ordinal && currentStack.size == 1) {
        goTo(TopLevelTab.Today)
    }

    val navBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val islandPadding = navBarPadding + IslandHeight + IslandBottomMargin * 2

    Box(Modifier.fillMaxSize()) {
        CompositionLocalProvider(LocalIslandPadding provides islandPadding) {
            HorizontalPager(
                state = pagerState,
                // Во вложенной странице горизонтальный свайп отдан жесту «назад»
                userScrollEnabled = currentStack.size == 1,
                key = { tabs[it].name },
                modifier = Modifier.fillMaxSize(),
            ) { page ->
                TabContent(backStacks.getValue(tabs[page]))
            }
        }

        // Пока открыта клавиатура, островок прячется, чтобы не мешать вводу
        AnimatedVisibility(
            visible = !WindowInsets.isImeVisible,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = IslandBottomMargin),
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
        ) {
            FloatingIsland(
                tabs = tabs,
                selected = tabs[pagerState.currentPage],
                indicatorPosition = { pagerState.position },
                onSelect = { tab ->
                    if (tab.ordinal == pagerState.currentPage) {
                        // Повторный тап по вкладке — вернуться к её корню
                        while (currentStack.size > 1) currentStack.removeLastOrNull()
                    } else {
                        goTo(tab)
                    }
                },
            )
        }
    }
}

/** Стек экранов одной вкладки. */
@Composable
private fun TabContent(backStack: NavBackStack<NavKey>) {
    SwipeBackHost(enabled = backStack.size > 1) {
        NavDisplay(
            backStack = backStack,
            onBack = { backStack.removeLastOrNull() },
            // Свои ViewModel и сохранённое состояние у каждой страницы стека
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
            ),
            transitionSpec = { pushTransition() },
            popTransitionSpec = { popTransition() },
            predictivePopTransitionSpec = { popTransition() },
            entryProvider = appEntryProvider(backStack),
        )
    }
}

/** Дробная позиция пейджера: 1.5 = на полпути между второй и третьей вкладкой. */
private val PagerState.position: Float
    get() = currentPage + currentPageOffsetFraction
