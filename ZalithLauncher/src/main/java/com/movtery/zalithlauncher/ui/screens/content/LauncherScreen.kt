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
                toAccountManageScreen = toAccountManageScreen
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

    Box(
        modifier = modifier.background(
            Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF2E1410),
                    Color(0xFF7A2E16),
                    Color(0xFF3A1711)
                )
            )
        ),
        contentAlignment = Alignment.Center
    ) {
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

            // Phải: để trống cho cân bố cục (tài khoản đã nằm ở thanh bên)
            Box(modifier = Modifier.weight(1f))
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
