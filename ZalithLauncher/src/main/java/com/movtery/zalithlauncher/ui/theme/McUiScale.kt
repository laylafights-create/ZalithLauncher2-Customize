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

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue

/**
 * Hệ số thu/phóng giao diện launcher do người dùng chọn (Cài đặt launcher → Kích thước giao diện).
 * Lưu bằng SharedPreferences riêng nên không phụ thuộc AllSettings.
 */
object McUiScale {
    const val DEFAULT_PERCENT = 85
    const val MIN_PERCENT = 60
    const val MAX_PERCENT = 120

    private const val PREFS = "mc_ui_scale"
    private const val KEY = "percent"

    private var loaded = false

    /** Hệ số hiện tại (1.0 = 100%). Là state của Compose nên giao diện tự cập nhật khi đổi. */
    var scale by mutableFloatStateOf(DEFAULT_PERCENT / 100f)
        private set

    fun percent(): Int = Math.round(scale * 100f)

    fun load(context: Context) {
        if (loaded) return
        loaded = true
        val p = context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getInt(KEY, DEFAULT_PERCENT)
        scale = p.coerceIn(MIN_PERCENT, MAX_PERCENT) / 100f
    }

    fun save(context: Context, percent: Int) {
        val p = percent.coerceIn(MIN_PERCENT, MAX_PERCENT)
        scale = p / 100f
        context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putInt(KEY, p).apply()
    }
}
