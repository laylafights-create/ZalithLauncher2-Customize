# Animation: chỉ còn hiệu ứng khoe cape

Đã loại bỏ toàn bộ hiệu ứng hành động. Nhân vật luôn đứng yên ở tư thế nghỉ; chuyển động duy nhất là **khoe cape**.

## Đã loại bỏ
- Idle (thở, chuyển trọng tâm, liếc nhìn) và lịch emote ngẫu nhiên mỗi 8s.
- Dáng đi (walking) và `evaluate_walking`.
- Toàn bộ emote (3 fidget gốc, Jump, Spin, Wave, Stomp, Dance, Peek, Headbang, Kick, Stretch, Taunt, Clap, Bow, Shrug, Think).
- Phản ứng chạm (`playInteract`), nhún khi chạm (`squashTap`), vung tay (`playSwing`).
- Dữ liệu keyframe gốc (CLIP_IDLE/SUB/INTERACT), `tools/gen_clip_data.py`, `tools/models/*.gltf` và test `clip_parity.rs` (chỉ phục vụ các clip trên).
- JNI: `nativePlayEmote`, `nativeCancelEmote`, `nativePlayInteract`, `nativeSquashTap`, `nativePlaySwing`, `nativeSetSquashDepth`.

## Còn giữ
- Khoe cape (`CAPE_POSE`, 3.65s) khi `setCape(...)` với `showOff = true`; sinh bởi `tools/gen_fun_clips.py`.
- Đầu bám theo con trỏ (gaze), kéo để xoay (`rotateYaw`), zoom, ánh sáng, skin/cape.

## API sau khi dọn
Đã xoá hẳn (không còn stub): `playEmote`, `cancelEmote`, `playInteract`, `squashTap`, `playSwing`, `setSquashDepth`, `playAnimation`,
`stopAnimation`, `setStill`, `PetEmote`, `PetAnimation`, `PetTapReaction` và tham số `tapReaction` của `PetPreview`.
Thay bằng `PetController.setHeadFollow(enabled)` (đầu có bám con trỏ hay không; màn Skin nên tắt). Hiệu ứng khoe cape không bị ảnh hưởng.
JNI: `nativePlayAnimation`/`nativeStopAnimation` -> `nativeSetHeadFollow`. Rust: `Skeleton::set_head_follow`, bỏ `AnimationId`, `squash`, `body_yaw`.

## Cách dùng
Sau khi sửa `tools/gen_fun_clips.py`: `python3 tools/gen_fun_clips.py` để sinh lại `rust/src/clip_fun.rs`.
Chạy `cargo test` trong `PetLib/rust` để xác nhận.

## Hiệu ứng khoe cape (CAPE_POSE) — v2 "anh hùng ra mắt" 5.4s
6 màn: dồn lực (cape bị ép sát) -> bật CAO (lơ lửng ~0.65s, đỉnh 7.5px, co chân) giơ tay lên trời và xoay 360° trên không (4 bước 90°), cape tung xoáy -> tiếp đất nhún cực mạnh, cape phồng như dù
-> tạo dáng (chân dạng, tay chống hông, thân quét trái-phải, cape cuộn sóng) -> màn kết "giật cape" (hai tay gom cape chéo trước ngực, cúi gập, rồi bung: dang tay, ngửa người, nhảy lên, cape bùng tung xoè rộng) -> thu về (cúi chào nhẹ, cape lắng).
- Cape có thêm trục **roll** (hất sang hai bên, `cape_quat(sway, roll)`) bên cạnh sway trước/sau; lò xo cape nảy hơn (zeta 0.32).
- Thời lượng clip 5.4s (+0.5s đuôi). `showOffMillis` mặc định đổi 3000 -> 4900ms; vòng xoay trên không lúc bật cao do `PetController.showOffCape` đặt yaw theo 4 bước 90°.

## Màn hình Skin: đứng yên + phóng to
- `PetController.setHeadFollow(false)`: đầu không bám con trỏ. Hành động khoe cape khi `setCape(...)` vẫn chạy rồi về tư thế nghỉ.
  `setHeadFollow(true)` cho đầu bám lại (dùng khi rời màn hình Skin).
- `PetController.setZoom(1.5f)`: phóng to quanh tâm khung (0.25..4, 1 = mặc định). Rust: `PetModel::set_zoom`, JNI `nativeSetZoom`.
- Ví dụ ở màn hình Skin:
    controller.setHeadFollow(false)
    controller.setZoom(1.5f)
    controller.setCape(capeBytes)   // vẫn khoe cape như cũ

## Tối ưu hiệu năng
- `PetController`: khi không còn gì thay đổi (không có lệnh mới, không khoe cape, các bộ làm mượt đã hội tụ sau 1,5s) thì **ngừng vẽ lại**
  khung hình y hệt; màn hình giữ khung cuối đã đăng. Mọi lệnh đi qua `post{}` / đổi kích thước surface / khoe cape tự đánh dấu "cần vẽ" lại.
  Nhân vật đứng yên giờ tốn ~0% CPU thay vì rasterize liên tục ở 60fps.
- `tools/gen_fun_clips.py`: kênh hằng (pivot hai tay) chỉ xuất 1 key; `clip_fun.rs` nhỏ đi ~9% (69,9KB -> 63,4KB nguồn).
