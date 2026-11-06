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

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.movtery.zalithlauncher.ui.theme.McColors

/** Kiểu nút Minecraft: [Play] xanh lá, [Stone] màu đá. */
enum class McButtonStyle { Play, Stone }

/**
 * Nút kiểu Minecraft Launcher: phẳng, viền đen mảnh, cạnh trên/trái sáng và
 * cạnh dưới/phải tối (đảo ngược khi nhấn) để tạo cảm giác khối nổi.
 */
@Composable
fun McButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: McButtonStyle = McButtonStyle.Stone,
    enabled: Boolean = true,
    bevel: Dp = 3.dp,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    val fill: Color
    val light: Color
    val dark: Color
    when (style) {
        McButtonStyle.Play -> {
            fill = McColors.PlayGreen; light = McColors.PlayGreenLight; dark = McColors.PlayGreenDark
        }
        McButtonStyle.Stone -> {
            fill = McColors.Stone; light = McColors.StoneLight; dark = McColors.StoneDark
        }
    }

    Box(
        modifier = modifier
            .alpha(if (enabled) 1f else 0.5f)
            .border(1.dp, McColors.Outline)
            .background(if (pressed) dark else fill)
            .drawBehind {
                val b = bevel.toPx()
                val top = if (pressed) dark else light
                val bottom = if (pressed) light else dark
                // cạnh sáng: trên + trái
                drawRect(top, Offset.Zero, Size(size.width, b))
                drawRect(top, Offset.Zero, Size(b, size.height))
                // cạnh tối: dưới + phải
                drawRect(bottom, Offset(0f, size.height - b), Size(size.width, b))
                drawRect(bottom, Offset(size.width - b, 0f), Size(b, size.height))
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick
            )
            .padding(contentPadding),
        contentAlignment = Alignment.Center
    ) {
        CompositionLocalProvider(LocalContentColor provides McColors.TextPrimary) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                content = content
            )
        }
    }
}

/** Khung nội dung phẳng với viền đen mảnh. */
@Composable
fun McPanel(
    modifier: Modifier = Modifier,
    color: Color = McColors.Panel,
    borderColor: Color = McColors.Outline,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .background(color)
            .border(1.dp, borderColor),
        content = content
    )
}

/**
 * Mục trên thanh điều hướng bên trái (giống Play / Installations / Skins... của Minecraft Launcher).
 * Mục đang chọn có nền sáng hơn và vạch xanh lá bên trái.
 */
@Composable
fun McSidebarItem(
    selected: Boolean,
    painter: Painter,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val contentColor = if (selected) McColors.TextPrimary else McColors.TextSecondary
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(if (selected) McColors.Panel else Color.Transparent)
            .drawBehind {
                if (selected) {
                    drawRect(
                        color = McColors.PlayGreenLight,
                        topLeft = Offset.Zero,
                        size = Size(3.dp.toPx(), size.height)
                    )
                }
            }
            .clickable(role = Role.Tab, onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                modifier = Modifier.size(26.dp),
                painter = painter,
                contentDescription = label,
                tint = contentColor
            )
            Text(
                text = label,
                color = contentColor,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Thẻ lối tắt trong khu vực "tin tức" của màn hình chính.
 */
@Composable
fun McNewsCard(
    title: String,
    painter: Painter,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = McColors.PlayGreenLight
) {
    Column(
        modifier = modifier
            .border(1.dp, McColors.Outline)
            .background(McColors.Background.copy(alpha = 0.92f))
            .clickable(role = Role.Button, onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .background(accent)
        )
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                modifier = Modifier.size(24.dp),
                painter = painter,
                contentDescription = null,
                tint = McColors.TextPrimary
            )
            Text(
                text = title,
                color = McColors.TextPrimary,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** Đường kẻ dọc mảnh phân tách thanh bên và nội dung. */
@Composable
fun McVerticalDivider(modifier: Modifier = Modifier) {
    Spacer(
        modifier = modifier
            .fillMaxHeight()
            .width(1.dp)
            .background(McColors.Outline)
    )
}
