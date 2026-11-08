#!/usr/bin/env python3
"""Sinh clip khoe cape (rust/src/clip_fun.rs). Chỉ dùng thư viện chuẩn.

Quy ước (gốc GLTF, độ / pixel):
  chi (tay/chân): x DƯƠNG = vung ra TRƯỚC, x âm = ra sau; tay phải z+ = dang ngoài, tay trái z- = dang ngoài
  thân/đầu: x ÂM = cúi/ngả ra TRƯỚC, x DƯƠNG = ngửa/ngả ra SAU; body y = xoay quanh trục dọc
  (v18, suy ra từ renderer: mặt trước = +Z, flip_quat đổi dấu x nên clip x -> Rx(-x) trong không gian render; thân x+ => đỉnh thân về -Z = ra sau.
   Khớp video chạy thật bản v17: x dương làm ngực/khăn hướng lên, cape nằm dưới thân. Đừng đảo lại theo cảm tính.)
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
LAG = {"head": 0.05, "armR": 0.03, "armL": 0.04, "cape": 0.12}


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


def impact(t, t0, hz=9.0, decay=0.18):
    """Rung tắt dần sau một cú chạm đất tại t0 (0 trước t0)."""
    if t < t0:
        return 0.0
    d = t - t0
    return math.exp(-d / decay) * math.sin(2 * math.pi * hz * d) * smooth((1.4 - d) / 0.5)  # tắt hẳn sau 1.4s


def quat_euler(x, y, z):
    x, y, z = (math.radians(v) / 2 for v in (x, y, z))
    qx = (math.sin(x), 0, 0, math.cos(x))
    qy = (0, math.sin(y), 0, math.cos(y))
    qz = (0, 0, math.sin(z), math.cos(z))
    q = qmul(qmul(qx, qy), qz)
    n = math.sqrt(sum(c * c for c in q))
    if q[3] < 0:
        n = -n  # q và -q là cùng một phép quay; chuẩn hoá w >= 0 (tay phải cộng dồn tới -360° = tư thế nghỉ)
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
    "body": (19.0, 0.82), "head": (13.0, 0.58), "armR": (15.0, 0.64), "armL": (15.0, 0.64),
    "legR": (22.0, 0.88), "legL": (22.0, 0.88), "cape": (10.0, 0.34),
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

# Kịch bản gốc (thời gian u, 6,3s) được kéo giãn theo từng đoạn để màn khoe cape dài và có nhịp:
#   0-1.5 bật/xoay/tiếp đất: giữ nguyên (Kotlin spin 0,78s, camera nảy 1,55s)
#   1.5-3.3 tạo dáng x2.0 | 3.3-4.2 quay người + hít vào x1.6 | 4.2-4.9 cúi xuống x2.0
#   4.9-5.65 giữ cúi x2.6 (dài, thở nhẹ) | 5.65-6.3 đứng dậy x2.0
SEGS = [(0.0, 1.5, 1.0), (1.5, 3.3, 2.0), (3.3, 4.2, 1.6), (4.2, 4.9, 2.0), (4.9, 5.65, 2.6), (5.65, 6.3, 2.0)]
_knots = [0.0]
for _a, _b, _k in SEGS:
    _knots.append(_knots[-1] + (_b - _a) * _k)
CAPE_DUR = _knots[-1]  # = 11.19s (v20 chậm lại). Kotlin: TURN_START 4.9, TURN_SECONDS 1.32, camera bám các mốc này


def warp(t):
    """Thời gian thật t -> thời gian kịch bản gốc u (tuyến tính từng đoạn)."""
    if t <= 0.0:
        return 0.0
    for i, (a, b, k) in enumerate(SEGS):
        if t <= _knots[i + 1]:
            return a + (t - _knots[i]) / k
    return SEGS[-1][1]


BODY_X_KEYS = [(0, 0), (.55, 18), (.95, -18), (1.2, -15), (1.6, 8), (2.0, -10), (3.3, -10), (3.6, -2), (3.95, 6), (4.2, 3), (4.4, -3), (4.65, -20),
    (4.9, -36), (5.05, -39), (5.25, -36), (5.55, -35), (5.75, -33), (5.92, -16), (6.05, 1), (6.14, 4), (6.22, -1), (6.3, 0)]


def dosc(t, t0, amp, hz, decay):
    """Dao động tắt dần bắt đầu tại t0 (follow-through khi một chuyển động dừng đột ngột)."""
    if t < t0:
        return 0.0
    d = t - t0
    return amp * math.exp(-d / decay) * math.sin(2 * math.pi * hz * d)


def _cape_base(t):
    # Kịch bản gốc 6.3s (t ở đây là u, xem SEGS):
    #  0-.7    dồn lực: hạ thấp, xoay lưng, vòng tay phải ra sau, cape bị kéo ép sát người
    #  .7-1.5  bùng nổ: bật CAO (lơ lửng ~0.65s), tay phải vút lên trời, xoay trọn 360° trên không (yaw do Kotlin), cape tung xoáy
    #  1.5-2.0 tiếp đất: nhún cực mạnh, cape phồng như cánh dù rồi rơi theo quán tính
    #  1.8-3.3 tạo dáng: chân dạng rộng, tay trái chống hông, thân quét từ trái sang phải, cape cuộn sóng hai bên
    #  3.2-4.0 QUAY NGƯỜI về mặt trước (yaw do Kotlin, smootherstep); cape tung ra theo vòng quay
    #  3.3-4.0 hồi phục: tay phải hạ xuống trước ngực, hít vào (anticipation)
    #  3.6-4.4 chuẩn bị: hít vào ưỡn nhẹ, đứng thẳng, đầu nhìn khán giả (anticipation)
    #  4.4-4.95 CÚI VỪA ~36° (x ÂM): chân phải trượt lùi, thân hạ theo nhịp (overshoot 39°), đầu trễ nhịp cúi thêm 20°,
    #          tay phải áp ngực, tay trái đưa ra sau làm đối trọng, cape bồng lên theo quán tính
    #  4.95-5.75 giữ cúi: thở, đảo trọng lượng, đầu liếc nhẹ sang bên (không đơ)
    #  5.75-6.3 đứng dậy: thân ưỡn nhẹ +4° (tự tin), ngẩng đầu nhìn lại rồi gật chào, về tư thế nghỉ
    #  Cape: sway DƯƠNG = ép vào lưng (cape.x giảm, dễ xuyên thân), ÂM = tung ra sau. Từ 3.3s chỉ dùng sway ÂM
    #  (cape.x >= ~6°): khi thân cúi ngang, cape nằm phủ trên lưng, không chui vào thân.
    hold = window(t, 1.75, 3.3, .3)
    flick = window(t, 2.3, 3.0, .2)  # tay trái rời hông hất cape ra
    bowwin = window(t, 4.9, 5.7, .25)    # nhịp thở khi giữ tư thế cúi sâu
    flutter = ((osc(14, 1.9, t, .9) + osc(7, 3.3, t, 2.1) + osc(4, 5.2, t, 1.4)) * hold
               + 20 * flick * math.sin(2 * math.pi * 1.6 * (t - 2.3))
               + 3.0 * bowwin * math.sin(2 * math.pi * 1.1 * t + .5))
    roll = (osc(26, 1.1, t, .3) * hold + 34 * flick * math.sin(2 * math.pi * 1.3 * (t - 2.3) + .6)
            + 20 * impact(t, 1.58, 5.0, .35) + 7 * impact(t, 4.7, 6.0, .3)
            + T([(0, 0), (.85, 0), (1.1, 62), (1.35, -62), (1.6, 26), (2.0, 0), (3.35, 0), (3.65, -24), (3.95, 14),
                 (4.5, -5), (5.0, 6), (5.6, -3), (6.1, 3), (6.3, 0)], t))
    tremble = 0.0  # run 8Hz ở 30fps trông như giật (jitter) chứ không phải mềm: bỏ
    ax = T([(0, 0), (.35, 26), (.7, 62), (.94, -130), (1.12, -202), (1.35, -196), (1.5, -188), (2.0, -178), (3.3, -180),
            (3.62, -268), (3.85, -304), (4.1, -300), (4.45, -292), (4.8, -284), (5.0, -281), (5.55, -285), (5.8, -296), (5.97, -330), (6.15, -353), (6.3, -360)], t)
    az = T([(0, 0), (.65, 14), (1.1, 14), (1.8, 7), (3.3, 8), (3.5, 32), (3.75, 10), (3.9, -10), (4.1, -14), (4.95, -18), (5.55, -18),
            (5.9, 0), (6.3, 0)], t)
    crouch = T([(0, 0), (.55, 1.1), (.7, .95), (.9, 0), (1.5, 0), (1.63, 1.35), (2.05, 0), (6.3, 0)], t)
    lift = T([(0, 0), (.85, 0), (1.18, 7.5), (1.5, 0), (6.3, 0)], t)
    stance = T([(0, 0), (1.55, 0), (1.95, 1), (3.3, 1), (3.75, 0), (6.3, 0)], t)
    bow = T([(0, 0), (4.1, 0), (4.9, 1), (5.55, 1), (6.0, 0), (6.3, 0)], t)
    inhale = T([(0, 0), (3.6, 0), (3.9, 1), (4.15, 0), (6.3, 0)], t)  # hít vào trước khi cúi: nâng ngực
    # Chân phải lùi ra sau và bắt chéo qua chân trái (dáng cúi chào), chân trái đỡ.
    tw = T([(0, 0), (3.2, 0), (3.45, 1), (3.85, .4), (4.05, 0), (6.3, 0)], t)  # dạng chân khi xoay người
    cross = T([(0, 0), (4.0, 0), (4.8, -8), (5.55, -8), (5.95, 0), (6.3, 0)], t)
    # --- Mềm hoá tay/chân khi chào: quán tính theo thân + follow-through + dao động trọng lượng ---
    bodyx = T(BODY_X_KEYS, t)
    lagx = T(BODY_X_KEYS, t - 0.3)
    fade_end = smooth((6.3 - t) / 0.35)  # mọi dao động phải tắt hẳn khi clip về tư thế nghỉ
    drag = -0.55 * (bodyx - lagx) * T([(0, 0), (3.9, 0), (4.3, 1), (6.3, 1)], t) * fade_end  # tay buông như con lắc, trễ sau thân
    arm_settle = (dosc(t, 4.95, 5.0, 1.1, 0.55) + dosc(t, 5.85, -6.0, 1.3, 0.35)) * fade_end  # lắng sau khi hạ / đứng dậy
    pat = dosc(t, 5.05, 3.0, 1.9, 0.5) * fade_end  # tay phải khẽ vỗ nhẹ vào ngực khi chạm
    sw = T([(0, 0), (4.3, 0), (4.8, 1), (5.7, 1), (6.1, 0), (6.3, 0)], t)  # cửa sổ nhún/đảo trọng lượng khi giữ cúi
    wsh = math.sin(2 * math.pi * 0.55 * t + 0.4) * sw
    leg_settle = (dosc(t, 4.9, 2.5, 1.3, 0.5) + dosc(t, 5.9, -3.0, 1.5, 0.35)) * fade_end
    return dict(
        armR=(ax + tremble + 0.8 * drag + pat, 0, az + osc(1.8, 1.9, t, 1) * hold + 3.0 * wsh + 2.0 * arm_settle),
        armL=(-46 * flick + 1.0 * drag + arm_settle + 2.0 * wsh + T([(0, 0), (.55, 34), (1.0, -14), (1.8, 16), (3.3, 16), (3.7, 26), (4.0, 34), (4.4, 8), (4.75, -22), (5.0, -30), (5.3, -27),
                               (5.55, -29), (5.8, -20), (6.0, -4), (6.15, 5), (6.3, 0)], t), 0,
              -30 * flick - 3.5 * wsh - 1.5 * arm_settle + T([(0, 0), (.55, -8), (1.1, -34), (1.9, -22), (3.3, -22), (3.5, -42), (3.8, -6), (4.6, -6), (5.5, -8), (5.9, -2), (6.3, 0)], t)),
        body=(bodyx + 1.6 * bowwin * math.sin(2 * math.pi * 1.1 * t),
              T([(0, 0), (.55, -28), (.95, 14), (1.3, 10), (1.9, -20), (2.6, 4), (3.0, 14), (3.2, -12), (3.45, -22), (3.8, -6), (4.0, 6), (4.25, 2), (4.5, 5), (5.0, -5), (5.4, 4), (5.8, -2), (6.3, 0)], t),
              osc(2.2, 1.2, t, .5) * hold + T([(0, 0), (3.8, 0), (4.5, 2), (4.95, -4), (5.4, -2), (5.85, 1), (6.3, 0)], t)),
        head=(T([(0, 0), (.55, 14), (1.0, -34), (1.5, -4), (1.9, -26), (3.3, -22), (3.75, -8), (4.05, -3), (4.3, 3), (4.55, 1), (4.8, -10), (5.05, -20), (5.3, -22),
                 (5.55, -19), (5.8, -14), (5.98, -1), (6.1, 6), (6.2, -5), (6.3, 0)], t)
              + 1.2 * bowwin * math.sin(2 * math.pi * 1.1 * t + 1.2),
              T([(0, 0), (.55, 22), (1.1, -10), (1.9, 16), (2.6, -6), (3.0, -18), (3.25, 8), (3.6, 12), (3.85, 3), (4.3, -6), (4.9, 4), (5.5, 2), (5.95, -4), (6.3, 0)], t),
              7 * hold + T([(0, 0), (4.2, 0), (4.9, 3), (5.3, -3), (5.7, 0), (6.05, 4), (6.3, 0)], t)),
        legR=(leg_settle + 2.0 * wsh + T([(0, 0), (.55, -18), (1.0, -46), (1.3, -38), (1.6, -16), (3.3, -12), (3.5, -46), (3.65, -34), (3.85, -6), (3.95, -6), (4.3, -6), (4.6, -10), (4.95, -19), (5.55, -19),
                 (5.85, -10), (5.98, -3), (6.3, 0)], t), 0, 11 * stance + 7 * crouch + cross + 12 * tw + 2.5 * wsh),
        legL=(-0.6 * leg_settle - 2.0 * wsh + T([(0, 0), (.55, 14), (1.0, 34), (1.3, 26), (1.6, 10), (3.3, 8), (3.5, 32), (3.68, 22), (3.88, 12), (4.0, 2), (4.4, 4), (4.95, 8), (5.55, 8),
                 (5.9, 3), (6.05, -2), (6.3, 0)], t), 0, -11 * stance - 7 * crouch - 12 * tw + 2.5 * wsh),
        body_y=(-1.9 * crouch - 0.9 * bow + 0.35 * inhale + 0.22 * impact(t, 1.55) + 0.12 * impact(t, 4.7, 7.0, .3)
                - 0.25 * osc(1, 1.4, t) * hold - 1.2 * T([(0, 0), (3.1, 0), (3.3, 1), (3.55, 0), (6.3, 0)], t) + 0.12 * bowwin * math.sin(2 * math.pi * 1.3 * t)), lift=lift,
        cape=T([(0, 0), (.55, 24), (.85, -30), (1.05, -92), (1.35, -110), (1.7, -112), (2.1, -52), (2.6, -30), (3.3, -26),
                (3.55, -42), (3.85, -14), (4.1, -4), (4.5, -8), (4.8, -17), (5.05, -13), (5.35, -8), (5.6, -7), (5.85, -15), (6.0, -12), (6.15, -5), (6.3, 0)], t) + flutter,
        cape_roll=roll)


def cape_pose(t):
    return _cape_base(warp(t))


SPECIAL = [("CAPE_POSE", "Khoe cape v15: anh hùng ra mắt, quay người rồi cúi chào vừa phải (dài ~11,2s)", CAPE_DUR, cape_pose)]


def f(v):
    s = f"{v:.6f}".rstrip("0").rstrip(".")
    return s if "." in s else s + ".0"


def emit(out, const, doc, dur, fn):
    ch, tr = clip(dur, fn)
    out.append(f"/// {doc}\n#[rustfmt::skip]\npub(crate) static {const}: Clip = Clip {{\n    channels: &[\n")
    for j in ("Body", "Head", "ArmRight", "ArmLeft", "LegRight", "LegLeft", "Cape"):
        rot = ", ".join("[" + ", ".join(f(c) for c in q) + "]" for q in ch[j])
        keys = tr.get(j, [])
        if keys and all(k == keys[0] for k in keys):
            keys = keys[:1]  # kênh hằng (vd. pivot tay): 1 key là đủ, sampler trả thẳng keys[0]
        trs = ", ".join("[" + ", ".join(f(c) for c in v) + "]" for v in keys)
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
