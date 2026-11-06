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

private val BlockSize = 28.dp

/**
 * Màn hình chính kiểu Minecraft Launcher:
 *  - phần trên: ảnh nền khối pixel + logo chữ + hàng thẻ lối tắt
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
        val toDownloadScreen: () -> Unit = {
            backStackViewModel.navigateToDownload()
        }
        val toSettingsScreen: () -> Unit = {
            backStackViewModel.mainScreen.removeAndNavigateTo(
                removes = backStackViewModel.clearBeforeNavKeys,
                screenKey = backStackViewModel.settingsScreen
            )
        }

        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                PixelLandscape(modifier = Modifier.matchParentSize())

                HeroTitle(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 28.dp, top = 24.dp)
                )

                ShortcutRow(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth(),
                    toVersionManageScreen = toVersionManageScreen,
                    toDownloadScreen = toDownloadScreen,
                    toAccountManageScreen = toAccountManageScreen,
                    toSettingsScreen = toSettingsScreen
                )
            }

            PlayBar(
                modifier = Modifier.fillMaxWidth(),
                onLaunchGame = onLaunchGame,
                onOpenVersionSettings = navigateToVersions,
                toVersionManageScreen = toVersionManageScreen,
                toAccountManageScreen = toAccountManageScreen
            )
        }
    }
}

/**
 * Ảnh nền tự vẽ: bầu trời + mặt đất ghép từ các khối cỏ / đất / đá.
 * Không dùng tài nguyên ảnh nào nên không phụ thuộc vào drawable.
 */
@Composable
private fun PixelLandscape(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val block = BlockSize.toPx()
        val cols = (size.width / block).toInt() + 1
        val rows = (size.height / block).toInt() + 1
        val horizon = (rows * 0.58f).toInt().coerceAtLeast(1)
        val horizonY = horizon * block

        // Bầu trời
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF0E1A2B), Color(0xFF4A7BB5)),
                startY = 0f,
                endY = horizonY
            ),
            size = Size(size.width, horizonY)
        )
        // Mặt trời vuông
        drawRect(
            color = Color(0xFFF5E6A8),
            topLeft = Offset(size.width * 0.78f, block * 1.5f),
            size = Size(block * 1.6f, block * 1.6f)
        )

        val grass = listOf(Color(0xFF5B8731), Color(0xFF4F7A2A), Color(0xFF67963A))
        val dirt = listOf(Color(0xFF8A5A3B), Color(0xFF7A4F33), Color(0xFF96633F))
        val stone = listOf(Color(0xFF6B6B6B), Color(0xFF5E5E5E), Color(0xFF777777))

        for (row in horizon until rows) {
            for (col in 0 until cols) {
                val noise = (((col * 31 + row * 17).toLong() * 2654435761L) ushr 8)
                    .toInt()
                    .and(0x7fffffff) % 3
                val color = when {
                    row == horizon -> grass[noise]
                    row <= horizon + 2 -> dirt[noise]
                    else -> stone[noise]
                }
                drawRect(
                    color = color,
                    topLeft = Offset(col * block, row * block),
                    size = Size(block, block)
                )
            }
        }

        // Làm tối nửa dưới để chữ / thẻ dễ đọc
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color.Transparent, Color(0xCC000000)),
                startY = size.height * 0.45f,
                endY = size.height
            )
        )
    }
}

@Composable
private fun HeroTitle(modifier: Modifier = Modifier) {
    val shadow = Shadow(color = Color.Black, offset = Offset(5f, 5f), blurRadius = 0f)
    Column(modifier = modifier) {
        Text(
            text = "MINECRAFT",
            style = TextStyle(
                fontSize = 44.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 4.sp,
                color = Color.White,
                shadow = shadow
            )
        )
        Text(
            text = "JAVA EDITION",
            style = TextStyle(
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 8.sp,
                color = Color(0xFFE0E0E0),
                shadow = shadow
            )
        )
    }
}

@Composable
private fun ShortcutRow(
    modifier: Modifier = Modifier,
    toVersionManageScreen: () -> Unit,
    toDownloadScreen: () -> Unit,
    toAccountManageScreen: () -> Unit,
    toSettingsScreen: () -> Unit
) {
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            McNewsCard(
                modifier = Modifier.width(210.dp),
                title = stringResource(R.string.page_title_version_list),
                painter = painterResource(R.drawable.ic_folder_zip_filled),
                onClick = toVersionManageScreen
            )
        }
        item {
            McNewsCard(
                modifier = Modifier.width(210.dp),
                title = stringResource(R.string.generic_download),
                painter = painterResource(R.drawable.ic_download_2_filled),
                onClick = toDownloadScreen,
                accent = McColors.Dirt
            )
        }
        item {
            McNewsCard(
                modifier = Modifier.width(210.dp),
                title = stringResource(R.string.page_title_account_list),
                painter = painterResource(R.drawable.ic_person_filled),
                onClick = toAccountManageScreen,
                accent = Color(0xFF4A7BB5)
            )
        }
        item {
            McNewsCard(
                modifier = Modifier.width(210.dp),
                title = stringResource(R.string.generic_setting),
                painter = painterResource(R.drawable.ic_settings_filled),
                onClick = toSettingsScreen,
                accent = McColors.StoneLight
            )
        }
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
    toAccountManageScreen: () -> Unit
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
                        toVersionManageScreen = toVersionManageScreen
                    )
                    version?.takeIf { !isRefreshing && it.isValid() }?.let { current ->
                        IconButton(onClick = { onOpenVersionSettings(current) }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_settings_filled),
                                contentDescription = stringResource(R.string.versions_manage_settings),
                                tint = McColors.TextSecondary
                            )
                        }
                    }
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

            // Phải: tài khoản hiện tại
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterEnd
            ) {
                AccountChip(
                    account = account,
                    onClick = toAccountManageScreen
                )
            }
        }
    }
}

@Composable
private fun VersionSelector(
    version: Version?,
    toVersionManageScreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showList by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, McColors.Outline)
                .background(McColors.Panel)
                .clickable { showList = true }
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.weight(1f)) {
                if (version != null) {
                    CommonVersionInfoLayout(
                        version = version,
                        iconSize = 32.dp
                    )
                } else {
                    Text(
                        text = stringResource(R.string.game_launch_no_version),
                        color = McColors.TextSecondary,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Icon(
                modifier = Modifier.size(24.dp),
                painter = painterResource(R.drawable.ic_arrow_drop_up_rounded),
                contentDescription = null,
                tint = McColors.TextSecondary
            )
        }

        DropdownMenu(
            expanded = showList,
            onDismissRequest = { showList = false },
            modifier = Modifier.width(280.dp)
        ) {
            val versions by VersionsManager.versions.collectAsStateWithLifecycle()
            versions.forEach { item ->
                DropdownMenuItem(
                    text = {
                        CommonVersionInfoLayout(
                            modifier = Modifier.fillMaxWidth(),
                            version = item,
                            iconSize = 28.dp
                        )
                    },
                    onClick = {
                        if (item != version) VersionsManager.saveVersion(item)
                        showList = false
                    }
                )
            }
            if (versions.isNotEmpty()) HorizontalDivider(color = McColors.Divider)
            DropdownMenuItem(
                text = { Text(text = stringResource(R.string.page_title_version_list)) },
                onClick = {
                    showList = false
                    toVersionManageScreen()
                }
            )
        }
    }
}

@Composable
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
