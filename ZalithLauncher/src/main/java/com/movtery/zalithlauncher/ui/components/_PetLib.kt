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

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random
import androidx.compose.ui.Modifier
import com.movtery.zalithlauncher.ui.theme.McModelScale
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import com.movtery.petlib.ArmModel
import com.movtery.petlib.PetAnimation
import com.movtery.petlib.PetController
import com.movtery.petlib.PetGazeRouter
import com.movtery.petlib.PetPreview
import com.movtery.petlib.PetTapReaction

/**
 * 设置皮肤，若皮肤数据传 null，则在 assets 中加载默认皮肤
 * @param skin PNG 皮肤数据
 * @param arm 手臂模型类别，为 null 则自动检测类型
 * @param onError 失败时在渲染线程的回调
 */
fun PetController.setSkinDefaultable(
    context: Context,
    skin: ByteArray?,
    arm: ArmModel?,
    onError: ((String) -> Unit)? = null
) {
    val nonNullSkin = skin ?: context.loadDefaultSkin(arm)
    setSkin(nonNullSkin, arm, onError)
}

private fun Context.loadDefaultSkin(arm: ArmModel?): ByteArray = if (arm == ArmModel.Slim) {
    assets.open("skin/alex.png")
} else {
    assets.open("skin/steve.png")
}.use { it.readBytes() }

/**
 * 玩家模型预览的中介组件：内部组装控制器生命周期、皮肤与披风加载、
 * 全局鼠标视线跟随与手势交互，调用端只需提供渲染数据。
 *
 * @param skin 皮肤数据，null 时使用按臂型区分的默认皮肤
 * @param cape 披风数据，null 时无披风
 * @param arm 手臂模型类别，null 按皮肤自动检测
 * @param yaw 整体偏航角（弧度），参数变化时模型转向该角度
 * @param animation 循环动画，null 不播放
 * @param animationSpeed 动画播放速度倍率
 * @param tapReaction 点按模型命中时的反馈形式
 * @param dragYaw 是否响应拖拽旋转偏航
 * @param gazeEnabled 是否跟随 Activity 全局鼠标位置（窗口坐标）；
 * 独立窗口中的预览与事件泵不同坐标系，应传 false
 * @param playful bật các hành động chờ nghịch ngợm, thỉnh thoảng chọn ngẫu nhiên một trò (không lặp lại liền kề):
 * nhìn quanh / ngẩng cúi, xoay một vòng / ngược chiều / hai vòng / bàn xoay trưng bày, lắc đầu, quay lưng,
 * xem mặt nghiêng, đảo thân như con lắc, rung lắc, ngó hai bên, đi bộ, đi thong thả, chạy, nhảy,
 * đi vòng, vừa chạy vừa xoay (xem [IdleAction])
 */
@Composable
fun PetModelView(
    modifier: Modifier = Modifier,
    skin: ByteArray? = null,
    cape: ByteArray? = null,
    arm: ArmModel? = null,
    yaw: Float = 0f,
    animation: PetAnimation? = PetAnimation.Idle,
    animationSpeed: Float = 1f,
    tapReaction: PetTapReaction = PetTapReaction.Squash,
    dragYaw: Boolean = true,
    gazeEnabled: Boolean = true,
    playful: Boolean = false,
) {
    val context = LocalContext.current
    val petController = remember {
        PetController(skin ?: context.loadDefaultSkin(arm), arm)
    }
    DisposableEffect(petController) {
        onDispose { petController.close() }
    }

    LaunchedEffect(petController, skin, arm) {
        petController.setSkinDefaultable(context, skin, arm)
    }
    LaunchedEffect(petController, cape) {
        petController.setCape(cape)
    }
    LaunchedEffect(petController, animation, animationSpeed) {
        if (animation != null) {
            petController.playAnimation(animation, animationSpeed)
        }
    }
    LaunchedEffect(petController, yaw) {
        petController.setModelYaw(yaw)
    }

    var previewBounds by remember { mutableStateOf<Rect?>(null) }

    // Hành động chờ nghịch ngợm: cứ vài giây lại chọn ngẫu nhiên một trò (không lặp lại trò vừa rồi),
    // xong trả mô hình về trạng thái gốc
    LaunchedEffect(petController, playful, animation, animationSpeed, yaw, gazeEnabled) {
        if (!playful) return@LaunchedEffect
        val rnd = Random.Default
        var last: IdleAction? = null
        delay(1800L)
        while (true) {
            delay(rnd.nextLong(3000L, 7000L))
            val bounds = previewBounds
            val canWalk = animation != null
            val canLook = !gazeEnabled && bounds != null
            val pool = IdleAction.entries.filter {
                it != last && (!it.needsWalk || canWalk) && (!it.needsGaze || canLook)
            }
            val action = pool.random(rnd)
            last = action
            try {
                petController.perform(action, yaw, bounds, rnd)
            } finally {
                petController.setGaze(null)
                if (animation != null) petController.playAnimation(animation, animationSpeed)
                petController.setModelYaw(yaw)
            }
        }
    }
    DisposableEffect(petController, gazeEnabled) {
        if (!gazeEnabled) {
            onDispose { }
        } else {
            val gazeTarget = object : PetGazeRouter.Target {
                override fun onCursor(x: Float, y: Float) {
                    val bounds = previewBounds ?: return
                    petController.setGaze(Pair(x - bounds.left, y - bounds.top))
                }

                override fun onGazeExpired() {
                    petController.setGaze(null)
                }
            }
            PetGazeRouter.register(gazeTarget)
            onDispose { PetGazeRouter.unregister(gazeTarget) }
        }
    }

    McModelScale.load(context)
    val modelScale = McModelScale.scale
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        PetPreview(
            controller = petController,
            modifier = Modifier
                .fillMaxWidth(modelScale.coerceAtMost(1f))
                .fillMaxHeight(modelScale.coerceAtMost(1f))
                .onGloballyPositioned { previewBounds = it.boundsInWindow() },
            tapReaction = tapReaction,
            dragYaw = dragYaw
        )
    }
}

/** Xoay mượt từ góc [from] đến [to] (radian) trong [durationMs] mili giây. */
private suspend fun PetController.turnYaw(from: Float, to: Float, durationMs: Long) {
    val steps = (durationMs / 16L).toInt().coerceAtLeast(1)
    for (i in 1..steps) {
        val t = i / steps.toFloat()
        val eased = t * t * (3f - 2f * t)
        setModelYaw(from + (to - from) * eased)
        delay(16L)
    }
}

/** Các hành động chờ của nhân vật khi bật [PetModelView] `playful`. */
private enum class IdleAction(val needsWalk: Boolean = false, val needsGaze: Boolean = false) {
    /** Đảo mắt nhìn quanh */
    LookAround(needsGaze = true),
    /** Ngẩng lên rồi cúi xuống nhìn */
    LookUpDown(needsGaze = true),
    /** Xoay một vòng */
    Spin,
    /** Xoay ngược chiều, nhanh */
    SpinReverse,
    /** Xoay liền hai vòng */
    DoubleSpin,
    /** Xoay chậm một vòng như bàn xoay trưng bày */
    Showcase,
    /** Lắc đầu ngó nghiêng */
    HeadShake,
    /** Quay lưng rồi quay lại */
    TurnBack,
    /** Quay sang hai bên cho xem mặt nghiêng */
    ShowProfile,
    /** Đảo thân qua lại như con lắc */
    Sway,
    /** Rung lắc nhanh, nhịp nhàng */
    Shimmy,
    /** Ngó trái, ngó phải */
    ScanSides,
    /** Đi bộ vài bước */
    Walk(needsWalk = true),
    /** Đi chậm thong thả, hơi nghiêng ngó */
    Stroll(needsWalk = true),
    /** Chạy nhanh, người lắc nhẹ */
    Run(needsWalk = true),
    /** Nhảy lắc lư theo nhịp */
    Dance(needsWalk = true),
    /** Đi vòng: quay lưng đi rồi quay về */
    WalkAround(needsWalk = true),
    /** Vừa chạy vừa xoay một vòng */
    RunSpin(needsWalk = true),
}

private suspend fun PetController.perform(
    action: IdleAction,
    yaw: Float,
    bounds: Rect?,
    rnd: Random,
) {
    val tau = (2 * PI).toFloat()
    val pi = PI.toFloat()
    when (action) {
        IdleAction.LookAround -> {
            val b = bounds ?: return
            repeat(rnd.nextInt(2, 4)) {
                setGaze(Pair(rnd.nextFloat() * b.width, rnd.nextFloat() * b.height * 0.6f))
                delay(rnd.nextLong(700L, 1300L))
            }
        }
        IdleAction.LookUpDown -> {
            val b = bounds ?: return
            setGaze(Pair(b.width / 2f, 0f))
            delay(900L)
            setGaze(Pair(b.width / 2f, b.height * 0.9f))
            delay(900L)
            setGaze(Pair(b.width * 0.15f, b.height * 0.35f))
            delay(700L)
        }
        IdleAction.Spin -> turnYaw(yaw, yaw + tau, 1500L)
        IdleAction.SpinReverse -> turnYaw(yaw, yaw - tau, 1100L)
        IdleAction.DoubleSpin -> turnYaw(yaw, yaw + 2 * tau, 2400L)
        IdleAction.Showcase -> turnYaw(yaw, yaw + tau, 4200L)
        IdleAction.HeadShake -> {
            var cur = yaw
            for (target in floatArrayOf(yaw + 0.6f, yaw - 0.6f, yaw + 0.5f, yaw - 0.4f, yaw)) {
                turnYaw(cur, target, 280L)
                cur = target
            }
        }
        IdleAction.TurnBack -> {
            turnYaw(yaw, yaw + pi, 700L)
            delay(1200L)
            turnYaw(yaw + pi, yaw + tau, 700L)
        }
        IdleAction.ShowProfile -> {
            turnYaw(yaw, yaw + pi / 2f, 600L)
            delay(900L)
            turnYaw(yaw + pi / 2f, yaw - pi / 2f, 1100L)
            delay(900L)
            turnYaw(yaw - pi / 2f, yaw, 600L)
        }
        IdleAction.Sway -> oscillateYaw(yaw, amplitude = 0.8f, cycles = 2.5f, durationMs = 3200L)
        IdleAction.Shimmy -> oscillateYaw(yaw, amplitude = 0.25f, cycles = 8f, durationMs = 1600L)
        IdleAction.ScanSides -> {
            turnYaw(yaw, yaw + 0.9f, 500L)
            delay(600L)
            turnYaw(yaw + 0.9f, yaw - 0.9f, 900L)
            delay(600L)
            turnYaw(yaw - 0.9f, yaw, 500L)
        }
        IdleAction.Walk -> {
            playAnimation(PetAnimation.Walking, 1.4f)
            delay(2500L)
        }
        IdleAction.Stroll -> {
            playAnimation(PetAnimation.Walking, 0.7f)
            turnYaw(yaw, yaw - 0.5f, 1200L)
            turnYaw(yaw - 0.5f, yaw + 0.5f, 1600L)
            turnYaw(yaw + 0.5f, yaw, 1200L)
        }
        IdleAction.Run -> {
            playAnimation(PetAnimation.Walking, 2.6f)
            oscillateYaw(yaw, amplitude = 0.12f, cycles = 6f, durationMs = 2200L)
        }
        IdleAction.Dance -> {
            playAnimation(PetAnimation.Walking, 1.8f)
            oscillateYaw(yaw, amplitude = 0.45f, cycles = 5f, durationMs = 2400L)
            turnYaw(yaw, yaw + tau, 900L)
        }
        IdleAction.WalkAround -> {
            playAnimation(PetAnimation.Walking, 1.0f)
            turnYaw(yaw, yaw + pi, 1100L)
            delay(900L)
            turnYaw(yaw + pi, yaw + tau, 1100L)
        }
        IdleAction.RunSpin -> {
            playAnimation(PetAnimation.Walking, 2.4f)
            turnYaw(yaw, yaw + tau, 2000L)
        }
    }
}

/** Đảo yaw quanh [base] theo hình sin: biên độ [amplitude] (radian), [cycles] chu kỳ trong [durationMs]. */
private suspend fun PetController.oscillateYaw(
    base: Float,
    amplitude: Float,
    cycles: Float,
    durationMs: Long,
) {
    val steps = (durationMs / 16L).toInt().coerceAtLeast(1)
    for (i in 1..steps) {
        val t = i / steps.toFloat()
        setModelYaw(base + amplitude * sin(2f * PI.toFloat() * cycles * t))
        delay(16L)
    }
    setModelYaw(base)
}
