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

package com.movtery.zalithlauncher.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import com.movtery.zalithlauncher.R
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.movtery.zalithlauncher.ui.theme.McColors

/*
 * Các hàm dưới đây trùng tên với Button / FilledTonalButton / OutlinedButton / TextButton / AlertDialog
 * của Material 3 nhưng vẽ theo phong cách Minecraft Launcher. Các file chỉ cần đổi import
 * từ androidx.compose.material3.X sang com.movtery.zalithlauncher.ui.components.X.
 */

/** Nút chính: xanh lá, giống nút Play. */
@Composable
fun Button(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    McButton(
        onClick = onClick,
        modifier = modifier.defaultMinSize(minHeight = 40.dp),
        style = McButtonStyle.Play,
        enabled = enabled,
        content = content
    )
}

/** Nút phụ: màu đá. */
@Composable
fun FilledTonalButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    McButton(
        onClick = onClick,
        modifier = modifier.defaultMinSize(minHeight = 40.dp),
        style = McButtonStyle.Stone,
        enabled = enabled,
        content = content
    )
}

/**
 * Nút viền: màu đá; [danger] = true sẽ đổi chữ sang màu đỏ (dùng cho hành động xoá).
 */
@Composable
fun OutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    danger: Boolean = false,
    content: @Composable RowScope.() -> Unit
) {
    McButton(
        onClick = onClick,
        modifier = modifier.defaultMinSize(minHeight = 40.dp),
        style = McButtonStyle.Stone,
        enabled = enabled
    ) {
        if (danger) {
            CompositionLocalProvider(LocalContentColor provides McColors.Error) {
                content()
            }
        } else {
            content()
        }
    }
}

/** Nút chữ phẳng, chữ xanh lá. */
@Composable
fun TextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    Box(
        modifier = modifier
            .alpha(if (enabled) 1f else 0.5f)
            .defaultMinSize(minHeight = 40.dp)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        CompositionLocalProvider(LocalContentColor provides McColors.PlayGreenLight) {
            androidx.compose.foundation.layout.Row(
                verticalAlignment = Alignment.CenterVertically,
                content = content
            )
        }
    }
}

/** Hộp thoại phẳng, nền xám than, góc gần vuông. */
@Composable
fun AlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: @Composable (() -> Unit)? = null,
    title: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
    properties: DialogProperties = DialogProperties()
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = confirmButton,
        modifier = modifier,
        dismissButton = dismissButton,
        title = title,
        text = text,
        shape = MaterialTheme.shapes.medium,
        containerColor = McColors.Panel,
        titleContentColor = McColors.TextPrimary,
        textContentColor = McColors.TextSecondary,
        tonalElevation = 0.dp,
        properties = properties
    )
}

/** Ô chọn vuông: viền đá, khi bật nền xanh lá và dấu tick trắng. */
@Composable
fun Checkbox(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val toggle = if (onCheckedChange != null) {
        Modifier.clickable(enabled = enabled, role = Role.Checkbox) { onCheckedChange(!checked) }
    } else Modifier
    Box(
        modifier = modifier
            .alpha(if (enabled) 1f else 0.5f)
            .then(toggle)
            .padding(10.dp)
            .size(20.dp)
            .background(if (checked) McColors.PlayGreen else McColors.Background)
            .border(2.dp, if (checked) McColors.PlayGreenLight else McColors.StoneLight),
        contentAlignment = Alignment.Center
    ) {
        if (checked) {
            Icon(
                painter = painterResource(R.drawable.ic_check),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

/** Nút chọn tròn: viền đá, chấm xanh lá khi được chọn. */
@Composable
fun RadioButton(
    selected: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val click = if (onClick != null) {
        Modifier.clickable(enabled = enabled, role = Role.RadioButton, onClick = onClick)
    } else Modifier
    Box(
        modifier = modifier
            .alpha(if (enabled) 1f else 0.5f)
            .then(click)
            .padding(10.dp)
            .size(20.dp)
            .background(McColors.Background, CircleShape)
            .border(2.dp, if (selected) McColors.PlayGreenLight else McColors.StoneLight, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Box(
                Modifier
                    .size(10.dp)
                    .background(McColors.PlayGreen, CircleShape)
            )
        }
    }
}
