#!/usr/bin/env python3
"""Play Store marketing screenshots for Shuffle Menu.

Builds 1080x1920 (9:16) PNGs in store/graphics/marketing/ from the raw phone
screenshots in store/screenshots/: a short headline on the brand background with
the real screenshot inside a phone frame, plus a "how to use" and a privacy card.

Requires Python 3 and Pillow. Run from the repo root:

    python3 store/tools/make_screenshots.py
"""
from pathlib import Path

from PIL import Image, ImageColor, ImageDraw, ImageFilter, ImageFont

ROOT = Path(__file__).resolve().parents[2]
SHOTS = ROOT / "store/screenshots"
OUT = ROOT / "store/graphics/marketing"

# Brand colors, shared with make_graphics.py.
BG = "#2E3A59"
BG_DEEP = "#222B44"
TILE = "#FFFFFF"
ACCENT = "#FFC857"
TEXT_SOFT = "#C9D1E6"
BEZEL = "#11151F"

W, H = 1080, 1920
FONT_BOLD = "/usr/share/fonts/truetype/noto/NotoSans-Bold.ttf"
FONT_REG = "/usr/share/fonts/truetype/noto/NotoSans-Regular.ttf"


def bold(size):
    return ImageFont.truetype(FONT_BOLD, size)


def reg(size):
    return ImageFont.truetype(FONT_REG, size)


def canvas():
    """Vertical gradient from BG to BG_DEEP."""
    top = ImageColor.getrgb(BG)
    bottom = ImageColor.getrgb(BG_DEEP)
    strip = Image.new("RGB", (1, H))
    for y in range(H):
        t = y / (H - 1)
        strip.putpixel((0, y), tuple(round(a + (b - a) * t) for a, b in zip(top, bottom)))
    return strip.resize((W, H))


def centered(d, y, text, font, fill):
    d.text((W / 2, y), text, font=font, fill=fill, anchor="ms")


def headline(d, lines, sub=None):
    y = 190
    for line in lines:
        centered(d, y, line, bold(76), TILE)
        y += 96
    if sub:
        centered(d, y + 10, sub, reg(40), TEXT_SOFT)


def phone(img, shot_path, top=470, width=700):
    """Paste a screenshot inside a rounded phone frame; it may run off the bottom."""
    shot = Image.open(shot_path).convert("RGB")
    h = round(shot.height * width / shot.width)
    shot = shot.resize((width, h), Image.LANCZOS)
    pad, r = 22, 70
    x = (W - width) // 2
    fw, fh = width + 2 * pad, h + 2 * pad

    shadow = Image.new("L", (W, H), 0)
    ImageDraw.Draw(shadow).rounded_rectangle(
        (x - pad, top - pad + 24, x - pad + fw, top - pad + fh + 24), r + pad, fill=150)
    shadow = shadow.filter(ImageFilter.GaussianBlur(36))
    img.paste(Image.new("RGB", (W, H), "#0B0E16"), (0, 0), shadow)

    d = ImageDraw.Draw(img)
    d.rounded_rectangle((x - pad, top - pad, x - pad + fw, top - pad + fh), r + pad, fill=BEZEL)
    mask = Image.new("L", shot.size, 0)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, width - 1, h - 1), r, fill=255)
    img.paste(shot, (x, top), mask)
    return x, top, width / Image.open(shot_path).width


def highlight(img, box):
    """Gold ring around a region (screen coordinates)."""
    d = ImageDraw.Draw(img)
    d.rounded_rectangle(box, 40, outline=ACCENT, width=10)


def tile_grid(d, cx, cy, size, gap, accent_tilt=True):
    """The brand 3x3 tile mark, centered on (cx, cy)."""
    total = 3 * size + 2 * gap
    x0, y0 = cx - total / 2, cy - total / 2
    for row in range(3):
        for col in range(3):
            x, y = x0 + col * (size + gap), y0 + row * (size + gap)
            if row == 1 and col == 1 and accent_tilt:
                continue
            d.rounded_rectangle((x, y, x + size, y + size), size * 0.25, fill=TILE)
    return x0 + size + gap, y0 + size + gap


def accent_tile(img, x, y, size, angle=25):
    t = Image.new("RGBA", (int(size * 1.6), int(size * 1.6)), (0, 0, 0, 0))
    o = (t.width - size) / 2
    ImageDraw.Draw(t).rounded_rectangle((o, o, o + size, o + size), size * 0.25, fill=ACCENT)
    t = t.rotate(angle, resample=Image.BICUBIC)
    img.paste(t, (int(x - o), int(y - o)), t)


def shot_hero():
    img = canvas()
    headline(ImageDraw.Draw(img), ["Your apps,", "in a new order"], "every time you open the menu")
    phone(img, SHOTS / "phone-1-light.png")
    return img


def shot_tap():
    img = canvas()
    headline(ImageDraw.Draw(img), ["Look, then tap"], "Tap any app to open it as usual")
    x, top, scale = phone(img, SHOTS / "phone-1-light.png")
    # Gmail tile in the raw 1080x2400 screenshot: about (600..740, 860..1060).
    highlight(img, (x + 590 * scale, top + 850 * scale, x + 750 * scale, top + 1065 * scale))
    return img


def shot_dark():
    img = canvas()
    headline(ImageDraw.Draw(img), ["Light or dark,", "clean and simple"], "Follows your phone's theme and colors")
    phone(img, SHOTS / "phone-2-dark.png")
    return img


def step_card(img, d, y, number, title, body):
    x0, x1 = 90, W - 90
    d.rounded_rectangle((x0, y, x1, y + 250), 44, fill="#3A4769")
    d.ellipse((x0 + 50, y + 65, x0 + 170, y + 185), fill=ACCENT)
    d.text((x0 + 110, y + 125), str(number), font=bold(68), fill=BG, anchor="mm")
    d.text((x0 + 220, y + 105), title, font=bold(52), fill=TILE, anchor="ls")
    d.text((x0 + 220, y + 175), body, font=reg(36), fill=TEXT_SOFT, anchor="ls")


def shot_how():
    img = canvas()
    d = ImageDraw.Draw(img)
    headline(d, ["Simple to use"], "Three steps, nothing to set up")
    ax, ay = tile_grid(d, W / 2, 620, 92, 26)
    accent_tile(img, ax, ay, 92)
    step_card(img, d, 880, 1, "Open Shuffle Menu", "from your home screen")
    step_card(img, d, 1170, 2, "Find your app", "the order is new each time")
    step_card(img, d, 1460, 3, "Tap to open it", "come back for a fresh order")
    return img


def shot_privacy():
    img = canvas()
    d = ImageDraw.Draw(img)
    headline(d, ["Private by design"], "Everything stays on your phone")
    ax, ay = tile_grid(d, W / 2, 620, 92, 26)
    accent_tile(img, ax, ay, 92)
    items = [
        ("No permissions", "nothing to allow or deny"),
        ("No internet", "the app never goes online"),
        ("No tracking or ads", "no accounts, no analytics"),
    ]
    for i, (title, body) in enumerate(items):
        y = 880 + i * 290
        d.rounded_rectangle((90, y, W - 90, y + 250), 44, fill="#3A4769")
        cx, cy = 200, y + 125
        d.ellipse((cx - 60, cy - 60, cx + 60, cy + 60), fill=ACCENT)
        d.line((cx - 28, cy + 2, cx - 6, cy + 26, cx + 30, cy - 24), fill=BG, width=14, joint="curve")
        d.text((310, y + 105), title, font=bold(52), fill=TILE, anchor="ls")
        d.text((310, y + 175), body, font=reg(36), fill=TEXT_SOFT, anchor="ls")
    return img


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    for name, make in [
        ("01-hero", shot_hero),
        ("02-how-to-use", shot_how),
        ("03-tap", shot_tap),
        ("04-dark", shot_dark),
        ("05-privacy", shot_privacy),
    ]:
        path = OUT / f"{name}.png"
        make().save(path, optimize=True)
        print(path.relative_to(ROOT))


if __name__ == "__main__":
    main()
