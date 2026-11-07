/*
 * Zalith Launcher 2
 * Copyright (C) 2025-2026 MovTery <movtery228@qq.com> and contributors
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

package com.movtery.zalithlauncher.ui.screens.main

import androidx.compose.foundation.horizontalScroll
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.style.TextOverflow
import com.movtery.zalithlauncher.game.account.AccountsManager
import com.movtery.zalithlauncher.game.account.getAccountTypeName
import com.movtery.zalithlauncher.ui.screens.content.elements.PlayerFace
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.runtime.derivedStateOf
import com.movtery.zalithlauncher.utils.animation.SmoothEasing
import kotlin.math.roundToInt
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.movtery.zalithlauncher.ui.components.McSidebarItem
import com.movtery.zalithlauncher.ui.components.McVerticalDivider
import com.movtery.zalithlauncher.ui.screens.content.FirstLoginMenu
import com.movtery.zalithlauncher.ui.theme.McColors
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.entryProvider
import com.movtery.zalithlauncher.BuildKeys
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.coroutine.TaskSystem
import com.movtery.zalithlauncher.game.version.installed.Version
import com.movtery.zalithlauncher.game.version.installed.VersionsManager
import com.movtery.zalithlauncher.path.PathManager
import com.movtery.zalithlauncher.setting.AllSettings
import com.movtery.zalithlauncher.ui.AndroidStringText
import com.movtery.zalithlauncher.ui.androidText
import com.movtery.zalithlauncher.ui.components.BackgroundCard
import com.movtery.zalithlauncher.ui.components.CardTitleLayout
import com.movtery.zalithlauncher.ui.screens.BackStackNavKey
import com.movtery.zalithlauncher.ui.screens.NavStateHost
import com.movtery.zalithlauncher.ui.screens.NestedNavDisplay
import com.movtery.zalithlauncher.ui.screens.NestedNavKey
import com.movtery.zalithlauncher.ui.screens.NormalNavKey
import com.movtery.zalithlauncher.ui.screens.TitledNavKey
import com.movtery.zalithlauncher.ui.screens.content.AccountManageScreen
import com.movtery.zalithlauncher.ui.screens.content.DownloadScreen
import com.movtery.zalithlauncher.ui.screens.content.FileSelectorScreen
import com.movtery.zalithlauncher.ui.screens.content.LauncherScreen
import com.movtery.zalithlauncher.ui.screens.content.LicenseScreen
import com.movtery.zalithlauncher.ui.screens.content.LogViewScreen
import com.movtery.zalithlauncher.ui.screens.content.MultiplayerScreen
import com.movtery.zalithlauncher.ui.screens.content.SettingsScreen
import com.movtery.zalithlauncher.ui.screens.content.VersionExportScreen
import com.movtery.zalithlauncher.ui.screens.content.VersionSettingsScreen
import com.movtery.zalithlauncher.ui.screens.content.VersionsManageScreen
import com.movtery.zalithlauncher.ui.screens.content.WebViewScreen
import com.movtery.zalithlauncher.ui.screens.content.addoninfo.AddonInfoScreen
import com.movtery.zalithlauncher.ui.screens.content.navigateToDownload
import com.movtery.zalithlauncher.ui.screens.navigateTo
import com.movtery.zalithlauncher.ui.screens.onBack
import com.movtery.zalithlauncher.ui.screens.rememberTransitionSpec
import com.movtery.zalithlauncher.ui.theme.backgroundColor
import com.movtery.zalithlauncher.ui.theme.cardColor
import com.movtery.zalithlauncher.ui.theme.onBackgroundColor
import com.movtery.zalithlauncher.ui.theme.onCardColor
import com.movtery.zalithlauncher.utils.animation.getAnimateTween
import com.movtery.zalithlauncher.utils.file.formatFileSize
import com.movtery.zalithlauncher.viewmodel.ErrorViewModel
import com.movtery.zalithlauncher.viewmodel.EventViewModel
import com.movtery.zalithlauncher.viewmodel.ModifyVersionViewModel
import com.movtery.zalithlauncher.viewmodel.ModpackImportViewModel
import com.movtery.zalithlauncher.viewmodel.ScreenBackStackViewModel
import com.movtery.zalithlauncher.viewmodel.sendKeepScreen

@Composable
fun MainScreen(
    screenBackStackModel: ScreenBackStackViewModel,
    eventViewModel: EventViewModel,
    modpackImportViewModel: ModpackImportViewModel,
    modifyVersionViewModel: ModifyVersionViewModel,
    submitError: (ErrorViewModel.ThrowableMessage) -> Unit
) {
    //监控当前是否有任务正在进行 (noTasks = true 表示当前没有任务)
    var noTasks by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        TaskSystem.tasksFlow.collect { tasks ->
            val empty = tasks.isEmpty()
            eventViewModel.sendKeepScreen(!empty)
            noTasks = empty
        }
    }

    val isTaskMenuExpanded = AllSettings.launcherTaskMenuExpanded.state

    fun changeTasksExpandedState() {
        AllSettings.launcherTaskMenuExpanded.save(!isTaskMenuExpanded)
    }

    /** 回到主页面通用函数 */
    val toMainScreen: () -> Unit = {
        screenBackStackModel.mainScreen.clearWith(NormalNavKey.LauncherMain)
    }

    /** 切换到某个一级页面（先清理上一级页面栈） */
    fun navigateTop(key: TitledNavKey) {
        screenBackStackModel.mainScreen.removeAndNavigateTo(
            removes = screenBackStackModel.clearBeforeNavKeys,
            screenKey = key
        )
    }

    val mainScreenKey = screenBackStackModel.mainScreen.currentKey
    val inLauncherScreen = mainScreenKey == null || mainScreenKey is NormalNavKey.LauncherMain
    val webUrl = (mainScreenKey as? NormalNavKey.WebScreen)?.url
    val inVersions = mainScreenKey is NormalNavKey.VersionsManager ||
            mainScreenKey is NestedNavKey.VersionSettings ||
            mainScreenKey is NestedNavKey.VersionExport
    val inAccounts = mainScreenKey is NormalNavKey.AccountManager
    val inPatchNotes = webUrl == PATCH_NOTES_URL
    val inNews = webUrl != null && !inPatchNotes
    val inSettings = mainScreenKey is NestedNavKey.Settings
    val inDownload = mainScreenKey is NestedNavKey.Download
    val inMultiplayer = mainScreenKey is NormalNavKey.Multiplayer
    // Các trang thuộc mục "Java Edition" có header + tab Play / Installations / Skins / Patch notes
    val inJavaSection = inLauncherScreen || inVersions || inAccounts || inPatchNotes

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = McColors.Background,
        contentColor = McColors.TextPrimary
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            // Vào Tin tức / Tải xuống / Chơi chung / Cài đặt thì thanh bên thu gọn để nhường chỗ cho nội dung;
            // bấm nút mũi tên quay lại ở thanh trên để mở lại.
            // Bấm vào bất kỳ mục nào trên thanh bên thì thanh bên tự ẩn; bấm tab nhỏ ở mép trái để mở lại.
            var sidebarOpen by remember { mutableStateOf(true) }
            val sidebarCollapsed = !sidebarOpen
            val hideSidebar: () -> Unit = { sidebarOpen = false }
            // Chỉ đọc giá trị animation ở pha layout: không recompose cả màn hình mỗi khung hình
            val sidebarFraction = animateFloatAsState(
                targetValue = if (sidebarCollapsed) 0f else 1f,
                animationSpec = tween(240, easing = SmoothEasing),
                label = "SidebarWidth"
            )
            val sidebarVisible by remember { derivedStateOf { sidebarFraction.value > 0.001f } }
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .clipToBounds()
                    .layout { measurable, constraints ->
                        val full = 190.dp.roundToPx()
                        val placeable = measurable.measure(
                            Constraints.fixed(full, constraints.maxHeight)
                        )
                        val w = (full * sidebarFraction.value).roundToInt()
                        layout(w, placeable.height) { placeable.placeRelative(0, 0) }
                    }
            ) {
            MinecraftSidebar(
                modifier = Modifier
                    .requiredWidth(190.dp)
                    .fillMaxHeight(),
                inJavaEdition = inJavaSection,
                inNews = inNews,
                inSettings = inSettings,
                inAccounts = inAccounts,
                inDownload = inDownload,
                inMultiplayer = inMultiplayer,
                toDownload = { hideSidebar(); screenBackStackModel.navigateToDownload() },
                toMultiplayer = { hideSidebar(); navigateTop(NormalNavKey.Multiplayer) },
                openFileManager = {
                    hideSidebar()
                    eventViewModel.sendEvent(
                        EventViewModel.Event.OpenFileManager(
                            rootPath = PathManager.DIR_FILES_EXTERNAL.absolutePath
                        )
                    )
                },
                toAccountScreen = { hideSidebar(); navigateTop(NormalNavKey.AccountManager(FirstLoginMenu.NONE)) },
                toNewsScreen = { hideSidebar(); navigateTop(NormalNavKey.WebScreen(NEWS_URL)) },
                toJavaEdition = { hideSidebar(); if (!inLauncherScreen) toMainScreen() },
                toSettingsScreen = { hideSidebar(); navigateTop(screenBackStackModel.settingsScreen) }
            )
            }

            if (sidebarVisible) McVerticalDivider()

            Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                if (inJavaSection) {
                    McJavaHeader(
                        modifier = Modifier.fillMaxWidth(),
                        inPlay = inLauncherScreen,
                        inInstallations = inVersions,
                        inSkins = inAccounts,
                        inPatchNotes = inPatchNotes,
                        showTaskChip = !(isTaskMenuExpanded || noTasks),
                        toPlay = { if (!inLauncherScreen) toMainScreen() },
                        toInstallations = { if (!inVersions) navigateTop(NormalNavKey.VersionsManager) },
                        toSkins = { if (!inAccounts) navigateTop(NormalNavKey.AccountManager(FirstLoginMenu.NONE)) },
                        toPatchNotes = { if (!inPatchNotes) navigateTop(NormalNavKey.WebScreen(PATCH_NOTES_URL)) },
                        changeExpandedState = { changeTasksExpandedState() }
                    )
                } else {
                    MinecraftTopBar(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        mainScreenKey = mainScreenKey,
                        inLauncherScreen = inLauncherScreen,
                        showTaskChip = !(isTaskMenuExpanded || noTasks),
                        onScreenBack = {
                            screenBackStackModel.mainScreen.backStack.removeFirstOrNull()
                        },
                        changeExpandedState = { changeTasksExpandedState() }
                    )
                }
                HorizontalDivider(color = McColors.Outline, thickness = 1.dp)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(McColors.Content)
                ) {
                    NavStateHost(rootKey = screenBackStackModel.mainScreen) {
                        NavigationUI(
                            modifier = Modifier.fillMaxSize(),
                            screenBackStackModel = screenBackStackModel,
                            toMainScreen = toMainScreen,
                            eventViewModel = eventViewModel,
                            modpackImportViewModel = modpackImportViewModel,
                            modifyVersionViewModel = modifyVersionViewModel,
                            submitError = submitError
                        )
                    }

                    AnimatedVisibility(
                        visible = !sidebarOpen,
                        modifier = Modifier.align(Alignment.TopStart),
                        enter = fadeIn(tween(160)),
                        exit = fadeOut(tween(100))
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(top = 8.dp)
                                .width(24.dp)
                                .height(44.dp)
                                .background(McColors.Panel)
                                .clickable { sidebarOpen = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                modifier = Modifier.size(22.dp).rotate(-90f),
                                painter = painterResource(R.drawable.ic_arrow_drop_down_rounded),
                                tint = McColors.TextSecondary,
                                contentDescription = null
                            )
                        }
                    }

                    TaskMenu(
                        isExpanded = isTaskMenuExpanded,
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(0.4f)
                            .align(Alignment.CenterStart)
                            .padding(all = 6.dp)
                    ) {
                        changeTasksExpandedState()
                    }
                }
            }
        }
    }
}

private const val NEWS_URL = "https://www.minecraft.net/en-us/articles"
private const val PATCH_NOTES_URL = "https://feedback.minecraft.net/hc/en-us/sections/360001186971-Release-Changelogs"

/**
 * Thanh bên giống Minecraft Launcher: tài khoản ở đầu, News, Java Edition, Settings ở cuối.
 */
@Composable
private fun MinecraftSidebar(
    inJavaEdition: Boolean,
    inNews: Boolean,
    inSettings: Boolean,
    inAccounts: Boolean,
    inDownload: Boolean,
    inMultiplayer: Boolean,
    toDownload: () -> Unit,
    toMultiplayer: () -> Unit,
    openFileManager: () -> Unit,
    toAccountScreen: () -> Unit,
    toNewsScreen: () -> Unit,
    toJavaEdition: () -> Unit,
    toSettingsScreen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val account by AccountsManager.currentAccountFlow.collectAsStateWithLifecycle()

    Column(modifier = modifier.background(McColors.Background)) {
        // Tài khoản đang dùng (bấm để mở danh sách tài khoản)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = toAccountScreen)
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val acc = account
            if (acc != null) {
                PlayerFace(account = acc, avatarSize = 32.dp)
            } else {
                Icon(
                    modifier = Modifier.size(32.dp),
                    painter = painterResource(R.drawable.ic_person_filled),
                    tint = McColors.TextSecondary,
                    contentDescription = null
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = acc?.username ?: stringResource(R.string.account_add_new_account),
                    color = McColors.TextPrimary,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (acc != null) {
                    Text(
                        text = getAccountTypeName(acc),
                        color = McColors.TextMuted,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Icon(
                modifier = Modifier.size(20.dp),
                painter = painterResource(R.drawable.ic_arrow_drop_down_rounded),
                tint = McColors.TextSecondary,
                contentDescription = null
            )
        }
        HorizontalDivider(color = McColors.Outline, thickness = 1.dp)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            McSideRow(
                selected = inNews,
                painter = painterResource(R.drawable.ic_article_outlined),
                label = stringResource(R.string.mc_sidebar_news),
                onClick = toNewsScreen
            )
            McSideRow(
                selected = inJavaEdition,
                painter = painterResource(R.drawable.ic_play_arrow_filled),
                label = stringResource(R.string.mc_sidebar_java_edition),
                subtitle = stringResource(R.string.mc_sidebar_minecraft),
                onClick = toJavaEdition
            )
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 6.dp),
                color = McColors.Outline,
                thickness = 1.dp
            )
            McSideRow(
                selected = inDownload,
                painter = painterResource(R.drawable.ic_download_2_filled),
                label = stringResource(R.string.generic_download),
                onClick = toDownload
            )
            McSideRow(
                selected = false,
                painter = painterResource(R.drawable.ic_folder_filled),
                label = stringResource(R.string.files_browse_folder),
                onClick = openFileManager
            )
        }

        HorizontalDivider(color = McColors.Outline, thickness = 1.dp)
        McSideRow(
            selected = inSettings,
            painter = painterResource(R.drawable.ic_settings_filled),
            label = stringResource(R.string.generic_setting),
            onClick = toSettingsScreen
        )
    }
}

@Composable
private fun McSideRow(
    selected: Boolean,
    painter: androidx.compose.ui.graphics.painter.Painter,
    label: String,
    onClick: () -> Unit,
    subtitle: String? = null,
) {
    val color = if (selected) McColors.TextPrimary else McColors.TextSecondary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (selected) McColors.Panel else Color.Transparent)
            .drawBehind {
                if (selected) {
                    drawRect(
                        color = McColors.PlayGreenLight,
                        size = androidx.compose.ui.geometry.Size(3.dp.toPx(), size.height)
                    )
                }
            }
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            modifier = Modifier.size(24.dp),
            painter = painter,
            tint = color,
            contentDescription = null
        )
        Column {
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    color = McColors.TextMuted,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1
                )
            }
            Text(
                text = label,
                color = color,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Header của mục Java Edition: dòng "MINECRAFT: JAVA EDITION" + tab Play / Installations / Skins / Patch notes,
 * kèm chip tiến trình tác vụ ở bên phải (Download / Multiplayer / Files nằm ở thanh bên).
 */
@Composable
private fun McJavaHeader(
    inPlay: Boolean,
    inInstallations: Boolean,
    inSkins: Boolean,
    inPatchNotes: Boolean,
    showTaskChip: Boolean,
    toPlay: () -> Unit,
    toInstallations: () -> Unit,
    toSkins: () -> Unit,
    toPatchNotes: () -> Unit,
    changeExpandedState: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.background(McColors.Background)) {
        Text(
            modifier = Modifier.padding(start = 16.dp, top = 10.dp),
            text = stringResource(R.string.mc_header_title),
            color = McColors.TextSecondary,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .padding(start = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .horizontalScroll(rememberScrollState())
            ) {
                McHeaderTab(stringResource(R.string.mc_tab_play), inPlay, toPlay)
                McHeaderTab(stringResource(R.string.mc_tab_installations), inInstallations, toInstallations)
                McHeaderTab(stringResource(R.string.mc_tab_skins), inSkins, toSkins)
                McHeaderTab(stringResource(R.string.mc_tab_patch_notes), inPatchNotes, toPatchNotes)
            }

            AnimatedVisibility(visible = showTaskChip) {
                IconButton(onClick = changeExpandedState) {
                    Icon(
                        modifier = Modifier.size(22.dp),
                        painter = painterResource(R.drawable.ic_assignment_filled),
                        tint = McColors.PlayGreenLight,
                        contentDescription = stringResource(R.string.main_task_menu)
                    )
                }
            }
        }
    }
}

@Composable
private fun McHeaderTab(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .clickable(onClick = onClick)
            .drawBehind {
                if (selected) {
                    drawRect(
                        color = McColors.PlayGreenLight,
                        topLeft = Offset(10.dp.toPx(), size.height - 3.dp.toPx()),
                        size = androidx.compose.ui.geometry.Size(size.width - 20.dp.toPx(), 3.dp.toPx())
                    )
                }
            }
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (selected) McColors.TextPrimary else McColors.TextSecondary,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1
        )
    }
}

/**
 * Thanh tiêu đề phía trên nội dung: nút quay lại + tiêu đề trang + chip tiến trình tác vụ.
 */
@Composable
private fun <E: TitledNavKey> MinecraftTopBar(
    mainScreenKey: E?,
    inLauncherScreen: Boolean,
    showTaskChip: Boolean,
    modifier: Modifier = Modifier,
    onScreenBack: () -> Unit,
    changeExpandedState: () -> Unit,
) {
    CompositionLocalProvider(LocalContentColor provides McColors.TextPrimary) {
        Row(
            modifier = modifier
                .background(McColors.Background)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

            AnimatedVisibility(visible = !inLauncherScreen) {
                IconButton(
                    onClick = {
                        if (!inLauncherScreen) {
                            backDispatcher?.onBackPressed() ?: onScreenBack()
                        }
                    }
                ) {
                    Icon(
                        modifier = Modifier.size(24.dp),
                        painter = painterResource(R.drawable.ic_arrow_back),
                        contentDescription = stringResource(R.string.generic_back)
                    )
                }
            }

            val parentRes = mainScreenKey?.title
            val childRes = (mainScreenKey as? BackStackNavKey<*>)?.currentKey?.title

            Crossfade(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = if (inLauncherScreen) 8.dp else 0.dp),
                targetState = parentRes to childRes
            ) { (parent, child) ->
                val style = MaterialTheme.typography.titleMedium
                if (parent == null) {
                    Text(
                        text = BuildKeys.LAUNCHER_IDENTIFIER.uppercase(),
                        style = style,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                        softWrap = false,
                        maxLines = 1
                    )
                } else {
                    val titleText = if (child != null) {
                        androidText(parent, androidText(" - "), child)
                    } else {
                        parent
                    }
                    AndroidStringText(
                        text = titleText,
                        style = style,
                        fontWeight = FontWeight.Bold,
                        softWrap = false,
                        maxLines = 1
                    )
                }
            }

            AnimatedVisibility(
                visible = showTaskChip,
                enter = slideInVertically(initialOffsetY = { -50 }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { -50 }) + fadeOut()
            ) {
                Row(
                    modifier = Modifier
                        .border(1.dp, McColors.Outline)
                        .background(McColors.Panel)
                        .clickable { changeExpandedState() }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .width(140.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LinearProgressIndicator(
                        modifier = Modifier.weight(1f),
                        color = McColors.PlayGreenLight,
                        trackColor = McColors.Outline
                    )
                    Icon(
                        modifier = Modifier.size(20.dp),
                        painter = painterResource(R.drawable.ic_assignment_filled),
                        contentDescription = stringResource(R.string.main_task_menu)
                    )
                }
            }
        }
    }
}

@Composable
private fun NavigationUI(
    modifier: Modifier = Modifier,
    screenBackStackModel: ScreenBackStackViewModel,
    toMainScreen: () -> Unit,
    eventViewModel: EventViewModel,
    modpackImportViewModel: ModpackImportViewModel,
    modifyVersionViewModel: ModifyVersionViewModel,
    submitError: (ErrorViewModel.ThrowableMessage) -> Unit
) {
    val backStack = screenBackStackModel.mainScreen.backStack
    val currentKey = backStack.lastOrNull()

    LaunchedEffect(currentKey) {
        screenBackStackModel.mainScreen.currentKey = currentKey
    }

    if (backStack.isNotEmpty()) {
        /** 导航至版本详细信息屏幕 */
        val navigateToVersions: (Version) -> Unit = { version ->
            screenBackStackModel.mainScreen.navigateTo(
                screenKey = NestedNavKey.VersionSettings(version),
                useClassEquality = true
            )
        }
        /** 导航至整合包导出屏幕 */
        val navigateToExport: (Version) -> Unit = { version ->
            screenBackStackModel.mainScreen.removeAndNavigateTo(
                remove = NestedNavKey.VersionSettings::class,
                screenKey = NestedNavKey.VersionExport(version),
                useClassEquality = true
            )
        }

        NestedNavDisplay(
            ownerKey = screenBackStackModel.mainScreen,
            backStack = backStack,
            modifier = modifier,
            onBack = {
                onBack(backStack)
            },
            transitionSpec = rememberTransitionSpec(),
            popTransitionSpec = rememberTransitionSpec(),
            entryProvider = entryProvider {
                entry<NormalNavKey.LauncherMain> {
                    LauncherScreen(
                        backStackViewModel = screenBackStackModel,
                        navigateToVersions = navigateToVersions,
                        onLaunchGame = { version ->
                            eventViewModel.sendEvent(
                                EventViewModel.Event.Launch.Game(version)
                            )
                        }
                    )
                }
                entry<NestedNavKey.Settings> { key ->
                    SettingsScreen(
                        key = key,
                        backStackViewModel = screenBackStackModel,
                        openLicenseScreen = { raw ->
                            backStack.navigateTo(NormalNavKey.License(raw))
                        },
                        eventViewModel = eventViewModel,
                        submitError = submitError
                    )
                }
                entry<NormalNavKey.License> { key ->
                    LicenseScreen(
                        key = key,
                        backStackViewModel = screenBackStackModel
                    )
                }
                entry<NormalNavKey.AccountManager> { key ->
                    AccountManageScreen(
                        key = key,
                        backStackViewModel = screenBackStackModel,
                        backToMainScreen = toMainScreen,
                        openLink = { url ->
                            eventViewModel.sendEvent(EventViewModel.Event.OpenLink(url))
                        },
                        eventViewModel = eventViewModel,
                        submitError = submitError
                    )
                }
                entry<NormalNavKey.WebScreen> { key ->
                    WebViewScreen(
                        key = key,
                        backStackViewModel = screenBackStackModel,
                        eventViewModel = eventViewModel
                    )
                }
                entry<NormalNavKey.VersionsManager> {
                    VersionsManageScreen(
                        backScreenViewModel = screenBackStackModel,
                        navigateToVersions = navigateToVersions,
                        navigateToExport = navigateToExport,
                        eventViewModel = eventViewModel,
                        submitError = submitError
                    )
                }
                entry<NormalNavKey.FileSelector> { key ->
                    FileSelectorScreen(
                        key = key,
                        backScreenViewModel = screenBackStackModel
                    ) {
                        backStack.removeLastOrNull()
                    }
                }
                entry<NestedNavKey.VersionSettings> { key ->
                    VersionSettingsScreen(
                        key = key,
                        modifyViewModel = modifyVersionViewModel,
                        backScreenViewModel = screenBackStackModel,
                        backToMainScreen = toMainScreen,
                        onExportModpack = {
                            navigateToExport(key.version)
                        },
                        eventViewModel = eventViewModel,
                        submitError = submitError
                    )
                }
                entry<NestedNavKey.VersionExport> { key ->
                    VersionExportScreen(
                        key = key,
                        backScreenViewModel = screenBackStackModel,
                        eventViewModel = eventViewModel,
                        backToMainScreen = toMainScreen
                    )
                }
                entry<NestedNavKey.Download> { key ->
                    DownloadScreen(
                        key = key,
                        backScreenViewModel = screenBackStackModel,
                        eventViewModel = eventViewModel,
                        modpackImportViewModel = modpackImportViewModel,
                        submitError = submitError
                    )
                }
                entry<NestedNavKey.AddonInfo> { key ->
                    AddonInfoScreen(
                        key = key,
                        mainScreenKey = screenBackStackModel.mainScreen.currentKey,
                        addonInfoScreenKey = key.currentKey,
                        eventViewModel = eventViewModel,
                        submitError = submitError,
                    )
                }
                entry<NormalNavKey.Multiplayer> {
                    MultiplayerScreen(
                        backScreenViewModel = screenBackStackModel,
                        eventViewModel = eventViewModel
                    )
                }
                entry<NormalNavKey.LogView> { key ->
                    LogViewScreen(
                        key = key,
                        backStackViewModel = screenBackStackModel,
                    )
                }
            }
        )
    } else {
        Box(modifier)
    }
}

@Composable
private fun TaskMenu(
    isExpanded: Boolean,
    modifier: Modifier = Modifier,
    changeExpandedState: () -> Unit = {}
) {
    val tasks by TaskSystem.tasksFlow.collectAsStateWithLifecycle()
    val show = isExpanded && tasks.isNotEmpty()

    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    AnimatedVisibility(
        modifier = modifier,
        enter = slideInHorizontally(
            initialOffsetX = { if (isRtl) it else -it },
            animationSpec = getAnimateTween()
        ) + fadeIn(),
        exit = slideOutHorizontally(
            targetOffsetX = { if (isRtl) it else -it },
            animationSpec = getAnimateTween()
        ) + fadeOut(),
        visible = show
    ) {
        BackgroundCard(
            modifier = Modifier
                .fillMaxSize()
                .padding(all = 6.dp),
            influencedByBackground = false,
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(
                containerColor = backgroundColor(),
                contentColor = onBackgroundColor()
            ),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp)
        ) {
            Column {
                CardTitleLayout(blur = 0) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp)
                            .padding(top = 8.dp, bottom = 4.dp)
                    ) {
                        IconButton(
                            modifier = Modifier
                                .size(28.dp)
                                .align(Alignment.CenterStart),
                            onClick = changeExpandedState
                        ) {
                            Icon(
                                modifier = Modifier.size(28.dp),
                                painter = painterResource(R.drawable.ic_arrow_left_rounded),
                                contentDescription = stringResource(R.string.generic_collapse)
                            )
                        }

                        Text(
                            modifier = Modifier.align(Alignment.Center),
                            text = stringResource(R.string.main_task_menu)
                        )
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(1f),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    items(tasks, key = { it.id }) { task ->
                        val taskProgress by task.progress.collectAsStateWithLifecycle()
                        val taskMessage by task.message.collectAsStateWithLifecycle()
                        val rateBytesPerSec by task.rateBytesPerSec.collectAsStateWithLifecycle()

                        TaskItem(
                            taskProgress = taskProgress,
                            taskMessage = taskMessage,
                            rateBytesPerSec = rateBytesPerSec,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                        ) {
                            //取消任务
                            TaskSystem.cancelTask(task.id)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TaskItem(
    taskProgress: Float,
    taskMessage: AndroidStringText?,
    rateBytesPerSec: Long?,
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.large,
    color: Color = cardColor(false),
    contentColor: Color = onCardColor(),
    onCancelClick: () -> Unit = {}
) {
    Surface(
        modifier = modifier,
        shape = shape,
        color = color,
        contentColor = contentColor,
    ) {
        Row(
            modifier = Modifier.padding(all = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                modifier = Modifier
                    .size(24.dp)
                    .align(Alignment.CenterVertically),
                onClick = onCancelClick
            ) {
                Icon(
                    modifier = Modifier.size(20.dp),
                    painter = painterResource(R.drawable.ic_close),
                    contentDescription = stringResource(R.string.generic_cancel)
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .align(Alignment.CenterVertically)
            ) {
                taskMessage?.let { message ->
                    AndroidStringText(
                        text = message,
                        style = MaterialTheme.typography.labelMedium
                    )
                }

                if (taskProgress < 0) { //负数则代表不确定
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    LinearProgressIndicator(
                        progress = { taskProgress },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    taskProgress.takeIf { it >= 0f }?.let { progress ->
                        Text(
                            text = "${(progress * 100).toInt()}%",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                    rateBytesPerSec?.let { bytes ->
                        val text = remember(bytes) { "${formatFileSize(bytes)}/s" }
                        Text(
                            text = text,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }
    }
}