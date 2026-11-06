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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/gpl-3.0.txt>.
 */

package com.movtery.zalithlauncher.ui.screens.content.elements

import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.game.path.GamePath
import com.movtery.zalithlauncher.game.path.GamePathManager
import com.movtery.zalithlauncher.game.version.installed.Version
import com.movtery.zalithlauncher.ui.androidText
import com.movtery.zalithlauncher.utils.logging.Logger
import com.movtery.zalithlauncher.utils.string.getMessageOrToString
import com.movtery.zalithlauncher.ui.theme.McColors
import com.movtery.zalithlauncher.viewmodel.ErrorViewModel

private const val MC_TAG = "McInstallationElements"

/**
 * Tab lọc của màn "Installations" (All / Vanilla / Modded) giống Minecraft Launcher:
 * chữ trắng, vạch xanh lá bên dưới tab đang chọn.
 */
@Composable
fun McFilterTab(
    value: VersionCategory,
    versionsCount: Int,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val color = if (selected) McColors.TextPrimary else McColors.TextSecondary
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clickable(role = Role.Tab, onClick = onClick)
            .drawBehind {
                if (selected) {
                    drawRect(
                        color = McColors.PlayGreenLight,
                        topLeft = Offset(0f, size.height - 3.dp.toPx()),
                        size = Size(size.width, 3.dp.toPx())
                    )
                }
            }
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(value.textRes) + " ($versionsCount)",
            color = color,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1
        )
    }
}

/**
 * Một dòng "installation": biểu tượng, tên, thông tin phiên bản và các nút phụ.
 * Dòng đang chọn có nền sáng hơn và vạch xanh lá ở cạnh trái.
 */
@Composable
fun McVersionRow(
    version: Version,
    selected: Boolean,
    submitError: (ErrorViewModel.ThrowableMessage) -> Unit,
    modifier: Modifier = Modifier,
    onSelected: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onRenameClick: () -> Unit = {},
    onCopyClick: () -> Unit = {},
    onExportClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {},
    onPinned: () -> Unit = {}
) {
    val valid = version.isValid()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, McColors.Outline)
            .background(if (selected) McColors.PanelHigh else McColors.Panel)
            .drawBehind {
                if (selected) {
                    drawRect(
                        color = McColors.PlayGreenLight,
                        size = Size(4.dp.toPx(), size.height)
                    )
                }
            }
            .clickable(role = Role.RadioButton) { if (!selected) onSelected() }
            .padding(start = 14.dp, top = 6.dp, bottom = 6.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CommonVersionInfoLayout(
            modifier = Modifier.weight(1f),
            version = version,
            iconSize = 40.dp
        )

        IconButton(
            enabled = valid,
            onClick = {
                val current = version.pinnedState
                runCatching {
                    version.setPinnedAndSave(!current)
                }.onFailure { e ->
                    Logger.error(MC_TAG, "Failed to save version config!", e)
                    submitError(
                        ErrorViewModel.ThrowableMessage(
                            title = androidText(R.string.versions_config_failed_to_save),
                            message = androidText(e.getMessageOrToString())
                        )
                    )
                }.onSuccess { onPinned() }
            }
        ) {
            Icon(
                modifier = Modifier.size(22.dp),
                painter = painterResource(
                    if (version.pinnedState) R.drawable.ic_pinned_filled else R.drawable.ic_pinned_outlined
                ),
                tint = if (version.pinnedState) McColors.PlayGreenLight else McColors.TextSecondary,
                contentDescription = stringResource(R.string.versions_manage_pin)
            )
        }

        IconButton(onClick = onSettingsClick, enabled = valid) {
            Icon(
                modifier = Modifier.size(22.dp),
                painter = painterResource(R.drawable.ic_settings_filled),
                tint = McColors.TextSecondary,
                contentDescription = stringResource(R.string.versions_manage_settings)
            )
        }

        Box {
            var expanded by remember { mutableStateOf(false) }
            IconButton(onClick = { expanded = true }) {
                Icon(
                    modifier = Modifier.size(22.dp),
                    painter = painterResource(R.drawable.ic_more_horiz),
                    tint = McColors.TextSecondary,
                    contentDescription = stringResource(R.string.generic_more)
                )
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                containerColor = McColors.PanelHigh
            ) {
                McMenuItem(R.drawable.ic_edit_filled, R.string.generic_rename) {
                    onRenameClick(); expanded = false
                }
                McMenuItem(R.drawable.ic_file_copy_filled, R.string.generic_copy) {
                    onCopyClick(); expanded = false
                }
                McMenuItem(R.drawable.ic_folder_zip_filled, R.string.versions_export) {
                    onExportClick(); expanded = false
                }
                McMenuItem(R.drawable.ic_delete_filled, R.string.generic_delete) {
                    onDeleteClick(); expanded = false
                }
            }
        }
    }
}

@Composable
private fun McMenuItem(icon: Int, label: Int, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(text = stringResource(label), color = McColors.TextPrimary) },
        leadingIcon = {
            Icon(
                modifier = Modifier.size(20.dp),
                painter = painterResource(icon),
                tint = McColors.TextSecondary,
                contentDescription = null
            )
        },
        onClick = onClick
    )
}

/**
 * Một thư mục game (game path) trong cột trái, kiểu danh sách phẳng.
 */
@Composable
fun McGamePathItem(
    item: GamePath,
    selected: Boolean,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit = {},
    onRename: () -> Unit = {},
    onDelete: () -> Unit = {}
) {
    val notDefault = item.id != GamePathManager.DEFAULT_ID
    Row(
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else DisabledAlpha)
            .background(if (selected) McColors.Panel else Color.Transparent)
            .drawBehind {
                if (selected) {
                    drawRect(
                        color = McColors.PlayGreenLight,
                        size = Size(3.dp.toPx(), size.height)
                    )
                }
            }
            .clickable(enabled = enabled, role = Role.RadioButton, onClick = onClick)
            .padding(start = 12.dp, top = 8.dp, bottom = 8.dp, end = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE),
                text = if (notDefault) item.title else stringResource(R.string.versions_manage_game_path_default),
                color = if (selected) McColors.TextPrimary else McColors.TextSecondary,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1
            )
            Text(
                modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE),
                text = item.path,
                color = McColors.TextMuted,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1
            )
        }
        if (notDefault) {
            Box {
                var expanded by remember { mutableStateOf(false) }
                IconButton(modifier = Modifier.size(32.dp), onClick = { expanded = true }) {
                    Icon(
                        modifier = Modifier.size(18.dp),
                        painter = painterResource(R.drawable.ic_more_horiz),
                        tint = McColors.TextSecondary,
                        contentDescription = stringResource(R.string.generic_more)
                    )
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    containerColor = McColors.PanelHigh
                ) {
                    McMenuItem(R.drawable.ic_edit_filled, R.string.generic_rename) {
                        onRename(); expanded = false
                    }
                    McMenuItem(R.drawable.ic_delete_filled, R.string.generic_delete) {
                        onDelete(); expanded = false
                    }
                }
            }
        }
    }
}
