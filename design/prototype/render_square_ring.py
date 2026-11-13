# -*- coding: utf-8 -*-
"""
方形环（可调圆角）可行性验证渲染
输出: design/prototype/dial-square-ring.png
"""
import math
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from PIL import Image, ImageDraw, ImageFilter  # noqa: E402

from render_prototype import (  # noqa: E402
    CV, Theme, mix, with_a, rgba, draw_card, draw_footer, draw_pager,
    face_bg, COURSES, hhmm, URGENT, SEED, TEXT_DARK, TEXT_LIGHT,
)

# ----------------------------------------------------------------------------
# 圆角矩形环的路径参数化（按弧长）
# ----------------------------------------------------------------------------
class RRPath:
    """从 12 点方向开始、顺时针的圆角矩形路径，按弧长取点。"""

    def __init__(self, x0, y0, x1, y1, r):
        self.r = max(0.0, min(r, (x1 - x0) / 2.0, (y1 - y0) / 2.0))
        r = self.r
        cx = (x0 + x1) / 2.0
        self.segs = [
            ("line", (cx, y0), (x1 - r, y0), (x1 - r) - cx),
            ("arc", (x1 - r, y0 + r), r, -90.0, 0.0, math.pi * r / 2),
            ("line", (x1, y0 + r), (x1, y1 - r), (y1 - r) - (y0 + r)),
            ("arc", (x1 - r, y1 - r), r, 0.0, 90.0, math.pi * r / 2),
            ("line", (x1 - r, y1), (x0 + r, y1), (x1 - r) - (x0 + r)),
            ("arc", (x0 + r, y1 - r), r, 90.0, 180.0, math.pi * r / 2),
            ("line", (x0, y1 - r), (x0, y0 + r), (y1 - r) - (y0 + r)),
            ("arc", (x0 + r, y0 + r), r, 180.0, 270.0, math.pi * r / 2),
            ("line", (x0 + r, y0), (cx, y0), cx - (x0 + r)),
        ]
        self.total = sum(s[-1] for s in self.segs)

    def point(self, s):
        s = s % self.total
        for seg in self.segs:
            ln = seg[-1]
            if ln <= 0:
                continue
            if s <= ln:
                if seg[0] == "line":
                    _, p0, p1, _ = seg
                    t = s / ln
                    return (p0[0] + (p1[0] - p0[0]) * t, p0[1] + (p1[1] - p0[1]) * t)
                _, c, r, a0, a1, _ = seg
                a = math.radians(a0 + (a1 - a0) * (s / ln))
                return (c[0] + r * math.cos(a), c[1] + r * math.sin(a))
            s -= ln
        return self.point(self.total * 0.999)

    def tangent(self, s, eps=0.6):
        x1, y1 = self.point(s)
        x2, y2 = self.point(s + eps)
        dx, dy = x2 - x1, y2 - y1
        ln = math.hypot(dx, dy) or 1.0
        return dx / ln, dy / ln


def stroke_path(cv, path, s0, s1, width, color, alpha=1.0, step=0.45):
    """沿路径描粗线（圆头 = 端点多画一个圆点）。"""
    length = s1 - s0
    n = max(2, int(length / step) + 1)
    layer = Image.new("RGBA", cv.img.size, (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)
    r = width / 2.0
    for i in range(n + 1):
        x, y = path.point(s0 + length * i / n)
        d.ellipse([(x - r) * cv.s, (y - r) * cv.s,
                   (x + r) * cv.s, (y + r) * cv.s], fill=with_a(color, 255))
    if alpha < 1.0:
        layer.putalpha(layer.getchannel("A").point(lambda v: int(v * alpha)))
    cv.composite(layer)


def draw_ticks_on(cv, path, theme, ring_w):
    ma, mi = (58, 26) if theme.light else (80, 40)
    s = 0.0
    step = 10.0
    idx = 0
    while s < path.total:
        major = idx % 5 == 0
        px, py = path.point(s)
        tx, ty = path.tangent(s)
        nx, ny = ty, -tx
        r0 = ring_w / 2 + 5
        r1 = r0 + (10.0 if major else 5.0)
        cv.line([(px + nx * r0, py + ny * r0), (px + nx * r1, py + ny * r1)],
                with_a(theme.variant, ma if major else mi),
                2.0 if major else 1.1)
        s += step
        idx += 1


# ----------------------------------------------------------------------------
# 课程 → 路径上的分段（按弧长比例，课间压缩）
# ----------------------------------------------------------------------------
def segments(courses, break_cap=12.0):
    items = []
    for i, (name, s, e, color) in enumerate(courses):
        items.append(dict(kind="course", name=name, s=s, e=e, color=color, dur=float(e - s)))
        if i < len(courses) - 1:
            gap = courses[i + 1][1] - e
            if gap > 0:
                items.append(dict(kind="break", s=e, e=courses[i + 1][1],
                                  dur=min(float(gap), break_cap)))
    total = sum(it["dur"] for it in items) or 1.0
    acc = 0.0
    for it in items:
        it["f0"] = acc / total
        acc += it["dur"]
        it["f1"] = acc / total
    return items


def frac_in(it, now_min):
    span = max(1e-6, it["e"] - it["s"])
    t = min(1.0, max(0.0, (now_min - it["s"]) / span))
    return it["f0"] + (it["f1"] - it["f0"]) * t


# ----------------------------------------------------------------------------
# 一帧：方形环
# ----------------------------------------------------------------------------
def render_sqring(radius_dp, light=False, s=4, now_min=None):
    accent = COURSES[1][3]
    now_min = 9 * 60 + 6 + 19 / 60 if now_min is None else now_min
    theme = Theme(accent, light)
    cv = CV(454, 454, s)
    face_bg(cv, "square", theme.bg)

    ring_inset = 26.0
    ring_w = 12.0
    rect = (ring_inset, ring_inset, 454 - ring_inset, 454 - ring_inset)
    path = RRPath(rect[0], rect[1], rect[2], rect[3], radius_dp)

    draw_ticks_on(cv, path, theme, ring_w)
    stroke_path(cv, path, 0, path.total, ring_w, theme.track)

    items = segments(COURSES)
    gap = 7.0  # dp
    futures = [it for it in items if it["kind"] == "course" and it["s"] > now_min]
    next_start = futures[0]["s"] if futures else None

    marker = None
    for it in items:
        if it["kind"] == "break":
            if it["s"] <= now_min < it["e"]:
                marker = ("break", frac_in(it, now_min))
            continue
        rc = theme.ring(it["color"])
        s0 = it["f0"] * path.total + gap / 2
        s1 = it["f1"] * path.total - gap / 2
        if now_min >= it["e"]:
            stroke_path(cv, path, s0, s1, ring_w, rc, 0.24)
        elif now_min <= it["s"]:
            a = 0.66 if it["s"] == next_start else 0.46
            stroke_path(cv, path, s0, s1, ring_w, rc, a)
        else:
            sn = frac_in(it, now_min) * path.total
            stroke_path(cv, path, sn, s1, ring_w, rc, 0.28)
            stroke_path(cv, path, s0, sn, ring_w, rc, 1.0)
            marker = ("course", frac_in(it, now_min), rc)

    if marker:
        kind = marker[0]
        frac = marker[1]
        col = marker[2] if kind == "course" else theme.variant
        px, py = path.point(frac * path.total)
        from render_prototype import draw_glow
        draw_glow(cv, [(px, py)], ring_w * 1.5, col, 0.34, 9)
        cv.dot(px, py, ring_w * 0.82, with_a(theme.bg, 255))
        cv.dot(px, py, ring_w * 0.46, with_a(mix(col, (255, 255, 255), 0.12), 255))

    # 内容内缩：圆角越大，内接区域越小
    ci = max(38 + 0.293 * radius_dp, 46.0)
    card = (ci, ci, 454 - ci, ci + 92)
    tfont = 130 - 0.25 * radius_dp
    draw_card(cv, theme, card, 22, COURSES[1][0], "08:50 - 09:30", "剩余", "23:41",
              title_size=26, sub_size=14, value_size=34, label_size=11,
              dot_r=6, pad_x=24, title_dy=-15, sub_dy=17,
              label_dy=-19, value_dy=15, accent=accent)

    card_bottom = card[3]
    tcy = card_bottom + 22 + tfont * 0.52
    cv.text(227, tcy, hhmm(now_min), tfont, with_a(theme.text, 255), "bold", "mm")
    date_y = tcy + tfont * 0.52 + 24
    cv.text(227, date_y, "10月31日 周六", 21, with_a(theme.variant, 232), "reg", "mm")
    draw_footer(cv, theme, date_y + 46, "下一节 10:00 · 英语", COURSES[2][3], size=15)
    draw_pager(cv, theme, y=408)

    mask = Image.new("L", cv.img.size, 0)
    ImageDraw.Draw(mask).rounded_rectangle(
        [4 * s, 4 * s, 450 * s, 450 * s], radius=56 * s, fill=255)
    cv.img.putalpha(Image.composite(cv.img.getchannel("A"),
                                    Image.new("L", cv.img.size, 0), mask))
    return cv.img.resize((908, 908), Image.LANCZOS)


# ----------------------------------------------------------------------------
# 出图
# ----------------------------------------------------------------------------
def build(out_path, B=2):
    board_w, board_h = 1570, 900
    cv = CV(board_w, board_h, B)
    cv.d.rectangle([0, 0, board_w * B, board_h * B], fill=rgba("#0A0B0E"))

    g = Image.new("RGBA", cv.img.size, (0, 0, 0, 0))
    ImageDraw.Draw(g).ellipse([-400 * B, -700 * B, 1100 * B, 300 * B],
                              fill=(0x18, 0x24, 0x3A, 255))
    g = g.filter(ImageFilter.GaussianBlur(220 * B))
    g.putalpha(g.getchannel("A").point(lambda v: int(v * 0.5)))
    cv.composite(g)

    cv.text(60, 66, "方形环 + 可调圆角（示意 32 / 72 / 112 dp）", 32,
            rgba("#F2F3F7"), "bold", "lm")
    cv.text(60, 108, "同一个圆角参数：拉到 227 dp（边长一半）就退化成圆形表盘，圆环和方环共用一套绘制代码",
            15, rgba("#8D92A0"), "reg", "lm")

    specs = [(32, "圆角 32 dp", "方中带圆，卡片几乎贴满宽度"),
             (72, "圆角 72 dp", "圆角明显，但与环形刻度不冲突"),
             (112, "圆角 112 dp", "接近胶囊，内部内容自动内缩")]
    x0, y0, gapx = 60, 176, 44
    for i, (rad, cap, desc) in enumerate(specs):
        x = x0 + i * (454 + gapx)
        face = render_sqring(rad)
        face = face.resize((454 * B, 454 * B), Image.LANCZOS)
        sh = Image.new("RGBA", cv.img.size, (0, 0, 0, 0))
        ImageDraw.Draw(sh).rounded_rectangle(
            [(x + 6) * B, (y0 + 12) * B, (x + 460) * B, (y0 + 466) * B],
            radius=56 * B, fill=(0, 0, 0, 255))
        sh = sh.filter(ImageFilter.GaussianBlur(26 * B))
        sh.putalpha(sh.getchannel("A").point(lambda v: int(v * 0.55)))
        cv.composite(sh)
        cv.img.alpha_composite(face, (int(x * B), int(y0 * B)))
        cv.rrect((x + 4, y0 + 4, x + 450, y0 + 450), 56,
                 outline=rgba("#31343D", 255), width=1.2)
        cv.text(x + 227, y0 + 490, cap, 18, rgba("#E7E9F0"), "bold", "mm")
        cv.text(x + 227, y0 + 518, desc, 13.5, rgba("#8D92A0"), "reg", "mm")

    cv.text(60, board_h - 42,
            "圆角值只是 Path 的一个参数（RoundRect 的 CornerRadius）；弧段用 PathMeasure 切成子路径描边，圆环就是圆角=半径的特例。",
            13, rgba("#6E7380"), "reg", "lm")

    cv.img.resize((board_w, board_h), Image.LANCZOS).convert("RGB").save(out_path, quality=95)
    print("saved", out_path)


if __name__ == "__main__":
    here = os.path.dirname(os.path.abspath(__file__))
    build(os.path.join(here, "dial-square-ring.png"))
