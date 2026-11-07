#!/usr/bin/env python3
"""Sinh các clip nghịch ngợm (rust/src/clip_fun.rs). Chỉ dùng thư viện chuẩn.

Quy ước (gốc GLTF, đơn vị độ / pixel):
  chi: x âm = vung ra trước, x dương = ra sau; tay phải z+ = dang ra ngoài, tay trái z- = dang ra ngoài
  thân: x dương = nghiêng người ra trước; đầu: x dương = cúi, x âm = ngửa
  body y = xoay quanh trục dọc; body_pos y âm = hạ xuống
Mỗi clip: 30 FPS, bắt đầu/kết thúc ở tư thế nghỉ để fade mượt.
"""
import math
from pathlib import Path

FPS = 30
OUT = Path(__file__).resolve().parent.parent / "rust" / "src" / "clip_fun.rs"
REST_Y = -0.64


def smooth(t):
    t = max(0.0, min(1.0, t))
    return t * t * (3 - 2 * t)


def track(keys, t):
    """keys: [(time, value)] -> nội suy smoothstep."""
    if t <= keys[0][0]:
        return keys[0][1]
    for (t0, v0), (t1, v1) in zip(keys, keys[1:]):
        if t <= t1:
            return v0 + (v1 - v0) * smooth((t - t0) / (t1 - t0))
    return keys[-1][1]


def osc(amp, hz, t, phase=0.0):
    return amp * math.sin(2 * math.pi * hz * t + phase)


def env(t, dur, fade=0.25):
    """Bao: tăng dần ở đầu, giảm dần ở cuối để dao động bắt đầu/kết thúc ở 0."""
    return smooth(t / fade) * smooth((dur - t) / fade)


def quat_euler(x, y, z):
    x, y, z = (math.radians(v) / 2 for v in (x, y, z))
    qx = (math.sin(x), 0, 0, math.cos(x))
    qy = (0, math.sin(y), 0, math.cos(y))
    qz = (0, 0, math.sin(z), math.cos(z))

    def mul(a, b):
        ax, ay, az, aw = a
        bx, by, bz, bw = b
        return (aw * bx + ax * bw + ay * bz - az * by,
                aw * by - ax * bz + ay * bw + az * bx,
                aw * bz + ax * by - ay * bx + az * bw,
                aw * bw - ax * bx - ay * by - az * bz)

    q = mul(mul(qx, qy), qz)
    n = math.sqrt(sum(c * c for c in q))
    return tuple(c / n for c in q)


CAPE_REST = (0.0, 0.996438, -0.084327, 0.0)  # khóa đầu của cape trong CLIP_IDLE


def cape_quat(sway_deg):
    """Quay cape quanh trục x (đung đưa) so với tư thế nghỉ của idle."""
    a = math.radians(sway_deg) / 2
    rx = (math.sin(a), 0.0, 0.0, math.cos(a))
    bx, by, bz, bw = rx
    cx, cy, cz, cw = CAPE_REST
    q = (bw * cx + bx * cw + by * cz - bz * cy,
         bw * cy - bx * cz + by * cw + bz * cx,
         bw * cz + bx * cy - by * cx + bz * cw,
         bw * cw - bx * cx - by * cy - bz * cz)
    n = math.sqrt(sum(c * c for c in q))
    return tuple(c / n for c in q)


def clip(name, dur, fn):
    """fn(t) -> dict: body, head, armR, armL, legR, legL (euler độ), cape (độ x),
    body_y, lift (nâng cả người, px), armR_p/armL_p/legR_p/legL_p (+ offset pivot)."""
    n = int(round(dur * FPS)) + 1
    ch = {k: [] for k in ("Body", "Head", "ArmRight", "ArmLeft", "LegRight", "LegLeft", "Cape")}
    tr = {k: [] for k in ("Body", "ArmRight", "ArmLeft", "LegRight", "LegLeft")}
    for i in range(n):
        t = i / FPS
        p = fn(t)
        z3 = (0, 0, 0)
        ch["Body"].append(quat_euler(*p.get("body", z3)))
        ch["Head"].append(quat_euler(*p.get("head", z3)))
        ch["ArmRight"].append(quat_euler(*p.get("armR", z3)))
        ch["ArmLeft"].append(quat_euler(*p.get("armL", z3)))
        ch["LegRight"].append(quat_euler(*p.get("legR", z3)))
        ch["LegLeft"].append(quat_euler(*p.get("legL", z3)))
        ch["Cape"].append(cape_quat(p.get("cape", 0.0)))
        lift = p.get("lift", 0.0)
        by = p.get("body_y", 0.0)
        tr["Body"].append((p.get("body_x", 0.0), REST_Y + lift + by, 0.0))
        tr["ArmRight"].append((5.0, 10.0 + lift, 0.0))
        tr["ArmLeft"].append((-5.0, 10.0 + lift, 0.0))
        tr["LegRight"].append((1.9, max(0.0, lift + p.get("legR_up", 0.0)), 0.0))
        tr["LegLeft"].append((-1.9, max(0.0, lift + p.get("legL_up", 0.0)), 0.0))
    return name, ch, tr


# ---------- các clip ----------

def jump(t):  # nhảy lên vui sướng
    d = 1.3
    crouch = track([(0, 0), (.25, 1), (.4, 0)], t)
    air = track([(0, 0), (.4, 0), (.6, 1), (.8, 0), (1.3, 0)], t)
    lift = 4.5 * math.sin(math.pi * smooth(air)) if .4 <= t <= .8 else 0.0
    up = track([(0, 0), (.4, 0), (.6, 1), (.8, 0), (1.3, 0)], t)
    return dict(
        body_y=-1.5 * crouch + 0, lift=lift,
        armR=(-150 * up + 20 * crouch, 0, 25 * up), armL=(-150 * up + 20 * crouch, 0, -25 * up),
        legR=(-35 * crouch + 25 * up, 0, 0), legL=(-35 * crouch + 25 * up, 0, 0),
        body=(8 * crouch, 0, 0), head=(-10 * up, 0, 0),
        cape=-15 * up + 10 * crouch, legR_up=0, legL_up=0)


def spin(t):  # xoay vòng vòng như balerina
    d = 1.6
    a = track([(0, 0), (.3, 0), (1.3, 360), (1.6, 360)], t)
    arms = track([(0, 0), (.3, 1), (1.3, 1), (1.6, 0)], t)
    return dict(body=(0, a, 0), head=(0, 0, 0),
                armR=(0, 0, 80 * arms), armL=(0, 0, -80 * arms),
                legR=(0, 0, 0), legL=(0, 0, 0), cape=-25 * arms)


def wave(t):  # vẫy tay chào
    d = 2.2
    up = track([(0, 0), (.35, 1), (d - .4, 1), (d, 0)], t)
    w = osc(28, 2.4, t) * up
    return dict(armR=(-150 * up, 0, 30 * up + w), head=(0, -12 * up, 6 * up),
                body=(0, -6 * up, 0), cape=5 * up)


def stomp(t):  # giậm chân dỗi
    d = 2.2
    e = env(t, d, .2)
    s = math.sin(2 * math.pi * 2.5 * t)
    return dict(
        body=(0, 0, osc(4, 2.5, t) * e), head=(8 * e, 0, osc(6, 2.5, t, .5) * e),
        armR=(-20 * e, 0, -8 * e), armL=(-20 * e, 0, 8 * e),
        legR=(-40 * max(0, s) * e, 0, 0), legL=(-40 * max(0, -s) * e, 0, 0),
        legR_up=1.5 * max(0, s) * e, legL_up=1.5 * max(0, -s) * e,
        body_y=-1.0 * max(0, -abs(s)) * e, cape=osc(10, 2.5, t) * e)


def dance(t):  # lắc lư nhún nhảy
    d = 3.0
    e = env(t, d, .3)
    s = lambda a, ph=0: osc(a, 2.0, t, ph) * e
    bounce = abs(math.sin(math.pi * 2.0 * t)) * e
    return dict(
        body=(0, s(15), s(6)), head=(s(6, 1.2), s(-12), s(10)),
        armR=(-60 * e + s(35, 0), 0, 25 * e + s(10)), armL=(-60 * e + s(35, math.pi), 0, -25 * e + s(10)),
        legR=(s(18, math.pi), 0, 0), legL=(s(18), 0, 0),
        lift=1.2 * bounce, cape=s(18))


def peek(t):  # lén lén nhìn quanh, tay che mắt
    d = 3.0
    lean = track([(0, 0), (.4, 1), (d - .4, 1), (d, 0)], t)
    look = osc(30, .9, t) * lean
    return dict(
        body=(14 * lean, 0, 0), head=(10 * lean, look, 0),
        armR=(-95 * lean, 15 * lean, 8 * lean), armL=(15 * lean, 0, -10 * lean),
        legR=(-10 * lean, 0, 0), legL=(8 * lean, 0, 0), cape=-8 * lean)


def headbang(t):  # lắc đầu rock
    d = 2.4
    e = env(t, d, .25)
    nod = osc(35, 3.0, t) * e
    return dict(
        head=(nod + 8 * e, 0, osc(8, 1.5, t) * e), body=(osc(6, 3.0, t, math.pi) * e, 0, 0),
        armR=(-70 * e + osc(10, 3.0, t) * e, 0, 12 * e), armL=(-100 * e, 0, -8 * e),
        legR=(osc(8, 3.0, t) * e, 0, 0), legL=(-8 * e, 0, 0), cape=osc(12, 3.0, t) * e)


def kick(t):  # đá chân vu vơ
    d = 1.8
    k = track([(0, 0), (.35, -.2), (.6, 1), (.85, 0.6), (d, 0)], t)
    lean = track([(0, 0), (.4, 1), (1.2, 0.4), (d, 0)], t)
    return dict(
        legR=(-75 * max(0, k) + 10 * min(0, k), 0, 0), legL=(5 * lean, 0, 0),
        body=(-6 * lean, 0, 0), head=(6 * lean, 10 * lean, 0),
        armL=(30 * lean, 0, -20 * lean), armR=(-35 * lean, 0, 15 * lean),
        legR_up=2.0 * max(0, k), cape=12 * lean)


def stretch(t):  # vươn vai ngáp
    d = 2.6
    s = track([(0, 0), (.6, 1), (1.8, 1), (d, 0)], t)
    shake = osc(1.2, 8.0, t) * s * smooth((1.8 - t) / .2) if t > .6 else 0
    return dict(
        armR=(-170 * s + shake, 0, 14 * s), armL=(-170 * s - shake, 0, -14 * s),
        body=(-8 * s, 0, 0), head=(-22 * s, 0, 0),
        legR=(0, 0, 0), legL=(0, 0, 0), lift=0.5 * s, cape=14 * s)


def taunt(t):  # le lưỡi chọc ghẹo: lắc mông, vẫy tay bên đầu
    d = 2.6
    e = env(t, d, .3)
    return dict(
        head=(osc(6, 3.0, t) * e, 0, 14 * e + osc(6, 3.0, t) * e),
        armR=(-40 * e, 0, 60 * e), armL=(-40 * e, 0, -60 * e),
        body=(0, osc(14, 3.0, t) * e, osc(4, 3.0, t, 1.5) * e),
        legR=(osc(10, 3.0, t) * e, 0, 0), legL=(osc(10, 3.0, t, math.pi) * e, 0, 0),
        cape=osc(16, 3.0, t) * e)


CLIPS = [
    ("FUN_JUMP", "Nhảy lên vui sướng", 1.3, jump),
    ("FUN_SPIN", "Xoay vòng", 1.6, spin),
    ("FUN_WAVE", "Vẫy tay chào", 2.2, wave),
    ("FUN_STOMP", "Giậm chân dỗi", 2.2, stomp),
    ("FUN_DANCE", "Nhún nhảy", 3.0, dance),
    ("FUN_PEEK", "Lén nhìn quanh", 3.0, peek),
    ("FUN_HEADBANG", "Lắc đầu rock", 2.4, headbang),
    ("FUN_KICK", "Đá chân vu vơ", 1.8, kick),
    ("FUN_STRETCH", "Vươn vai", 2.6, stretch),
    ("FUN_TAUNT", "Chọc ghẹo", 2.6, taunt),
]


def f(v):
    s = f"{v:.6f}".rstrip("0").rstrip(".")
    return s if "." in s else s + ".0"


def main():
    out = ["//! Clip nghịch ngợm bổ sung. Sinh bởi tools/gen_fun_clips.py — đừng sửa tay.\n",
           "use crate::clip_data::{Channel, Clip, Joint};\n\n"]
    joints = [("Body", "Body"), ("Head", "Head"), ("ArmRight", "ArmRight"), ("ArmLeft", "ArmLeft"),
              ("LegRight", "LegRight"), ("LegLeft", "LegLeft"), ("Cape", "Cape")]
    for const, doc, dur, fn in CLIPS:
        _, ch, tr = clip(const, dur, fn)
        out.append(f"/// {doc}\n#[rustfmt::skip]\npub(crate) static {const}: Clip = Clip {{\n    channels: &[\n")
        for j, jn in joints:
            rot = ", ".join("[" + ", ".join(f(c) for c in q) + "]" for q in ch[j])
            trs = ""
            if j in tr:
                trs = ", ".join("[" + ", ".join(f(c) for c in v) + "]" for v in tr[j])
            out.append(f"        Channel {{\n            joint: Joint::{jn},\n            rotation: &[{rot}],\n            translation: &[{trs}],\n        }},\n")
        out.append("    ],\n};\n")
    names = ", ".join(c[0] for c in CLIPS)
    out.append("\n/// Toàn bộ pool animation ngẫu nhiên khi đứng chờ (3 gốc + các clip nghịch ngợm)\n")
    out.append(f"pub(crate) static ALL_SUBS: &[Clip] = &[\n    crate::clip_data::CLIP_SUB_1,\n    crate::clip_data::CLIP_SUB_2,\n    crate::clip_data::CLIP_SUB_3,\n    {names},\n];\n")
    OUT.write_text("".join(out), encoding="utf-8")
    print("wrote", OUT, OUT.stat().st_size, "bytes")


if __name__ == "__main__":
    main()
