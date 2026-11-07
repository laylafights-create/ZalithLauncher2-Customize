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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.graphics.Color
import java.io.File
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

/**
 * Kích thước model nhân vật 3D (màn hình Chơi và Skin), tính theo % so với khung hiển thị.
 */
object McModelScale {
    const val DEFAULT_PERCENT = 100
    const val MIN_PERCENT = 40
    const val MAX_PERCENT = 100

    private const val PREFS = "mc_ui_scale"
    private const val KEY = "model_percent"

    private var loaded = false

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

/**
 * Nền phía sau nhân vật ở màn hình Chơi: chọn một bộ màu có sẵn hoặc dùng ảnh riêng.
 */
object McStageBackground {
    /** Chỉ số bộ màu; [CUSTOM] = dùng ảnh người dùng chọn. */
    const val CUSTOM = -1

    val presets: List<List<Color>> = listOf(
        listOf(Color(0xFF2E1410), Color(0xFF7A2E16), Color(0xFF3A1711)), // 0 Nether (mặc định)
        listOf(Color(0xFF14304F), Color(0xFF3F7FB5), Color(0xFF2D5A2D)), // 1 Overworld
        listOf(Color(0xFF0B0D1A), Color(0xFF1B2250), Color(0xFF0B0D1A)), // 2 Đêm
        listOf(Color(0xFF0F2415), Color(0xFF2F6B3A), Color(0xFF15301B)), // 3 Rừng
        listOf(Color(0xFF0E0A18), Color(0xFF3B2A5A), Color(0xFF0E0A18)), // 4 The End
        listOf(Color(0xFF1E1E1F), Color(0xFF2A2A2B), Color(0xFF1E1E1F)), // 5 Xám tối
    )

    private const val PREFS = "mc_ui_scale"
    private const val KEY = "stage_bg"
    private const val FILE_NAME = "mc_stage_bg.img"

    private var loaded = false

    var selected by mutableIntStateOf(0)
        private set

    /** Tăng mỗi khi ảnh tùy chọn đổi để giao diện tải lại. */
    var customVersion by mutableIntStateOf(0)
        private set

    fun customFile(context: Context): File = File(context.filesDir, FILE_NAME)

    fun load(context: Context) {
        if (loaded) return
        loaded = true
        selected = context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getInt(KEY, 0)
        if (selected == CUSTOM && !customFile(context).exists()) selected = 0
    }

    fun select(context: Context, index: Int) {
        selected = index
        context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putInt(KEY, index).apply()
    }

    /** Sao chép ảnh người dùng chọn vào bộ nhớ app và chọn làm nền. */
    fun setCustom(context: Context, uri: android.net.Uri): Boolean {
        return runCatching {
            context.contentResolver.openInputStream(uri)?.use { input ->
                customFile(context).outputStream().use { input.copyTo(it) }
            } ?: return false
            customVersion++
            select(context, CUSTOM)
            true
        }.getOrDefault(false)
    }
}
