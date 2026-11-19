"""Two alternative JalyClient logos (same 188x37 size as the main one): Frost and Gingerbread."""
import os, random, sys
sys.path.insert(0, os.path.dirname(__file__))
from gen_art import GLYPHS, TEX, hexc, mix, save, SNOW  # noqa
from PIL import Image


def mask_for(word="JalyClient", scale=3):
    gw = 5 * scale + scale
    W, H = len(word) * gw + 8, 9 * scale + 10
    mask = [[False] * H for _ in range(W)]
    for i, ch in enumerate(word):
        ox, oy = 4 + i * gw, 6
        for gy, row in enumerate(GLYPHS[ch]):
            for gx, c in enumerate(row):
                if c == "#":
                    for yy in range(scale):
                        for xx in range(scale):
                            mask[ox + gx * scale + xx][oy + gy * scale + yy] = True
    return mask, W, H


def outline(px, m, W, H, col, width=1):
    for y in range(H):
        for x in range(W):
            if m[x][y]:
                continue
            if any(0 <= x + dx < W and 0 <= y + dy < H and m[x + dx][y + dy] for dx in range(-width, width + 1) for dy in range(-width, width + 1)):
                px[x, y] = col


def frost():
    m, W, H = mask_for()
    img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    px = img.load()
    outline(px, m, W, H, hexc("#0B1F3D"))
    rnd = random.Random(3)
    for y in range(H):
        for x in range(W):
            if m[x][y]:
                t = (y - 6) / 27
                c = mix(hexc("#FFFFFF"), hexc("#4C9BE8"), max(0, min(1, t)))
                if (x * 3 + y * 5) % 11 == 0:
                    c = hexc("#E8F6FF")
                px[x, y] = c
    # icicles under the letters
    for x in range(W):
        bottom = max((y for y in range(H) if m[x][y]), default=None)
        if bottom is not None and rnd.random() < 0.28:
            ln = rnd.randint(1, 3)
            for k in range(1, ln + 1):
                if bottom + k < H:
                    px[x, bottom + k] = hexc("#BFE3FF", 255 - k * 50)
    # sparkles
    for _ in range(14):
        x, y = rnd.randint(2, W - 3), rnd.randint(1, H - 2)
        if not m[x][y]:
            continue
        px[x, y] = (255, 255, 255, 255)
    # snow caps
    for x in range(W):
        for y in range(H):
            if m[x][y]:
                px[x, y] = SNOW
                break
    save(img, f"{TEX}/gui/logo_frost.png")


def gingerbread():
    m, W, H = mask_for()
    img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    px = img.load()
    outline(px, m, W, H, hexc("#3B1E0C"))
    for y in range(H):
        for x in range(W):
            if m[x][y]:
                t = (y - 6) / 27
                px[x, y] = mix(hexc("#D08A4A"), hexc("#8C4E22"), max(0, min(1, t)))
    # wavy white icing along the top edge and dots of icing
    for x in range(W):
        tops = [y for y in range(H) if m[x][y] and (y == 0 or not m[x][y - 1])]
        for top in tops:
            depth = 2 + (1 if (x // 2) % 3 == 0 else 0)
            for k in range(depth):
                if top + k < H and m[x][top + k]:
                    px[x, top + k] = SNOW
    rnd = random.Random(9)
    for _ in range(40):
        x, y = rnd.randint(0, W - 1), rnd.randint(12, H - 6)
        if m[x][y] and m[x][min(H - 1, y + 1)] and px[x, y][0] < 240:
            px[x, y] = rnd.choice([hexc("#E0384F"), hexc("#3FB950"), hexc("#FFD94A")])
    save(img, f"{TEX}/gui/logo_ginger.png")


frost()
gingerbread()
print("logos ok")
