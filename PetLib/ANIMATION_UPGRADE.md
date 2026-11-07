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

## v11: sửa đầu ngửa ra sau khi cúi chào
- Nguyên nhân: lúc clip chạy, đầu vẫn bị gaze (bám con trỏ) ghi đè và tính trong không gian thế giới, không kể góc gập của thân, nên đầu bị kéo về phía camera trong khi thân cúi -> trông như ngửa ra sau. Đầu còn trễ theo `tau_recenter` (0,35s) so với thân đang gập nhanh.
- `pose.rs`: khi có track clip (`!tracks.is_empty()`) thì bỏ qua gaze, đầu chỉ theo clip với `tau_gaze` (0,1s). Parent của phép tính gaze nay gồm cả `body_rot`.
- `gen_fun_clips.py`: đầu cúi thêm lúc giữ cúi (4,9s: 10 -> 16°, 5,2s: 14 -> 21°, 5,55s: 12 -> 18°); clip_fun.rs đã sinh lại.
- Chưa chạy được `cargo test`/build Android trong môi trường này.

### v11.1: animation khi quay người về mặt trước (3,0 – 4,0s)
- Dồn lực rồi quay: thân xoắn ngược (wind-up) trước, rồi bị kéo trễ theo vòng quay và lắng lại có overshoot; đầu quay trước thân (nhìn về camera trước), liếc qua vai rồi vào nhịp.
- Hạ thấp lúc bắt đầu quay (body_y -1,2), hai chân bước xoay MẠNH (x ±46°/32° ngược pha, dạng ngang thêm 12°), hai tay dang ra giữ thăng bằng rồi thu vào trước ngực để vào tư thế cúi.
- Chưa build/chạy thử được trong môi trường này.

## v14: sửa cúi chào bị ngả NGƯỢC ra sau
- Nguyên nhân thật: quy ước trong clip bị ngược. Trong cảnh render (mặt trước = +Z, cape ở lưng z = -2), `body.x` / `head.x` DƯƠNG làm thân/đầu ngả ra SAU, tay/chân x dương vung ra TRƯỚC. Trước đây clip cúi dùng x dương (88°) nên thân ngửa ra sau 88°, cape cũng bị kẹt.
- Đã đảo dấu x cho toàn bộ đoạn cúi (từ 3,6s): thân -88°, đầu thêm -16..-21° (cúi xuống), tay phải áp trước ngực (x tích lũy -268..-318° để không xoay ngược từ vị trí giơ lên trời), tay trái buông, chân phải lùi x -24°, chân trái đỡ +8°. Đã kiểm tra bằng tính toán: hướng đỉnh thân lúc giữ cúi (5,2s) z = +1,0 (hướng về mặt trước).
- Các đoạn trước 3,3s giữ nguyên (đã được duyệt); chỉ chú thích lại quy ước trong `gen_fun_clips.py`.
- Chưa build/chạy thử được trong môi trường này.

## v15: màn khoe cape dài ~9,7s (trước 6,3s), nhịp chỉn chu hơn
- `gen_fun_clips.py`: kịch bản gốc 6,3s được kéo giãn theo từng đoạn (`SEGS`/`warp`): bật/xoay/tiếp đất giữ nguyên (0 – 1,5s); tạo dáng x1,7 (1,5 – 4,56s); quay người + hít vào x1,35 (đến 5,78s); cúi xuống x1,6 (đến 6,9s); **giữ cúi x2,4 (6,9 – 8,7s, thở nhẹ)**; đứng dậy x1,6 (đến 9,74s). Mọi chuyển động chậm lại nên mềm hơn, các cú bước/xoay không còn gắt.
- `PetController.kt` bám theo mốc mới: `TURN_START` 4,39s / `TURN_SECONDS` 1,12s, `SHOW_OFF_ACTIVE_MILLIS` 11500, camera đẩy nhẹ 2,0 – 4,7s, đẩy vào lúc cúi 5,5 – 9,4s, dừng 9,8s, lưới an toàn recenter 6,5s. Spin 360° (0,78s) và camera nảy (1,55s) giữ nguyên.
- Rust lấy thời lượng từ dữ liệu clip nên không phải sửa.
- Chưa build/chạy thử được trong môi trường này.

## v16: sửa cúi chào vẫn ngả NGƯỢC ra sau (hoàn nguyên đảo dấu của v14)
- Bằng chứng: video chạy thật bản v15 cho thấy lúc cúi vẫn thấy ngực/khăn quàng, cằm hướng lên, đầu ở cao hơn hông trên màn hình (thân ngả ra sau). Phép tính "hướng đỉnh thân z = +1" của v14 dựa trên giả định quy ước `flip_quat` mà thực tế render không theo, nên đảo dấu x ở v14 làm cúi chào bị ngược.
- Quy ước đúng (đã khớp với đoạn trước 3,3s đã được duyệt): thân/đầu **x DƯƠNG = cúi ra trước**, x âm = ngửa ra sau; tay/chân x ÂM = vung ra trước, x dương = ra sau.
- `gen_fun_clips.py`: đảo lại dấu x của toàn bộ đoạn cúi (từ 3,6s) cho thân (cúi +88°, hít vào -6°), đầu (+16..21° lúc giữ cúi), tay phải (về lại -78° áp ngực, bỏ cộng dồn -360), tay trái (-62° buông xuống), chân phải lùi +24°, chân trái đỡ -8°. Ghi chú quy ước ở đầu file đã sửa. `clip_fun.rs` đã sinh lại.
- Kiểm tra bằng tính toán theo quy ước thực: lúc giữ cúi đỉnh thân hướng +Z (về phía camera), mặt đầu hướng xuống, hai tay buông thẳng xuống. Các đoạn trước 3,6s và Kotlin/pose.rs không đổi.
- Chưa build Android/`cargo test` được trong môi trường này; cần build lại thư viện Rust (clip nằm trong .so) rồi chạy thử.

## v17: nâng cấp nhịp cúi chào
- Giữ cúi: thân đảo trọng lượng nhẹ trái/phải (body y -3 -> +3 -> -1) nên không đơ.
- Đầu nghiêng nhẹ (z -3..+3°) khi giữ cúi, rồi nghiêng 4° lúc đứng dậy.
- Kết: ngẩng đầu lên (-5°) rồi gật chào nhẹ (+5°) trước khi về tư thế nghỉ.
- Chưa build/chạy thử được trong môi trường này; cần build lại thư viện Rust.
