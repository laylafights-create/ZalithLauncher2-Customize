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

package com.movtery.zalithlauncher.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.movtery.zalithlauncher.ui.theme.McColors

/**
 * Công tắc kiểu Minecraft: rãnh vuông viền đen, bật = xanh lá, tắt = đá tối.
 * Nút trượt là một khối đá có cạnh sáng/tối giống [McButton].
 * Tham số [colors] được giữ lại để tương thích với chỗ gọi cũ nhưng không còn được dùng.
 */
@Composable
@Suppress("UNUSED_PARAMETER")
fun DefaultSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: SwitchColors = SwitchDefaults.colors(),
    interactionSource: MutableInteractionSource? = null
) {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()

    val trackWidth = 48.dp
    val trackHeight = 24.dp
    val thumbWidth = 22.dp

    val thumbOffset by animateDpAsState(
        targetValue = if (checked) trackWidth - thumbWidth - 8.dp else 0.dp,
        animationSpec = tween(120),
        label = "McSwitchThumb"
    )
    val trackColor by animateColorAsState(
        targetValue = if (checked) McColors.PlayGreen else McColors.Background,
        animationSpec = tween(120),
        label = "McSwitchTrack"
    )

    Box(
        modifier = modifier
            .alpha(if (enabled) 1f else 0.5f)
            .toggleable(
                value = checked,
                interactionSource = source,
                indication = null,
                enabled = enabled,
                role = Role.Switch,
                onValueChange = onCheckedChange
            )
            .size(trackWidth, trackHeight)
            .background(trackColor)
            .border(2.dp, McColors.Outline)
            .padding(2.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        val fill = if (pressed) McColors.StoneDark else McColors.Stone
        val light = McColors.StoneLight
        val dark = McColors.StoneDark
        Box(
            Modifier
                .offset(x = thumbOffset)
                .size(thumbWidth, trackHeight - 8.dp)
                .background(fill)
                .drawBehind {
                    val b = 2.dp.toPx()
                    val top = if (pressed) dark else light
                    val bottom = if (pressed) light else dark
                    drawRect(top, Offset.Zero, Size(size.width, b))
                    drawRect(top, Offset.Zero, Size(b, size.height))
                    drawRect(bottom, Offset(0f, size.height - b), Size(size.width, b))
                    drawRect(bottom, Offset(size.width - b, 0f), Size(b, size.height))
                }
                .border(1.dp, McColors.Outline)
        )
    }
}
