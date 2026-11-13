# -*- coding: utf-8 -*-
"""
ClassTool 表盘页 · 界面原型渲染脚本
输出:
  design/prototype/dial-prototype.png    多状态总览
  design/prototype/dial-annotated.png    元素标注说明
坐标系: 每块表盘按 454 x 454 dp 设计, 内部以 S 倍超采样后缩小, 保证边缘平滑。
"""
import math
import os

from PIL import Image, ImageDraw, ImageFilter, ImageFont

# ----------------------------------------------------------------------------
# 字体
# ----------------------------------------------------------------------------
FONT_DIR = r"C:\Windows\Fonts"
FONT_FILES = {"reg": "Deng.ttf", "bold": "Dengb.ttf", "light": "Dengl.ttf"}
_font_cache = {}


def font(size, weight="reg"):
    key = (round(size, 2), weight)
    if key not in _font_cache:
        path = os.path.join(FONT_DIR, FONT_FILES.get(weight, FONT_FILES["reg"]))
        _font_cache[key] = ImageFont.truetype(path, int(round(size)))
    return _font_cache[key]


# ----------------------------------------------------------------------------
# 颜色
# ----------------------------------------------------------------------------
def rgba(hexstr, a=255):
    h = hexstr.lstrip("#")
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), a)


def mix(c1, c2, t):
    return tuple(int(round(c1[i] * (1 - t) + c2[i] * t)) for i in range(3))


def with_a(c, a):
    return (c[0], c[1], c[2], int(round(a)))


def rgb_to_hsl(c):
    r, g, b = [v / 255.0 for v in c[:3]]
    mx, mn = max(r, g, b), min(r, g, b)
    l = (mx + mn) / 2
    if mx == mn:
        return 0.0, 0.0, l
    d = mx - mn
    s = d / (2 - mx - mn) if l > 0.5 else d / (mx + mn)
    if mx == r:
        h = ((g - b) / d + (6 if g < b else 0)) / 6
    elif mx == g:
        h = ((b - r) / d + 2) / 6
    else:
        h = ((r - g) / d + 4) / 6
    return h, s, l


def hsl_to_rgb(h, s, l):
    def hue2rgb(p, q, t):
        if t < 0:
            t += 1
        if t > 1:
            t -= 1
        if t < 1 / 6:
            return p + (q - p) * 6 * t
        if t < 1 / 2:
            return q
        if t < 2 / 3:
            return p + (q - p) * (2 / 3 - t) * 6
        return p

    if s == 0:
        return tuple(int(round(v * 255)) for v in (l, l, l))
    q = l * (1 + s) if l < 0.5 else l + s - l * s
    p = 2 * l - q
    return tuple(
        int(round(hue2rgb(p, q, t) * 255)) for t in (h + 1 / 3, h, h - 1 / 3)
    )


def adjust(c, dh=0.0, ds=1.0, dl=0.0, target_l=None):
    h, s, l = rgb_to_hsl(c)
    l = target_l if target_l is not None else max(0.0, min(1.0, l + dl))
    return hsl_to_rgb((h + dh) % 1.0, max(0.0, min(1.0, s * ds)), l)


# ----------------------------------------------------------------------------
# 画布
# ----------------------------------------------------------------------------
class CV:
    def __init__(self, w, h, s=1):
        self.w, self.h, self.s = w, h, s
        self.img = Image.new("RGBA", (int(w * s), int(h * s)), (0, 0, 0, 0))
        self.d = ImageDraw.Draw(self.img)

    def _p(self, v):
        return v * self.s

    def _box(self, box):
        return [int(round(v * self.s)) for v in box]

    def rrect(self, box, radius, fill=None, outline=None, width=0.0):
        self.d.rounded_rectangle(
            self._box(box),
            radius=int(round(radius * self.s)),
            fill=fill,
            outline=outline,
            width=max(1, int(round(width * self.s))) if outline else 0,
        )

    def ellipse(self, box, fill=None, outline=None, width=0.0):
        self.d.ellipse(
            self._box(box),
            fill=fill,
            outline=outline,
            width=max(1, int(round(width * self.s))) if outline else 0,
        )

    def dot(self, cx, cy, r, fill):
        self.ellipse((cx - r, cy - r, cx + r, cy + r), fill=fill)

    def line(self, pts, fill, width=1.0):
        self.d.line(
            [(int(round(x * self.s)), int(round(y * self.s))) for x, y in pts],
            fill=fill,
            width=max(1, int(round(width * self.s))),
            joint="curve",
        )

    def text(self, x, y, s, size, fill, weight="reg", anchor="mm"):
        self.d.text(
            (self._p(x), self._p(y)),
            s,
            font=font(size * self.s, weight),
            fill=fill,
            anchor=anchor,
        )

    def text_len(self, s, size, weight="reg"):
        return font(size * self.s, weight).getlength(s) / self.s

    def composite(self, layer):
        self.img.alpha_composite(layer)

    def blur(self, radius):
        self.img = self.img.filter(ImageFilter.GaussianBlur(radius * self.s))
        self.d = ImageDraw.Draw(self.img)


def arc_points(cx, cy, R, a0, a1, step=1.0):
    n = max(2, int(abs(math.radians(a1 - a0)) * R / step) + 1)
    out = []
    for i in range(n + 1):
        a = math.radians(a0 + (a1 - a0) * i / n)
        out.append((cx + R * math.cos(a), cy + R * math.sin(a)))
    return out


def draw_arc(cv, cx, cy, R, a0, a1, width, color, alpha=1.0):
    """沿圆弧绘制圆头粗线, 颜色为 RGB 三元组。"""
    if a1 == a0:
        return
    layer = Image.new("RGBA", cv.img.size, (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)
    r = width / 2
    for x, y in arc_points(cx, cy, R, a0, a1):
        d.ellipse(
            [
                (x - r) * cv.s,
                (y - r) * cv.s,
                (x + r) * cv.s,
                (y + r) * cv.s,
            ],
            fill=with_a(color, 255),
        )
    if alpha < 1.0:
        layer.putalpha(layer.getchannel("A").point(lambda v: int(v * alpha)))
    cv.composite(layer)


def draw_glow(cv, points, radius, color, alpha, blur):
    layer = Image.new("RGBA", cv.img.size, (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)
    for x, y in points:
        d.ellipse(
            [
                (x - radius) * cv.s,
                (y - radius) * cv.s,
                (x + radius) * cv.s,
                (y + radius) * cv.s,
            ],
            fill=with_a(color, 255),
        )
    layer = layer.filter(ImageFilter.GaussianBlur(blur * cv.s))
    layer.putalpha(layer.getchannel("A").point(lambda v: int(v * alpha)))
    cv.composite(layer)


def polar(cx, cy, R, deg):
    a = math.radians(deg)
    return cx + R * math.cos(a), cy + R * math.sin(a)


def hhmm(minutes):
    total = int(round(minutes))
    return "%02d:%02d" % (total // 60, total % 60)


# ----------------------------------------------------------------------------
# 主题
# ----------------------------------------------------------------------------
BASE_DARK = (0x17, 0x19, 0x1F)
BASE_LIGHT = (0xF7, 0xF8, 0xFC)
TEXT_DARK = (0xE9, 0xEA, 0xF0)
TEXT_LIGHT = (0x1B, 0x1B, 0x1F)
VARIANT_DARK = (0xAE, 0xB2, 0xBE)
VARIANT_LIGHT = (0x5A, 0x5D, 0x66)
URGENT = (0xFF, 0x6B, 0x60)
SEED = (0x9B, 0xD7, 0xFF)


class Theme:
    def __init__(self, accent, light=False):
        self.accent = accent
        self.light = light
        base = BASE_LIGHT if light else BASE_DARK
        self.bg = mix(base, accent, 0.055 if light else 0.085)
        self.text = TEXT_LIGHT if light else TEXT_DARK
        self.variant = VARIANT_LIGHT if light else VARIANT_DARK
        self.track = mix(self.bg, self.text, 0.16 if light else 0.10)
        self.card = mix(self.bg, accent, 0.20 if light else 0.19)
        self.card_border = with_a(mix(self.card, accent, 0.55), 150 if not light else 170)
        self.dot = mix(self.accent, (255, 255, 255), 0.15) if not light else adjust(accent, dl=-0.04)
        self.value = self.text

    def ring(self, c):
        if not self.light:
            return c
        return adjust(c, ds=1.02, target_l=0.52)


# ----------------------------------------------------------------------------
# 表盘绘制
# ----------------------------------------------------------------------------
GAP_DEG = 2.4



BREAK_CAP_MIN = 12.0


def build_segments(courses):
    """把一天的课表变成环上的分段: 课程按时长分弧, 课间压缩(最长 12 分钟等价)。"""
    items = []
    for i, (name, s, e, color) in enumerate(courses):
        items.append(dict(kind="course", name=name, s=s, e=e, color=color, dur=float(e - s)))
        if i < len(courses) - 1:
            nxt = courses[i + 1]
            gap = nxt[1] - e
            if gap > 0:
                items.append(dict(kind="break", name="课间", s=e, e=nxt[1],
                                  color=None, dur=min(float(gap), BREAK_CAP_MIN)))
    total = sum(it["dur"] for it in items) or 1.0
    acc = 0.0
    for it in items:
        it["a0"] = -90 + 360 * acc / total
        acc += it["dur"]
        it["a1"] = -90 + 360 * acc / total
    return items


def draw_bezel_ticks(cv, theme, geom):
    """装饰刻度, 纯表盘质感, 不表示具体时间。"""
    cx, cy, R, stroke = geom["cx"], geom["cy"], geom["R"], geom["stroke"]
    r0 = R + stroke * 0.55 + 5
    ma, mi = (58, 26) if theme.light else (80, 40)
    for a in range(0, 360, 6):
        major = a % 30 == 0
        r1 = r0 + (10.0 if major else 5.0)
        cv.line([polar(cx, cy, r0, a), polar(cx, cy, r1, a)],
                with_a(theme.variant, ma if major else mi),
                2.0 if major else 1.1)


def _pct(it, now_min, a0, a1):
    span = max(1e-6, it["e"] - it["s"])
    t = (now_min - it["s"]) / span
    t = min(1.0, max(0.0, t))
    return a0 + (a1 - a0) * t


def draw_day_ring(cv, theme, geom, courses, now_min, urgent=False):
    cx, cy, R, stroke = geom["cx"], geom["cy"], geom["R"], geom["stroke"]
    draw_bezel_ticks(cv, theme, geom)
    draw_arc(cv, cx, cy, R, -90, 270, stroke, theme.track, 1.0)

    items = build_segments(courses)
    futures = [it for it in items if it["kind"] == "course" and it["s"] > now_min]
    next_start = futures[0]["s"] if futures else None

    marker = None
    for it in items:
        if it["kind"] == "break":
            if it["s"] <= now_min < it["e"]:
                a_now = _pct(it, now_min, it["a0"], it["a1"])
                marker = ("break", it, a_now, theme.variant)
            continue

        rc = theme.ring(it["color"])
        a0 = it["a0"] + GAP_DEG / 2
        a1 = it["a1"] - GAP_DEG / 2
        if now_min >= it["e"]:
            draw_arc(cv, cx, cy, R, a0, a1, stroke, rc, 0.24)
        elif now_min <= it["s"]:
            nxt = 0.66 if it["s"] == next_start else 0.46
            draw_arc(cv, cx, cy, R, a0, a1, stroke, rc, nxt)
        else:
            a_now = _pct(it, now_min, a0, a1)
            rem = URGENT if urgent else rc
            draw_arc(cv, cx, cy, R, a_now, a1, stroke, rem, 0.85 if urgent else 0.28)
            draw_arc(cv, cx, cy, R, a0, a_now, stroke, rc, 1.0)
            marker = ("course", it, a_now, rem)

    if marker:
        kind, it, a_now, col = marker
        px, py = polar(cx, cy, R, a_now)
        draw_glow(cv, [(px, py)], stroke * 1.5, col, 0.34, 9)
        cv.dot(px, py, stroke * 0.82, with_a(theme.bg, 255))
        cv.dot(px, py, stroke * 0.46, with_a(mix(col, (255, 255, 255), 0.12), 255))
    return items


def draw_card(cv, theme, box, radius, title, sub, right_label=None,
              right_value=None, value_color=None, title_size=21, sub_size=13,
              value_size=26, label_size=10.5, dot_r=4.6, pad_x=20,
              title_dy=-12, sub_dy=14, label_dy=-14, value_dy=12,
              accent=None, dot=True):
    cv.rrect(box, radius, fill=with_a(theme.card, 235),
             outline=theme.card_border, width=1.2)
    x0, y0, x1, y1 = box
    cy = (y0 + y1) / 2
    tx = x0 + pad_x + dot_r * 3.5
    if dot:
        cv.dot(x0 + pad_x + 2, cy, dot_r, with_a(accent or theme.accent, 255))
    cv.text(tx, cy + title_dy, title, title_size, with_a(theme.text, 255), "bold", "lm")
    cv.text(tx, cy + sub_dy, sub, sub_size, with_a(theme.variant, 255), "reg", "lm")
    if right_value:
        cv.text(x1 - pad_x, cy + label_dy, right_label, label_size,
                with_a(theme.variant, 255), "reg", "rm")
        cv.text(x1 - pad_x, cy + value_dy, right_value, value_size,
                with_a(value_color or theme.value, 255), "bold", "rm")


def draw_footer(cv, theme, y, text, dot_color=None, size=13, cx=227):
    """居中一行提示 + 可选左侧小圆点。"""
    tw = cv.text_len(text, size, "reg")
    total = tw + (12 if dot_color else 0)
    x = cx - total / 2
    if dot_color:
        cv.dot(x + 4, y, 3.2, with_a(dot_color, 255))
        x += 12
    cv.text(x, y, text, size, with_a(theme.variant, 235), "reg", "lm")


def draw_pager(cv, theme, y=430):
    for i, x in enumerate((227 - 15, 227, 227 + 15)):
        active = i == 1
        cv.dot(x, y, 3.2 if not active else 4.0,
               with_a(theme.accent if active else mix(theme.variant, theme.bg, 0.55),
                     255 if active else 170))


def face_bg(cv, shape, color, inset=0.0):
    if shape == "round":
        cv.ellipse((4, 4, 450, 450), fill=with_a(color, 255))
    else:
        cv.rrect((4, 4, 450, 450), 56, fill=with_a(color, 255))


# 今天的课程（用于原型示意）
COURSES = [
    ("高等数学", 8 * 60, 8 * 60 + 40, (0x42, 0xA5, 0xF5)),
    ("数据结构", 8 * 60 + 50, 9 * 60 + 30, (0xAB, 0x47, 0xBC)),
    ("英语", 10 * 60, 10 * 60 + 40, (0x66, 0xBB, 0x6A)),
    ("大学物理", 14 * 60, 14 * 60 + 40, (0xFF, 0xA7, 0x26)),
    ("体育", 15 * 60, 15 * 60 + 40, (0x26, 0xC6, 0xDA)),
]


def render_state(state, light=False, shape="round", s=4):
    cv = CV(454, 454, s)

    if state == "active":
        course, accent = COURSES[1], COURSES[1][3]
        now_min = 9 * 60 + 6 + 19 / 60
        theme = Theme(accent, light)
        title, sub = course[0], "08:50 - 09:30"
        right_label, right_value = "剩余", "23:41"
        footer = "下一节 10:00 · 英语"
        footer_dot = COURSES[2][3]
        current_course, courses = course, COURSES
    elif state == "break":
        accent = COURSES[2][3]
        now_min = 9 * 60 + 38 + 22 / 60
        theme = Theme(accent, light)
        title, sub = "课间休息", "下一节 10:00 · 英语"
        right_label, right_value = "距上课", "21:38"
        footer = "今日 5 节 · 已完成 2 节"
        footer_dot = None
        current_course, courses = None, COURSES
    elif state == "prep":
        accent = COURSES[1][3]
        now_min = 8 * 60 + 47 + 20 / 60
        theme = Theme(accent, light)
        title, sub = COURSES[1][0], "08:50 - 09:30"
        right_label, right_value = "即将开始", "02:40"
        footer = "今日 5 节 · 已完成 1 节"
        footer_dot = None
        current_course, courses = None, COURSES
    elif state == "empty":
        accent = SEED
        now_min = 16 * 60 + 20
        theme = Theme(SEED, light)
        title, sub = "今天没有课程", "周六 · 无课程安排"
        right_label, right_value = None, None
        footer = "明天 08:00 · 高等数学"
        footer_dot = COURSES[0][3]
        current_course, courses = None, []
    else:  # urgent
        accent = URGENT
        now_min = 9 * 60 + 28 + 19 / 60
        theme = Theme(accent, light)
        title, sub = "数据结构", "08:50 - 09:30"
        right_label, right_value = "即将下课", "01:41"
        footer = "下一节 10:00 · 英语"
        footer_dot = COURSES[2][3]
        current_course, courses = COURSES[1], COURSES

    urgent = state == "urgent"

    if shape == "round":
        face_bg(cv, "round", theme.bg)
        geom = {"cx": 227, "cy": 227, "R": 186, "stroke": 12}
        draw_day_ring(cv, theme, geom, courses, now_min, urgent=urgent)
        draw_card(cv, theme, (104, 102, 350, 188), 24, title, sub,
                  right_label, right_value,
                  value_color=(0xFF, 0xB4, 0xA9) if urgent else theme.value,
                  title_size=18 if state == "empty" else 21,
                  accent=accent)
        cv.text(227, 252, hhmm(now_min), 92, with_a(theme.text, 255), "bold", "mm")
        cv.text(227, 318, "10月31日 周六", 19, with_a(theme.variant, 230), "reg", "mm")
        draw_footer(cv, theme, 360, footer, footer_dot)
        draw_pager(cv, theme)
    else:
        # 方形屏: 不放环, 用「卡片 + 超大时间」铺满, 元素与圆形页一致
        face_bg(cv, "square", theme.bg)
        draw_card(cv, theme, (40, 58, 414, 154), 26, title, sub,
                  right_label, right_value,
                  value_color=(0xFF, 0xB4, 0xA9) if urgent else theme.value,
                  title_size=26 if state != "empty" else 23,
                  sub_size=14, value_size=34, label_size=11,
                  dot_r=6, pad_x=24,
                  title_dy=-15, sub_dy=17, label_dy=-19, value_dy=15,
                  accent=accent)
        cv.text(227, 250, hhmm(now_min), 126, with_a(theme.text, 255), "bold", "mm")
        cv.text(227, 340, "10月31日 周六", 21, with_a(theme.variant, 232), "reg", "mm")
        draw_footer(cv, theme, 394, footer, footer_dot, size=15)
        draw_pager(cv, theme, y=430)

    # 约束在屏幕形状内
    mask = Image.new("L", cv.img.size, 0)
    md = ImageDraw.Draw(mask)
    if shape == "round":
        md.ellipse([4 * s, 4 * s, 450 * s, 450 * s], fill=255)
    else:
        md.rounded_rectangle([4 * s, 4 * s, 450 * s, 450 * s], radius=56 * s, fill=255)
    cv.img.putalpha(Image.composite(cv.img.getchannel("A"), Image.new("L", cv.img.size, 0), mask))
    return cv.img.resize((908, 908), Image.LANCZOS)


# ----------------------------------------------------------------------------
# 总览板
# ----------------------------------------------------------------------------
def _board_bg(cv, B, board_w, board_h):
    cv.d.rectangle([0, 0, board_w * B, board_h * B], fill=rgba("#0A0B0E"))
    g = Image.new("RGBA", cv.img.size, (0, 0, 0, 0))
    gd = ImageDraw.Draw(g)
    gd.ellipse([-400 * B, -700 * B, 1100 * B, 300 * B], fill=(0x18, 0x24, 0x3A, 255))
    g = g.filter(ImageFilter.GaussianBlur(220 * B))
    g.putalpha(g.getchannel("A").point(lambda v: int(v * 0.5)))
    cv.composite(g)


def _paste_frame(cv, state, light, shape, x, y, cap, desc, B):
    face = render_state(state, light=light, shape=shape)
    face = face.resize((454 * B, 454 * B), Image.LANCZOS)

    # 投影
    sh = Image.new("RGBA", cv.img.size, (0, 0, 0, 0))
    sd = ImageDraw.Draw(sh)
    if shape == "round":
        sd.ellipse([(x + 6) * B, (y + 12) * B, (x + 460) * B, (y + 466) * B],
                   fill=(0, 0, 0, 255))
    else:
        sd.rounded_rectangle([(x + 6) * B, (y + 12) * B, (x + 460) * B, (y + 466) * B],
                             radius=56 * B, fill=(0, 0, 0, 255))
    sh = sh.filter(ImageFilter.GaussianBlur(26 * B))
    sh.putalpha(sh.getchannel("A").point(lambda v: int(v * 0.55)))
    cv.composite(sh)

    cv.img.alpha_composite(face, (int(x * B), int(y * B)))

    # 表壳细边
    if shape == "round":
        cv.ellipse((x + 4, y + 4, x + 450, y + 450), outline=rgba("#31343D", 255),
                   width=1.2)
    else:
        cv.rrect((x + 4, y + 4, x + 450, y + 450), 56, outline=rgba("#31343D", 255),
                 width=1.2)

    cv.text(x + 227, y + 490, cap, 18, rgba("#E7E9F0"), "bold", "mm")
    cv.text(x + 227, y + 518, desc, 13.5, rgba("#8D92A0"), "reg", "mm")


def build_round_board(out_path, B=2):
    board_w, board_h = 1570, 1428
    cv = CV(board_w, board_h, B)
    _board_bg(cv, B, board_w, board_h)

    cv.text(60, 66, "圆形表盘 · 界面原型", 34, rgba("#F2F3F7"), "bold", "lm")
    cv.text(60, 108, "ClassTool（Wear OS）· 主界面左滑第二页 · 454 × 454 dp · 示意数据",
            15, rgba("#8D92A0"), "reg", "lm")

    specs = [
        ("active", False, "上课中", "课程色环 + 剩余时间，背景随课程颜色微染"),
        ("break", False, "课间休息", "无进行中课程时，卡片改为预告下一节"),
        ("prep", False, "预备铃", "上课前 3 分钟：卡片切到「即将开始」并倒数"),
        ("urgent", False, "即将下课", "紧急事件剩余 ≤ 10 分钟时跟随动态主题转红"),
        ("empty", False, "今天没有课程", "色环收起为底轨，提示明天第一节"),
        ("active", True, "日间模式", "跟随 App 的浅色主题与课程色亮度"),
    ]

    x0, y0, gapx, gapy = 60, 168, 44, 40
    row_h = 454 + 104
    for i, (state, light, cap, desc) in enumerate(specs):
        col, row = i % 3, i // 3
        x = x0 + col * (454 + gapx)
        y = y0 + row * (row_h + gapy)
        _paste_frame(cv, state, light, "round", x, y, cap, desc, B)

    cv.text(60, board_h - 42,
            "交互：当前课程  ←左滑  ｜  表盘（第 2 页）  ←左滑  ｜  更多      ·      全部由 Compose Canvas 绘制，无图片资源",
            13, rgba("#6E7380"), "reg", "lm")

    cv.img.resize((board_w, board_h), Image.LANCZOS).convert("RGB").save(out_path, quality=95)
    print("saved", out_path)


def build_square_board(out_path, B=2):
    board_w, board_h = 1570, 872
    cv = CV(board_w, board_h, B)
    _board_bg(cv, B, board_w, board_h)

    cv.text(60, 66, "方形表盘 · 不加环，卡片更宽、时间放大到 126 sp", 32,
            rgba("#F2F3F7"), "bold", "lm")
    cv.text(60, 108, "元素与圆形页完全一致（课程色卡片 / 大号时间 / 日期 / 下一节 / 页码点），只换了排布",
            15, rgba("#8D92A0"), "reg", "lm")

    specs = [
        ("active", False, "上课中", "整宽卡片贴顶，时间占满屏幕中部"),
        ("break", False, "课间休息", "同一个卡片位切换成下一节预告"),
        ("active", True, "日间模式", "浅色主题下同样铺满，不依赖色环"),
    ]

    x0, y0, gapx = 60, 176, 44
    for i, (state, light, cap, desc) in enumerate(specs):
        x = x0 + i * (454 + gapx)
        _paste_frame(cv, state, light, "square", x, y0, cap, desc, B)

    cv.text(60, board_h - 42,
            "方形屏不画色环：剩余时长 / 进度由卡片右侧的大号剩余时间承担，其余元素与圆形页一一对应。",
            13, rgba("#6E7380"), "reg", "lm")

    cv.img.resize((board_w, board_h), Image.LANCZOS).convert("RGB").save(out_path, quality=95)
    print("saved", out_path)


# ----------------------------------------------------------------------------
# 标注图
# ----------------------------------------------------------------------------
def build_annotated(out_path):
    Wd, Hd = 1500, 1120
    cv = CV(Wd, Hd, 1)
    cv.d.rectangle([0, 0, Wd, Hd], fill=rgba("#0A0B0E"))

    cv.text(56, 62, "表盘页 · 元素说明", 30, rgba("#F2F3F7"), "bold", "lm")
    cv.text(56, 100, "对应总览图中的「上课中」状态 · 454 × 454 圆形表盘", 14.5,
            rgba("#8D92A0"), "reg", "lm")

    # 表盘放大显示
    face = render_state("active", s=6).resize((760, 760), Image.LANCZOS)
    fx, fy = 56, 168
    sh = Image.new("RGBA", (Wd, Hd), (0, 0, 0, 0))
    ImageDraw.Draw(sh).ellipse([fx + 4, fy + 12, fx + 768, fy + 776], fill=(0, 0, 0, 255))
    sh = sh.filter(ImageFilter.GaussianBlur(26))
    sh.putalpha(sh.getchannel("A").point(lambda v: int(v * 0.6)))
    cv.composite(sh)
    cv.img.alpha_composite(face, (fx, fy))
    cv.ellipse((fx + 3, fy + 3, fx + 757, fy + 757), outline=rgba("#31343D", 255), width=1.4)

    k = 760 / 454.0

    def pt(x, y):
        return fx + x * k, fy + y * k

    # 标记点(454 坐标系)与右侧说明
    marks = [
        ((392, 108), "①", "今日课程环",
         "当天课程按时长铺成弧段（长课间会被压缩）：已上过的变暗、\n下一节略亮、当前课高亮，小圆点是「现在」的位置。"),
        ((352, 132), "②", "当前课程卡片",
         "课程色底 + 课程名 + 时间段，右上角是「剩余 mm:ss」，\n每秒刷新；无课时自动切换为课间/下一节预告。"),
        ((332, 224), "③", "大号时间",
         "HH:mm 每分钟更新。此页会隐藏顶部状态栏时间，避免重复。"),
        ((304, 318), "④", "日期 · 星期",
         "跟随 App 的调休设置，显示「生效后的星期」。"),
        ((322, 360), "⑤", "下一节预告",
         "一眼看到接下来要上的课；课间状态下这里改为今日进度。"),
        ((270, 430), "⑥", "页码指示",
         "共 3 页：当前课程 → 表盘 → 更多，左右滑动切换。"),
    ]

    legend_x = 880
    legend_y = 210
    step = 138
    for i, ((mx, my), num, title, body) in enumerate(marks):
        px, py = pt(mx, my)
        ly = legend_y + i * step
        # 引线
        cv.line([(px, py), ((px + legend_x - 58) / 2 + 10, py),
                 (legend_x - 52, ly - 10)], rgba("#3C4150"), 1.2)
        cv.dot(px, py, 3.0, rgba("#FFCF5C"))
        cv.dot(legend_x - 52, ly - 10, 2.2, rgba("#FFCF5C"))

        # 编号胶囊
        cv.rrect((legend_x - 40, ly - 26, legend_x + 4, ly + 8), 12,
                 fill=rgba("#FFCF5C"))
        cv.text(legend_x - 18, ly - 9, num, 17, rgba("#15161A"), "bold")
        cv.text(legend_x + 20, ly - 22, title, 19, rgba("#EDEFF5"), "bold", "lm")
        cv.text(legend_x + 20, ly - 4, body, 14, rgba("#9AA0AE"), "reg", "la")

    cv.line([(56, 1000), (Wd - 56, 1000)], rgba("#22242B"), 1.0)
    cv.text(56, 1034,
            "规则：无课 → 色环变暗；紧急事件剩余 ≤ 10 分钟 → 沿用现有动态主题转红；方形屏 / 日间模式自动适配。",
            13.5, rgba("#7E8391"), "reg", "lm")
    cv.text(56, 1064,
            "数据全部复用现有 ScheduleDataStore + eventsOnDate / findCurrentEventOnDate，不新增持久化。",
            13.5, rgba("#7E8391"), "reg", "lm")

    cv.img.convert("RGB").save(out_path, quality=95)
    print("saved", out_path)


if __name__ == "__main__":
    here = os.path.dirname(os.path.abspath(__file__))
    build_round_board(os.path.join(here, "dial-prototype.png"))
    build_square_board(os.path.join(here, "dial-square.png"))
    build_annotated(os.path.join(here, "dial-annotated.png"))
