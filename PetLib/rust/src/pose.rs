//! 玩家骨架

use glam::{EulerRot, Mat4, Quat, Vec2, Vec3};

use crate::animation::AnimFrame;
use crate::clip::{self, ClipPose};
use crate::clip_fun::CAPE_POSE;

pub const TAU_GAZE: f32 = 0.10;
pub const TAU_YAW: f32 = 0.15;
/// 动画轨道交叉淡入淡出时长(秒)。
pub const FADE_SECONDS: f32 = 0.2;
pub const TAU_RECENTER: f32 = 0.35;

/// 臂静止枢轴 [左, 右]:肩点(模型 22px 高),原始 GLTF 基坐标。
pub(crate) const ARM_REST_PIVOTS: [Vec3; 2] = [Vec3::new(-5., 10., 0.), Vec3::new(5., 10., 0.)];
/// 腿静止枢轴 [左, 右]:髋点(模型 12px 高),原始 GLTF 基坐标。
pub(crate) const LEG_REST_PIVOTS: [Vec3; 2] = [Vec3::new(-1.9, 0., 0.), Vec3::new(1.9, 0., 0.)];

/// 原始 GLTF 基 → 渲染基的 180°Y 根共轭(即 GLTF 骨架里
/// blockbench_export 节点的旋转):四元数 (x, y, z, w) → (−x, y, −z, w)。
pub(crate) fn flip_quat(q: Quat) -> Quat {
    Quat::from_xyzw(-q.x, q.y, -q.z, q.w)
}

/// `flip_quat` 的向量版本:(x, y, z) → (−x, y, −z)。
pub(crate) fn flip_vec(v: Vec3) -> Vec3 {
    Vec3::new(-v.x, v.y, -v.z)
}

/// 背对切断阈值：偏航相对摄像机方向超过此角放弃跟随。
const GAZE_OFF_YAW: f32 = 90f32.to_radians();
/// 背对恢复阈值：拖回此角以内才恢复（迟滞）。
const GAZE_ON_YAW: f32 = 75f32.to_radians();
/// 过扭放弃阈值：头-身相对水平扭角超过此角放弃跟随。
const GAZE_TWIST_OFF: f32 = 60f32.to_radians();
/// 过扭恢复阈值（迟滞）。
const GAZE_TWIST_ON: f32 = 50f32.to_radians();

fn wrap_pi(angle: f32) -> f32 {
    let two_pi = std::f32::consts::TAU;
    let a = angle % two_pi;
    if a > std::f32::consts::PI {
        a - two_pi
    } else if a < -std::f32::consts::PI {
        a + two_pi
    } else {
        a
    }
}

/// 相机正面基准偏航。
/// 角色静止时头部以此角度朝向相机。
pub fn front_yaw() -> f32 {
    30f32.atan2(70.)
}

pub fn rotation(x: f32, y: f32, z: f32) -> Quat {
    Quat::from_euler(EulerRot::XYZ, x, y, z)
}

/// Smootherstep (Perlin): đạo hàm bậc 1 và 2 đều về 0 ở hai đầu, nên chuyển cảnh giữa các
/// animation không còn "giật nhẹ" ở điểm bắt đầu/kết thúc như smoothstep. Đối xứng qua 0.5,
/// nên tổng trọng số của hai track crossfade vẫn bằng 1.
fn smoother(t: f32) -> f32 {
    let t = t.clamp(0., 1.);
    t * t * t * (t * (t * 6. - 15.) + 10.)
}

/// 相机基准四元数：把角色正面基准对齐到投影相机的屏幕平面。
pub fn view_quat() -> Quat {
    Quat::from_rotation_y(front_yaw())
        * Quat::from_rotation_x(-(23f32 / (30f32 * 30f32 + 70f32 * 70f32).sqrt()).atan())
}

/// Nguồn chuyển động: chỉ có hành động khoe cape; ngoài ra nhân vật đứng yên ở tư thế nghỉ.
#[derive(Clone, Copy, PartialEq, Debug)]
enum Source {
    /// 换披风时的展示姿势（单臂指天）
    CapePose,
}

/// 一条动画轨道：来源、本地时间与淡入淡出权重。
#[derive(Debug)]
struct Track {
    source: Source,
    time: f32,
    weight: f32,
    target: f32,
    /// Thời lượng của track một lần
    duration: f32,
}

impl Track {
    fn oneshot(source: Source, duration: f32) -> Self {
        Self {
            source,
            time: 0.,
            weight: 0.,
            target: 1.,
            duration,
        }
    }
}

#[derive(Clone, Copy)]
struct LimbSnapshot {
    arms: [Quat; 2],
    legs: [Quat; 2],
    arm_pivots: [Vec3; 2],
    leg_pivots: [Vec3; 2],
    head_rot: Quat,
    body_rot: Quat,
    body_pos: Vec3,
    head_pos: f32,
    cape: Vec2,
}

impl LimbSnapshot {
    /// 加权混合：四元数 slerp、向量/标量 lerp
    fn blend(self, other: Self, t: f32) -> Self {
        Self {
            arms: std::array::from_fn(|i| self.arms[i].slerp(other.arms[i], t)),
            legs: std::array::from_fn(|i| self.legs[i].slerp(other.legs[i], t)),
            arm_pivots: std::array::from_fn(|i| self.arm_pivots[i].lerp(other.arm_pivots[i], t)),
            leg_pivots: std::array::from_fn(|i| self.leg_pivots[i].lerp(other.leg_pivots[i], t)),
            head_rot: self.head_rot.slerp(other.head_rot, t),
            body_rot: self.body_rot.slerp(other.body_rot, t),
            body_pos: self.body_pos.lerp(other.body_pos, t),
            head_pos: Self::lerp_f(self.head_pos, other.head_pos, t),
            cape: self.cape.lerp(other.cape, t),
        }
    }

    fn lerp_f(a: f32, b: f32, t: f32) -> f32 {
        a + (b - a) * t
    }
}

/// 把剪辑姿态折算成骨架关节目标
fn snapshot_from_pose(pose: ClipPose) -> LimbSnapshot {
    LimbSnapshot {
        arms: pose.arms,
        legs: pose.legs,
        arm_pivots: pose.arm_pivots,
        leg_pivots: pose.leg_pivots,
        head_rot: pose.head_rot,
        body_rot: pose.body_rot,
        body_pos: pose.body_pos,
        head_pos: 0.,
        cape: pose.cape,
    }
}

/// 把欧拉动画帧折算成骨架关节目标；披风 z 未写时保持 `current_cape_z`
fn snapshot_from_frame(frame: AnimFrame, current_cape_z: f32) -> LimbSnapshot {
    LimbSnapshot {
        arms: frame.arms.map(|e| rotation(e.x, e.y, e.z)),
        legs: frame.legs.map(|e| rotation(e.x, e.y, e.z)),
        arm_pivots: ARM_REST_PIVOTS,
        leg_pivots: [
            LEG_REST_PIVOTS[0] + Vec3::new(0., frame.leg_lift[0], 0.),
            LEG_REST_PIVOTS[1] + Vec3::new(0., frame.leg_lift[1], 0.),
        ],
        head_rot: rotation(frame.head_rot.x, frame.head_rot.y, frame.head_rot.z),
        body_rot: rotation(frame.body_rot.x, frame.body_rot.y, frame.body_rot.z),
        body_pos: frame.body_pos,
        head_pos: frame.head_pos,
        cape: Vec2::new(frame.cape_x, frame.cape_z.unwrap_or(current_cape_z)),
    }
}

pub struct Skeleton {
    pub tau_gaze: f32,
    pub tau_yaw: f32,
    /// 动画轨道交叉淡入淡出时长(秒)
    pub fade_seconds: f32,
    pub tau_recenter: f32,
    /// 注视夹角限制（弧度，水平 / 垂直）。
    pub head_range: Vec2,
    /// Hệ số phóng to nhân vật quanh tâm khung hình (1 = mặc định).
    pub zoom: f32,

    gaze_target: Option<Vec2>,
    yaw_target: f32,

    /// Đầu có bám theo con trỏ hay không. Nhân vật luôn đứng yên ở tư thế nghỉ ngoài hành động khoe cape.
    follow_gaze: bool,
    /// Track hành động khoe cape (đang phát và đang fade-out cùng tồn tại, trộn theo thứ tự chèn).
    tracks: Vec<Track>,

    /// 平滑后的注视增量（弧度，水平 / 垂直）。
    gaze: Vec2,
    /// 背对切断锁存：越过切断角置位，拖回恢复角以下复位。
    gaze_yaw_cut: bool,
    /// 过扭切断锁存：需求扭角越过放弃角置位，回到恢复角以内复位。
    gaze_twist_cut: bool,
    model_yaw: f32,

    // 逐帧输出，render.rs 消费。
    pub arms: [Quat; 2],
    pub legs: [Quat; 2],
    pub arm_pivots: [Vec3; 2],
    pub leg_pivots: [Vec3; 2],
    pub head_rot: Quat,
    pub body_rot: Quat,
    pub body_pos: Vec3,
    pub head_pos: f32,
    pub cape: Vec2,
    /// 头部最终局部旋转
    pub head: Quat,
}

impl Default for Skeleton {
    fn default() -> Self {
        Self::new()
    }
}

impl Skeleton {
    pub fn new() -> Self {
        Self {
            tau_gaze: TAU_GAZE,
            tau_yaw: TAU_YAW,
            fade_seconds: FADE_SECONDS,
            tau_recenter: TAU_RECENTER,
            head_range: Vec2::new(60f32.to_radians(), 53f32.to_radians()),
            zoom: 1.,
            gaze_target: None,
            yaw_target: 0.,
            follow_gaze: true,
            tracks: Vec::new(),
            gaze: Vec2::ZERO,
            gaze_yaw_cut: false,
            gaze_twist_cut: false,
            model_yaw: 0.,
            arms: [Quat::IDENTITY; 2],
            legs: [Quat::IDENTITY; 2],
            arm_pivots: ARM_REST_PIVOTS,
            leg_pivots: LEG_REST_PIVOTS,
            head_rot: Quat::IDENTITY,
            body_rot: Quat::IDENTITY,
            body_pos: Vec3::ZERO,
            head_pos: 0.,
            cape: Vec2::ZERO,
            head: Quat::IDENTITY,
        }
    }

    pub fn set_gaze_target(&mut self, target: Option<Vec2>) {
        self.gaze_target = target;
    }

    pub fn gaze_target(&self) -> Option<Vec2> {
        self.gaze_target
    }

    /// 设置偏航目标角。
    pub fn set_yaw_target(&mut self, yaw: f32) {
        self.yaw_target += wrap_pi(yaw - self.yaw_target);
    }

    pub fn model_yaw(&self) -> f32 {
        self.model_yaw
    }

    pub fn head_follow(&self) -> bool {
        self.follow_gaze
    }

    /// Bật/tắt việc đầu bám theo con trỏ. Không ảnh hưởng hành động khoe cape đang chạy.
    pub fn set_head_follow(&mut self, enabled: bool) {
        self.follow_gaze = enabled;
    }

    /// 播放换披风展示姿势（单臂指天）；播放中重复调用会从头重播。
    pub fn play_cape_pose(&mut self) {
        for track in &mut self.tracks {
            track.target = 0.;
        }
        self.tracks.push(Track::oneshot(
            Source::CapePose,
            clip::clip_duration(&CAPE_POSE),
        ));
    }

    /// 换披风展示姿势的时长（秒）
    pub fn cape_pose_duration(&self) -> f32 {
        clip::clip_duration(&CAPE_POSE)
    }

    /// 推进所有轨道。
    fn advance(&mut self, dt: f32) {
        for track in &mut self.tracks {
            track.time = (track.time + dt.min(0.05)).min(track.duration);
            if track.target == 1. && track.time >= track.duration {
                track.target = 0.;
            }
        }

        // 权重线性斜坡与轨道回收。
        let rate = (dt / self.fade_seconds).max(0.);
        self.tracks.retain_mut(|track| {
            if track.weight < track.target {
                track.weight = (track.weight + rate).min(track.target);
            } else if track.weight > track.target {
                track.weight = (track.weight - rate).max(track.target);
            }
            track.weight > 0. || track.target > 0.
        });
    }

    fn rest_snapshot(cape_z: f32) -> LimbSnapshot {
        snapshot_from_frame(AnimFrame::rest_with_open_cape(), cape_z)
    }

    fn sample_track(&self, track: &Track) -> LimbSnapshot {
        match track.source {
            Source::CapePose => snapshot_from_pose(clip::sample(&CAPE_POSE, track.time)),
        }
    }

    /// 按权重混合所有轨道，权重之和不足 1 的部分回落到静止姿态
    fn blended(&self) -> LimbSnapshot {
        let cape_z = self.cape.y;
        let mut acc_weight = 0.;
        let mut acc: Option<LimbSnapshot> = None;
        for track in &self.tracks {
            if track.weight <= 0. {
                continue;
            }
            // Ease-in-out cho chuyển cảnh (smootherstep đối xứng nên tổng trọng số vẫn = 1)
            let w = smoother(track.weight);
            let pose = self.sample_track(track);
            acc = Some(match acc {
                None => pose,
                Some(s) => s.blend(pose, w / (acc_weight + w)),
            });
            acc_weight += w;
        }
        let mut result = acc.unwrap_or_else(|| Self::rest_snapshot(cape_z));
        if acc_weight < 1. {
            result = result.blend(Self::rest_snapshot(cape_z), 1. - acc_weight);
        }
        result
    }

    /// 逐帧推进。
    /// `now` 仅为兼容保留，内部计时全部由帧间隔 `dt` 驱动;
    /// `view` 为输出视图尺寸（像素），注视目标坐标与之同空间。
    pub fn update(&mut self, view: Vec2, _now: f32, dt: f32) {
        // 负间隔非法：平滑系数与动画进度都不允许倒退。
        let dt = dt.max(0.);
        self.advance(dt);
        let targets = self.blended();

        self.arms = targets.arms;
        self.legs = targets.legs;
        self.arm_pivots = targets.arm_pivots;
        self.leg_pivots = targets.leg_pivots;
        self.head_rot = targets.head_rot;
        self.body_rot = targets.body_rot;
        self.body_pos = targets.body_pos;
        self.head_pos = targets.head_pos;
        self.cape = targets.cape;

        // 整体偏航平滑。
        self.model_yaw += (self.yaw_target - self.model_yaw) * (1. - (-dt / self.tau_yaw).exp());
        let shift = wrap_pi(self.model_yaw) - self.model_yaw;
        self.model_yaw += shift;
        self.yaw_target += shift;

        // 头部姿态：
        // 追踪中  世界空间解算指向输入点的朝向，受颈部活动范围截断，以 τ_gaze 平滑追随；
        //        背对摄像机或头-身水平扭角超限时放弃跟随，头部回正到身体朝向。
        // 未追踪  与身体朝向对齐（仅动画摆动），以 τ_recenter 平滑回正。
        let parent = Quat::from_rotation_y(self.model_yaw + front_yaw());
        let (head_target, tau) = match self.gaze_target.filter(|_| self.head_follow()) {
            Some(_) => {
                let raw = self.solve_gaze(view);
                // 背对切断（迟滞）：model_yaw 即相对相机正面的偏航。
                let facing = wrap_pi(self.model_yaw).abs();
                if facing > GAZE_OFF_YAW {
                    self.gaze_yaw_cut = true;
                } else if facing < GAZE_ON_YAW {
                    self.gaze_yaw_cut = false;
                }
                // 过扭放弃（迟滞）：头-身相对水平扭角。
                let twist = wrap_pi(raw.x - self.model_yaw).abs();
                if twist > GAZE_TWIST_OFF {
                    self.gaze_twist_cut = true;
                } else if twist < GAZE_TWIST_ON {
                    self.gaze_twist_cut = false;
                }
                if self.gaze_yaw_cut || self.gaze_twist_cut {
                    self.gaze = Vec2::ZERO;
                    (self.head_rot, self.tau_recenter)
                } else {
                    let target = Vec2::new(
                        raw.x.clamp(-self.head_range.x, self.head_range.x),
                        raw.y.clamp(-self.head_range.y, self.head_range.y),
                    );
                    self.gaze += (target - self.gaze) * (1. - (-dt / self.tau_gaze).exp());
                    // 头部局部旋转换入渲染基做注视合成，再换回原始基输出
                    let head_world = view_quat()
                        * Quat::from_rotation_y(self.gaze.x)
                        * Quat::from_rotation_x(self.gaze.y)
                        * flip_quat(self.head_rot);
                    (flip_quat(parent.inverse() * head_world), self.tau_gaze)
                }
            }
            None => {
                self.gaze = Vec2::ZERO;
                self.gaze_yaw_cut = false;
                self.gaze_twist_cut = false;
                (self.head_rot, self.tau_recenter)
            }
        };
        self.head = self
            .head
            .slerp(head_target, 1. - (-dt / tau).exp())
            .normalize();
    }

    /// 以相机隐含距离解算指向光标的 yaw/pitch（未按颈部活动范围夹取）。
    fn solve_gaze(&self, view: Vec2) -> Vec2 {
        let Some(cursor) = self.gaze_target else {
            return Vec2::ZERO;
        };
        // 眼睛的世界位置：根偏航 ∘ 颈部（头部枢轴，含 bob）+ 相机基准上移。
        let neck = Mat4::from_rotation_y(self.model_yaw + front_yaw())
            * Mat4::from_translation(Vec3::Y * (24. + self.head_pos));
        let view_basis = view_quat();
        let eye = neck.transform_point3(Vec3::ZERO) + view_basis * (Vec3::Y * 3.);
        let p = crate::render::project(eye, view.x, view.y);
        let distance = Vec3::new(30., 23., 70.).length();
        let unit = view.x / 44.;
        let dx = (cursor.x - p.x) / unit;
        let dy = (p.y - cursor.y) / unit;
        let h = dx.atan2(distance);
        let v = (-dy / (dx * dx + dy * dy + distance * distance).sqrt()).asin();
        Vec2::new(h, v)
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn gaze_recenters_after_target_removed() {
        let mut s = Skeleton::new();
        let view = Vec2::new(450., 525.);
        s.set_gaze_target(Some(Vec2::new(400., 100.)));
        for i in 0..120 {
            s.update(view, i as f32 / 60., 1. / 60.);
        }
        assert!(s.gaze.x > 0.01);
        s.set_gaze_target(None);
        for i in 120..600 {
            s.update(view, i as f32 / 60., 1. / 60.);
        }
        assert!(s.gaze.length() < 0.001, "gaze = {:?}", s.gaze);
    }

    #[test]
    fn gaze_follows_directly_within_twist_limit() {
        let mut s = Skeleton::new();
        let view = Vec2::new(450., 525.);
        // 需求角在自然范围内：直接跟随，注视收敛到需求角本身。
        s.set_gaze_target(Some(Vec2::new(920., 262.5)));
        for i in 0..300 {
            s.update(view, i as f32 / 60., 1. / 60.);
        }
        let expected = s.solve_gaze(view);
        assert!((s.gaze.x - expected.x).abs() < 0.001);
    }

    #[test]
    fn gaze_abandons_beyond_twist_limit() {
        let mut s = Skeleton::new();
        let view = Vec2::new(450., 525.);
        // 需求扭角远超自然范围：放弃跟随，头部回正到身体朝向。
        s.set_gaze_target(Some(Vec2::new(5000., 262.5)));
        for i in 0..600 {
            s.update(view, i as f32 / 60., 1. / 60.);
        }
        assert!(s.gaze.length() < 0.001, "gaze = {:?}", s.gaze);
        assert!(
            s.head.abs_diff_eq(s.head_rot, 0.2),
            "head must realign with body"
        );
    }

    #[test]
    fn back_facing_cuts_gaze_with_hysteresis() {
        let mut s = Skeleton::new();
        let view = Vec2::new(450., 525.);
        s.set_gaze_target(Some(Vec2::new(920., 262.5)));
        for i in 0..90 {
            s.update(view, i as f32 / 60., 1. / 60.);
        }
        let expected = s.solve_gaze(view);
        assert!((s.gaze.x - expected.x).abs() < 0.001);
        // 背对：切断跟随，头部回正。
        s.set_yaw_target(std::f32::consts::PI);
        for i in 90..180 {
            s.update(view, i as f32 / 60., 1. / 60.);
        }
        assert!(s.gaze.length() < 0.001);
        assert!(
            s.head.abs_diff_eq(s.head_rot, 0.2),
            "head must realign while back-facing"
        );
        // 拖回 82°（处于 75°~90° 迟滞区间）：保持切断。
        s.set_yaw_target(82f32.to_radians());
        for i in 180..270 {
            s.update(view, i as f32 / 60., 1. / 60.);
        }
        assert!(
            s.gaze.length() < 0.001,
            "hysteresis must hold within (75°, 90°) from camera"
        );
        // 回到正面：恢复跟随。
        s.set_yaw_target(0.);
        for i in 270..420 {
            s.update(view, i as f32 / 60., 1. / 60.);
        }
        let expected = s.solve_gaze(view);
        assert!((s.gaze.x - expected.x).abs() < 0.001);
    }

    #[test]
    fn gaze_rules_mirror_for_rightward_turns() {
        // 上述规则的镜像：身体向右转、光标在屏幕左侧，判定量级必须一致。
        let mut s = Skeleton::new();
        let view = Vec2::new(450., 525.);
        // 需求角超夹角上限、未过扭：贴在负向夹角上限。
        s.set_gaze_target(Some(Vec2::new(-443., 262.5)));
        for i in 0..90 {
            s.update(view, i as f32 / 60., 1. / 60.);
        }
        let expected = s.solve_gaze(view);
        assert!((s.gaze.x - expected.x).abs() < 0.001);
        // 需求扭角远超自然范围：放弃。
        s.set_gaze_target(Some(Vec2::new(-4500., 262.5)));
        for i in 90..180 {
            s.update(view, i as f32 / 60., 1. / 60.);
        }
        assert!(s.gaze.length() < 0.001, "gaze = {:?}", s.gaze);
        // 光标回到可跟随位置，继续背对切断的验证。
        s.set_gaze_target(Some(Vec2::new(-443., 262.5)));
        // 背对（负方向）切断。
        s.set_yaw_target(-std::f32::consts::PI);
        for i in 180..270 {
            s.update(view, i as f32 / 60., 1. / 60.);
        }
        assert!(s.gaze.length() < 0.001);
        // 拖回 -82°（处于 -90°~-75° 迟滞区间）：保持切断。
        s.set_yaw_target(-82f32.to_radians());
        for i in 270..360 {
            s.update(view, i as f32 / 60., 1. / 60.);
        }
        assert!(
            s.gaze.length() < 0.001,
            "hysteresis must hold within (-90°, -75°) from camera"
        );
        // 回到正面：恢复跟随，看向左侧光标。
        s.set_yaw_target(0.);
        for i in 360..450 {
            s.update(view, i as f32 / 60., 1. / 60.);
        }
        let expected = s.solve_gaze(view);
        assert!((s.gaze.x - expected.x).abs() < 0.001);
    }

    #[test]
    fn fast_full_turn_drag_keeps_direction() {
        let mut s = Skeleton::new();
        let view = Vec2::new(450., 525.);
        let tau = std::f32::consts::TAU;
        // 18 帧内把目标拖过一整圈(整宽快拖)
        let mut prev = s.model_yaw();
        let mut total = 0.;
        for i in 1..=18 {
            s.set_yaw_target(i as f32 / 18. * tau);
            s.update(view, i as f32 / 60., 1. / 60.);
            // 规范化意义下的帧间位移必须始终朝拖拽方向
            let step = wrap_pi(s.model_yaw() - prev);
            assert!(step > -1e-6, "yaw reversed at frame {i}: {step}");
            total += step;
            prev = s.model_yaw();
        }
        // 松手后继续沿同方向收敛到整圈目标
        for i in 18..180 {
            s.update(view, i as f32 / 60., 1. / 60.);
            let step = wrap_pi(s.model_yaw() - prev);
            total += step;
            prev = s.model_yaw();
        }
        assert!(total > tau - 0.1, "must have chased the full turn: {total}");
        // 稳定后视觉停在整圈位置(与起点同相)
        assert!(wrap_pi(s.model_yaw()).abs() < 0.05);
    }

    #[test]
    fn model_yaw_approaches_target() {
        let mut s = Skeleton::new();
        let view = Vec2::new(450., 525.);
        s.set_yaw_target(std::f32::consts::FRAC_PI_2);
        for i in 0..600 {
            s.update(view, i as f32 / 60., 1. / 60.);
        }
        assert!((s.model_yaw - std::f32::consts::FRAC_PI_2).abs() < 0.001);
    }

    #[test]
    fn recentered_head_aligns_with_body() {
        let mut s = Skeleton::new();
        let view = Vec2::new(450., 525.);
        s.set_yaw_target(std::f32::consts::FRAC_PI_2);
        for i in 0..600 {
            s.update(view, i as f32 / 60., 1. / 60.);
        }
        // 无输入时头部与身体朝向对齐，不额外朝相机偏转。
        assert!(
            s.head.abs_diff_eq(s.head_rot, 0.2),
            "head={:?} sway={:?}",
            s.head,
            s.head_rot
        );
    }

    /// Khi đứng yên (không có cape pose) nhân vật phải ở đúng tư thế nghỉ, không có chuyển động nào.
    #[test]
    fn stands_perfectly_still_without_cape_pose() {
        let mut s = Skeleton::new();
        let view = Vec2::new(450., 525.);
        // 20 giây không có hành động nào được phát.
        for i in 0..(60 * 20) {
            s.update(view, i as f32 / 60., 1. / 60.);
            assert!(s.tracks.is_empty(), "no tracks expected: {:?}", s.tracks);
            assert!(s.body_pos.abs_diff_eq(Vec3::ZERO, 1e-6));
            assert!(s.arms.iter().all(|q| q.abs_diff_eq(Quat::IDENTITY, 1e-6)));
            assert!(s.legs.iter().all(|q| q.abs_diff_eq(Quat::IDENTITY, 1e-6)));
            assert_eq!(s.leg_pivots, LEG_REST_PIVOTS);
            assert_eq!(s.arm_pivots, ARM_REST_PIVOTS);
        }
        assert!((s.cape.x - std::f32::consts::PI * 0.06).abs() < 0.01);
    }

    /// Hành động khoe cape: đổi tư thế rõ rệt, rồi quay về đúng tư thế nghỉ và dọn sạch track.
    #[test]
    fn cape_pose_plays_then_returns_to_rest() {
        let mut s = Skeleton::new();
        let view = Vec2::new(450., 525.);
        s.play_cape_pose();
        let duration = s.cape_pose_duration();
        let mut raised = false;
        let frames = ((duration + 1.) * 60.) as usize;
        for i in 0..frames {
            s.update(view, i as f32 / 60., 1. / 60.);
            assert!(s.arms.iter().all(|q| q.is_finite()));
            assert!(s.tracks.len() <= 1, "tracks must stay bounded: {:?}", s.tracks);
            raised |= !s.arms[1].abs_diff_eq(Quat::IDENTITY, 0.5);
        }
        assert!(raised, "cape pose must move the arm");
        assert!(s.tracks.is_empty(), "track must be reclaimed: {:?}", s.tracks);
        assert!(s.body_pos.abs_diff_eq(Vec3::ZERO, 0.001));
        assert!(s.arms.iter().all(|q| q.abs_diff_eq(Quat::IDENTITY, 0.01)));
    }

    /// Gọi lại giữa chừng: phát lại từ đầu, không tích tụ track.
    #[test]
    fn cape_pose_restart_does_not_leak_tracks() {
        let mut s = Skeleton::new();
        let view = Vec2::new(450., 525.);
        for i in 0..600 {
            if i % 20 == 0 {
                s.play_cape_pose();
            }
            s.update(view, i as f32 / 60., 1. / 60.);
            assert!(s.tracks.len() <= 12, "tracks leaked: {}", s.tracks.len());
            assert!(s.cape.is_finite() && s.head.is_finite() && s.body_pos.is_finite());
            assert!(s.tracks.iter().all(|t| (0.0..=1.0).contains(&t.weight)));
        }
    }

    /// Tắt bám con trỏ không cắt hành động khoe cape.
    #[test]
    fn disabling_head_follow_keeps_cape_pose() {
        let mut s = Skeleton::new();
        let view = Vec2::new(450., 525.);
        s.play_cape_pose();
        s.set_head_follow(false);
        assert!(!s.head_follow());
        for i in 0..30 {
            s.update(view, i as f32 / 60., 1. / 60.);
        }
        assert!(s
            .tracks
            .iter()
            .any(|t| t.source == Source::CapePose && t.target == 1.));
    }
}
