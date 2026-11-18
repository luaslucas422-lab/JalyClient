"""Generates every texture JalyClient ships (all original pixel art).

Run: python3 tools/gen_art.py   (from the repo root)
Outputs into mod/src/main/resources, launcher/ui and installer/.
Also writes HatModels.java so the 3D hats match their textures.
"""
import math
import os
import random
import json
from PIL import Image, ImageDraw, ImageFilter

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(ROOT, "mod/src/main/resources")
ASSETS = os.path.join(RES, "assets/jalyclient")
TEX = os.path.join(ASSETS, "textures")
JAVA = os.path.join(ROOT, "mod/src/main/java/fr/darkerdawn/jaly/cosmetic")


def hexc(h, a=255):
    h = h.lstrip("#")
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), a)


def shade(c, f):
    return tuple(max(0, min(255, int(v * f))) for v in c[:3]) + (c[3] if len(c) > 3 else 255,)


def mix(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3)) + (255,)


def save(img, path):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    img.save(path)


PINE = hexc("#192C0C")
GOLD = hexc("#C99E02")
GOLD_L = hexc("#FDE68A")
RED = hexc("#B11A33")
RED_L = hexc("#E0384F")
SNOW = hexc("#F6F4F7")
ICE = hexc("#9BC3E7")
GREEN = hexc("#2E7D32")
GREEN_D = hexc("#1B5E20")
BROWN = hexc("#6B4423")

# ---------------------------------------------------------------- hats
# cuboid: (x, y, z, w, h, d, color)   y up is negative (head top is y=-8)
HATS = {
    "santa": [
        (-4.5, -9.5, -4.5, 9, 2, 9, "#F4F4F4"),
        (-4.0, -12.5, -4.0, 8, 3, 8, "#C8102E"),
        (-3.0, -15.0, -2.5, 6, 3, 6, "#C8102E"),
        (-2.0, -16.5, -0.5, 4, 2, 4, "#B00C26"),
        (-1.0, -16.0, 3.0, 2, 2, 3, "#B00C26"),
        (-1.5, -15.5, 5.5, 3, 3, 3, "#FFFFFF"),
    ],
    "antlers": [
        (-4.0, -16.0, 0.0, 1, 8, 1, "#7A4E2A"),
        (-7.0, -12.0, 0.0, 3, 1, 1, "#7A4E2A"),
        (-7.0, -15.0, 0.0, 1, 3, 1, "#8C5B32"),
        (-4.0, -14.0, -2.0, 1, 1, 2, "#8C5B32"),
        (3.0, -16.0, 0.0, 1, 8, 1, "#7A4E2A"),
        (4.0, -12.0, 0.0, 3, 1, 1, "#7A4E2A"),
        (6.0, -15.0, 0.0, 1, 3, 1, "#8C5B32"),
        (3.0, -14.0, -2.0, 1, 1, 2, "#8C5B32"),
        (-1.0, -3.0, -4.6, 2, 2, 1, "#FF2020"),
    ],
    "elf": [
        (-4.5, -9.0, -4.5, 9, 2, 9, "#C8102E"),
        (-3.5, -12.0, -3.5, 7, 3, 7, "#2E8B3A"),
        (-2.5, -15.0, -2.0, 5, 3, 5, "#2E8B3A"),
        (-1.5, -17.0, -0.5, 3, 2, 3, "#27752F"),
        (-1.0, -18.0, 1.5, 2, 2, 3, "#27752F"),
        (-1.0, -17.5, 4.5, 2, 2, 2, "#F2C94C"),
    ],
    "tophat": [
        (-5.5, -9.0, -5.5, 11, 1, 11, "#1C1C22"),
        (-3.5, -16.0, -3.5, 7, 7, 7, "#22222A"),
        (-4.0, -11.0, -4.0, 8, 2, 8, "#B11A33"),
        (-3.0, -12.0, -4.6, 2, 2, 1, "#2E7D32"),
        (-1.5, -12.3, -4.8, 1, 1, 1, "#E01E37"),
    ],
    "halo": [
        (-4.0, 0.0, -4.0, 8, 1, 1, "#FFD94A"),
        (-4.0, 0.0, 3.0, 8, 1, 1, "#FFD94A"),
        (-4.0, 0.0, -3.0, 1, 1, 6, "#FFD94A"),
        (3.0, 0.0, -3.0, 1, 1, 6, "#FFD94A"),
        (-1.0, -8.0, -0.5, 2, 6, 1, "#FFE680"),
        (-3.0, -6.0, -0.5, 6, 2, 1, "#FFE680"),
        (-2.0, -7.0, -0.5, 4, 4, 1, "#FFF2B3"),
    ],
}
HATS.update({
    "beanie": [(-4.5, -9.0, -4.5, 9, 2, 9, "#F4F4F4"), (-4.0, -12.0, -4.0, 8, 3, 8, "#C8102E"), (-3.0, -13.0, -3.0, 6, 1, 6, "#C8102E"), (-1.5, -15.5, -1.5, 3, 3, 3, "#FFFFFF")],
    "holly": [(-4.5, -9.5, -4.5, 9, 1, 1, "#2E7D32"), (-4.5, -9.5, 3.5, 9, 1, 1, "#2E7D32"), (-4.5, -9.5, -3.5, 1, 1, 7, "#2E7D32"), (3.5, -9.5, -3.5, 1, 1, 7, "#2E7D32"),
              (-3.0, -11.0, -4.6, 2, 2, 1, "#1E5E28"), (1.0, -11.0, -4.6, 2, 2, 1, "#1E5E28"), (-1.0, -10.5, -4.8, 1, 1, 1, "#E01E37"), (0.5, -10.8, -4.8, 1, 1, 1, "#E01E37")],
    "gift": [(-4.0, -14.0, -4.0, 8, 6, 8, "#2C6FB0"), (-1.0, -14.5, -4.5, 2, 7, 9, "#FDE68A"), (-4.5, -14.5, -1.0, 9, 7, 2, "#FDE68A"), (-3.0, -16.0, -1.0, 2, 2, 2, "#FDE68A"), (1.0, -16.0, -1.0, 2, 2, 2, "#FDE68A")],
    "pet_snowman": [(-3, -6, -3, 6, 6, 6, "#FAFCFF"), (-2, -10, -2, 4, 4, 4, "#FAFCFF"), (-1.5, -13, -1.5, 3, 3, 3, "#FAFCFF"), (-1.5, -14, -1.5, 3, 1, 3, "#1C1C22"), (-1, -16, -1, 2, 2, 2, "#1C1C22"), (-0.5, -12, -2.5, 1, 1, 1, "#F28C28"), (-2.5, -10.5, -2.5, 5, 1, 5, "#B11A33")],
    "pet_present": [(-3, -6, -3, 6, 6, 6, "#B11A33"), (-1, -6.5, -3.5, 2, 7, 7, "#FDE68A"), (-3.5, -6.5, -1, 7, 7, 2, "#FDE68A"), (-2, -8, -1, 2, 2, 2, "#FDE68A"), (0, -8, -1, 2, 2, 2, "#FDE68A")],
    "pet_penguin": [(-2.5, -7, -2, 5, 7, 4, "#1C1C22"), (-1.5, -6, -2.3, 3, 5, 1, "#F4F4F4"), (-2, -10, -2, 4, 3, 4, "#1C1C22"), (-0.5, -9, -3, 1, 1, 1, "#F28C28"), (-1.5, -9.5, -2.2, 1, 1, 1, "#FFFFFF"), (0.5, -9.5, -2.2, 1, 1, 1, "#FFFFFF"), (-2, -11, -2, 4, 1, 4, "#C8102E"), (-1, -12, -1, 2, 1, 2, "#C8102E"), (-0.5, -13, -0.5, 1, 1, 1, "#FFFFFF")],
    "pet_tree": [(-0.5, -2, -0.5, 1, 2, 1, "#5A3A22"), (-3, -5, -3, 6, 3, 6, "#1E6B34"), (-2, -8, -2, 4, 3, 4, "#1E6B34"), (-1, -10, -1, 2, 2, 2, "#1E6B34"), (-0.5, -11.5, -0.5, 1, 1, 1, "#FFD94A"), (-3.2, -4, -1, 1, 1, 1, "#E01E37"), (2.2, -6, 0, 1, 1, 1, "#FDE68A"), (-1, -7, -2.2, 1, 1, 1, "#4CC3FF")],
    "pet_reindeer": [(-2, -5, -3, 4, 3, 6, "#8C5B32"), (-1.5, -8, -4.5, 3, 3, 3, "#8C5B32"), (-0.5, -6.5, -5, 1, 1, 1, "#FF2020"), (-2, -2, -3, 1, 2, 1, "#6B4423"), (1, -2, -3, 1, 2, 1, "#6B4423"), (-2, -2, 2, 1, 2, 1, "#6B4423"), (1, -2, 2, 1, 2, 1, "#6B4423"), (-1.5, -10, -3.5, 1, 2, 1, "#5A3A22"), (0.5, -10, -3.5, 1, 2, 1, "#5A3A22")],
})
HAT_NAMES = {"santa": "Santa Hat", "antlers": "Reindeer Antlers", "elf": "Elf Hat", "tophat": "Snowman Top Hat", "halo": "Star Halo"}


def build_hats():
    lines = []
    for hid, cubes in HATS.items():
        img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
        px = img.load()
        u, v, rowh = 0, 0, 0
        placed = []
        rnd = random.Random(hid)
        for (x, y, z, w, h, d, col) in cubes:
            rw, rh = 2 * (w + d), d + h
            if u + rw > 64:
                u, v, rowh = 0, v + rowh, 0
            placed.append((u, v))
            c = hexc(col)
            for yy in range(rh):
                for xx in range(rw):
                    f = 1.0 + (rnd.random() - 0.5) * 0.10
                    if yy < d:
                        f *= 1.12  # top/bottom faces lighter
                    px[u + xx, v + yy] = shade(c, f)
            u += rw
            rowh = max(rowh, rh)
        save(img, f"{TEX}/hat/{hid}.png")
        parts = []
        for i, ((x, y, z, w, h, d, col), (uu, vv)) in enumerate(zip(cubes, placed)):
            parts.append(f'        r.addChild("p{i}", ModelPartBuilder.create().uv({uu}, {vv}).cuboid({x}f, {y}f, {z}f, {w}f, {h}f, {d}f), ModelTransform.pivot(0f, 0f, 0f));')
        lines.append(f"    static TexturedModelData {hid}() {{\n        ModelData data = new ModelData();\n        ModelPartData r = data.getRoot();\n" + "\n".join(parts) + "\n        return TexturedModelData.of(data, 64, 64);\n    }\n")
        # front-view icon 32x32
        icon = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
        dr = ImageDraw.Draw(icon)
        xs = [c[0] for c in cubes] + [c[0] + c[3] for c in cubes]
        ys = [c[1] for c in cubes] + [c[1] + c[4] for c in cubes]
        minx, maxx, miny, maxy = min(xs), max(xs), min(ys), max(ys)
        sc = min(28 / (maxx - minx), 28 / (maxy - miny))
        ox = 16 - (minx + maxx) / 2 * sc
        oy = 16 - (miny + maxy) / 2 * sc
        for (x, y, z, w, h, d, col) in sorted(cubes, key=lambda c: -c[2]):
            dr.rectangle([ox + x * sc, oy + y * sc, ox + (x + w) * sc - 1, oy + (y + h) * sc - 1], fill=hexc(col), outline=shade(hexc(col), 0.7))
        save(icon, f"{TEX}/gui/hat_{hid}.png")
    java = """package fr.darkerdawn.jaly.cosmetic;

import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;

/** Generated by tools/gen_art.py - do not edit by hand. */
final class HatModels {
    private HatModels() {}

""" + "\n".join(lines) + "}\n"
    with open(os.path.join(JAVA, "HatModels.java"), "w") as f:
        f.write(java)


# ---------------------------------------------------------------- capes (256x128 = 4x HD)
def cape_design(kind, w, h):
    """Return an RGBA image w x h with the cape design."""
    img = Image.new("RGBA", (w, h))
    px = img.load()
    rnd = random.Random(kind)
    if kind == "candy_cane":
        for y in range(h):
            for x in range(w):
                px[x, y] = RED if ((x + y) // 6) % 2 == 0 else SNOW
        d = ImageDraw.Draw(img)
        d.rectangle([0, 0, w - 1, h - 1], outline=GOLD, width=2)
    elif kind == "tree":
        for y in range(h):
            for x in range(w):
                px[x, y] = mix(hexc("#0E2A14"), hexc("#163D20"), y / h)
        d = ImageDraw.Draw(img)
        cx = w // 2
        for i, (top, bot, half) in enumerate([(10, 26, 8), (20, 38, 12), (30, 52, 16)]):
            d.polygon([(cx, top), (cx - half, bot), (cx + half, bot)], fill=hexc("#2E8B3A"), outline=hexc("#1E5E28"))
        d.rectangle([cx - 3, 52, cx + 2, 57], fill=BROWN)
        d.polygon([(cx, 4), (cx + 2, 8), (cx + 5, 8), (cx + 3, 11), (cx + 4, 14), (cx, 12), (cx - 4, 14), (cx - 3, 11), (cx - 5, 8), (cx - 2, 8)], fill=hexc("#FFD94A"))
        for _ in range(16):
            yy = rnd.randint(16, 50)
            span = int((yy - 8) * 0.33)
            xx = cx + rnd.randint(-span, span)
            d.rectangle([xx - 1, yy - 1, xx + 1, yy + 1], fill=rnd.choice([RED_L, GOLD_L, ICE, SNOW]))
        d.rectangle([0, h - 6, w - 1, h - 1], fill=SNOW)
        for _ in range(25):
            x, y = rnd.randint(1, w - 2), rnd.randint(1, h - 10)
            px[x, y] = SNOW
    elif kind == "snowflake":
        for y in range(h):
            for x in range(w):
                px[x, y] = mix(hexc("#2C5D8F"), hexc("#9BC3E7"), y / h)
        d = ImageDraw.Draw(img)
        cx, cy = w // 2, h // 2 - 2
        for a in range(6):
            ang = a * math.pi / 3
            ex, ey = cx + math.cos(ang) * 16, cy + math.sin(ang) * 16
            d.line([(cx, cy), (ex, ey)], fill=SNOW, width=3)
            for t in (0.45, 0.75):
                bx, by = cx + math.cos(ang) * 16 * t, cy + math.sin(ang) * 16 * t
                for s in (-1, 1):
                    a2 = ang + s * 0.8
                    d.line([(bx, by), (bx + math.cos(a2) * 5, by + math.sin(a2) * 5)], fill=SNOW, width=2)
        d.ellipse([cx - 3, cy - 3, cx + 3, cy + 3], fill=hexc("#D0E2F0"))
        for _ in range(30):
            x, y = rnd.randint(1, w - 2), rnd.randint(1, h - 2)
            px[x, y] = SNOW
    elif kind == "gingerbread":
        for y in range(h):
            for x in range(w):
                px[x, y] = shade(hexc("#9C5A2B"), 1 + (rnd.random() - 0.5) * 0.08)
        d = ImageDraw.Draw(img)
        for x in range(0, w, 4):
            d.line([(x, 3), (x + 2, 6), (x + 4, 3)], fill=SNOW, width=1)
            d.line([(x, h - 7), (x + 2, h - 4), (x + 4, h - 7)], fill=SNOW, width=1)
        cx, cy = w // 2, h // 2
        body = hexc("#C27A3C")
        d.ellipse([cx - 7, cy - 22, cx + 7, cy - 8], fill=body)
        d.rounded_rectangle([cx - 8, cy - 9, cx + 8, cy + 10], 4, fill=body)
        d.rounded_rectangle([cx - 17, cy - 8, cx + 17, cy - 2], 3, fill=body)
        d.rounded_rectangle([cx - 9, cy + 6, cx - 2, cy + 22], 3, fill=body)
        d.rounded_rectangle([cx + 2, cy + 6, cx + 9, cy + 22], 3, fill=body)
        for yy in (cy - 4, cy + 1, cy + 6):
            d.ellipse([cx - 1, yy - 1, cx + 1, yy + 1], fill=RED_L)
        d.rectangle([cx - 4, cy - 18, cx - 3, cy - 17], fill=SNOW)
        d.rectangle([cx + 3, cy - 18, cx + 4, cy - 17], fill=SNOW)
        d.arc([cx - 4, cy - 17, cx + 4, cy - 11], 20, 160, fill=SNOW, width=1)
    elif kind == "aurora":
        for y in range(h):
            for x in range(w):
                px[x, y] = mix(hexc("#060B1E"), hexc("#13244A"), y / h)
        for band, col in ((0, "#3DFFB0"), (1, "#3DC8FF"), (2, "#B35CFF")):
            c = hexc(col)
            for x in range(w):
                yc = 16 + band * 7 + math.sin(x * 0.25 + band * 1.7) * 4
                for y in range(h):
                    dy = abs(y - yc)
                    if dy < 7:
                        t = (1 - dy / 7) * 0.6
                        p = px[x, y]
                        px[x, y] = mix(p, c, t)
        for _ in range(35):
            x, y = rnd.randint(0, w - 1), rnd.randint(0, h // 2)
            px[x, y] = SNOW
        d = ImageDraw.Draw(img)
        d.rectangle([0, h - 8, w - 1, h - 1], fill=SNOW)
        for x0 in range(2, w, 9):
            d.polygon([(x0, h - 8), (x0 + 4, h - 22 - rnd.randint(0, 6)), (x0 + 8, h - 8)], fill=hexc("#0B1A12"))
    elif kind == "golden":
        for y in range(h):
            for x in range(w):
                px[x, y] = mix(hexc("#7A0E1E"), hexc("#B11A33"), y / h)
        d = ImageDraw.Draw(img)
        d.rectangle([1, 1, w - 2, h - 2], outline=hexc("#FFD94A"), width=2)
        # big candy J
        jx, jy = w // 2 + 4, 10
        for i in range(34):
            col = hexc("#FFD94A") if (i // 3) % 2 == 0 else SNOW
            d.rectangle([jx - 3, jy + i, jx + 3, jy + i], fill=col)
        for a in range(0, 181, 6):
            ang = math.radians(a)
            x = jx - 9 + math.cos(ang) * 9
            y = jy + 34 + math.sin(ang) * 9
            col = hexc("#FFD94A") if (a // 18) % 2 == 0 else SNOW
            d.ellipse([x - 3, y - 3, x + 3, y + 3], fill=col)
        for _ in range(14):
            x, y = rnd.randint(4, w - 5), rnd.randint(4, h - 5)
            d.line([(x - 2, y), (x + 2, y)], fill=GOLD_L)
            d.line([(x, y - 2), (x, y + 2)], fill=GOLD_L)
    elif kind == "sweater":
        for y in range(h):
            for x in range(w):
                px[x, y] = RED if (y // 4) % 2 == 0 or True else RED
        d = ImageDraw.Draw(img)
        for y0 in (6, h - 10):
            for x in range(0, w, 6):
                d.polygon([(x, y0 + 4), (x + 3, y0), (x + 6, y0 + 4)], fill=SNOW)
        for x in range(0, w, 8):
            for y in range(16, h - 14, 8):
                d.rectangle([x + 3, y, x + 4, y + 1], fill=hexc("#2E7D32"))
                d.point((x + 2, y + 1), fill=hexc("#2E7D32")); d.point((x + 5, y + 1), fill=hexc("#2E7D32"))
        d.rectangle([w // 2 - 6, h // 2 - 6, w // 2 + 6, h // 2 + 6], fill=hexc("#2E7D32"))
        d.polygon([(w // 2, h // 2 - 5), (w // 2 - 5, h // 2 + 5), (w // 2 + 5, h // 2 + 5)], fill=SNOW)
    elif kind == "mint":
        for y in range(h):
            for x in range(w):
                px[x, y] = hexc("#2BB673") if ((x + y) // 6) % 2 == 0 else SNOW
        ImageDraw.Draw(img).rectangle([0, 0, w - 1, h - 1], outline=hexc("#C99E02"), width=2)
    elif kind == "snowy_night":
        for y in range(h):
            for x in range(w):
                px[x, y] = mix(hexc("#060B1F"), hexc("#2B4A80"), y / h)
        d = ImageDraw.Draw(img)
        d.ellipse([w - 16, 6, w - 6, 16], fill=hexc("#FFF8DC"))
        for _ in range(45):
            x, y = rnd.randint(0, w - 1), rnd.randint(0, h - 1)
            px[x, y] = SNOW
        d.rectangle([0, h - 10, w - 1, h - 1], fill=SNOW)
        d.rectangle([6, h - 22, 22, h - 10], fill=hexc("#6B4426")); d.polygon([(3, h - 22), (14, h - 30), (25, h - 22)], fill=SNOW)
        d.rectangle([10, h - 18, 14, h - 14], fill=hexc("#FFC861"))
    elif kind == "present":
        for y in range(h):
            for x in range(w):
                px[x, y] = hexc("#2C6FB0") if ((x // 5) + (y // 5)) % 2 == 0 else hexc("#3A82C8")
        d = ImageDraw.Draw(img)
        d.rectangle([w // 2 - 3, 0, w // 2 + 2, h - 1], fill=hexc("#FDE68A"))
        d.rectangle([0, h // 2 - 3, w - 1, h // 2 + 2], fill=hexc("#FDE68A"))
        d.ellipse([w // 2 - 12, h // 2 - 10, w // 2 - 2, h // 2], outline=hexc("#FDE68A"), width=2)
        d.ellipse([w // 2 + 1, h // 2 - 10, w // 2 + 11, h // 2], outline=hexc("#FDE68A"), width=2)
    return img


CAPES = {"candy_cane": "Candy Cane", "tree": "Christmas Tree", "snowflake": "Snowflake", "gingerbread": "Gingerbread", "aurora": "Northern Lights", "golden": "Golden Jaly", "sweater": "Ugly Sweater", "mint": "Mint Candy", "snowy_night": "Snowy Night", "present": "Present Wrap"}


def build_capes():
    S = 4
    for cid in CAPES:
        img = Image.new("RGBA", (64 * S, 32 * S), (0, 0, 0, 0))
        design = cape_design(cid, 10 * S, 16 * S)
        edge = shade(design.getpixel((0, 0)), 0.7)
        d = ImageDraw.Draw(img)
        d.rectangle([0, 0, 22 * S - 1, 17 * S - 1], fill=edge)
        img.paste(design, (1 * S, 1 * S))
        img.paste(design.transpose(Image.FLIP_LEFT_RIGHT), (12 * S, 1 * S))
        ely = cape_design(cid, 24 * S, 22 * S)
        img.paste(ely, (22 * S, 0))
        save(img, f"{TEX}/cape/{cid}.png")


# ---------------------------------------------------------------- logo (pixel font)
GLYPHS = {
    "J": ["..###", "...#.", "...#.", "...#.", "#..#.", "#..#.", ".##..", ".....", "....."],
    "a": [".....", ".....", ".###.", "....#", ".####", "#...#", ".####", ".....", "....."],
    "l": [".##..", "..#..", "..#..", "..#..", "..#..", "..#..", ".###.", ".....", "....."],
    "y": [".....", ".....", "#...#", "#...#", "#...#", ".####", "....#", "#...#", ".###."],
    "C": [".###.", "#...#", "#....", "#....", "#....", "#...#", ".###.", ".....", "....."],
    "i": ["..#..", ".....", ".##..", "..#..", "..#..", "..#..", ".###.", ".....", "....."],
    "e": [".....", ".....", ".###.", "#...#", "#####", "#....", ".###.", ".....", "....."],
    "n": [".....", ".....", "####.", "#...#", "#...#", "#...#", "#...#", ".....", "....."],
    "t": ["..#..", "..#..", "####.", "..#..", "..#..", "..#.#", "...#.", ".....", "....."],
}


def build_logo(scale=3):
    word = "JalyClient"
    gw = 5 * scale + scale  # glyph width + spacing
    W = len(word) * gw + 8
    H = 9 * scale + 10
    mask = Image.new("L", (W, H), 0)
    m = mask.load()
    starts = []
    for i, ch in enumerate(word):
        g = GLYPHS[ch]
        ox, oy = 4 + i * gw, 6
        starts.append(ox)
        for gy, row in enumerate(g):
            for gx, c in enumerate(row):
                if c == "#":
                    for yy in range(scale):
                        for xx in range(scale):
                            m[ox + gx * scale + xx, oy + gy * scale + yy] = 255
    img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    px = img.load()
    # shadow + outline
    for y in range(H):
        for x in range(W):
            if m[x, y]:
                continue
            near = any(0 <= x + dx < W and 0 <= y + dy < H and m[x + dx, y + dy] for dx in (-1, 0, 1) for dy in (-1, 0, 1))
            if near:
                px[x, y] = hexc("#14060A")
            elif x >= 2 and y >= 2 and m[x - 2, y - 2]:
                px[x, y] = hexc("#000000", 120)
    split = starts[4]
    for y in range(H):
        for x in range(W):
            if m[x, y]:
                if x < split:  # "Jaly" candy cane
                    px[x, y] = RED if ((x + y) // 4) % 2 == 0 else SNOW
                else:  # "Client" gold
                    t = (y - 6) / (9 * scale)
                    px[x, y] = mix(hexc("#FFF1A8"), hexc("#D9A400"), max(0, min(1, t)))
    # snow caps on top pixels
    rnd = random.Random(7)
    for x in range(W):
        for y in range(H):
            if m[x, y]:
                if y > 0 and not m[x, y - 1]:
                    px[x, y] = SNOW
                    if y > 1 and rnd.random() < 0.55:
                        px[x, y - 1] = SNOW
                break
    save(img, f"{TEX}/gui/logo.png")
    return img


# ---------------------------------------------------------------- title screen layers (480x270, tileable)
TW, TH = 480, 270


def periodic(x, terms):
    return sum(a * math.sin(2 * math.pi * k * x / TW + p) for (a, k, p) in terms)


def build_title():
    out = f"{TEX}/gui/title"
    rnd = random.Random(42)
    # sky
    sky = Image.new("RGBA", (TW, TH))
    px = sky.load()
    for y in range(TH):
        t = y / TH
        c = mix(hexc("#060B1F"), hexc("#16275A"), min(1, t * 1.4)) if t < 0.7 else mix(hexc("#16275A"), hexc("#3A5A94"), (t - 0.7) / 0.3)
        for x in range(TW):
            px[x, y] = c
    stars = []
    for _ in range(140):
        x, y = rnd.randint(0, TW - 1), rnd.randint(0, int(TH * 0.55))
        px[x, y] = hexc("#FFFFFF") if rnd.random() < 0.5 else hexc("#BFD4FF")
        if rnd.random() < 0.15:
            stars.append((x, y))
    d = ImageDraw.Draw(sky)
    mx, my = 372, 46
    for r, a in ((26, 18), (20, 30), (16, 50)):
        glow = Image.new("RGBA", (TW, TH), (0, 0, 0, 0))
        ImageDraw.Draw(glow).ellipse([mx - r, my - r, mx + r, my + r], fill=(255, 250, 220, a))
        sky = Image.alpha_composite(sky, glow)
    d = ImageDraw.Draw(sky)
    d.ellipse([mx - 12, my - 12, mx + 12, my + 12], fill=hexc("#FFF8DC"))
    d.ellipse([mx - 5, my - 6, mx - 1, my - 2], fill=hexc("#EDE3BE"))
    d.ellipse([mx + 3, my + 2, mx + 7, my + 6], fill=hexc("#EDE3BE"))
    save(sky, f"{out}/sky.png")
    # aurora (transparent, tileable)
    aur = Image.new("RGBA", (TW, TH), (0, 0, 0, 0))
    ap = aur.load()
    for band, col, base in ((0, (61, 255, 176), 40), (1, (61, 200, 255), 56), (2, (179, 92, 255), 70)):
        for x in range(TW):
            yc = base + periodic(x, [(10, 2, band), (5, 5, band * 2)])
            hgt = 18 + periodic(x, [(6, 3, band + 1)])
            for y in range(max(0, int(yc - hgt)), min(TH, int(yc + 8))):
                t = (y - (yc - hgt)) / (hgt + 8)
                a = int(90 * math.sin(math.pi * max(0, min(1, t))))
                old = ap[x, y]
                ap[x, y] = (col[0], col[1], col[2], min(255, old[3] + a))
    save(aur.filter(ImageFilter.GaussianBlur(1.2)), f"{out}/aurora.png")
    # far mountains
    far = Image.new("RGBA", (TW, TH), (0, 0, 0, 0))
    fp = far.load()
    for x in range(TW):
        top = 150 + periodic(x, [(22, 2, 0.3), (14, 5, 1.1), (6, 11, 2.0)])
        for y in range(int(top), TH):
            depth = y - top
            col = hexc("#C9D6EA") if depth < 6 + periodic(x, [(3, 7, 0.5)]) and top < 150 else hexc("#24375F")
            if col == hexc("#24375F") and (x // 2 + y // 3) % 7 == 0:
                col = hexc("#2A3F6B")
            fp[x, y] = col
    save(far, f"{out}/far.png")
    # mid hills + blocky village (Minecraft-like, everything on an 8px block grid)
    mid = Image.new("RGBA", (TW, TH), (0, 0, 0, 0))
    md = ImageDraw.Draw(mid)
    B = 8
    heights = {}
    for bx in range(TW // B):
        x = bx * B
        hh = 190 + periodic(x, [(10, 2, 2.2), (6, 4, 0.7)])
        heights[bx] = int(hh // B) * B
    for bx, top in heights.items():
        x = bx * B
        md.rectangle([x, top, x + B - 1, TH], fill=hexc("#DCE6F2"))
        md.rectangle([x, top + 3, x + B - 1, TH], fill=hexc("#C3D2E6"))
        md.rectangle([x, top, x + B - 1, top + 1], fill=hexc("#FFFFFF"))
    chimneys = []
    windows = []
    # blocky spruce trees
    for bx in [1, 5, 9, 18, 22, 27, 33, 40, 44, 52, 57]:
        x = bx * B
        g = heights[bx % (TW // B)]
        md.rectangle([x + 3, g - 10, x + 4, g - 1], fill=hexc("#4A2F1A"))
        for i, wdt in enumerate([7, 5, 3, 1]):
            yy = g - 10 - i * 6
            md.rectangle([x + 4 - wdt, yy - 5, x + 4 + wdt - 1, yy], fill=hexc("#163824"))
            md.rectangle([x + 4 - wdt, yy - 5, x + 4 + wdt - 1, yy - 4], fill=hexc("#F2F6FA"))
    # cabins with lit windows
    for bx in [12, 30, 48]:
        x = bx * B
        g = min(heights[bx], heights[bx + 1], heights[bx + 2], heights[bx + 3])
        md.rectangle([x, g - 24, x + 31, g], fill=hexc("#6B4426"))
        for yy in range(g - 24, g, 4):
            md.line([(x, yy), (x + 31, yy)], fill=hexc("#5A381F"))
        md.polygon([(x - 4, g - 24), (x + 16, g - 38), (x + 36, g - 24)], fill=hexc("#7A2F2F"))
        md.polygon([(x - 4, g - 24), (x + 16, g - 38), (x + 36, g - 24), (x + 34, g - 24), (x + 16, g - 35), (x - 2, g - 24)], fill=hexc("#FFFFFF"))
        md.line([(x - 4, g - 24), (x + 16, g - 38), (x + 36, g - 24)], fill=hexc("#FFFFFF"), width=2)
        md.rectangle([x + 23, g - 40, x + 27, g - 30], fill=hexc("#5E5E66"))
        md.rectangle([x + 23, g - 41, x + 27, g - 40], fill=hexc("#FFFFFF"))
        chimneys.append((x + 25, g - 42))
        for wx in (x + 4, x + 20):
            md.rectangle([wx, g - 18, wx + 7, g - 11], fill=hexc("#FFC861"))
            md.line([(wx + 3, g - 18), (wx + 3, g - 11)], fill=hexc("#6B4426"))
            windows.append((wx, g - 18, 8, 8))
        md.rectangle([x + 13, g - 13, x + 18, g - 1], fill=hexc("#3D2614"))
        for i in range(9):
            lx = x - 2 + i * 4
            ly = g - 24 + (1 if i % 2 else 0)
            md.point((lx, ly), fill=[RED_L, GOLD_L, hexc("#4CC3FF"), hexc("#5CFF7A")][i % 4])
    save(mid, f"{out}/mid.png")
    # near: stepped snow blocks, cube snowmen, presents, lanterns
    near = Image.new("RGBA", (TW, TH), (0, 0, 0, 0))
    nd = ImageDraw.Draw(near)
    B = 16
    for bx in range(TW // B):
        x = bx * B
        top = 236 + (B if periodic(x, [(1, 3, 0.4), (0.6, 7, 1.0)]) > 0.3 else 0)
        nd.rectangle([x, top, x + B - 1, TH], fill=hexc("#EEF3F8"))
        nd.rectangle([x, top + B, x + B - 1, TH], fill=hexc("#D6E1EE"))
        nd.rectangle([x, top, x + B - 1, top + 1], fill=hexc("#FFFFFF"))
        nd.line([(x, top + B), (x + B - 1, top + B)], fill=hexc("#C3D2E6"))
        for _ in range(3):
            sx, sy = x + rnd.randint(1, 14), top + rnd.randint(3, 14)
            nd.point((sx, sy), fill=hexc("#C9D7E8"))
    lights = []
    for (x, kind) in [(40, "snowman"), (120, "presents"), (210, "lantern"), (300, "snowman"), (380, "presents"), (440, "lantern")]:
        g = 236
        if kind == "snowman":
            nd.rectangle([x, g - 16, x + 15, g - 1], fill=hexc("#FAFCFF"), outline=hexc("#C9D7E8"))
            nd.rectangle([x + 2, g - 28, x + 13, g - 17], fill=hexc("#FAFCFF"), outline=hexc("#C9D7E8"))
            nd.rectangle([x + 4, g - 37, x + 11, g - 29], fill=hexc("#FAFCFF"), outline=hexc("#C9D7E8"))
            nd.rectangle([x + 5, g - 34, x + 6, g - 33], fill=hexc("#1A1A1A"))
            nd.rectangle([x + 9, g - 34, x + 10, g - 33], fill=hexc("#1A1A1A"))
            nd.rectangle([x + 7, g - 32, x + 9, g - 31], fill=hexc("#F28C28"))
            nd.rectangle([x + 2, g - 29, x + 13, g - 27], fill=RED)
            nd.rectangle([x + 3, g - 41, x + 12, g - 38], fill=hexc("#1C1C22"))
            nd.rectangle([x + 5, g - 46, x + 10, g - 41], fill=hexc("#1C1C22"))
            for yy in (g - 24, g - 21, g - 12, g - 8):
                nd.rectangle([x + 7, yy, x + 8, yy + 1], fill=hexc("#1A1A1A"))
        elif kind == "presents":
            for (ox, w, h, col, rib) in [(0, 14, 12, RED, GOLD), (15, 10, 16, hexc("#2E7D32"), RED_L), (26, 12, 9, hexc("#2C6FB0"), SNOW)]:
                nd.rectangle([x + ox, g - h, x + ox + w - 1, g - 1], fill=col)
                nd.rectangle([x + ox + w // 2 - 1, g - h, x + ox + w // 2, g - 1], fill=rib)
                nd.rectangle([x + ox, g - h // 2 - 1, x + ox + w - 1, g - h // 2], fill=rib)
                nd.rectangle([x + ox + w // 2 - 3, g - h - 2, x + ox + w // 2 + 2, g - h - 1], fill=rib)
        else:
            nd.rectangle([x + 3, g - 30, x + 4, g - 1], fill=hexc("#2A2A30"))
            nd.rectangle([x, g - 38, x + 7, g - 30], fill=hexc("#2A2A30"))
            nd.rectangle([x + 1, g - 37, x + 6, g - 31], fill=hexc("#FFC861"))
            lights.append((x + 1, g - 37, 6, 7))
    save(near, f"{out}/near.png")
    # sleigh silhouette (generic sleigh + reindeer)
    sl = Image.new("RGBA", (64, 16), (0, 0, 0, 0))
    sd = ImageDraw.Draw(sl)
    sil = hexc("#0A1022")
    sd.rectangle([2, 6, 14, 10], fill=sil)
    sd.rectangle([4, 3, 6, 6], fill=sil)
    sd.line([(1, 12), (16, 12)], fill=sil)
    sd.line([(16, 12), (17, 10)], fill=sil)
    for i, x in enumerate((24, 36, 48)):
        sd.rectangle([x, 7, x + 7, 9], fill=sil)
        sd.rectangle([x + 7, 4, x + 8, 7], fill=sil)
        sd.rectangle([x + 8, 3, x + 10, 4], fill=sil)
        sd.line([(x + 8, 3), (x + 7, 0)], fill=sil)
        sd.line([(x + 10, 3), (x + 11, 0)], fill=sil)
        sd.line([(x + 1, 10), (x, 13)], fill=sil)
        sd.line([(x + 6, 10), (x + 7, 13)], fill=sil)
        sd.line([(x - 9 if i else 15, 8), (x, 8)], fill=sil)
    sd.point((59, 3), fill=hexc("#FF3030"))
    save(sl, f"{out}/sleigh.png")
    return {"stars": stars, "chimneys": chimneys, "windows": windows, "lanterns": lights}


# ---------------------------------------------------------------- lodge (J menu hub) 320x180
def build_lodge():
    W, H = 320, 180
    rnd = random.Random(99)
    img = Image.new("RGBA", (W, H))
    d = ImageDraw.Draw(img)
    # wall planks
    for x in range(0, W, 10):
        c = hexc("#5A3A22") if (x // 10) % 2 else hexc("#61402A")
        d.rectangle([x, 0, x + 9, 140], fill=c)
        d.line([(x, 0), (x, 140)], fill=hexc("#47301C"))
        for _ in range(3):
            yy = rnd.randint(4, 136)
            d.point((x + rnd.randint(2, 7), yy), fill=hexc("#4C3220"))
    d.rectangle([0, 112, W, 140], fill=hexc("#4A2F1C"))
    d.line([(0, 112), (W, 112)], fill=hexc("#7A5232"), width=2)
    # floor
    for y in range(140, H, 6):
        d.rectangle([0, y, W, y + 5], fill=hexc("#7A4E2C") if (y // 6) % 2 else hexc("#704727"))
        d.line([(0, y), (W, y)], fill=hexc("#5C3A20"))
        off = rnd.randint(0, 40)
        for x in range(off, W, 48):
            d.line([(x, y), (x, y + 5)], fill=hexc("#5C3A20"))
    # rug
    d.ellipse([96, 150, 236, 176], fill=hexc("#8E1B2C"))
    d.ellipse([102, 153, 230, 173], outline=hexc("#D9A400"), width=1)
    d.ellipse([112, 156, 220, 170], outline=hexc("#D9A400"), width=1)
    # garland along top
    for x in range(0, W, 2):
        y = 6 + int(3 * abs(math.sin(x * math.pi / 40)))
        d.rectangle([x, y, x + 1, y + 3], fill=hexc("#1E5E28") if x % 4 else hexc("#2E7D32"))
    bulbs = []
    for x in range(10, W, 20):
        y = 6 + int(3 * abs(math.sin(x * math.pi / 40))) + 4
        bulbs.append((x, y))
    # window (Settings? no: window is decor with snow) 150..200 x 22..82
    d.rectangle([148, 20, 202, 84], fill=hexc("#3D2614"))
    d.rectangle([152, 24, 198, 80], fill=hexc("#132447"))
    for y in range(24, 81):
        d.line([(152, y), (198, y)], fill=mix(hexc("#0C1733"), hexc("#2B4A80"), (y - 24) / 56))
    d.rectangle([152, 72, 198, 80], fill=hexc("#E8EEF6"))
    for x0 in (156, 170, 184):
        d.polygon([(x0, 72), (x0 + 5, 56), (x0 + 10, 72)], fill=hexc("#0F2A1A"))
    d.line([(175, 24), (175, 80)], fill=hexc("#3D2614"), width=2)
    d.line([(152, 50), (198, 50)], fill=hexc("#3D2614"), width=2)
    d.rectangle([146, 82, 204, 86], fill=hexc("#F2F6FA"))
    # notice board (Mods) 92..144 x 28..80
    d.rectangle([90, 28, 144, 80], fill=hexc("#3D2614"))
    d.rectangle([93, 31, 141, 77], fill=hexc("#B9875A"))
    for _ in range(60):
        d.point((rnd.randint(94, 140), rnd.randint(32, 76)), fill=hexc("#A57447"))
    for (nx, ny, col) in [(97, 35, "#FFF7D6"), (118, 34, "#D6F0FF"), (100, 55, "#FFE0E6"), (121, 54, "#E5FFD6")]:
        d.rectangle([nx, ny, nx + 16, ny + 17], fill=hexc(col))
        for ly in range(ny + 5, ny + 16, 3):
            d.line([(nx + 3, ly), (nx + 13, ly)], fill=hexc("#8A8A9A"))
        d.rectangle([nx + 7, ny - 1, nx + 9, ny + 1], fill=RED_L)
    # painting (Edit HUD) 96..138 x 90..124
    d.rectangle([96, 90, 138, 124], fill=hexc("#C99E02"))
    d.rectangle([99, 93, 135, 121], fill=hexc("#2B4A80"))
    d.polygon([(99, 121), (112, 102), (122, 112), (128, 105), (135, 114), (135, 121)], fill=hexc("#E8EEF6"))
    d.rectangle([102, 96, 112, 101], outline=hexc("#FDF437"))
    d.rectangle([124, 96, 132, 99], outline=hexc("#FDF437"))
    # mirror (Cosmetics) 160..192 x 92..156
    d.rounded_rectangle([158, 92, 194, 156], 6, fill=hexc("#C99E02"))
    d.rounded_rectangle([162, 96, 190, 150], 4, fill=hexc("#9EC2E0"))
    d.line([(166, 100), (174, 110)], fill=hexc("#D9ECFA"), width=2)
    d.line([(170, 98), (180, 110)], fill=hexc("#D9ECFA"), width=1)
    d.rectangle([164, 156, 168, 162], fill=hexc("#8A6A00"))
    d.rectangle([184, 156, 188, 162], fill=hexc("#8A6A00"))
    # clock (Settings) center 217,30 r=10
    d.ellipse([206, 18, 228, 40], fill=hexc("#3D2614"))
    d.ellipse([208, 20, 226, 38], fill=hexc("#F6F0DC"))
    for a in range(12):
        ang = a * math.pi / 6
        d.point((217 + math.cos(ang) * 7, 29 + math.sin(ang) * 7), fill=hexc("#3D2614"))
    d.rectangle([215, 40, 219, 60], fill=hexc("#3D2614"))
    d.ellipse([213, 58, 221, 66], fill=hexc("#C99E02"))
    # fireplace (Packs) 232..312 x 64..150
    d.rectangle([232, 64, 312, 150], fill=hexc("#6E6E78"))
    for y in range(64, 150, 6):
        off = 0 if (y // 6) % 2 else 6
        for x in range(232 + off, 312, 12):
            d.rectangle([x, y, x + 11, y + 5], outline=hexc("#585862"))
    d.rectangle([228, 60, 316, 68], fill=hexc("#5A3A22"))
    d.rectangle([228, 60, 316, 61], fill=hexc("#7A5232"))
    d.rectangle([250, 96, 294, 150], fill=hexc("#120A08"))
    for x in range(228, 317, 2):
        d.rectangle([x, 56, x + 1, 59], fill=hexc("#2E7D32") if x % 4 else hexc("#1E5E28"))
        if x % 12 == 0:
            d.point((x, 58), fill=RED_L)
    for i, (sx, col) in enumerate([(238, RED), (264, hexc("#2E7D32")), (290, RED)]):
        d.rectangle([sx, 70, sx + 9, 84], fill=col)
        d.rectangle([sx - 4, 80, sx + 9, 86], fill=col)
        d.rectangle([sx - 1, 68, sx + 10, 72], fill=SNOW)
    # tree (Mods? no: Gifts are presents; tree is decor + Mods quick) 8..80 x 14..140
    cx = 44
    for i, (top, bot, half) in enumerate([(22, 56, 18), (40, 86, 26), (64, 116, 34), (90, 138, 40)]):
        d.polygon([(cx, top), (cx - half, bot), (cx + half, bot)], fill=hexc("#1E6B34"))
        d.polygon([(cx, top + 4), (cx - half + 6, bot - 2), (cx - half + 2, bot)], fill=hexc("#16542A"))
        for x in range(cx - half, cx + half, 3):
            d.point((x, bot - 1), fill=hexc("#F2F6FA"))
    d.rectangle([cx - 5, 138, cx + 4, 146], fill=hexc("#5A3A22"))
    d.polygon([(cx, 10), (cx + 3, 17), (cx + 9, 17), (cx + 4, 21), (cx + 6, 28), (cx, 24), (cx - 6, 28), (cx - 4, 21), (cx - 9, 17), (cx - 3, 17)], fill=hexc("#FFD94A"))
    tree_lights = []
    for _ in range(26):
        yy = rnd.randint(30, 132)
        half = (yy - 14) * 0.31
        xx = int(cx + rnd.uniform(-half, half))
        tree_lights.append((xx, yy))
    for (ox, oy) in [(30, 70), (56, 96), (36, 112), (60, 60), (24, 124)]:
        d.ellipse([ox - 2, oy - 2, ox + 2, oy + 2], fill=rnd.choice([RED_L, GOLD_L, ICE]))
    # presents under tree (Gifts) 10..96 x 128..160
    for (x, y, w, h, col, rib) in [(8, 140, 22, 18, RED, GOLD_L), (30, 134, 18, 24, hexc("#2C6FB0"), SNOW), (50, 144, 24, 14, hexc("#2E7D32"), RED_L), (74, 138, 18, 20, GOLD, RED)]:
        d.rectangle([x, y, x + w, y + h], fill=col)
        d.rectangle([x + w // 2 - 1, y, x + w // 2 + 1, y + h], fill=rib)
        d.rectangle([x, y + h // 2 - 1, x + w, y + h // 2 + 1], fill=rib)
        d.ellipse([x + w // 2 - 5, y - 4, x + w // 2 - 1, y], outline=rib)
        d.ellipse([x + w // 2 + 1, y - 4, x + w // 2 + 5, y], outline=rib)
    save(img, f"{TEX}/gui/lodge.png")
    # fire frames 44x40 x4 (stacked vertically)
    fire = Image.new("RGBA", (44, 160), (0, 0, 0, 0))
    fd = ImageDraw.Draw(fire)
    for f in range(4):
        oy = f * 40
        fr = random.Random(f * 13 + 1)
        fd.rectangle([4, oy + 33, 40, oy + 38], fill=hexc("#5A3A22"))
        fd.rectangle([10, oy + 30, 34, oy + 34], fill=hexc("#6B4426"))
        for layer, col, scale in ((0, "#C23B14", 1.0), (1, "#F2771C", 0.72), (2, "#FFD34A", 0.45)):
            pts = []
            for i in range(9):
                x = 6 + i * 4
                hgt = (14 + fr.randint(0, 12)) * scale * (1 - abs(i - 4) / 6)
                pts.append((x, oy + 32 - hgt))
            fd.polygon([(5, oy + 33)] + pts + [(39, oy + 33)], fill=hexc(col))
    save(fire, f"{TEX}/gui/fire.png")
    return {"bulbs": bulbs, "tree_lights": tree_lights}


# ---------------------------------------------------------------- icon
def build_icon(size=128):
    s = size / 32
    img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.rounded_rectangle([0, 0, 31, 31], 6, fill=hexc("#163A1E"))
    d.rounded_rectangle([1, 1, 30, 30], 5, outline=hexc("#C99E02"))
    d.rectangle([2, 25, 29, 29], fill=SNOW)
    for x in range(2, 30, 3):
        d.point((x, 24), fill=SNOW)
    # candy-cane J
    pts = [(19, y) for y in range(5, 17)]
    for a in range(0, 181, 12):
        ang = math.radians(a)
        pts.append((int(round(15 + math.cos(ang) * 4)), int(round(17 + math.sin(ang) * 4))))
    for i, (x, y) in enumerate(pts):
        col = RED if (i // 2) % 2 == 0 else SNOW
        d.rectangle([x - 1, y - 1, x + 1, y + 1], fill=col)
    d.polygon([(25, 3), (26, 5), (28, 5), (26, 6), (27, 8), (25, 7), (23, 8), (24, 6), (22, 5), (24, 5)], fill=hexc("#FFD94A"))
    for (x, y) in [(6, 6), (9, 12), (5, 17), (27, 15)]:
        d.point((x, y), fill=SNOW)
    big = img.resize((size, size), Image.NEAREST)
    return big


# ---------------------------------------------------------------- resource packs
def tex16(rows, pal):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    for y, row in enumerate(rows):
        for x, c in enumerate(row):
            if c != "." and c in pal:
                px[x, y] = pal[c]
    return img


SWORD = [
    "...............A",
    "..............AB",
    ".............ABA",
    "............ABA.",
    "...........ABA..",
    "..........ABA...",
    ".........ABA....",
    "........ABA.....",
    "...GG..ABA......",
    "...GGGABA.......",
    "....GGBA........",
    ".....HGG........",
    "....HHGGG.......",
    "...HH..GG.......",
    "..HH............",
    ".K..............",
]


def candy_sword(c1, c2, guard, handle):
    img = tex16(SWORD, {"A": c1, "B": c2, "G": guard, "H": handle, "K": shade(guard, 0.8)})
    px = img.load()
    for y in range(16):
        for x in range(16):
            p = px[x, y]
            if p[3] and (p[:3] == c1[:3] or p[:3] == c2[:3]):
                px[x, y] = c1 if ((x + y) // 2) % 2 == 0 else c2
    return img


def build_packs():
    base = os.path.join(RES, "resourcepacks")
    # ---- Pack 1: Jaly Christmas (on by default)
    p1 = os.path.join(base, "jaly_christmas")
    tx = os.path.join(p1, "assets/minecraft/textures")
    with open(os.path.join(p1, "pack.mcmeta") if os.makedirs(p1, exist_ok=True) is None else "", "w") as f:
        json.dump({"pack": {"pack_format": 34, "description": "§cJaly §fChristmas §7- candy cane swords, festive hotbar"}}, f, indent=2)
    swords = {
        "wooden": (hexc("#A0522D"), SNOW, hexc("#6B4423"), hexc("#3D2614")),
        "stone": (hexc("#8A8A92"), SNOW, hexc("#5E5E66"), hexc("#3D2614")),
        "iron": (hexc("#D8DCE2"), RED, hexc("#9AA0A8"), hexc("#3D2614")),
        "golden": (hexc("#FFD94A"), SNOW, hexc("#C99E02"), hexc("#3D2614")),
        "diamond": (RED, SNOW, hexc("#2E7D32"), hexc("#1B5E20")),
        "netherite": (hexc("#5B2A86"), hexc("#E0C3FF"), hexc("#2A2A30"), hexc("#1C1C22")),
    }
    for k, (a, b, g, h) in swords.items():
        save(candy_sword(a, b, g, h), f"{tx}/item/{k}_sword.png")
    # golden apple -> gold ornament bauble
    orn = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    od = ImageDraw.Draw(orn)
    od.ellipse([2, 4, 13, 15], fill=hexc("#E0B000"))
    od.ellipse([4, 6, 7, 9], fill=hexc("#FFF2B3"))
    od.line([(3, 10), (12, 10)], fill=hexc("#B11A33"))
    od.rectangle([6, 1, 9, 4], fill=hexc("#9AA0A8"))
    od.point((7, 0), fill=hexc("#9AA0A8"))
    save(orn, f"{tx}/item/golden_apple.png")
    # totem -> gingerbread man
    gb = tex16([
        "......BBBB......", ".....BBBBBB.....", ".....BWBBWB.....", ".....BBBBBB.....", "......BWWB......",
        "..BBBBBBBBBBBB..", ".BBBBBBBBBBBBBB.", ".BB..BBRBB..BB..", ".....BBBBB......", ".....BBRBB......",
        ".....BBBBB......", "....BBB.BBB.....", "....BB...BB.....", "...BBB...BBB....", "...BB.....BB....", "................",
    ], {"B": hexc("#B8743A"), "W": SNOW, "R": RED_L})
    save(gb, f"{tx}/item/totem_of_undying.png")
    # snowball sparkle
    sb = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    sd = ImageDraw.Draw(sb)
    sd.ellipse([3, 3, 12, 12], fill=hexc("#F4F8FF"), outline=hexc("#BFD4EA"))
    sd.point((6, 5), fill=hexc("#FFFFFF"))
    sd.line([(13, 1), (13, 5)], fill=hexc("#9BC3E7"))
    sd.line([(11, 3), (15, 3)], fill=hexc("#9BC3E7"))
    save(sb, f"{tx}/item/snowball.png")
    # ender pearl -> snow globe
    eg = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    ed = ImageDraw.Draw(eg)
    ed.ellipse([2, 1, 13, 12], fill=hexc("#7FB6E8", 230), outline=hexc("#D0E2F0"))
    ed.polygon([(7, 4), (4, 10), (11, 10)], fill=hexc("#1E6B34"))
    for p in [(4, 4), (10, 3), (11, 7), (5, 7)]:
        ed.point(p, fill=SNOW)
    ed.rectangle([3, 12, 12, 14], fill=hexc("#8E1B2C"))
    save(eg, f"{tx}/item/ender_pearl.png")
    # cookie -> gingerbread cookie star
    ck = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    cd = ImageDraw.Draw(ck)
    cd.polygon([(8, 1), (10, 6), (15, 6), (11, 9), (13, 15), (8, 11), (3, 15), (5, 9), (1, 6), (6, 6)], fill=hexc("#B8743A"), outline=hexc("#8A5428"))
    cd.point((8, 7), fill=SNOW)
    cd.point((6, 9), fill=RED_L)
    cd.point((10, 9), fill=hexc("#5CFF7A"))
    save(ck, f"{tx}/item/cookie.png")
    # hotbar 182x22: pine glass + gold border + candy stripe accents
    hb = Image.new("RGBA", (182, 22), (0, 0, 0, 0))
    hd = ImageDraw.Draw(hb)
    hd.rectangle([0, 0, 181, 21], fill=hexc("#14240C", 215))
    hd.rectangle([0, 0, 181, 21], outline=hexc("#C99E02"))
    for i in range(9):
        x = 1 + i * 20
        hd.rectangle([x + 1, 2, x + 18, 19], outline=hexc("#2E4A22"))
    for x in range(1, 181):
        if (x // 3) % 2 == 0:
            hb.putpixel((x, 1), RED)
        else:
            hb.putpixel((x, 1), SNOW)
    save(hb, f"{tx}/gui/sprites/hud/hotbar.png")
    sel = Image.new("RGBA", (24, 23), (0, 0, 0, 0))
    sdd = ImageDraw.Draw(sel)
    for i in range(24 * 2 + 23 * 2):
        pass
    for x in range(24):
        for y in range(23):
            edge = x in (0, 1, 22, 23) or y in (0, 1, 21, 22)
            if edge:
                sel.putpixel((x, y), RED if ((x + y) // 2) % 2 == 0 else SNOW)
    sdd.rectangle([2, 2, 21, 20], outline=hexc("#FDE68A"))
    save(sel, f"{tx}/gui/sprites/hud/hotbar_selection.png")
    xb = Image.new("RGBA", (182, 5), hexc("#14240C", 220))
    ImageDraw.Draw(xb).rectangle([0, 0, 181, 4], outline=hexc("#0A1206"))
    save(xb, f"{tx}/gui/sprites/hud/experience_bar_background.png")
    xp = Image.new("RGBA", (182, 5))
    for x in range(182):
        for y in range(5):
            xp.putpixel((x, y), (RED if ((x + y) // 3) % 2 == 0 else SNOW) if 0 < y < 4 else hexc("#7A0E1E"))
    save(xp, f"{tx}/gui/sprites/hud/experience_bar_progress.png")
    pk = build_icon(64)
    save(pk, os.path.join(p1, "pack.png"))

    # ---- Pack 2: Jaly Winter Wonderland (off by default)
    p2 = os.path.join(base, "jaly_winter")
    os.makedirs(p2, exist_ok=True)
    with open(os.path.join(p2, "pack.mcmeta"), "w") as f:
        json.dump({"pack": {"pack_format": 34, "description": "§bJaly §fWinter Wonderland §7- snowy grass, frosted leaves"}}, f, indent=2)
    mdl = os.path.join(p2, "assets/minecraft/models/block")
    bs = os.path.join(p2, "assets/minecraft/blockstates")
    os.makedirs(mdl, exist_ok=True)
    os.makedirs(bs, exist_ok=True)
    with open(os.path.join(mdl, "jaly_snowy_grass.json"), "w") as f:
        json.dump({"parent": "minecraft:block/cube_bottom_top", "textures": {"top": "minecraft:block/snow", "bottom": "minecraft:block/dirt", "side": "minecraft:block/grass_block_snow", "particle": "minecraft:block/snow"}}, f, indent=2)
    var = [{"model": "minecraft:block/jaly_snowy_grass"}, {"model": "minecraft:block/jaly_snowy_grass", "y": 90}, {"model": "minecraft:block/jaly_snowy_grass", "y": 180}, {"model": "minecraft:block/jaly_snowy_grass", "y": 270}]
    with open(os.path.join(bs, "grass_block.json"), "w") as f:
        json.dump({"variants": {"snowy=false": var, "snowy=true": {"model": "minecraft:block/grass_block_snow"}}}, f, indent=2)
    leaf_cols = {"oak": "#3E8E3A", "spruce": "#2F5E3A", "birch": "#6FA34E", "jungle": "#2F8F2A", "acacia": "#5C9E2E", "dark_oak": "#2E6B26", "mangrove": "#4E8F2C"}
    for name, col in leaf_cols.items():
        r = random.Random(name)
        img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
        px = img.load()
        base_c = hexc(col)
        for y in range(16):
            for x in range(16):
                v = r.random()
                if v < 0.12:
                    continue  # holes like fancy leaves
                c = shade(base_c, 0.75 + r.random() * 0.45)
                if y < 5 and r.random() < 0.55 - y * 0.1:
                    c = hexc("#F4F8FF") if r.random() < 0.7 else hexc("#DCE8F5")
                elif r.random() < 0.08:
                    c = hexc("#EEF4FB")
                px[x, y] = c
        save(img, f"{p2}/assets/minecraft/textures/block/jaly_frosty_{name}_leaves.png")
        with open(os.path.join(mdl, f"{name}_leaves.json"), "w") as f:
            json.dump({"parent": "minecraft:block/cube_all", "textures": {"all": f"minecraft:block/jaly_frosty_{name}_leaves"}}, f, indent=2)
    pk2 = build_icon(64)
    pk2 = pk2.convert("RGBA")
    ImageDraw.Draw(pk2).rectangle([0, 0, 63, 63], outline=hexc("#9BC3E7"), width=3)
    save(pk2, os.path.join(p2, "pack.png"))


# ---------------------------------------------------------------- wings (64x32, one 12x16x1 cuboid per wing at uv 0,0)
WINGS = {"angel": "Angel Wings", "candy": "Candy Wings", "ice": "Ice Wings", "fairy": "Fairy Light Wings", "gold": "Golden Wings"}


def wing_face(kind):
    img = Image.new("RGBA", (12, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    rnd = random.Random(kind)
    shape = [(0, 0), (11, 2), (11, 6), (9, 9), (10, 12), (6, 15), (3, 13), (0, 8)]
    base = {"angel": "#F4F6FF", "candy": "#F6F4F7", "ice": "#BFE3FF", "fairy": "#2E7D32", "gold": "#FFD94A"}[kind]
    d.polygon(shape, fill=hexc(base))
    px = img.load()
    for y in range(16):
        for x in range(12):
            if px[x, y][3] == 0:
                continue
            c = hexc(base)
            if kind == "candy" and ((x + y) // 2) % 2 == 0:
                c = RED
            if kind in ("angel", "gold") and (x + y * 2) % 5 == 0:
                c = shade(c, 0.85)
            if kind == "ice":
                c = mix(hexc("#E8F6FF"), hexc("#6FB6EE"), x / 12)
                if rnd.random() < 0.12:
                    c = SNOW
            if kind == "fairy" and rnd.random() < 0.18:
                c = rnd.choice([RED_L, GOLD_L, hexc("#4CC3FF"), SNOW])
            px[x, y] = c
    d.line(shape + [shape[0]], fill=shade(hexc(base), 0.7) if kind != "ice" else hexc("#5AA0D8"))
    return img


def build_wings():
    for wid in WINGS:
        img = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
        f = wing_face(wid)
        img.paste(f, (1, 1))
        img.paste(f.transpose(Image.FLIP_LEFT_RIGHT), (14, 1))
        edge = f.getpixel((5, 8))
        for x in range(1, 13):
            img.putpixel((x, 0), edge)
            img.putpixel((x + 12, 0), edge)
        for y in range(1, 17):
            img.putpixel((0, y), edge)
            img.putpixel((13, y), edge)
        save(img, f"{TEX}/wings/{wid}.png")
        icon = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
        big = f.resize((12, 16)).resize((14, 19), Image.NEAREST)
        icon.paste(big.transpose(Image.FLIP_LEFT_RIGHT), (2, 6), big.transpose(Image.FLIP_LEFT_RIGHT))
        icon.paste(big, (16, 6), big)
        save(icon, f"{TEX}/gui/wings_{wid}.png")


TRAILS = {"snow": ("#F4F8FF", "#9BC3E7"), "candy": ("#E0384F", "#F6F4F7"), "hearts": ("#FF5C8A", "#FFC2D4"), "notes": ("#5CFF7A", "#4CC3FF"),
          "gold": ("#FFD94A", "#FFF2B3"), "stars": ("#FFFFFF", "#FDE68A"), "aurora": ("#3DFFB0", "#B35CFF"), "cherry": ("#FFB7D5", "#FF8FBF"),
          "embers": ("#FF8A1F", "#FFD34A"), "totem": ("#5CFF7A", "#FFD94A")}


def build_trail_icons():
    for tid, (a, b) in TRAILS.items():
        r = random.Random(tid)
        img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
        d = ImageDraw.Draw(img)
        for i in range(9):
            x, y = 4 + i * 3 + r.randint(-1, 1), 24 - i * 2 + r.randint(-2, 2)
            s2 = 1 + i // 3
            d.rectangle([x, y, x + s2, y + s2], fill=hexc(a if i % 2 else b))
        save(img, f"{TEX}/gui/trail_{tid}.png")


def heart(c1, c2, shine):
    rows = [".##.##.", "#######", "#######", ".#####.", "..###..", "...#..."]
    img = Image.new("RGBA", (9, 9), (0, 0, 0, 0))
    px = img.load()
    for y in range(9):
        for x in range(9):
            pass
    out = ["..##.##..", ".#AA#AA#.", "#ASAAAAA#", "#AAAAAAA#", ".#AAAAA#.", "..#AAA#..", "...#A#...", "....#....", "........."]
    pal = {"#": c2, "A": c1, "S": shine}
    for y, row in enumerate(out):
        for x, ch in enumerate(row):
            if ch in pal:
                px[x, y] = pal[ch]
    half = img.copy()
    hp = half.load()
    for y in range(9):
        for x in range(5, 9):
            hp[x, y] = (0, 0, 0, 0)
    return img, half


def build_more_packs():
    base = os.path.join(RES, "resourcepacks")
    specs = {
        "jaly_frost": ("§bJaly §fFrost PvP §7- icy swords, ice hearts, frozen hotbar",
                       {"wooden": ("#A7D3F2", "#EAF6FF"), "stone": ("#8FB8DA", "#DDEFFF"), "iron": ("#CFE8FF", "#7FB6E8"), "golden": ("#BFE3FF", "#FFE680"),
                        "diamond": ("#7FDFFF", "#FFFFFF"), "netherite": ("#5B7FB0", "#BFE3FF")},
                       ("#9BE1FF", "#2B6A9E", "#FFFFFF"), ("#0E2236", "#9BC3E7"), ("#7FDFFF", "#FFFFFF")),
        "jaly_candy": ("§dJaly §fCandy Land §7- lollipop swords, candy hearts, sweet items",
                       {"wooden": ("#FF9EC7", "#FFFFFF"), "stone": ("#B7A5FF", "#FFFFFF"), "iron": ("#7FF0C8", "#FFFFFF"), "golden": ("#FFD94A", "#FF9EC7"),
                        "diamond": ("#FF5CA8", "#FFFFFF"), "netherite": ("#A05CFF", "#FFC2E6")},
                       ("#FF6FAE", "#B0236A", "#FFFFFF"), ("#3A1630", "#FF9EC7"), ("#FF9EC7", "#7FF0C8")),
    }
    for key, (desc, swords, hcol, hotbar, xpcols) in specs.items():
        p = os.path.join(base, key)
        tx = os.path.join(p, "assets/minecraft/textures")
        os.makedirs(p, exist_ok=True)
        with open(os.path.join(p, "pack.mcmeta"), "w") as f:
            json.dump({"pack": {"pack_format": 34, "description": desc}}, f, indent=2)
        for k, (a, b) in swords.items():
            save(candy_sword(hexc(a), hexc(b), hexc("#C99E02") if key == "jaly_candy" else hexc("#5AA0D8"), hexc("#3D2614") if key == "jaly_frost" else hexc("#FFFFFF")), f"{tx}/item/{k}_sword.png")
        full, half = heart(hexc(hcol[0]), hexc(hcol[1]), hexc(hcol[2]))
        for n in ("full", "full_blinking", "hardcore_full", "hardcore_full_blinking"):
            save(full, f"{tx}/gui/sprites/hud/heart/{n}.png")
        for n in ("half", "half_blinking", "hardcore_half", "hardcore_half_blinking"):
            save(half, f"{tx}/gui/sprites/hud/heart/{n}.png")
        hb = Image.new("RGBA", (182, 22), (0, 0, 0, 0))
        hd = ImageDraw.Draw(hb)
        hd.rectangle([0, 0, 181, 21], fill=hexc(hotbar[0], 215))
        hd.rectangle([0, 0, 181, 21], outline=hexc(hotbar[1]))
        for i in range(9):
            hd.rectangle([2 + i * 20, 2, 19 + i * 20, 19], outline=shade(hexc(hotbar[1]), 0.6))
        save(hb, f"{tx}/gui/sprites/hud/hotbar.png")
        sel = Image.new("RGBA", (24, 23), (0, 0, 0, 0))
        ImageDraw.Draw(sel).rectangle([0, 0, 23, 22], outline=hexc(hotbar[1]), width=2)
        ImageDraw.Draw(sel).rectangle([2, 2, 21, 20], outline=hexc("#FFFFFF"))
        save(sel, f"{tx}/gui/sprites/hud/hotbar_selection.png")
        xp = Image.new("RGBA", (182, 5))
        for x in range(182):
            for y in range(5):
                xp.putpixel((x, y), hexc(xpcols[0] if ((x + y) // 3) % 2 == 0 else xpcols[1]) if 0 < y < 4 else hexc(hotbar[0]))
        save(xp, f"{tx}/gui/sprites/hud/experience_bar_progress.png")
        items = {}
        if key == "jaly_frost":
            items["golden_apple"] = ("ellipse", "#BFE3FF", "#FFFFFF")
            items["ender_pearl"] = ("ellipse", "#6FB6EE", "#E8F6FF")
            items["totem_of_undying"] = ("crystal", "#9BE1FF", "#FFFFFF")
        else:
            items["apple"] = ("ellipse", "#E0384F", "#FFFFFF")
            items["golden_apple"] = ("ellipse", "#D98A2B", "#FFE0A0")
            items["snowball"] = ("ellipse", "#FFF2F7", "#FFC2D4")
            items["totem_of_undying"] = ("crystal", "#FF9EC7", "#FFFFFF")
        for name, (kind, c1, c2) in items.items():
            im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
            d = ImageDraw.Draw(im)
            if kind == "ellipse":
                d.ellipse([2, 3, 13, 14], fill=hexc(c1), outline=shade(hexc(c1), 0.7))
                d.ellipse([4, 5, 7, 8], fill=hexc(c2))
                if name in ("apple", "golden_apple") and key == "jaly_candy":
                    d.line([(8, 0), (8, 4)], fill=hexc("#C8A26A"), width=2)
            else:
                d.polygon([(8, 0), (14, 6), (8, 15), (2, 6)], fill=hexc(c1), outline=shade(hexc(c1), 0.7))
                d.line([(8, 2), (8, 12)], fill=hexc(c2))
            save(im, f"{tx}/item/{name}.png")
        icon = build_icon(64).convert("RGBA")
        ImageDraw.Draw(icon).rectangle([0, 0, 63, 63], outline=hexc(hotbar[1]), width=3)
        save(icon, os.path.join(p, "pack.png"))
        save(icon, f"{TEX}/gui/pack_{key}.png")
    for k in ("jaly_christmas", "jaly_winter"):
        Image.open(os.path.join(base, k, "pack.png")).save(f"{TEX}/gui/pack_{k}.png")


# ---------------------------------------------------------------- installer bitmaps
def build_installer(logo):
    out = os.path.join(ROOT, "installer")
    os.makedirs(out, exist_ok=True)
    big = Image.new("RGB", (164, 314))
    px = big.load()
    for y in range(314):
        c = mix(hexc("#060B1F"), hexc("#2B4A80"), y / 314)
        for x in range(164):
            px[x, y] = c[:3]
    d = ImageDraw.Draw(big)
    r = random.Random(3)
    for _ in range(90):
        d.point((r.randint(0, 163), r.randint(0, 313)), fill=(255, 255, 255))
    d.rectangle([0, 270, 163, 313], fill=(238, 243, 248))
    for x0 in (8, 40, 110, 140):
        d.polygon([(x0, 272), (x0 + 10, 236), (x0 + 20, 272)], fill=(22, 56, 36))
    ic = build_icon(96).convert("RGBA")
    big.paste(ic, (34, 60), ic)
    lg = logo.resize((logo.width // 2 * 2 // 2, logo.height // 2 * 2 // 2), Image.NEAREST) if logo.width > 160 else logo
    lg = logo.resize((150, int(logo.height * 150 / logo.width)), Image.NEAREST)
    big.paste(lg, (7, 170), lg)
    big.save(os.path.join(out, "wizard.bmp"))
    small = build_icon(55).convert("RGB")
    small.save(os.path.join(out, "wizard_small.bmp"))
    build_icon(256).save(os.path.join(out, "icon.ico"), sizes=[(16, 16), (24, 24), (32, 32), (48, 48), (64, 64), (128, 128), (256, 256)])


def main():
    os.makedirs(JAVA, exist_ok=True)
    build_hats()
    build_capes()
    logo = build_logo()
    meta = {"title": build_title(), "lodge": build_lodge()}
    icon = build_icon(128)
    save(icon, f"{ASSETS}/icon.png")
    build_packs()
    build_wings()
    build_trail_icons()
    build_more_packs()
    build_installer(logo)
    ui = os.path.join(ROOT, "launcher/ui")
    os.makedirs(ui, exist_ok=True)
    icon.save(os.path.join(ui, "icon.png"))
    logo.resize((logo.width * 2, logo.height * 2), Image.NEAREST).save(os.path.join(ui, "logo.png"))
    for n in ("sky", "aurora", "far", "mid", "near", "sleigh"):
        Image.open(f"{TEX}/gui/title/{n}.png").save(os.path.join(ui, f"title_{n}.png"))
    with open(os.path.join(ROOT, "tools/art_meta.json"), "w") as f:
        json.dump(meta, f)
    print("art done", {k: {kk: len(vv) for kk, vv in v.items()} for k, v in meta.items()})


if __name__ == "__main__":
    main()
