# Nâng cấp animation (hướng tới độ chỉn chu kiểu Essential)

## Walking (rust/src/animation.rs)
- Nhấc chân (leg_lift): chân vung tới được nhấc lên rồi đặt xuống dứt khoát; chỉ một chân nhấc tại một thời điểm.
- Thân nghiêng nhẹ về trước, nghiêng ngang theo chân chống, đầu gật nhẹ theo mỗi bước, tay thêm hài bậc hai.
- Giữ nguyên các tần số cũ nên WALKING_PERIOD vẫn đúng. Thêm 2 test mới.

## Chuyển cảnh (rust/src/pose.rs)
- Trọng số crossfade dùng smootherstep thay smoothstep: bắt đầu/kết thúc êm hơn, tổng trọng số vẫn = 1.
- snapshot_from_frame nhận leg_lift vào trục y của chân.

## Idle (tools/gen_fun_clips.py)
- Chu kỳ 8s, 3 lớp: thở (hít nhanh/thở ra chậm, ngực nâng, tay trễ theo thân), chuyển trọng tâm (hông lệch,
  thân nghiêng ngược, chân chống/chân thả lỏng), liếc nhìn hai lần + một lần thở dài. Lặp liền mạch (đã kiểm chứng).

## Emote mới
Clap(13), Bow(14), Shrug(15), Think(16) -> thêm vào ALL_SUBS (cũng nằm trong pool ngẫu nhiên) và PetEmote.kt.

## Cách dùng
Sau khi sửa tools/gen_fun_clips.py: `python3 tools/gen_fun_clips.py` để sinh lại rust/src/clip_fun.rs.
Chạy `cargo test` trong PetLib/rust để xác nhận.

## Hiệu ứng khoe cape (CAPE_POSE)
Viết lại thành kịch bản "anh hùng ra mắt" 3.65s: dồn lực -> bật người, tay phải vút lên trời (overshoot) -> tiếp đất nhún mạnh
-> tạo dáng chân dạng rộng, tay trái chống hông, cape phần phật -> hạ tay chậm. Thời lượng giữ nguyên nên code gọi không đổi.
