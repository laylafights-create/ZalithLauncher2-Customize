#!/usr/bin/env python3
"""Sinh clip khoe cape (rust/src/clip_fun.rs). Chỉ dùng thư viện chuẩn.

Quy ước (gốc GLTF, độ / pixel):
  chi: x âm = vung ra trước/lên trời, x dương = ra sau; tay phải z+ = dang ngoài, tay trái z- = dang ngoài
  thân: x dương = ngả người ra trước; đầu: x dương = cúi, x âm = ngửa; body y = xoay quanh trục dọc
  body_y âm = hạ xuống, lift dương = nhấc cả người khỏi đất

Chống "cứng nhắc":
  - nội suy Hermite (vận tốc liên tục) thay vì smoothstep dừng ở mỗi key
  - chuẩn bị (anticipation) + overshoot bằng key phụ
  - overlapping action: đầu/tay/cape trễ so với thân (LAG)
  - clip kéo dài thêm TAIL giây để các khớp trễ kịp về tư thế nghỉ
"""
import math
from pathlib import Path

FPS = 30
OUT = Path(__file__).resolve().parent.parent / "rust" / "src" / "clip_fun.rs"
REST_Y = -0.64
TAIL = 0.5
LAG = {"head": 0.03, "armR": 0.02, "armL": 0.03, "cape": 0.10}


def smooth(t):
    t = max(0.0, min(1.0, t))
    return t * t * (3 - 2 * t)


def T(keys, t):
    """Hermite qua các key (time, value); tiếp tuyến Catmull-Rom, về 0 tại cực trị để không vọt quá."""
    n = len(keys)
    if t <= keys[0][0]:
        return keys[0][1]
    if t >= keys[-1][0]:
        return keys[-1][1]
    m = []
    for i in range(n):
        if i == 0 or i == n - 1:
            m.append(0.0)
            continue
        a, b, c = keys[i - 1], keys[i], keys[i + 1]
        if (b[1] - a[1]) * (c[1] - b[1]) <= 0:
            m.append(0.0)
        else:
            m.append((c[1] - a[1]) / (c[0] - a[0]))
    for i in range(n - 1):
        (t0, v0), (t1, v1) = keys[i], keys[i + 1]
        if t <= t1:
            h = t1 - t0
            s = (t - t0) / h
            h00 = 2 * s ** 3 - 3 * s ** 2 + 1
            h10 = s ** 3 - 2 * s ** 2 + s
            h01 = -2 * s ** 3 + 3 * s ** 2
            h11 = s ** 3 - s ** 2
            return h00 * v0 + h10 * h * m[i] + h01 * v1 + h11 * h * m[i + 1]
    return keys[-1][1]


def osc(amp, hz, t, phase=0.0):
    return amp * math.sin(2 * math.pi * hz * t + phase)


def env(t, dur, fade=0.35):
    return smooth(t / fade) * smooth((dur - t) / fade)


def window(t, a, b, fade=0.3):
    """1 trong [a, b], mượt ở hai đầu."""
    return smooth((t - a) / fade) * smooth((b - t) / fade)


def quat_euler(x, y, z):
    x, y, z = (math.radians(v) / 2 for v in (x, y, z))
    qx = (math.sin(x), 0, 0, math.cos(x))
    qy = (0, math.sin(y), 0, math.cos(y))
    qz = (0, 0, math.sin(z), math.cos(z))
    q = qmul(qmul(qx, qy), qz)
    n = math.sqrt(sum(c * c for c in q))
    return tuple(c / n for c in q)


def qmul(a, b):
    ax, ay, az, aw = a
    bx, by, bz, bw = b
    return (aw * bx + ax * bw + ay * bz - az * by,
            aw * by - ax * bz + ay * bw + az * bx,
            aw * bz + ax * by - ay * bx + az * bw,
            aw * bw - ax * bx - ay * by - az * bz)


CAPE_REST = (0.0, 0.996438, -0.084327, 0.0)  # tư thế cape lúc nghỉ (khóa đầu của clip idle cũ)


def cape_quat(sway_deg, roll_deg=0.0):
    """sway: lắc trước/sau quanh trục x; roll: hất sang hai bên quanh trục z (áp sau cùng, giữ dạng Rx·Ry(π)·Rz)."""
    a = math.radians(sway_deg) / 2
    r = math.radians(roll_deg) / 2
    q = qmul(qmul((math.sin(a), 0.0, 0.0, math.cos(a)), CAPE_REST), (0.0, 0.0, math.sin(r), math.cos(r)))
    n = math.sqrt(sum(c * c for c in q))
    return tuple(c / n for c in q)


# Bộ lọc lò xo (second-order) cho từng khớp: biến chuyển động "đúng từng key" thành chuyển động có quán tính,
# overshoot rồi lắng lại (follow-through). Đây là thứ làm animation bớt cứng/máy móc.
# (omega rad/s, zeta): zeta thấp = nảy nhiều; omega cao = bám sát mục tiêu.
SPRING = {
    "body": (22.0, 0.80), "head": (15.0, 0.55), "armR": (18.0, 0.60), "armL": (18.0, 0.60),
    "legR": (26.0, 0.85), "legL": (26.0, 0.85), "cape": (11.0, 0.32),
}
SUBSTEPS = 8  # bước tích phân mỗi khung 30fps (240Hz) để lò xo ổn định


def micro(t, dur):
    """Chuyển động vi mô luôn chạy trong emote (thở, trôi nhẹ) để các đoạn giữ tư thế không bị đơ."""
    e = smooth(t / 0.35) * smooth((dur - t) / 0.45)
    br = (1 - math.cos(2 * math.pi * 0.28 * t)) / 2
    return dict(
        body_y=0.10 * br * e,
        body=(-0.8 * br * e, 1.1 * math.sin(2 * math.pi * 0.21 * t) * e, 0.9 * math.sin(2 * math.pi * 0.17 * t + 1) * e),
        head=(0.8 * math.sin(2 * math.pi * 0.31 * t + .4) * e, 1.6 * math.sin(2 * math.pi * 0.23 * t) * e,
              1.0 * math.sin(2 * math.pi * 0.19 * t + 2) * e),
        armR=(1.2 * math.sin(2 * math.pi * 0.27 * t + .5) * e, 0, 0.9 * br * e),
        armL=(1.2 * math.sin(2 * math.pi * 0.27 * t + 2.5) * e, 0, -0.9 * br * e),
        cape=2.0 * math.sin(2 * math.pi * 0.4 * t + 1) * e)


def add3(a, b):
    return tuple(x + y for x, y in zip(a, b))


def clip(dur, fn):
    """fn(t) với t∈[0,dur] -> dict tư thế. Mỗi khớp đi qua bộ lọc lò xo (SPRING) + chuyển động vi mô;
    khớp trễ (LAG) lấy mẫu fn ở thời điểm sớm hơn. Clip kéo dài thêm TAIL để lò xo lắng hẳn về nghỉ."""
    n = int(round((dur + TAIL) * FPS)) + 1
    ch = {k: [] for k in ("Body", "Head", "ArmRight", "ArmLeft", "LegRight", "LegLeft", "Cape")}
    tr = {k: [] for k in ("Body", "ArmRight", "ArmLeft", "LegRight", "LegLeft")}
    z3 = (0.0, 0.0, 0.0)
    joint_of = {"Body": "body", "Head": "head", "ArmRight": "armR", "ArmLeft": "armL",
                "LegRight": "legR", "LegLeft": "legL"}
    lag_of = {"head": "head", "armR": "armR", "armL": "armL", "cape": "cape"}

    def target(ts):
        """Mục tiêu (chưa lọc) của mọi khớp tại thời điểm ts, dạng vector số thực."""
        base = fn(max(0.0, min(dur + TAIL, ts)))
        mic = micro(max(0.0, min(dur + TAIL, ts)), dur)
        out = {}
        for j in ("body", "head", "armR", "armL", "legR", "legL"):
            src = base
            if j in lag_of:
                src = fn(max(0.0, min(dur + TAIL, ts - LAG[lag_of[j]])))
            v = tuple(float(c) for c in src.get(j, z3))
            m = mic.get(j)
            if m is not None and j not in ("legR", "legL"):
                v = add3(v, tuple(float(c) for c in m))
            out[j] = v
        src = fn(max(0.0, min(dur + TAIL, ts - LAG["cape"])))
        out["cape"] = (float(src.get("cape", 0.0)) + mic["cape"], float(src.get("cape_roll", 0.0)))
        out["_lift"] = (float(base.get("lift", 0.0)),)
        out["_body_y"] = (float(base.get("body_y", 0.0)) + mic["body_y"],)
        return out

    first = target(0.0)
    x = {k: list(v) for k, v in first.items() if not k.startswith("_")}
    vel = {k: [0.0] * len(v) for k, v in x.items()}
    dt = 1.0 / (FPS * SUBSTEPS)
    final = target(dur + TAIL)

    for i in range(n):
        t = i / FPS
        if i > 0:
            for sub in range(SUBSTEPS):
                ts = (i - 1) / FPS + (sub + 1) * dt
                tg = target(ts)
                for k in x:
                    om, ze = SPRING[k]
                    for a in range(len(x[k])):
                        acc = om * om * (tg[k][a] - x[k][a]) - 2 * ze * om * vel[k][a]
                        vel[k][a] += acc * dt
                        x[k][a] += vel[k][a] * dt
        cur = {k: list(v) for k, v in x.items()}
        # Vài khung cuối kéo về đúng tư thế kết thúc để nối mượt với animation nền.
        k_end = n - 1 - i
        if k_end < 3:
            w = (3 - k_end) / 3.0
            for k in cur:
                for a in range(len(cur[k])):
                    cur[k][a] = cur[k][a] * (1 - w) + final[k][a] * w
        tg = target(t)
        ch["Body"].append(quat_euler(*cur["body"]))
        ch["Head"].append(quat_euler(*cur["head"]))
        ch["ArmRight"].append(quat_euler(*cur["armR"]))
        ch["ArmLeft"].append(quat_euler(*cur["armL"]))
        ch["LegRight"].append(quat_euler(*cur["legR"]))
        ch["LegLeft"].append(quat_euler(*cur["legL"]))
        ch["Cape"].append(cape_quat(cur["cape"][0], cur["cape"][1]))
        lift = tg["_lift"][0]
        tr["Body"].append((0.0, REST_Y + lift + tg["_body_y"][0], 0.0))
        tr["ArmRight"].append((5.0, 10.0, 0.0))
        tr["ArmLeft"].append((-5.0, 10.0, 0.0))
        tr["LegRight"].append((1.9, max(0.0, lift), 0.0))
        tr["LegLeft"].append((-1.9, max(0.0, lift), 0.0))
    return ch, tr


# ============ clip khoe cape: "anh hùng ra mắt" ============

CAPE_DUR = 5.4


def cape_pose(t):
    # Kịch bản 5.4s, 6 màn:
    #  0-.7    dồn lực: hạ thấp, xoay lưng, vòng tay phải ra sau, cape bị kéo ép sát người
    #  .7-1.5  bùng nổ: bật CAO (lơ lửng ~0.65s), tay phải vút lên trời, xoay trọn 360° trên không (yaw do Kotlin), cape tung xoáy
    #  1.5-2.0 tiếp đất: nhún cực mạnh, cape phồng như cánh dù rồi rơi theo quán tính
    #  1.8-3.4 tạo dáng: chân dạng rộng, tay trái chống hông, thân quét từ trái sang phải, cape cuộn sóng hai bên
    #  3.3-4.55 màn kết "giật cape": hạ tay phải, hai tay gom cape chéo trước ngực + cúi gập (3.3-3.8),
    #          rồi BUNG: hai tay dang rộng, ngửa người, nhảy lên, cape bùng tung xoè rộng (3.8-4.55)
    #  4.5-5.4 thu về: hạ tay, cúi chào nhẹ, cape lắng dần
    hold = window(t, 1.75, 3.3, .3)
    finale = window(t, 3.45, 4.5, .25)
    flutter = (osc(14, 1.9, t, .9) + osc(7, 3.3, t, 2.1)) * hold + osc(11, 2.4, t, .4) * finale
    roll = (osc(26, 1.1, t, .3) * hold + osc(40, 1.9, t, 1.2) * finale
            + T([(0, 0), (.85, 0), (1.1, 62), (1.35, -62), (1.6, 26), (2.0, 0), (3.75, 0), (4.05, 55), (4.35, -48), (4.7, 0), (5.4, 0)], t))
    tremble = osc(1.6, 8.0, t) * hold
    ax = T([(0, 0), (.35, 26), (.7, 62), (.9, -130), (1.1, -202), (1.35, -196), (1.5, -188), (2.0, -178), (3.3, -180),
            (3.6, -44), (3.78, -42), (4.0, -12), (4.55, -10), (5.0, 8), (5.4, 0)], t)
    az = T([(0, 0), (.65, 14), (1.1, 14), (1.8, 7), (3.3, 8), (3.6, -15), (3.78, -16), (4.0, 84), (4.55, 78), (5.0, 6), (5.4, 0)], t)
    crouch = T([(0, 0), (.55, 1.1), (.7, .95), (.9, 0), (1.5, 0), (1.63, 1.35), (2.05, 0),
                (3.35, 0), (3.72, 1.1), (3.92, .1), (4.55, 0), (4.8, .5), (5.1, 0), (5.4, 0)], t)
    lift = T([(0, 0), (.85, 0), (1.18, 7.5), (1.5, 0), (3.75, 0), (4.02, 5.5), (4.35, 0), (5.4, 0)], t)
    stance = T([(0, 0), (1.55, 0), (1.95, 1), (3.3, 1), (3.7, .3), (4.05, 1), (4.55, .8), (5.0, 0), (5.4, 0)], t)
    bow = T([(0, 0), (4.5, 0), (4.85, 1), (5.15, 0), (5.4, 0)], t)
    return dict(
        armR=(ax + tremble, 0, az + osc(1.8, 1.9, t, 1) * hold),
        armL=(T([(0, 0), (.55, 34), (1.0, -14), (1.8, 16), (3.3, 16), (3.6, -66), (3.78, -64), (4.0, -12), (4.55, -10), (5.0, 0), (5.4, 0)], t), 0,
              T([(0, 0), (.55, -8), (1.1, -34), (1.9, -22), (3.3, -22), (3.6, 12), (3.78, 13), (4.0, -84), (4.55, -78), (5.0, -4), (5.4, 0)], t)),
        body=(T([(0, 0), (.55, 18), (.95, -18), (1.2, -15), (1.6, 8), (2.0, -10), (3.3, -10), (3.7, 16), (4.0, -18), (4.55, -8), (4.9, 22), (5.2, 4), (5.4, 0)], t),
              T([(0, 0), (.55, -28), (.95, 14), (1.3, 10), (1.9, -20), (2.6, 4), (3.3, 22), (3.8, -10), (4.2, 8), (4.6, 0), (5.4, 0)], t),
              osc(2.2, 1.2, t, .5) * hold + osc(3, 1.5, t) * finale),
        head=(T([(0, 0), (.55, 14), (1.0, -34), (1.5, -4), (1.9, -26), (3.3, -22), (3.7, 10), (4.05, -30), (4.55, -14), (4.9, 20), (5.2, 2), (5.4, 0)], t),
              T([(0, 0), (.55, 22), (1.1, -10), (1.9, 16), (2.6, -6), (3.3, -18), (3.8, 10), (4.4, -6), (5.4, 0)], t),
              7 * hold + 5 * finale),
        legR=(T([(0, 0), (.55, -18), (1.0, -46), (1.3, -38), (1.6, -16), (3.3, -12), (3.8, -6), (4.55, -6), (5.1, 0), (5.4, 0)], t), 0, 11 * stance + 7 * crouch),
        legL=(T([(0, 0), (.55, 14), (1.0, 34), (1.3, 26), (1.6, 10), (3.3, 8), (3.8, 4), (4.55, 4), (5.1, 0), (5.4, 0)], t), 0, -11 * stance - 7 * crouch),
        body_y=-1.9 * crouch - 0.9 * bow - 0.25 * osc(1, 1.4, t) * hold + 0.35 * osc(1, 2.6, t) * finale, lift=lift,
        cape=T([(0, 0), (.55, 24), (.85, -30), (1.05, -92), (1.35, -110), (1.7, -112), (2.1, -52), (2.6, -30), (3.3, -26),
                (3.65, 20), (4.05, -130), (4.4, -96), (4.7, -50), (5.0, -8), (5.4, 0)], t) + flutter,
        cape_roll=roll)


SPECIAL = [("CAPE_POSE", "Khoe cape: anh hùng ra mắt", CAPE_DUR, cape_pose)]


def f(v):
    s = f"{v:.6f}".rstrip("0").rstrip(".")
    return s if "." in s else s + ".0"


def emit(out, const, doc, dur, fn):
    ch, tr = clip(dur, fn)
    out.append(f"/// {doc}\n#[rustfmt::skip]\npub(crate) static {const}: Clip = Clip {{\n    channels: &[\n")
    for j in ("Body", "Head", "ArmRight", "ArmLeft", "LegRight", "LegLeft", "Cape"):
        rot = ", ".join("[" + ", ".join(f(c) for c in q) + "]" for q in ch[j])
        trs = ", ".join("[" + ", ".join(f(c) for c in v) + "]" for v in tr[j]) if j in tr else ""
        out.append(f"        Channel {{\n            joint: Joint::{j},\n            rotation: &[{rot}],\n            translation: &[{trs}],\n        }},\n")
    out.append("    ],\n};\n")


def main():
    out = ["//! Clip khoe cape (CAPE_POSE). Sinh bởi tools/gen_fun_clips.py — đừng sửa tay.\n",
           "use crate::clip_data::{Channel, Clip, Joint};\n\n"]
    for c in SPECIAL:
        emit(out, *c)
    OUT.write_text("".join(out), encoding="utf-8")
    print("wrote", OUT, OUT.stat().st_size, "bytes")


if __name__ == "__main__":
    main()
