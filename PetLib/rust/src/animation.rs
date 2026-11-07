use glam::Vec3;

/// 单帧动画姿态
/// 所有欧拉角为绝对值，弧度 (x, y, z)，XYZ 序，原始 GLTF 基
#[derive(Clone, Copy)]
pub struct AnimFrame {
    /// 躯干方块欧拉旋转。
    pub body_rot: Vec3,
    /// 头部欧拉旋转。
    pub head_rot: Vec3,
    /// 双臂欧拉旋转 [左, 右]。
    pub arms: [Vec3; 2],
    /// 双腿欧拉旋转 [左, 右]。
    pub legs: [Vec3; 2],
    /// 躯干方块位置增量。
    pub body_pos: Vec3,
    /// 头部位置增量（仅 y）。
    pub head_pos: f32,
    /// Nhấc chân khỏi mặt đất (pixel, dương = lên) [trái, phải]: chân đang vung tới sẽ nhấc lên,
    /// chân chống chạm đất dứt khoát. Chỉ tác động lên trục y của chân.
    pub leg_lift: [f32; 2],
    /// 披风欧拉 x。
    pub cape_x: f32,
    /// 披风欧拉 z。
    /// None 表示不触碰骨架当前值。
    pub cape_z: Option<f32>,
}

impl AnimFrame {
    /// resetJoints 等价的静止姿态
    pub fn rest() -> Self {
        Self {
            cape_x: std::f32::consts::PI * 0.06,
            cape_z: Some(0.),
            ..Self::default()
        }
    }

    /// 静止姿态且披风 z 不写
    pub fn rest_with_open_cape() -> Self {
        let mut rest = Self::rest();
        rest.cape_z = None;
        rest
    }
}

impl Default for AnimFrame {
    fn default() -> Self {
        Self {
            body_rot: Vec3::ZERO,
            head_rot: Vec3::ZERO,
            arms: [Vec3::ZERO; 2],
            legs: [Vec3::ZERO; 2],
            body_pos: Vec3::ZERO,
            head_pos: 0.,
            leg_lift: [0.; 2],
            cape_x: 0.,
            cape_z: Some(0.),
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn rest_pose_is_still() {
        let f = AnimFrame::rest();
        assert_eq!(f.body_pos, Vec3::ZERO);
        assert_eq!(f.leg_lift, [0.; 2]);
        assert!(f.arms.iter().chain(f.legs.iter()).all(|v| *v == Vec3::ZERO));
        assert!(AnimFrame::rest_with_open_cape().cape_z.is_none());
    }
}
