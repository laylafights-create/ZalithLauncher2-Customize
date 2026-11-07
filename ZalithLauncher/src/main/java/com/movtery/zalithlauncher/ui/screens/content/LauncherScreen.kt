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

package com.movtery.zalithlauncher.ui.screens.content

import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.movtery.zalithlauncher.game.versioninfo.MinecraftVersion
import com.movtery.zalithlauncher.game.versioninfo.MinecraftVersions
import com.movtery.zalithlauncher.game.versioninfo.models.isType
import com.movtery.zalithlauncher.ui.components.McPanel
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalContext
import com.movtery.petlib.PetAnimation
import com.movtery.zalithlauncher.game.account.wardrobe.toArmModel
import com.movtery.zalithlauncher.ui.components.PetModelView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import com.movtery.zalithlauncher.ui.screens.content.elements.VersionIconImage
import androidx.compose.foundation.layout.Spacer
import com.movtery.zalithlauncher.ui.theme.McStageBackground
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.PaddingValues
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.game.account.AccountsManager
import com.movtery.zalithlauncher.game.account.getAccountTypeName
import com.movtery.zalithlauncher.game.version.installed.Version
import com.movtery.zalithlauncher.game.version.installed.VersionsManager
import com.movtery.zalithlauncher.ui.base.BaseScreen
import com.movtery.zalithlauncher.ui.components.McButton
import com.movtery.zalithlauncher.ui.components.McButtonStyle
import com.movtery.zalithlauncher.ui.components.McNewsCard
import com.movtery.zalithlauncher.ui.screens.NestedNavKey
import com.movtery.zalithlauncher.ui.screens.NormalNavKey
import com.movtery.zalithlauncher.ui.screens.content.elements.CommonVersionInfoLayout
import com.movtery.zalithlauncher.ui.screens.content.elements.PlayerFace
import com.movtery.zalithlauncher.ui.theme.McColors
import com.movtery.zalithlauncher.viewmodel.ScreenBackStackViewModel


/**
 * Màn hình chính kiểu Minecraft Launcher:
 *  - phần trên: nhân vật 3D của tài khoản đứng giữa nền gradient
 *  - phần dưới: thanh Play (chọn phiên bản | nút PLAY | tài khoản)
 */
@Composable
fun LauncherScreen(
    backStackViewModel: ScreenBackStackViewModel,
    navigateToVersions: (Version) -> Unit,
    onLaunchGame: (Version?) -> Unit,
) {
    BaseScreen(
        screenKey = NormalNavKey.LauncherMain,
        currentKey = backStackViewModel.mainScreen.currentKey
    ) { _ ->
        val toVersionManageScreen: () -> Unit = {
            backStackViewModel.mainScreen.removeAndNavigateTo(
                remove = NestedNavKey.VersionSettings::class,
                screenKey = NormalNavKey.VersionsManager
            )
        }
        val toAccountManageScreen: () -> Unit = {
            backStackViewModel.mainScreen.navigateTo(
                screenKey = NormalNavKey.AccountManager(FirstLoginMenu.NONE)
            )
        }

        Column(modifier = Modifier.fillMaxSize()) {
            PlayerStage(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            )

            PlayBar(
                modifier = Modifier.fillMaxWidth(),
                onLaunchGame = onLaunchGame,
                onOpenVersionSettings = navigateToVersions,
                toVersionManageScreen = toVersionManageScreen,
                toAccountManageScreen = toAccountManageScreen,
                onInstallVersion = { mc ->
                    backStackViewModel.navigateToDownload(
                        targetScreen = backStackViewModel.downloadGameScreen.apply {
                            navigateTo(
                                NormalNavKey.DownloadGame.Addons(
                                    gameVersion = mc.version.id,
                                    type = mc.type
                                )
                            )
                        }
                    )
                }
            )
        }
    }
}

/**
 * Sân khấu giữa màn hình: nền gradient nâu đỏ, nhân vật 3D của tài khoản đang chọn
 * đứng giữa (kéo để xoay, chạm để có phản ứng).
 */
@Composable
private fun PlayerStage(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val account by AccountsManager.currentAccountFlow.collectAsStateWithLifecycle()

    // Đọc file skin của tài khoản; không có thì PetModelView dùng skin mặc định (Steve/Alex)
    val skin by produceState<ByteArray?>(null, account?.uniqueUUID, account?.skinModelType) {
        value = withContext(Dispatchers.IO) {
            account?.getSkinFile()
                ?.takeIf { it.exists() }
                ?.runCatching { readBytes() }
                ?.getOrNull()
        }
    }
    val cape by produceState<ByteArray?>(null, account?.uniqueUUID) {
        value = withContext(Dispatchers.IO) {
            account?.getCapeFile()
                ?.takeIf { it.exists() }
                ?.runCatching { readBytes() }
                ?.getOrNull()
        }
    }

    McStageBackground.load(context)
    val bgIndex = McStageBackground.selected
    val customVer = McStageBackground.customVersion
    val customBitmap by produceState<androidx.compose.ui.graphics.ImageBitmap?>(null, bgIndex, customVer) {
        value = if (bgIndex == McStageBackground.CUSTOM) {
            withContext(Dispatchers.IO) {
                runCatching {
                    val file = McStageBackground.customFile(context)
                    val bounds = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    android.graphics.BitmapFactory.decodeFile(file.absolutePath, bounds)
                    var sample = 1
                    while (bounds.outWidth / sample > 2400 || bounds.outHeight / sample > 2400) sample *= 2
                    val opts = android.graphics.BitmapFactory.Options().apply { inSampleSize = sample }
                    android.graphics.BitmapFactory.decodeFile(file.absolutePath, opts)?.asImageBitmap()
                }.getOrNull()
            }
        } else null
    }
    val presetColors = McStageBackground.presets.getOrElse(bgIndex) { McStageBackground.presets[0] }

    Box(
        modifier = modifier.background(Brush.verticalGradient(colors = presetColors)),
        contentAlignment = Alignment.Center
    ) {
        customBitmap?.let { bmp ->
            androidx.compose.foundation.Image(
                modifier = Modifier.fillMaxSize(),
                bitmap = bmp,
                contentDescription = null,
                contentScale = androidx.compose.ui.layout.ContentScale.Crop
            )
        }
        PetModelView(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.5f),
            skin = skin,
            cape = cape,
            arm = account?.skinModelType?.toArmModel(),
            animation = PetAnimation.Idle,
            gazeEnabled = false
        )
    }
}

/**
 * Thanh Play phía dưới: [chọn phiên bản] — [PLAY] — [tài khoản].
 */
@Composable
private fun PlayBar(
    modifier: Modifier = Modifier,
    onLaunchGame: (Version?) -> Unit,
    onOpenVersionSettings: (Version) -> Unit,
    toVersionManageScreen: () -> Unit,
    toAccountManageScreen: () -> Unit,
    onInstallVersion: (MinecraftVersion) -> Unit
) {
    val version by VersionsManager.currentVersion.collectAsStateWithLifecycle()
    val isRefreshing by VersionsManager.isRefreshing.collectAsStateWithLifecycle()
    val account by AccountsManager.currentAccountFlow.collectAsStateWithLifecycle()

    Column(modifier = modifier.background(McColors.Background)) {
        HorizontalDivider(color = McColors.Outline, thickness = 2.dp)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Trái: chọn phiên bản + nút cài đặt phiên bản
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    modifier = Modifier.widthIn(max = 340.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    VersionSelector(
                        modifier = Modifier.weight(1f),
                        version = version,
                        toVersionManageScreen = toVersionManageScreen,
                        onInstallVersion = onInstallVersion
                    )
                }
            }

            // Giữa: nút PLAY
            McButton(
                modifier = Modifier
                    .width(240.dp)
                    .height(56.dp),
                style = McButtonStyle.Play,
                enabled = !isRefreshing,
                onClick = { onLaunchGame(null) }
            ) {
                Text(
                    text = stringResource(R.string.main_launch_game).uppercase(),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Phải: để trống cho cân bố cục (tài khoản đã nằm ở thanh bên)
            Box(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun VersionSelector(
    version: Version?,
    toVersionManageScreen: () -> Unit,
    onInstallVersion: (MinecraftVersion) -> Unit,
    modifier: Modifier = Modifier
) {
    var showPicker by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        McButton(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            style = McButtonStyle.Stone,
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
            onClick = { showPicker = true }
        ) {
            if (version != null) {
                val info = remember(version) { version.getVersionInfo() }
                val valid = remember(version) { version.isValid() }
                VersionIconImage(
                    modifier = Modifier.size(32.dp),
                    version = version
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = version.getVersionName(),
                        color = McColors.TextPrimary,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val detail = if (!valid) {
                        stringResource(R.string.versions_manage_invalid)
                    } else {
                        buildList {
                            info?.let {
                                add(it.minecraftVersion)
                                it.loaderInfos.forEach { l -> add("${l.loader.displayName} ${l.version}") }
                            }
                        }.joinToString("  ·  ")
                    }
                    if (detail.isNotEmpty()) {
                        Text(
                            text = detail,
                            color = McColors.TextSecondary,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            } else {
                Text(
                    modifier = Modifier.weight(1f),
                    text = stringResource(R.string.game_launch_no_version),
                    color = McColors.TextSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                modifier = Modifier.size(24.dp),
                painter = painterResource(R.drawable.ic_arrow_drop_up_rounded),
                contentDescription = null,
                tint = McColors.TextSecondary
            )
        }
    }

    if (showPicker) {
        VersionPickerDialog(
            current = version,
            onDismiss = { showPicker = false },
            toVersionManageScreen = toVersionManageScreen,
            onInstallVersion = onInstallVersion
        )
    }
}

/** Bộ lọc loại phiên bản trong danh sách "Tất cả phiên bản". */
private enum class VersionTab(val labelRes: Int) {
    Release(R.string.mc_picker_release),
    Snapshot(R.string.mc_picker_snapshot),
    Old(R.string.mc_picker_old),
    AprilFools(R.string.mc_picker_april_fools)
}

/**
 * Hộp thoại chọn phiên bản kiểu TLauncher:
 *  - phần trên: các phiên bản đã cài (chạm để chọn chơi)
 *  - phần dưới: tất cả phiên bản Minecraft (lọc theo loại + tìm kiếm);
 *    bản chưa cài chạm vào sẽ mở màn hình cài đặt đúng phiên bản đó.
 */
@Composable
private fun VersionPickerDialog(
    current: Version?,
    onDismiss: () -> Unit,
    toVersionManageScreen: () -> Unit,
    onInstallVersion: (MinecraftVersion) -> Unit
) {
    val installed by VersionsManager.versions.collectAsStateWithLifecycle()
    val allVersions by MinecraftVersions.allVersions.collectAsStateWithLifecycle()
    var loadFailed by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var tab by remember { mutableStateOf(VersionTab.Release) }
    var query by remember { mutableStateOf("") }

    // Tải danh sách phiên bản chính thức (có cache 1 ngày trong MinecraftVersions)
    LaunchedEffect(Unit) {
        if (allVersions.isEmpty()) {
            loading = true
            loadFailed = runCatching { MinecraftVersions.refreshVersions(force = false) }.isFailure
            loading = false
        }
    }

    // Tập tên bản Minecraft đã cài, để đánh dấu "Đã cài"
    val installedIds = remember(installed) {
        installed.flatMap { listOfNotNull(it.getVersionName(), it.getVersionInfo()?.minecraftVersion) }.toSet()
    }

    val filtered = remember(allVersions, tab, query) {
        allVersions.filter { mc ->
            val okType = when (tab) {
                VersionTab.Release -> mc.isType(release = true, snapshot = false, aprilFools = false, old = false)
                VersionTab.Snapshot -> mc.isType(release = false, snapshot = true, aprilFools = false, old = false)
                VersionTab.Old -> mc.isType(release = false, snapshot = false, aprilFools = false, old = true)
                VersionTab.AprilFools -> mc.isType(release = false, snapshot = false, aprilFools = true, old = false)
            }
            okType && (query.isBlank() || mc.version.id.contains(query.trim(), ignoreCase = true))
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        McPanel(
            modifier = Modifier
                .widthIn(max = 460.dp)
                .fillMaxWidth(0.92f)
                .heightIn(max = 520.dp),
            color = McColors.Content
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Tiêu đề
                Text(
                    text = stringResource(R.string.mc_picker_title),
                    color = McColors.TextPrimary,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 16.dp, top = 14.dp, end = 16.dp, bottom = 8.dp)
                )
                HorizontalDivider(color = McColors.Outline, thickness = 2.dp)

                LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                    // ---- Đã cài ----
                    item {
                        PickerHeader(stringResource(R.string.mc_picker_installed))
                    }
                    if (installed.isEmpty()) {
                        item {
                            Text(
                                text = stringResource(R.string.game_launch_no_version),
                                color = McColors.TextMuted,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }
                    items(installed, key = { "i:" + it.getVersionName() }) { item ->
                        val selected = item == current
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (selected) McColors.PanelHigh else Color.Transparent)
                                .clickable {
                                    if (!selected) VersionsManager.saveVersion(item)
                                    onDismiss()
                                }
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CommonVersionInfoLayout(
                                modifier = Modifier.weight(1f),
                                version = item,
                                iconSize = 28.dp
                            )
                            if (selected) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_check),
                                    contentDescription = null,
                                    tint = McColors.PlayGreenLight,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                    item {
                        Text(
                            text = stringResource(R.string.page_title_version_list),
                            color = McColors.PlayGreenLight,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onDismiss()
                                    toVersionManageScreen()
                                }
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                        )
                        HorizontalDivider(color = McColors.Divider)
                    }

                    // ---- Tất cả phiên bản ----
                    item {
                        PickerHeader(stringResource(R.string.mc_picker_all_versions))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            VersionTab.entries.forEach { t ->
                                val sel = t == tab
                                Text(
                                    text = stringResource(t.labelRes),
                                    color = if (sel) Color.White else McColors.TextSecondary,
                                    fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal,
                                    style = MaterialTheme.typography.labelMedium,
                                    maxLines = 1,
                                    modifier = Modifier
                                        .background(if (sel) McColors.PlayGreen else McColors.Panel)
                                        .border(1.dp, if (sel) McColors.PlayGreenLight else McColors.Outline)
                                        .clickable { tab = t }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                        BasicTextField(
                            value = query,
                            onValueChange = { query = it },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(color = McColors.TextPrimary),
                            cursorBrush = SolidColor(McColors.PlayGreenLight),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                .background(McColors.Panel)
                                .border(1.dp, McColors.Outline)
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            decorationBox = { inner ->
                                Box {
                                    if (query.isEmpty()) {
                                        Text(
                                            text = stringResource(R.string.mc_picker_search),
                                            color = McColors.TextMuted,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                    inner()
                                }
                            }
                        )
                    }

                    when {
                        loading && allVersions.isEmpty() -> item {
                            Text(
                                text = stringResource(R.string.mc_picker_loading),
                                color = McColors.TextMuted,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                        loadFailed && allVersions.isEmpty() -> item {
                            Text(
                                text = stringResource(R.string.mc_picker_load_failed),
                                color = McColors.Error,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                        else -> items(filtered, key = { "a:" + it.version.id }) { mc ->
                            val isInstalled = mc.version.id in installedIds
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onDismiss()
                                        if (isInstalled) {
                                            installed.firstOrNull {
                                                it.getVersionName() == mc.version.id ||
                                                        it.getVersionInfo()?.minecraftVersion == mc.version.id
                                            }?.let { VersionsManager.saveVersion(it) }
                                        } else {
                                            onInstallVersion(mc)
                                        }
                                    }
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = mc.version.id,
                                    color = McColors.TextPrimary,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = stringResource(
                                        if (isInstalled) R.string.mc_picker_state_installed
                                        else R.string.mc_picker_state_download
                                    ),
                                    color = if (isInstalled) McColors.PlayGreenLight else McColors.TextMuted,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PickerHeader(text: String) {
    Text(
        text = text.uppercase(),
        color = McColors.TextMuted,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        style = MaterialTheme.typography.labelSmall,
        modifier = Modifier
            .fillMaxWidth()
            .background(McColors.Background)
            .padding(horizontal = 16.dp, vertical = 6.dp)
    )
}

@Composable
@Suppress("unused")
private fun AccountChip(
    account: com.movtery.zalithlauncher.game.account.Account?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (account != null) {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = account.username,
                    color = McColors.TextPrimary,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = getAccountTypeName(account),
                    color = McColors.TextMuted,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            PlayerFace(
                account = account,
                avatarSize = 44.dp
            )
        } else {
            Text(
                text = stringResource(R.string.account_add_new_account),
                color = McColors.TextPrimary,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Icon(
                painter = painterResource(R.drawable.ic_add),
                contentDescription = null,
                tint = McColors.TextPrimary
            )
        }
    }
}
