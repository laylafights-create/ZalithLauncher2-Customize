#!/usr/bin/env python3
"""Sinh các clip nghịch ngợm + clip khoe cape (rust/src/clip_fun.rs). Chỉ dùng thư viện chuẩn.

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
TAIL = 0.25
LAG = {"head": 0.06, "armR": 0.045, "armL": 0.06, "cape": 0.16}


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


CAPE_REST = (0.0, 0.996438, -0.084327, 0.0)  # khóa đầu của cape trong CLIP_IDLE


def cape_quat(sway_deg):
    a = math.radians(sway_deg) / 2
    q = qmul((math.sin(a), 0.0, 0.0, math.cos(a)), CAPE_REST)
    n = math.sqrt(sum(c * c for c in q))
    return tuple(c / n for c in q)


def clip(dur, fn):
    """fn(t) với t∈[0,dur] -> dict tư thế. Khớp trễ (LAG) lấy mẫu fn ở thời điểm sớm hơn."""
    n = int(round((dur + TAIL) * FPS)) + 1
    ch = {k: [] for k in ("Body", "Head", "ArmRight", "ArmLeft", "LegRight", "LegLeft", "Cape")}
    tr = {k: [] for k in ("Body", "ArmRight", "ArmLeft", "LegRight", "LegLeft")}
    z3 = (0, 0, 0)

    def at(t, lag=0.0):
        return fn(max(0.0, min(dur, t - lag)))

    for i in range(n):
        t = i / FPS
        p = at(t)
        ph, pr, pl, pc = (at(t, LAG[k]) for k in ("head", "armR", "armL", "cape"))
        ch["Body"].append(quat_euler(*p.get("body", z3)))
        ch["Head"].append(quat_euler(*ph.get("head", z3)))
        ch["ArmRight"].append(quat_euler(*pr.get("armR", z3)))
        ch["ArmLeft"].append(quat_euler(*pl.get("armL", z3)))
        ch["LegRight"].append(quat_euler(*p.get("legR", z3)))
        ch["LegLeft"].append(quat_euler(*p.get("legL", z3)))
        ch["Cape"].append(cape_quat(pc.get("cape", 0.0)))
        lift = p.get("lift", 0.0)
        tr["Body"].append((0.0, REST_Y + lift + p.get("body_y", 0.0), 0.0))
        tr["ArmRight"].append((5.0, 10.0, 0.0))
        tr["ArmLeft"].append((-5.0, 10.0, 0.0))
        tr["LegRight"].append((1.9, max(0.0, lift), 0.0))
        tr["LegLeft"].append((-1.9, max(0.0, lift), 0.0))
    return ch, tr


def loop_clip(period, fn):
    """Clip lặp liền mạch: fn tuần hoàn theo `period`; khớp trễ dùng thời gian âm (không kẹp)."""
    n = int(round(period * FPS)) + 1
    ch = {k: [] for k in ("Body", "Head", "ArmRight", "ArmLeft", "LegRight", "LegLeft", "Cape")}
    tr = {k: [] for k in ("Body", "ArmRight", "ArmLeft", "LegRight", "LegLeft")}
    z3 = (0, 0, 0)
    for i in range(n):
        t = i / FPS
        p = fn(t)
        ph, pr, pl, pc = (fn(t - LAG[k]) for k in ("head", "armR", "armL", "cape"))
        ch["Body"].append(quat_euler(*p.get("body", z3)))
        ch["Head"].append(quat_euler(*ph.get("head", z3)))
        ch["ArmRight"].append(quat_euler(*pr.get("armR", z3)))
        ch["ArmLeft"].append(quat_euler(*pl.get("armL", z3)))
        ch["LegRight"].append(quat_euler(*p.get("legR", z3)))
        ch["LegLeft"].append(quat_euler(*p.get("legL", z3)))
        ch["Cape"].append(cape_quat(pc.get("cape", 0.0)))
        tr["Body"].append((p.get("body_x", 0.0), REST_Y + p.get("body_y", 0.0), 0.0))
        tr["ArmRight"].append((5.0, 10.0, 0.0))
        tr["ArmLeft"].append((-5.0, 10.0, 0.0))
        tr["LegRight"].append((1.9, 0.0, 0.0))
        tr["LegLeft"].append((-1.9, 0.0, 0.0))
    return ch, tr


# ============ các clip ============

def jump(t):
    crouch = T([(0, 0), (.4, 1), (.6, .2), (.62, 0), (1.15, 0), (1.3, .8), (1.7, 0)], t)
    lift = T([(0, 0), (.58, 0), (.85, 5.2), (1.12, 0)], t)
    ax = T([(0, 0), (.4, 40), (.62, -70), (.85, -168), (1.1, -125), (1.5, 18), (1.9, 0)], t)
    az = T([(0, 0), (.6, 8), (.85, 38), (1.1, 22), (1.4, -3), (1.8, 0)], t)
    return dict(
        body_y=-1.5 * crouch, lift=lift,
        armR=(ax, 0, az), armL=(ax, 0, -az),
        legR=(T([(0, 0), (.4, -14), (.85, -32), (1.1, -8), (1.8, 0)], t), 0, T([(0, 0), (.85, 13), (1.2, 0)], t)),
        legL=(T([(0, 0), (.4, 10), (.85, 24), (1.1, 6), (1.8, 0)], t), 0, T([(0, 0), (.85, -13), (1.2, 0)], t)),
        body=(T([(0, 0), (.4, 13), (.7, -9), (1.1, -4), (1.35, 9), (1.8, 0)], t), 0, 0),
        head=(T([(0, 0), (.4, 12), (.85, -20), (1.3, 7), (1.8, 0)], t), 0, 0),
        cape=T([(0, 0), (.4, 12), (.85, -28), (1.2, 14), (1.8, 0)], t))


def spin(t):
    ang = T([(0, 0), (.4, -28), (1.7, 382), (2.0, 356), (2.25, 360)], t)
    arms = T([(0, 0), (.5, 0), (.95, 82), (1.7, 82), (2.1, 0)], t)
    return dict(
        body=(T([(0, 0), (.4, 4), (1.7, -3), (2.25, 0)], t), ang, 0),
        armR=(T([(0, 0), (.4, 25), (.95, -8), (2.1, 0)], t), 0, arms),
        armL=(T([(0, 0), (.4, -15), (.95, -8), (2.1, 0)], t), 0, -arms),
        legR=(0, 0, 0), legL=(T([(0, 0), (.6, 0), (1.0, 38), (1.7, 38), (2.1, 0)], t), 0, 0),
        head=(T([(0, 0), (1.0, -6), (2.25, 0)], t), 0, 0),
        lift=0.7 * window(t, .7, 1.8, .35), cape=34 * window(t, .5, 1.9, .5))


def wave(t):
    up = T([(0, 0), (.3, -.12), (.75, 1), (2.0, 1), (2.5, 0)], t)
    w = window(t, .85, 2.05, .25)
    return dict(
        armR=(-150 * up + 12 * (1 - up) * smooth(t / .3) * (1 if t < .75 else 0), 0, 24 * up + osc(26, 2.1, t) * w),
        armL=(osc(2, 1.0, t) * up, 0, -6 * up),
        head=(T([(0, 0), (.8, -6), (2.0, -4), (2.6, 0)], t), T([(0, 0), (.8, -12), (2.0, -9), (2.6, 0)], t), 7 * up + osc(2, 2.1, t) * w),
        body=(0, -7 * up, osc(2.5, 1.05, t) * w), legR=(-6 * up, 0, 0), legL=(4 * up, 0, 0),
        body_y=osc(.25, 2.1, t, 1.2) * w, cape=6 * up + osc(5, 2.1, t, 1) * w)


def stomp(t):
    dur = 2.8
    e = env(t, dur, .4)
    ph = 2 * math.pi * 1.6 * t
    s = math.sin(ph)
    liftR, liftL = max(0, s) * e, max(0, -s) * e
    dip = -(1 - abs(s)) ** 6 * e
    return dict(
        legR=(-42 * liftR ** .8, 0, 0), legL=(-42 * liftL ** .8, 0, 0),
        body_y=1.0 * dip, head=(9 * e - 8 * dip, osc(7, 1.6, t, .6) * e, 0),
        body=(10 * e - 3 * dip, osc(3, 0.8, t) * e, osc(3.5, 1.6, t) * e),
        armR=(-28 * e + osc(9, 3.2, t) * e, 0, -7 * e), armL=(-28 * e + osc(9, 3.2, t, 1.3) * e, 0, 7 * e),
        cape=osc(10, 1.6, t, 1) * e)


def dance(t):
    dur = 3.6
    e = env(t, dur, .45)
    f = 1.8
    ph = 2 * math.pi * f * t
    bounce = (1 - math.cos(ph)) / 2
    return dict(
        body_y=-1.0 * bounce * e, lift=0,
        body=(5 * (1 - bounce) * e, osc(15, f / 2, t) , osc(5, f / 2, t, math.pi / 2) * e),
        head=(osc(8, f, t, .8) * e, osc(14, f / 2, t, 1.0) * e, osc(10, f / 2, t) * e),
        armR=(-70 * e + osc(42, f / 2, t) * e, 0, 22 * e + osc(6, f, t) * e),
        armL=(-70 * e + osc(42, f / 2, t, math.pi) * e, 0, -22 * e - osc(6, f, t, 1) * e),
        legR=(osc(20, f / 2, t, math.pi) * e, 0, 7 * e), legL=(osc(20, f / 2, t) * e, 0, -7 * e),
        cape=osc(18, f / 2, t, .6) * e) | {"body": (5 * (1 - bounce) * e, osc(15, f / 2, t) * e, osc(5, f / 2, t, math.pi / 2) * e)}


def peek(t):
    lean = T([(0, 0), (.55, 1), (2.65, 1), (3.3, 0)], t)
    hy = T([(0, 0), (.55, 0), (1.0, -36), (1.6, -36), (2.1, 32), (2.6, 32), (3.0, 0), (3.3, 0)], t)
    return dict(
        body=(16 * lean, hy * .3, 0), head=(9 * lean + osc(2, 1.3, t) * lean, hy, 0),
        armR=(-98 * lean, 16 * lean, 9 * lean), armL=(18 * lean, 0, -12 * lean),
        legR=(10 * lean, 0, 0), legL=(-9 * lean, 0, 0), lift=0.5 * lean,
        body_y=osc(.18, 1.2, t) * lean, cape=-10 * lean)


def headbang(t):
    dur = 2.8
    e = env(t, dur, .4)
    f = 2.2
    ph = 2 * math.pi * f * t
    return dict(
        head=(10 * e + 30 * math.sin(ph) * e, osc(6, f / 2, t) * e, osc(7, f / 2, t, .5) * e),
        body=(osc(6, f, t, math.pi + .4) * e, osc(7, f / 2, t) * e, 0),
        body_y=-0.6 * (1 - math.cos(ph)) / 2 * e,
        armL=(-55 * e, 0, -9 * e), armR=(-42 * e + osc(14, f * 2, t) * e, 0, 14 * e),
        legR=(osc(9, f / 2, t) * e, 0, 5 * e), legL=(-10 * e, 0, -5 * e), cape=osc(14, f, t, 1) * e)


def kick(t):
    kx = T([(0, 0), (.4, 38), (.7, -85), (.95, -72), (1.5, 6), (2.1, 0)], t)
    lean = T([(0, 0), (.4, 5), (.7, -10), (1.0, -5), (1.6, 0)], t)
    return dict(
        legR=(kx, 0, 4 * window(t, .4, 1.2, .3)), legL=(-4 * window(t, .3, 1.4, .3), 0, 0),
        body=(lean, T([(0, 0), (.7, -9), (1.5, 0)], t), 0),
        head=(T([(0, 0), (.4, 6), (.8, 14), (1.5, 0)], t), T([(0, 0), (.7, 10), (1.5, 0)], t), 0),
        armR=(T([(0, 0), (.5, -35), (.95, 25), (1.6, 0)], t), 0, 14 * window(t, .4, 1.2, .3)),
        armL=(T([(0, 0), (.5, 28), (.95, -30), (1.6, 0)], t), 0, -16 * window(t, .4, 1.2, .3)),
        cape=T([(0, 0), (.4, -14), (.75, 24), (1.4, 0)], t), lift=0)


def stretch(t):
    up = T([(0, 0), (.4, -.1), (1.1, 1), (2.1, 1), (3.0, 0)], t)
    shake = window(t, 1.2, 2.1, .2)
    tremble = osc(1.6, 7.0, t) * shake
    rel = window(t, 2.2, 2.9, .2)
    return dict(
        armR=(-172 * up + tremble + osc(10, 5, t) * rel, 0, 15 * up),
        armL=(-172 * up - tremble - osc(10, 5, t, 1) * rel, 0, -15 * up),
        body=(-10 * up, 0, 0), head=(-24 * up + 10 * rel, 0, 0), lift=0.6 * up,
        legR=(-3 * up, 0, 2 * up), legL=(3 * up, 0, -2 * up), cape=15 * up)


def taunt(t):
    dur = 3.0
    e = env(t, dur, .4)
    f = 1.6
    return dict(
        head=(osc(6, f * 2, t) * e - 4 * e, osc(5, f, t) * e, 15 * e + osc(6, f * 2, t, 1) * e),
        armR=(-96 * e, 0, -14 * e + osc(18, f * 2.5, t) * e), armL=(-96 * e, 0, 14 * e + osc(18, f * 2.5, t, 1.4) * e),
        body=(-4 * e, osc(15, f, t) * e, osc(4, f, t, 1.57) * e),
        legR=(osc(10, f, t) * e, 0, 4 * e), legL=(osc(10, f, t, math.pi) * e, 0, -4 * e),
        body_y=-0.4 * abs(math.sin(math.pi * f * t)) * e, cape=osc(16, f, t, .5) * e)


# ============ emote mới (kiểu bánh xe emote của Essential) ============

def clap(t):
    dur = 2.6
    e = env(t, dur, .4)
    c = (1 + math.sin(2 * math.pi * 3.0 * t - math.pi / 2)) / 2   # 1 = hai tay chạm nhau
    z = 9 - 24 * c
    return dict(
        armR=(-76 * e + 5 * c * e, -10 * e, z * e), armL=(-76 * e + 5 * c * e, 10 * e, -z * e),
        body=(-3 * e, osc(2.5, 1.5, t) * e, osc(2, 1.5, t, 1.0) * e),
        head=(-5 * e + 2.5 * c * e, osc(4, 0.75, t) * e, osc(3, 1.5, t, .5) * e),
        body_y=-0.35 * c * e, legR=(osc(4, 1.5, t) * e, 0, 3 * e), legL=(osc(4, 1.5, t, math.pi) * e, 0, -3 * e),
        cape=osc(7, 3.0, t, .7) * e)


def bow(t):
    b = T([(0, 0), (.3, -.1), (.85, 1), (1.4, 1), (1.95, -.07), (2.4, 0)], t)
    hd = T([(0, 0), (.3, -.1), (.9, 1), (1.4, 1), (2.0, -.1), (2.4, 0)], t)
    return dict(
        body=(38 * b, 0, 0), head=(-12 * b + 14 * hd * b, 0, 0),
        armR=(-38 * b, 8 * b, -9 * b), armL=(22 * b, 0, -6 * b),
        legR=(-3 * b, 0, 1.5 * b), legL=(3 * b, 0, -1.5 * b), body_y=-0.35 * b, cape=-12 * b)


def shrug(t):
    up = T([(0, 0), (.22, -.12), (.5, 1), (1.4, 1), (1.95, 0)], t)
    shake = window(t, .7, 1.55, .2)
    return dict(
        armR=(-26 * up + osc(2, 2.5, t) * shake, 0, 30 * up), armL=(-26 * up + osc(2, 2.5, t, 1) * shake, 0, -30 * up),
        head=(4 * up, osc(10, 1.6, t) * shake, 9 * up), body=(-1.5 * up, 0, 2.5 * up),
        body_y=0.34 * up, legR=(0, 0, 2 * up), legL=(0, 0, -2 * up), cape=7 * up)


def think(t):
    dur = 3.2
    e = env(t, dur, .5)
    look = window(t, 1.4, 2.3, .35)
    return dict(
        armR=(-112 * e + osc(1.5, 0.8, t) * e, -14 * e, -22 * e), armL=(-48 * e, 22 * e, 42 * e),
        head=((8 - 20 * look) * e, 14 * look * e, 10 * e), body=(5 * e, -5 * e, 2.5 * e),
        legR=(0, 0, 3 * e), legL=(-3 * e, 0, -1 * e), body_y=-0.1 * e, cape=3 * e + osc(2, 0.6, t) * e)


# ============ idle sống động (lặp 8 giây) ============
# Lớp 1: thở (4s/nhịp, hít nhanh - thở ra chậm), ngực nâng, vai nhích lên, tay trễ theo thân.
# Lớp 2: chuyển trọng tâm (8s/chu kỳ): hông lệch, thân nghiêng ngược, chân chống/chân thả lỏng.
# Lớp 3: liếc nhìn xung quanh hai lần (không lặp đều đặn) + thở dài một lần ở nửa sau.
# Chỉ dùng hài nguyên và window() (bằng 0 ở hai đầu) để lặp liền mạch; không dùng T().

def idle_live(t):
    P = 8.0
    t = t % P
    w = 2 * math.pi / P
    ws = math.sin(w * t)                      # trọng tâm: -1 chân trái, +1 chân phải
    wc = math.cos(w * t)
    x = 2 * w * t                              # nhịp thở 4s
    br = (1 - math.cos(x)) / 2                 # 0..1
    br = br ** 1.25                            # hít vào nhanh hơn thở ra
    sigh = window(t, 5.0, 7.2, .7)             # một lần thở dài sâu hơn
    br_d = br * (1 + 0.55 * sigh)
    glance1 = window(t, 1.6, 3.1, .5)          # liếc sang phải
    glance2 = window(t, 4.9, 6.3, .55)         # liếc xuống/trái
    s1 = lambda a, f=1, ph=0.0: a * math.sin(f * w * t + ph)
    return dict(
        body_y=0.14 * br_d - 0.03,
        body_x=0.28 * ws,
        body=(-1.2 * br_d + 0.6, s1(1.8, 1, .4) + s1(.6, 2, 1.0), -1.7 * ws + s1(.5, 2, .9)),
        head=(-1.1 * br_d + 1.0 + 4.5 * glance2 - 2.0 * glance1,
              11 * glance1 - 9 * glance2 + s1(2.0, 1, 1.2) + 0.8 * ws,
              1.6 * ws + 3.5 * glance1 - 2.5 * glance2 + s1(.8, 2, .5)),
        armR=(s1(4.2, 2, .6) + s1(1.2, 4, 2.0) + 1.0 * br_d, s1(1.2, 1, 1.0), 4 + 1.3 * br_d + 0.9 * ws),
        armL=(s1(4.2, 2, .6 + math.pi) + s1(1.2, 4, 3.2) + 1.0 * br_d, s1(-1.2, 1, 1.0), -4 - 1.3 * br_d + 0.9 * ws),
        legR=(0.6 + s1(1.2, 1, .8), 0, 1.4 + 1.4 * max(0.0, ws)),
        legL=(0.6 + s1(1.2, 1, .8 + math.pi), 0, -1.4 - 1.4 * max(0.0, -ws)),
        cape=s1(5.0, 2, 1.0) + s1(2.5, 4, .5) + 2.0 * br_d + 1.5 * ws)


# ============ clip khoe cape: giơ một tay lên trời ============

def cape_pose(t):
    # Kịch bản "anh hùng ra mắt" (3.65s):
    #  0-.6   dồn lực: hạ thấp người, xoay lưng, tay phải vung ra sau, cape rủ ra trước
    #  .6-1.1 bùng nổ: bật người lên, tay phải vút thẳng lên trời (overshoot), ngả người ưỡn ngực, cape hất tung
    #  1.1-1.5 tiếp đất: nhún mạnh (impact), cape còn bay theo quán tính (LAG) rồi phồng lên
    #  1.5-2.7 tạo dáng: chân dạng rộng, ngực ưỡn, nắm đấm bên trái chống hông, tay phải run nhẹ, cape phần phật
    #  2.7-3.65 hạ tay chậm, cape dịu dần về nghỉ
    hold = window(t, 1.45, 2.6, .3)
    flutter = osc(9, 1.7, t, .9) * hold + osc(5, 3.1, t, 2.1) * hold
    tremble = osc(1.4, 7.5, t) * hold
    ax = T([(0, 0), (.3, 22), (.6, 52), (.8, -120), (1.0, -196), (1.2, -184), (1.5, -176), (2.6, -178), (3.2, 12), (3.65, 0)], t)
    az = T([(0, 0), (.55, 12), (1.0, 12), (1.5, 7), (2.6, 8), (3.4, 0), (3.65, 0)], t)
    crouch = T([(0, 0), (.5, 1), (.62, .9), (.8, 0), (1.3, 0), (1.42, .85), (1.75, 0), (3.65, 0)], t)
    lift = T([(0, 0), (.8, 0), (1.0, 2.4), (1.22, 0), (3.65, 0)], t)
    stance = T([(0, 0), (1.35, 0), (1.7, 1), (2.6, 1), (3.3, 0), (3.65, 0)], t)
    return dict(
        armR=(ax + tremble, 0, az + osc(1.6, 1.7, t, 1) * hold),
        armL=(T([(0, 0), (.5, 30), (.9, -10), (1.5, 16), (2.7, 16), (3.4, 0), (3.65, 0)], t), 0,
              T([(0, 0), (.5, -8), (1.0, -26), (1.6, -20), (2.7, -20), (3.4, 0), (3.65, 0)], t)),
        body=(T([(0, 0), (.5, 16), (.85, -16), (1.1, -13), (1.45, 5), (1.8, -10), (2.6, -10), (3.3, 0), (3.65, 0)], t),
              T([(0, 0), (.5, -24), (.85, 12), (1.2, 9), (1.8, -8), (2.6, -8), (3.3, 0), (3.65, 0)], t),
              osc(1.8, 1.2, t, .5) * hold),
        head=(T([(0, 0), (.5, 12), (.95, -30), (1.4, -4), (1.75, -24), (2.6, -24), (3.3, 0), (3.65, 0)], t),
              T([(0, 0), (.5, 18), (1.0, -8), (1.8, 14), (2.6, 12), (3.3, 0), (3.65, 0)], t), 6 * hold),
        legR=(T([(0, 0), (.5, -16), (1.0, -26), (1.4, -14), (2.6, -12), (3.4, 0), (3.65, 0)], t), 0, 9 * stance + 6 * crouch),
        legL=(T([(0, 0), (.5, 12), (1.0, 18), (1.4, 8), (2.6, 8), (3.4, 0), (3.65, 0)], t), 0, -9 * stance - 6 * crouch),
        body_y=-1.7 * crouch - 0.2 * osc(1, 1.4, t) * hold, lift=lift,
        cape=T([(0, 0), (.5, 16), (.9, -22), (1.05, -64), (1.3, -52), (1.6, -30), (2.6, -24), (3.3, 0), (3.65, 0)], t) + flutter)


CLIPS = [
    ("FUN_JUMP", "Nhảy lên vui sướng", 1.8, jump),
    ("FUN_SPIN", "Xoay vòng như ballerina", 2.25, spin),
    ("FUN_WAVE", "Vẫy tay chào", 2.6, wave),
    ("FUN_STOMP", "Giậm chân dỗi", 2.8, stomp),
    ("FUN_DANCE", "Nhún nhảy", 3.6, dance),
    ("FUN_PEEK", "Lén nhìn quanh", 3.3, peek),
    ("FUN_HEADBANG", "Lắc đầu rock", 2.8, headbang),
    ("FUN_KICK", "Đá chân vu vơ", 2.1, kick),
    ("FUN_STRETCH", "Vươn vai", 3.0, stretch),
    ("FUN_TAUNT", "Chọc ghẹo", 3.0, taunt),
    ("FUN_CLAP", "Vỗ tay", 2.6, clap),
    ("FUN_BOW", "Cúi chào", 2.4, bow),
    ("FUN_SHRUG", "Nhún vai", 1.95, shrug),
    ("FUN_THINK", "Ngẫm nghĩ", 3.2, think),
]
SPECIAL = [("CAPE_POSE", "Khoe cape: giơ một tay lên trời", 3.65, cape_pose)]


def f(v):
    s = f"{v:.6f}".rstrip("0").rstrip(".")
    return s if "." in s else s + ".0"


def emit(out, const, doc, dur, fn, looping=False):
    ch, tr = (loop_clip(dur, fn) if looping else clip(dur, fn))
    out.append(f"/// {doc}\n#[rustfmt::skip]\npub(crate) static {const}: Clip = Clip {{\n    channels: &[\n")
    for j in ("Body", "Head", "ArmRight", "ArmLeft", "LegRight", "LegLeft", "Cape"):
        rot = ", ".join("[" + ", ".join(f(c) for c in q) + "]" for q in ch[j])
        trs = ", ".join("[" + ", ".join(f(c) for c in v) + "]" for v in tr[j]) if j in tr else ""
        out.append(f"        Channel {{\n            joint: Joint::{j},\n            rotation: &[{rot}],\n            translation: &[{trs}],\n        }},\n")
    out.append("    ],\n};\n")


def main():
    out = ["//! Clip nghịch ngợm + khoe cape. Sinh bởi tools/gen_fun_clips.py — đừng sửa tay.\n",
           "use crate::clip_data::{Channel, Clip, Joint};\n\n"]
    for c in CLIPS + SPECIAL:
        emit(out, *c)
    emit(out, "IDLE_LIVE", "Idle sống động, lặp 8 giây", 8.0, idle_live, looping=True)
    names = ", ".join(c[0] for c in CLIPS)
    out.append("\n/// Pool animation ngẫu nhiên khi đứng chờ (3 gốc + các clip nghịch ngợm)\n")
    out.append(f"pub(crate) static ALL_SUBS: &[Clip] = &[\n    crate::clip_data::CLIP_SUB_1,\n    crate::clip_data::CLIP_SUB_2,\n    crate::clip_data::CLIP_SUB_3,\n    {names},\n];\n")
    OUT.write_text("".join(out), encoding="utf-8")
    print("wrote", OUT, OUT.stat().st_size, "bytes")


if __name__ == "__main__":
    main()
