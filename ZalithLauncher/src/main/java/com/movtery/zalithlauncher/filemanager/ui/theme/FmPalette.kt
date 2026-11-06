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

package com.movtery.zalithlauncher.filemanager.ui.theme

import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.movtery.zalithlauncher.ui.theme.McColors

@Composable
fun fmBackgroundColor(): Color = McColors.Content
@Composable
fun fmOnBackgroundColor(): Color = McColors.TextSecondary

@Composable
fun fmCardColor(): Color = McColors.Panel
@Composable
fun fmOnCardColor(): Color = McColors.TextPrimary

@Composable
fun fmSecondaryTextColor(): Color = McColors.TextMuted

@Composable
fun fmErrorColor(): Color = McColors.Error

@Composable
fun fmSelectionColor(): Color = McColors.PlayGreen.copy(alpha = 0.25f)

@Composable
fun fmTopbarColors(): TopAppBarColors = TopAppBarDefaults.topAppBarColors(
    containerColor = Color.Transparent,
    scrolledContainerColor = Color.Transparent,
    navigationIconContentColor = fmOnBackgroundColor(),
    titleContentColor = fmOnBackgroundColor(),
    actionIconContentColor = fmOnBackgroundColor(),
    subtitleContentColor = fmOnBackgroundColor(),
)

object FmAnimations {
    /** 滑动触发回弹动画时长（ms） */
    const val SWIPE_BACK_MS = 220
    /** 定位高亮闪烁停留时长（ms） */
    const val LOCATE_HIGHLIGHT_MS = 1400L
    /** 内容切换淡出时长（ms） */
    const val FADE_OUT_MS = 120
    /** 内容切换淡入时长（ms） */
    const val FADE_IN_MS = 180
}
