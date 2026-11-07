//! Lấy mẫu keyframe clip (chỉ còn clip khoe cape).

use glam::{Mat3, Quat, Vec2, Vec3, Vec4};

use crate::clip_data::{Clip, Joint, CLIP_FPS};
use crate::pose::{flip_quat, ARM_REST_PIVOTS, LEG_REST_PIVOTS};

/// 采样剪辑得到的骨架关节目标
#[derive(Clone, Copy)]
pub(crate) struct ClipPose {
    pub body_rot: Quat,
    pub head_rot: Quat,
    /// [左, 右]
    pub arms: [Quat; 2],
    /// [左, 右]
    pub legs: [Quat; 2],
    pub body_pos: Vec3,
    /// 臂枢轴 [左, 右](肩点,像素)
    pub arm_pivots: [Vec3; 2],
    /// 腿枢轴 [左, 右](髋点,像素)
    pub leg_pivots: [Vec3; 2],
    /// 披风欧拉 (x, z)
    pub cape: Vec2,
}

/// 采样剪辑在时间 `t`（秒）的姿态
pub(crate) fn sample(clip: &Clip, t: f32) -> ClipPose {
    let mut pose = ClipPose {
        body_rot: Quat::IDENTITY,
        head_rot: Quat::IDENTITY,
        arms: [Quat::IDENTITY; 2],
        legs: [Quat::IDENTITY; 2],
        body_pos: Vec3::ZERO,
        arm_pivots: ARM_REST_PIVOTS,
        leg_pivots: LEG_REST_PIVOTS,
        cape: Vec2::ZERO,
    };
    for channel in clip.channels {
        match channel.joint {
            Joint::Body => {
                pose.body_rot = sample_rotation(channel.rotation, t);
                if !channel.translation.is_empty() {
                    pose.body_pos = sample_translation(channel.translation, t);
                }
            }
            Joint::Head => pose.head_rot = sample_rotation(channel.rotation, t),
            Joint::ArmLeft => {
                pose.arms[0] = sample_rotation(channel.rotation, t);
                if !channel.translation.is_empty() {
                    pose.arm_pivots[0] = sample_translation(channel.translation, t);
                }
            }
            Joint::ArmRight => {
                pose.arms[1] = sample_rotation(channel.rotation, t);
                if !channel.translation.is_empty() {
                    pose.arm_pivots[1] = sample_translation(channel.translation, t);
                }
            }
            Joint::LegLeft => {
                pose.legs[0] = sample_rotation(channel.rotation, t);
                if !channel.translation.is_empty() {
                    pose.leg_pivots[0] = sample_translation(channel.translation, t);
                }
            }
            Joint::LegRight => {
                pose.legs[1] = sample_rotation(channel.rotation, t);
                if !channel.translation.is_empty() {
                    pose.leg_pivots[1] = sample_translation(channel.translation, t);
                }
            }
            // 披风欧拉编码渲染基下的世界朝向:局部旋转先经根共轭再分解
            Joint::Cape => pose.cape = cape_euler(flip_quat(sample_rotation(channel.rotation, t))),
        }
    }
    pose
}

/// Catmull-Rom (uniform) giữa p1 và p2 tại t ∈ [0, 1]: vận tốc liên tục tại mọi key,
/// khác với nội suy tuyến tính/slerp vốn gãy vận tốc ở mỗi khung 30fps (nguyên nhân gây cảm giác "cứng").
fn catmull(p0: Vec4, p1: Vec4, p2: Vec4, p3: Vec4, t: f32) -> Vec4 {
    let t2 = t * t;
    let t3 = t2 * t;
    (p1 * 2.
        + (p2 - p0) * t
        + (p0 * 2. - p1 * 5. + p2 * 4. - p3) * t2
        + (p1 * 3. - p0 - p2 * 3. + p3) * t3)
        * 0.5
}

/// Chỉ số khung lân cận: clip tuần hoàn (khoá đầu == khoá cuối) thì quấn vòng để không gãy ở mối nối.
fn neighbours(len: usize, looped: bool, i0: usize) -> (usize, usize) {
    let i1 = i0 + 1;
    let prev = if i0 > 0 {
        i0 - 1
    } else if looped {
        len.saturating_sub(2)
    } else {
        0
    };
    let next = if i1 + 1 < len {
        i1 + 1
    } else if looped {
        usize::min(1, len - 1)
    } else {
        len - 1
    };
    (prev, next)
}

fn quat_vec(q: Quat) -> Vec4 {
    Vec4::new(q.x, q.y, q.z, q.w)
}

fn sample_rotation(keys: &[[f32; 4]], t: f32) -> Quat {
    if keys.is_empty() {
        return Quat::IDENTITY;
    }
    let frame = (t * CLIP_FPS).max(0.);
    let last = keys.len() - 1;
    // 生成数据的 6 位舍入会让模长轻微偏离 1,采样时归一化
    let quat = |k: &[f32; 4]| Quat::from_xyzw(k[0], k[1], k[2], k[3]).normalize();
    if last == 0 {
        return quat(&keys[0]);
    }
    let index = (frame as usize).min(last - 1);
    let frac = (frame - index as f32).clamp(0., 1.);
    let looped = quat(&keys[0]).dot(quat(&keys[last])).abs() > 0.999_999;
    let (ip, in_) = neighbours(keys.len(), looped, index);
    // Đưa các khoá lân cận về cùng bán cầu với q1 để spline không bị lật dấu (q và -q là một phép quay).
    let align = |q: Quat, reference: Vec4| {
        let v = quat_vec(q);
        if v.dot(reference) < 0. {
            -v
        } else {
            v
        }
    };
    let q1 = quat_vec(quat(&keys[index]));
    let q2 = align(quat(&keys[index + 1]), q1);
    let q0 = align(quat(&keys[ip]), q1);
    let q3 = align(quat(&keys[in_]), q2);
    let v = catmull(q0, q1, q2, q3, frac);
    Quat::from_xyzw(v.x, v.y, v.z, v.w).normalize()
}

fn sample_translation(keys: &[[f32; 3]], t: f32) -> Vec3 {
    if keys.is_empty() {
        return Vec3::ZERO;
    }
    let frame = (t * CLIP_FPS).max(0.);
    let last = keys.len() - 1;
    let vec = |k: &[f32; 3]| Vec4::new(k[0], k[1], k[2], 0.);
    if last == 0 {
        return Vec3::new(keys[0][0], keys[0][1], keys[0][2]);
    }
    let index = (frame as usize).min(last - 1);
    let frac = (frame - index as f32).clamp(0., 1.);
    let looped = (vec(&keys[0]) - vec(&keys[last])).length() < 1e-4;
    let (ip, in_) = neighbours(keys.len(), looped, index);
    let v = catmull(
        vec(&keys[ip]),
        vec(&keys[index]),
        vec(&keys[index + 1]),
        vec(&keys[in_]),
        frac,
    );
    Vec3::new(v.x, v.y, v.z)
}

/// 把披风四元数分解为渲染端欧拉 (x, z)：目标旋转 = rotation(x, π, z)，
/// 即 Rx(x)·Ry(π)·Rz(z)，要求目标不含 y 分量（披风剪辑满足）。
pub(crate) fn cape_euler(q: Quat) -> Vec2 {
    // glam Mat3 的 x/y/z_axis 即矩阵列
    let m = Mat3::from_quat(q);
    let x = m.z_axis.y.atan2(-m.z_axis.z);
    let z = m.y_axis.x.atan2(-m.x_axis.x);
    Vec2::new(x, z)
}

pub(crate) fn clip_duration(clip: &Clip) -> f32 {
    let keys = clip
        .channels
        .iter()
        .map(|c| c.rotation.len().max(c.translation.len()))
        .max()
        .unwrap_or(1);
    (keys.max(1) - 1) as f32 / CLIP_FPS
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::clip_fun::CAPE_POSE;
    use crate::pose::rotation;

    #[test]
    fn cape_euler_roundtrips_every_key() {
        for channel in CAPE_POSE.channels {
            if channel.joint != Joint::Cape {
                continue;
            }
            for key in channel.rotation {
                let q = flip_quat(Quat::from_xyzw(key[0], key[1], key[2], key[3]));
                let e = cape_euler(q);
                let restored = rotation(e.x, std::f32::consts::PI, e.y);
                // 四元数双覆盖：按旋转角比较而非分量
                assert!(
                    restored.angle_between(q) < 0.01,
                    "cape euler roundtrip failed for {key:?}: {e:?}"
                );
            }
        }
    }

    #[test]
    fn cape_pose_starts_at_rest() {
        // Khung đầu của clip khoe cape phải là tư thế nghỉ: cape ở (0.06π, 0),
        // tay/thân/đầu thẳng.
        let pose = sample(&CAPE_POSE, 0.);
        let angle = |q: Quat| 2. * q.w.acos().abs();
        assert!(angle(pose.body_rot) < 0.05);
        assert!(angle(pose.head_rot) < 0.05);
        for arm in pose.arms {
            assert!(angle(arm) < 0.1, "arm at rest must hang straight");
        }
        let base = std::f32::consts::PI * 0.06;
        assert!((pose.cape.x - base).abs() < 0.15, "cape x = {}", pose.cape.x);
        assert!(pose.cape.y.abs() < 0.1, "cape z = {}", pose.cape.y);
    }

    #[test]
    fn cape_pose_is_finite_and_returns_to_rest() {
        let duration = clip_duration(&CAPE_POSE);
        assert!(duration > 0.5);
        let steps = (duration / 0.033).ceil() as usize;
        for i in 0..=steps {
            let pose = sample(&CAPE_POSE, duration * i as f32 / steps as f32);
            for q in [pose.body_rot, pose.head_rot] {
                assert!(q.is_finite());
                assert!((q.length() - 1.).abs() < 0.001);
            }
            assert!(pose.body_pos.is_finite() && pose.cape.is_finite());
            for pivot in pose.arm_pivots.iter().chain(pose.leg_pivots.iter()) {
                assert!(pivot.is_finite());
            }
        }
        // Bật cao thật sự: thân phải nâng lên rõ rệt so với tư thế nghỉ.
        let peak = (0..=steps)
            .map(|i| sample(&CAPE_POSE, duration * i as f32 / steps as f32).body_pos.y)
            .fold(f32::MIN, f32::max);
        assert!(peak > 4., "leap too low: {peak}");
        let a = sample(&CAPE_POSE, 0.);
        let b = sample(&CAPE_POSE, duration);
        assert!(a.arms[1].abs_diff_eq(b.arms[1], 0.05));
        assert!(a.body_rot.abs_diff_eq(b.body_rot, 0.05));
        assert!(a.cape.abs_diff_eq(b.cape, 0.1));
    }
}
