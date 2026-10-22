package dev.chaik.remember.app

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import dev.chaik.remember.app.navigation.TopLevelTab
import dev.chaik.remember.app.navigation.appEntryProvider
import dev.chaik.remember.app.ui.FloatingIsland
import dev.chaik.remember.app.ui.IslandHeight
import dev.chaik.remember.core.ui.LocalIslandPadding
import dev.chaik.remember.core.ui.theme.ReMemberTheme

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
 * Вкладки островка + их стеки экранов.
 *
 * У каждой вкладки свой стек: например, «Ещё → Настройки» остаётся открытым,
 * пока вы переключаетесь на другие вкладки. Состояние экранов (прокрутка и т. п.)
 * сохраняется через [rememberSaveableStateHolder].
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MainScaffold() {
    var currentTab by rememberSaveable { mutableStateOf(TopLevelTab.Today) }
    val backStacks = TopLevelTab.entries.associateWith { rememberNavBackStack(it.root) }
    val backStack = backStacks.getValue(currentTab)
    val stateHolder = rememberSaveableStateHolder()

    // «Назад» на корне любой вкладки, кроме «Сегодня», возвращает на «Сегодня»
    BackHandler(enabled = currentTab != TopLevelTab.Today && backStack.size == 1) {
        currentTab = TopLevelTab.Today
    }

    val navBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val islandPadding = navBarPadding + IslandHeight + IslandBottomMargin * 2

    Box(Modifier.fillMaxSize()) {
        CompositionLocalProvider(LocalIslandPadding provides islandPadding) {
            key(currentTab) {
                stateHolder.SaveableStateProvider(currentTab.name) {
                    NavDisplay(
                        backStack = backStack,
                        onBack = { backStack.removeLastOrNull() },
                        entryProvider = appEntryProvider(backStack),
                    )
                }
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
                tabs = TopLevelTab.entries,
                selected = currentTab,
                onSelect = { tab ->
                    if (tab == currentTab) {
                        // Повторный тап по вкладке — вернуться к её корню
                        while (backStack.size > 1) backStack.removeLastOrNull()
                    } else {
                        currentTab = tab
                    }
                },
            )
        }
    }
}
