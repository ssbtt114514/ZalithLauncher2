/*
 * Zalith Launcher 2
 * Copyright (C) 2025 MovTery <movtery228@qq.com> and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/gpl-3.0.txt>.
 */

package com.movtery.zalithlauncher.ui.screens.content.download

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.movtery.zalithlauncher.game.download.assets.platform.PlatformClasses
import com.movtery.zalithlauncher.ui.screens.NestedNavKey
import com.movtery.zalithlauncher.ui.screens.NormalNavKey
import com.movtery.zalithlauncher.ui.screens.TitledNavKey
import com.movtery.zalithlauncher.ui.screens.content.download.assets.download.DownloadAssetsScreen
import com.movtery.zalithlauncher.ui.screens.content.download.assets.search.FavoritesScreen
import com.movtery.zalithlauncher.ui.screens.navigateTo
import com.movtery.zalithlauncher.ui.screens.onBack
import com.movtery.zalithlauncher.ui.screens.rememberTransitionSpec
import com.movtery.zalithlauncher.viewmodel.ErrorViewModel
import com.movtery.zalithlauncher.viewmodel.EventViewModel
import com.movtery.zalithlauncher.viewmodel.ScreenBackStackViewModel
import com.movtery.zalithlauncher.ui.screens.content.navigateToDownload

/**
 * 收藏屏幕的包装层。
 *
 * 内部 NavDisplay 路由：
 * - [NormalNavKey.Favorites]         → 收藏列表
 * - [NormalNavKey.DownloadAssets]    → 资源详情下载页（用户点击资源卡片后跳转）
 *
 * 点击游戏版本收藏卡片时，跳转到游戏下载屏幕的 Addons 子页面。
 */
@Composable
fun DownloadFavoritesScreen(
    key: NestedNavKey.DownloadFavorites,
    mainScreenKey: TitledNavKey?,
    downloadScreenKey: TitledNavKey?,
    downloadFavoritesScreenKey: TitledNavKey?,
    onCurrentKeyChange: (TitledNavKey?) -> Unit,
    submitError: (ErrorViewModel.ThrowableMessage) -> Unit,
    eventViewModel: EventViewModel,
    backScreenViewModel: ScreenBackStackViewModel
) {
    val backStack = key.backStack
    val stackTopKey = backStack.lastOrNull()
    LaunchedEffect(stackTopKey) {
        onCurrentKeyChange(stackTopKey)
    }

    if (backStack.isNotEmpty()) {
        NavDisplay(
            backStack = backStack,
            modifier = Modifier.fillMaxSize(),
            onBack = { onBack(backStack) },
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator()
            ),
            transitionSpec = rememberTransitionSpec(),
            popTransitionSpec = rememberTransitionSpec(),
            entryProvider = entryProvider {
                entry<NormalNavKey.Favorites> {
                    FavoritesScreen(
                        mainScreenKey = mainScreenKey,
                        downloadScreenKey = downloadScreenKey,
                        downloadFavoritesScreenKey = downloadFavoritesScreenKey,
                        onSwapToDownload = { platform, classes, projectId, iconUrl ->
                            //跳转到对应资源类型的下载屏幕，并把 DownloadAssets 推入栈顶
                            val targetScreen = when (classes) {
                                PlatformClasses.MOD -> backScreenViewModel.downloadModScreen
                                PlatformClasses.MOD_PACK -> backScreenViewModel.downloadModPackScreen
                                PlatformClasses.RESOURCE_PACK -> backScreenViewModel.downloadResourcePackScreen
                                PlatformClasses.SAVES -> backScreenViewModel.downloadSavesScreen
                                PlatformClasses.SHADERS -> backScreenViewModel.downloadShadersScreen
                            }
                            targetScreen.backStack.navigateTo(
                                NormalNavKey.DownloadAssets(
                                    platform = platform,
                                    projectId = projectId,
                                    classes = classes,
                                    iconUrl = iconUrl
                                )
                            )
                            //切换整个下载屏幕到对应分类
                            backScreenViewModel.navigateToDownload(targetScreen)
                        },
                        onSwapToGameVersion = { versionId ->
                            //跳转到游戏下载屏幕，并把 Addons 推入栈顶
                            backScreenViewModel.downloadGameScreen.backStack.navigateTo(
                                NormalNavKey.DownloadGame.Addons(versionId)
                            )
                            backScreenViewModel.navigateToDownload(backScreenViewModel.downloadGameScreen)
                        }
                    )
                }
                entry<NormalNavKey.DownloadAssets> { assetsKey ->
                    DownloadAssetsScreen(
                        mainScreenKey = mainScreenKey,
                        parentScreenKey = key,
                        parentCurrentKey = downloadScreenKey,
                        currentKey = downloadFavoritesScreenKey,
                        key = assetsKey,
                        eventViewModel = eventViewModel,
                        onItemClicked = { _, _, _, _ -> },
                        nestedNavKeyClass = key::class.java
                    )
                }
            }
        )
    } else {
        Box(Modifier.fillMaxSize())
    }
}
