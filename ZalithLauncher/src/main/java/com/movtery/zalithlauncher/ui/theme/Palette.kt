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

package com.movtery.zalithlauncher.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.movtery.zalithlauncher.ui.components.influencedByBackgroundColor

/*
 * Các hàm màu dùng chung cho toàn bộ launcher, ánh xạ thẳng sang [McColors]
 * (phong cách Minecraft Launcher: xám than, phẳng, viền đen mảnh).
 * Chữ ký hàm được giữ nguyên để các màn hình cũ không cần sửa.
 */

/** Màu nền vùng nội dung. */
@Composable
fun backgroundColor(): Color = McColors.Content

@Composable
fun onBackgroundColor(): Color = McColors.TextSecondary

/**
 * Màu nền thẻ / dialog.
 * @param influencedByBackground vẫn hỗ trợ độ mờ khi người dùng đặt ảnh nền tuỳ chỉnh
 */
@Composable
fun cardColor(
    influencedByBackground: Boolean = true
): Color = influencedByBackgroundColor(
    color = McColors.Panel,
    enabled = influencedByBackground
)

@Composable
fun onCardColor(): Color = McColors.TextPrimary

/** Màu thanh tiêu đề trên cùng của thẻ: tối hơn thân thẻ, giống thanh tiêu đề của launcher. */
@Composable
fun cardTitleColor(
    alpha: Float = 0.5f
): Color = McColors.Background.copy(alpha = alpha.coerceAtLeast(0.85f))

/** Màu từng dòng (item) nằm trong thẻ. */
@Composable
@Suppress("UNUSED_PARAMETER")
fun itemColor(
    influencedByBackground: Boolean = true,
    isDark: Boolean = true
): Color = influencedByBackgroundColor(
    color = McColors.PanelHigh,
    enabled = influencedByBackground
)

@Composable
fun onItemColor() = McColors.TextPrimary
