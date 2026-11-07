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

## Khoe cape v3 (vẫn 5.4s, giữ nguyên mốc thời gian để Kotlin/Rust không phải đổi)
- **Rung tiếp đất**: hàm `impact()` thêm rung tắt dần lên thân (body_y) và cape (roll) ngay sau cú đáp ở 1.55s và 4.36s.
- **Hất cape (flick)**: ở 2.3-3.0s tay trái rời hông vung ra, cape cuộn sóng theo (roll + sway thêm).
- **Giật cape mạnh hơn**: cape kéo ép thêm (36°) rồi bật -148° ở màn kết; cape flutter thêm hài bậc 3 lúc tạo dáng.
- **Chào quý tộc**: 4.55-5.25s tay phải đặt ngang ngực, tay trái ra sau lưng, cúi người mượt, đầu theo thân (bản đầu vẫy tay ngang mặt trông như dab nên đã bỏ).
- **Camera nhấn (Kotlin, `PetController.cameraPunch`)**: zoom đẩy nhẹ lúc tạo dáng, nảy +13% khi tiếp đất, +16% khi cape bung; tự trả về đúng `setZoom` của người dùng (không đè zoom đã đặt).
- Clip bắt đầu/kết thúc đúng tư thế nghỉ như bản cũ (đã kiểm tra khung đầu/cuối). Sinh lại bằng `python3 tools/gen_fun_clips.py`.
- Chưa chạy `cargo test` (môi trường không có cargo): nên chạy `cargo test` trong `PetLib/rust` trước khi build.

## Mượt hơn (v3.1)
- **Vòng xoay 360° liên tục**: thay 4 nấc 90° (giật nhịp) bằng yaw đi theo smootherstep 0.78→1.38s, cập nhật mỗi khung (`cameraPunchTick`).
- Nhấn camera vào mượt (smootherstep 80ms) thay vì tuyến tính, hết "pop".
- Clip: bỏ rung tay 8Hz (jitter ở 30fps), tăng độ trễ chồng chéo (đầu/tay/cape), lò xo mềm hơn (omega thấp, zeta cao hơn một chút), giãn nhẹ hai cú bật nhanh nhất (tay lúc bật cao, cape lúc giật) để không còn bước >35°/khung.

## Khoe cape v4 (từ 3,3s: cúi chào quý tộc, cape không xuyên thân)
- Thay màn "giật cape / bung cape" (3,3 – 4,55s) và đoạn thu về bằng **cúi chào quý tộc**: chụm chân, tay phải đặt trước ngực, tay trái ra sau lưng, chân phải bước trước / chân trái lùi sau, thân cúi ~30° (4,55 – 4,95s giữ), rồi đứng dậy về tư thế nghỉ lúc 5,4s. Mốc 5,4s giữ nguyên.
- Quy ước góc cape trong clip: `sway` **dương = ép cape vào lưng** (cape.x giảm, dễ xuyên thân), **âm = tung ra sau**. Màn cúi chào giữ sway ≈ -2..-4 (cape.x ≥ ~7°).
- Render (`render.rs`): `cape.x` bị kẹp ≥ `CAPE_MIN_ANGLE` (0,05 rad) nên các cú ép sát (khoảng 0,4 – 0,9s ở màn dồn lực) không còn cắm vào thân. Cape vẫn là một khối.
- Camera (`PetController.cameraPunch`): bỏ cú nảy +16% lúc 4,0s, thay bằng đẩy chậm +6% trong 3,8 – 5,2s.
- Chưa chạy `cargo test` (không có mạng để tải crate): nên chạy trong `PetLib/rust`.

### v4.1: cúi chào mượt hơn (cùng chuẩn "đầu tư" như đoạn đầu)
- Mỗi khớp có nhiều key hơn thay vì giữ tư thế phẳng: **anticipation** (hít vào, ngực nâng, ngả nhẹ ra sau ở 3,5 – 4,05s), **overshoot** (thân gập 34° rồi lắng 31°, đứng dậy ngả -3° rồi về 0).
- **Overlapping**: đầu trễ nhịp cúi thêm (13°), tay trái nâng ra sau làm đối trọng, tay phải vẫy vòng ra ngoài thấp (flourish), chân phải trượt trước/chân trái lùi.
- **Không đơ khi giữ cúi** (4,55 – 5,1s): thân, đầu, body_y và cape đung đưa nhẹ theo nhịp thở (`bowwin`).
- **Cape** hất ra theo quán tính khi thân gập (4,3s) và khi đứng dậy (5,2s), roll lắc nhẹ hai bên, lắng bằng lò xo cũ (omega 10, zeta 0,34). Vẫn chỉ dùng sway âm nên cape.x ≥ ~6°.
- Chạm chân lúc 4,3s có rung tắt dần rất nhẹ (`impact`). Bước nhảy tối đa mỗi khung sau 3,3s ≤ 12° (tay phải lúc hạ từ trên trời), các khớp còn lại ≤ 6,5°.

### v4.2: quay người về mặt trước rồi mới cúi chào
- Trước đây nhân vật quay lưng suốt clip và chỉ xoay lại lúc 4,9s (nên cúi chào khi còn quay lưng). Giờ `PetController.cameraPunchTick` quay người theo smootherstep **3,2 – 4,0s** (`TURN_START`, `TURN_SECONDS`), cùng chiều với vòng xoay 360° và thêm 180°, xong trước khi thân gập (4,15s+).
- `recenterAfterShowOff` còn lại là lưới an toàn: lúc đó đã ở mặt trước nên không xoay thêm.
- Cape tung ra theo vòng quay (sway -42° lúc 3,55s, roll -24° rồi +14°) rồi lắng xuống; sau 4,0s cape ở sau lưng, quay lưng khỏi camera.
- Chưa chạy được `cargo test`/build Android trong môi trường này.

## Khoe cape v5: cúi sâu theo ảnh tham khảo (clip 6,3s)
- Cúi chào **sâu ~85°** (thân gần ngang): đầu buông xuống, **tay phải áp trước ngực** (x -78°, vào trong 18°), tay trái buông thẳng xuống, **chân phải lùi ra sau và bắt chéo** qua chân trái (x +24°, z -14°), chân trái đỡ (x -8°). Hạ thấp thêm 1,2px (body_y).
- Nhịp: quay người 3,2 – 4,0s -> hít vào 3,6 – 4,15s -> gập 4,2 – 4,9s (overshoot 88° rồi lắng 84°) -> giữ có thở 4,9 – 5,7s -> đứng dậy 5,65 – 6,3s (ngả -3° rồi về nghỉ).
- Clip dài **6,3s** (trước 5,4s). Rust lấy thời lượng từ dữ liệu clip nên không phải sửa; Kotlin: `SHOW_OFF_ACTIVE_MILLIS` 6500 -> 7500, camera dừng ở 6,2s, đẩy camera vào 4,0 – 6,1s.
- Cape: khi thân ngang, lưng hướng lên và cape nằm phủ trên lưng (cape.x ≥ ~7°), không chui vào thân.
- Chưa build Android/`cargo test` được trong môi trường này.
