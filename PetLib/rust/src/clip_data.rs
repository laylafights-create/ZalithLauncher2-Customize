//! Kiểu dữ liệu keyframe cho clip khoe cape (dữ liệu nằm trong clip_fun.rs).

/// 关键帧采样率(每秒帧数)
pub(crate) const CLIP_FPS: f32 = 30.0;

#[derive(Clone, Copy, PartialEq, Eq, Debug)]
pub(crate) enum Joint {
    Body,
    Head,
    ArmLeft,
    ArmRight,
    LegLeft,
    LegRight,
    Cape,
}

#[derive(Clone, Copy)]
pub(crate) struct Channel {
    pub joint: Joint,
    /// 旋转四元数键帧 (x, y, z, w),GLTF 原始键
    pub rotation: &'static [[f32; 4]],
    /// 平移键帧(像素,GLTF 单位 × 16);空表示无通道
    pub translation: &'static [[f32; 3]],
}

#[derive(Clone, Copy)]
pub(crate) struct Clip {
    pub channels: &'static [Channel],
}
