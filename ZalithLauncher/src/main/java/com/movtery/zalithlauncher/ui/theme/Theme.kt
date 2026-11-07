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

import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.movtery.zalithlauncher.setting.enums.isLauncherInDarkTheme
import com.movtery.zalithlauncher.utils.festival.Festival
import com.movtery.zalithlauncher.utils.festival.LocalFestivals
import com.movtery.zalithlauncher.viewmodel.BackgroundViewModel
import com.movtery.zalithlauncher.viewmodel.LocalBackgroundViewModel

/**
 * Hệ số thu nhỏ toàn bộ giao diện launcher (1.0 = kích thước gốc).
 * Giảm số này để GUI nhỏ hơn, tăng để GUI to hơn.
 */
const val McUiScale = 0.85f

/**
 * Theme của launcher, phong cách Minecraft Launcher.
 *
 * Các tham số [darkTheme] và [dynamicColor] được giữ lại để không phá chữ ký hàm
 * mà các Activity khác đang gọi; giao diện luôn dùng bảng màu tối của Minecraft.
 */
@Composable
@Suppress("UNUSED_PARAMETER")
fun ZalithLauncherTheme(
    darkTheme: Boolean = isLauncherInDarkTheme(),
    dynamicColor: Boolean = false,
    backgroundViewModel: BackgroundViewModel? = null,
    festivals: List<Festival> = emptyList(),
    content: @Composable () -> Unit
) {
    val base = LocalDensity.current
    val scaled = remember(base) {
        Density(
            density = base.density * McUiScale,
            fontScale = base.fontScale
        )
    }
    CompositionLocalProvider(
        LocalDensity provides scaled,
        LocalBackgroundViewModel provides backgroundViewModel,
        LocalFestivals provides festivals
    ) {
        MaterialExpressiveTheme(
            colorScheme = MinecraftColorScheme,
            motionScheme = MotionScheme.standard(),
            shapes = MinecraftShapes,
            typography = AppTypography,
            content = content
        )
    }
}
