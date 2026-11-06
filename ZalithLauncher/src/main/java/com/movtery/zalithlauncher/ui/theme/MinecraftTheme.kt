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

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Bảng màu mô phỏng giao diện Minecraft Launcher chính thức:
 * nền xám than, nút "Play" xanh lá, nút phụ màu đá, đường viền đen mảnh.
 */
object McColors {
    val Background = Color(0xFF1E1E1F)
    val Content = Color(0xFF262627)
    val Panel = Color(0xFF313233)
    val PanelHigh = Color(0xFF3A3B3C)
    val Outline = Color(0xFF0F0F10)
    val Divider = Color(0xFF48494A)

    val TextPrimary = Color(0xFFFFFFFF)
    val TextSecondary = Color(0xFFC6C6C6)
    val TextMuted = Color(0xFF8B8B8C)

    // Nút "Play" xanh lá
    val PlayGreen = Color(0xFF3C8527)
    val PlayGreenLight = Color(0xFF52A535)
    val PlayGreenDark = Color(0xFF2A5E1B)

    // Nút phụ màu đá
    val Stone = Color(0xFF58585A)
    val StoneLight = Color(0xFF7A7A7C)
    val StoneDark = Color(0xFF3A3A3C)

    val Dirt = Color(0xFF8A5A3B)
    val Error = Color(0xFFE0524D)
}

/** Minecraft Launcher chỉ có giao diện tối. */
val MinecraftColorScheme = darkColorScheme(
    primary = McColors.PlayGreen,
    onPrimary = Color.White,
    primaryContainer = McColors.PlayGreenDark,
    onPrimaryContainer = Color.White,
    secondary = McColors.Stone,
    onSecondary = Color.White,
    secondaryContainer = McColors.Stone,
    onSecondaryContainer = Color.White,
    tertiary = McColors.Dirt,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF5E3D27),
    onTertiaryContainer = Color.White,
    error = McColors.Error,
    onError = Color.White,
    errorContainer = Color(0xFF7A2A27),
    onErrorContainer = Color.White,
    background = McColors.Background,
    onBackground = McColors.TextPrimary,
    surface = McColors.Panel,
    onSurface = McColors.TextPrimary,
    surfaceVariant = McColors.Divider,
    onSurfaceVariant = McColors.TextSecondary,
    outline = Color(0xFF6D6D6E),
    outlineVariant = McColors.Divider,
    scrim = Color.Black,
    inverseSurface = Color(0xFFE6E6E6),
    inverseOnSurface = Color(0xFF1E1E1F),
    inversePrimary = McColors.PlayGreenLight,
    surfaceDim = Color(0xFF151516),
    surfaceBright = McColors.PanelHigh,
    surfaceContainerLowest = Color(0xFF151516),
    surfaceContainerLow = McColors.Background,
    surfaceContainer = McColors.Content,
    surfaceContainerHigh = McColors.Panel,
    surfaceContainerHighest = McColors.PanelHigh,
)

/** Góc gần như vuông, giống phong cách khối của Minecraft. */
val MinecraftShapes = Shapes(
    extraSmall = RoundedCornerShape(0.dp),
    small = RoundedCornerShape(0.dp),
    medium = RoundedCornerShape(2.dp),
    large = RoundedCornerShape(2.dp),
    extraLarge = RoundedCornerShape(4.dp),
)
