#!/usr/bin/env python3
"""Builds every Glitch icon and store graphic from the logo, store/brand/glitch-icon.png.

The logo is a white ring broken by a tilted gold tile on navy. This script writes:
  - app/src/main/res/mipmap-*/ic_launcher_foreground.png  (adaptive-icon foreground, per density)
  - app/src/main/res/mipmap-*/ic_launcher_monochrome.png  (Android 13+ themed icon)
  - @color/ic_launcher_background in values/colors.xml     (the logo's navy)
  - store/brand/glitch-mark.png       (the ring and tile on transparent, for other graphics)
  - store/graphics/icon-512.png       (Play Store hi-res icon, 512x512, 32-bit PNG)
  - store/graphics/feature-graphic.png (Play Store feature graphic, 1024x500, 24-bit PNG)

Requires Python 3 and Pillow. Run from the repo root after changing the logo:

    python3 store/tools/make_graphics.py
"""
import re
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[2]
LOGO = ROOT / "store/brand/glitch-icon.png"
MARK = ROOT / "store/brand/glitch-mark.png"
RES = ROOT / "app/src/main/res"
OUT = ROOT / "store/graphics"

# Brand colors, sampled from the logo.
BG = "#010F3E"
WHITE = "#FFFFFF"
ACCENT = "#EBB64E"
TEXT_SOFT = "#C9D1E6"

# Adaptive icons are 108dp; launchers may crop to any shape inside the central 66dp circle.
# The mark's farthest pixel from the ring centre is kept within this radius (dp).
SAFE_RADIUS_DP = 31
DENSITIES = {"mdpi": 1, "hdpi": 1.5, "xhdpi": 2, "xxhdpi": 3, "xxxhdpi": 4}

FONT_BOLD = "/usr/share/fonts/truetype/noto/NotoSans-Bold.ttf"
FONT_REG = "/usr/share/fonts/truetype/noto/NotoSans-Regular.ttf"


def rgb(hex_color):
    return tuple(int(hex_color[i:i + 2], 16) for i in (1, 3, 5))


def extract_mark(logo):
    """Separate the ring and tile from the navy background (anti-aliased edges kept)."""
    bg = rgb(BG)
    src = logo.convert("RGB")
    out = Image.new("RGBA", src.size)
    px, dst = src.load(), out.load()
    for y in range(src.height):
        for x in range(src.width):
            p = px[x, y]
            # Alpha from how far the pixel moved away from navy, relative to the closer
            # of the two foreground colors (gold is nearer to navy than white is).
            dist = max(abs(c - b) for c, b in zip(p, bg))
            a = min(1.0, dist / 150)
            if a < 0.04:
                dst[x, y] = (0, 0, 0, 0)
                continue
            color = tuple(max(0, min(255, round(b + (c - b) / a))) for c, b in zip(p, bg))
            dst[x, y] = (*color, round(a * 255))
    return out


def ring_centre(mark):
    """Centre of the white ring (bounding box of near-white pixels)."""
    white = mark.convert("RGB").point(lambda v: 255 if v > 200 else 0).convert("L")
    alpha = mark.getchannel("A").point(lambda v: 255 if v > 200 else 0)
    box = Image.composite(white, Image.new("L", mark.size), alpha).getbbox()
    return (box[0] + box[2]) / 2, (box[1] + box[3]) / 2


def max_radius(mark, cx, cy):
    a = mark.getchannel("A").load()
    r = 0
    for y in range(0, mark.height, 2):
        for x in range(0, mark.width, 2):
            if a[x, y] > 20:
                r = max(r, ((x - cx) ** 2 + (y - cy) ** 2) ** 0.5)
    return r


def foreground_canvas(mark, cx, cy, radius):
    """Square canvas centred on the ring, sized so the mark sits inside the safe zone."""
    side = round(radius * 108 / SAFE_RADIUS_DP)
    canvas = Image.new("RGBA", (side, side))
    canvas.alpha_composite(mark, (round(side / 2 - cx), round(side / 2 - cy)))
    return canvas


def write_launcher(canvas):
    mono = Image.new("RGBA", canvas.size, (255, 255, 255, 0))
    mono.putalpha(canvas.getchannel("A"))
    for name, scale in DENSITIES.items():
        folder = RES / f"mipmap-{name}"
        folder.mkdir(parents=True, exist_ok=True)
        size = round(108 * scale)
        canvas.resize((size, size), Image.LANCZOS).save(folder / "ic_launcher_foreground.png", optimize=True)
        mono.resize((size, size), Image.LANCZOS).save(folder / "ic_launcher_monochrome.png", optimize=True)
    colors = RES / "values/colors.xml"
    text = re.sub(
        r'(<color name="ic_launcher_background">)#[0-9A-Fa-f]+(</color>)',
        rf"\g<1>#FF{BG[1:]}\g<2>",
        colors.read_text(),
    )
    colors.write_text(text)


def write_store(logo, mark, cx, cy, radius):
    OUT.mkdir(parents=True, exist_ok=True)
    logo.convert("RGBA").resize((512, 512), Image.LANCZOS).save(OUT / "icon-512.png", optimize=True)

    # Tight crop of the mark around the ring centre, reused by make_screenshots.py.
    r = round(radius) + 4
    mark.crop((round(cx - r), round(cy - r), round(cx + r), round(cy + r))).save(MARK, optimize=True)

    w, h, ss = 1024, 500, 2
    img = Image.new("RGBA", (w * ss, h * ss), BG)
    m = Image.open(MARK).resize((300 * ss, 300 * ss), Image.LANCZOS)
    img.alpha_composite(m, (90 * ss, 100 * ss))
    d = ImageDraw.Draw(img)
    bold = lambda s: ImageFont.truetype(FONT_BOLD, s * ss)
    reg = lambda s: ImageFont.truetype(FONT_REG, s * ss)
    x = 470
    lines = [
        (205, "Glitch", bold(72), WHITE),
        (268, "Glitch the autopilot.", reg(34), TEXT_SOFT),
        (312, "Look, then tap.", reg(34), TEXT_SOFT),
        (372, "No permissions · No internet · No tracking", bold(22), ACCENT),
    ]
    for y, text, font, fill in lines:
        d.text((x * ss, y * ss), text, font=font, fill=fill, anchor="ls")
        right = d.textbbox((x * ss, 0), text, font=font)[2] / ss
        assert right < w - 40, f"text overflows feature graphic: {text!r} ends at {right}"
    img.convert("RGB").resize((w, h), Image.LANCZOS).save(OUT / "feature-graphic.png", optimize=True)


if __name__ == "__main__":
    logo = Image.open(LOGO)
    mark = extract_mark(logo)
    cx, cy = ring_centre(mark)
    radius = max_radius(mark, cx, cy)
    write_launcher(foreground_canvas(mark, cx, cy, radius))
    write_store(logo, mark, cx, cy, radius)
    print(f"Ring centre ({cx:.0f}, {cy:.0f}), mark radius {radius:.0f}px. Wrote launcher icons and store graphics.")
