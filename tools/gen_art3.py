"""JalyClient 1.3 art: animated capes, more wings, OG items, logo glyph, gear, PvP + Crystal packs, 3D item models."""
import json, math, os, random, sys
sys.path.insert(0, os.path.dirname(__file__))
from gen_art import *  # noqa
from PIL import Image, ImageDraw

# ---------- more capes (static + animated frames _f0.._f7)
def cape_img(draw_fn, name):
    S = 4
    img = Image.new("RGBA", (64 * S, 32 * S), (0, 0, 0, 0))
    design = Image.new("RGBA", (10 * S, 16 * S)); draw_fn(design)
    ely = Image.new("RGBA", (24 * S, 22 * S)); draw_fn(ely)
    edge = shade(design.getpixel((0, 0)), 0.7)
    ImageDraw.Draw(img).rectangle([0, 0, 22 * S - 1, 17 * S - 1], fill=edge)
    img.paste(design, (S, S)); img.paste(design.transpose(Image.FLIP_LEFT_RIGHT), (12 * S, S)); img.paste(ely, (22 * S, 0))
    save(img, f"{TEX}/cape/{name}.png")

def fill(im, fn):
    px = im.load()
    for y in range(im.height):
        for x in range(im.width):
            px[x, y] = fn(x, y)

def holly(im):
    fill(im, lambda x, y: hexc("#1E5E28") if (x // 4 + y // 4) % 2 else hexc("#2E7D32"))
    d = ImageDraw.Draw(im); r = random.Random(5)
    for _ in range(im.width * im.height // 120):
        x, y = r.randint(2, im.width - 4), r.randint(2, im.height - 4)
        d.ellipse([x, y, x + 3, y + 3], fill=RED_L)

def fireplace(im):
    fill(im, lambda x, y: mix(hexc("#2A0E06"), hexc("#7A2A0E"), y / im.height))
    d = ImageDraw.Draw(im); r = random.Random(9)
    for i in range(im.width // 3):
        x = r.randint(0, im.width); h = r.randint(im.height // 4, im.height // 2)
        d.polygon([(x - 3, im.height), (x, im.height - h), (x + 3, im.height)], fill=r.choice([hexc("#F2771C"), hexc("#FFD34A"), hexc("#C23B14")]))

def og(im):
    fill(im, lambda x, y: mix(hexc("#1A0A00"), hexc("#3A1A05"), y / im.height))
    d = ImageDraw.Draw(im); w, h = im.size
    d.rectangle([1, 1, w - 2, h - 2], outline=hexc("#FFD94A"), width=2)
    # big "OG"
    cx, cy, s = w // 2, h // 2, max(2, w // 14)
    d.ellipse([cx - 4 * s - s, cy - 3 * s, cx - s, cy + 3 * s], outline=hexc("#FFD94A"), width=s)
    d.arc([cx + s, cy - 3 * s, cx + 5 * s, cy + 3 * s], 40, 330, fill=hexc("#FFD94A"), width=s)
    d.line([(cx + 3 * s, cy), (cx + 5 * s, cy)], fill=hexc("#FFD94A"), width=s)

for n, f in (("holly", holly), ("fireplace", fireplace), ("og", og)):
    cape_img(f, n)

def anim_cape(name, frame_fn, frames=8):
    for i in range(frames):
        cape_img(lambda im, i=i: frame_fn(im, i / frames), f"{name}_f{i}")
    save(Image.open(f"{TEX}/cape/{name}_f0.png"), f"{TEX}/cape/{name}.png")

anim_cape("candy_flow", lambda im, t: fill(im, lambda x, y: RED if ((x + y + int(t * 12)) // 6) % 2 == 0 else SNOW))
def aurora_frame(im, t):
    w, h = im.size
    fill(im, lambda x, y: mix(hexc("#060B1E"), hexc("#13244A"), y / h))
    px = im.load()
    for band, col in ((0, (61, 255, 176)), (1, (61, 200, 255)), (2, (179, 92, 255))):
        for x in range(w):
            yc = h * 0.3 + band * h * 0.12 + math.sin(x * 0.2 + band + t * 6.283) * h * 0.06
            for y in range(h):
                dy = abs(y - yc)
                if dy < h * 0.1:
                    px[x, y] = mix(px[x, y], col + (255,), (1 - dy / (h * 0.1)) * 0.7)
anim_cape("aurora_flow", aurora_frame)
def snow_frame(im, t):
    w, h = im.size
    fill(im, lambda x, y: mix(hexc("#1B2D5A"), hexc("#3A5A94"), y / h))
    r = random.Random(3); px = im.load()
    for _ in range(40):
        x, y0 = r.randint(0, w - 2), r.randint(0, h)
        y = int((y0 + t * h) % h)
        for dx in (0, 1):
            for dy in (0, 1):
                if x + dx < w and y + dy < h:
                    px[x + dx, y + dy] = SNOW
anim_cape("snowfall", snow_frame)

# ---------- wings: dragon, butterfly, crystal, krampus, og
def wing_shape(kind):
    img = Image.new("RGBA", (12, 16), (0, 0, 0, 0)); d = ImageDraw.Draw(img); r = random.Random(kind); px = img.load()
    if kind == "dragon":
        d.polygon([(0, 0), (11, 1), (11, 4), (8, 6), (10, 9), (6, 10), (8, 13), (3, 15), (0, 10)], fill=hexc("#7A1424"))
        for x0 in (2, 5, 8):
            d.line([(0, 0), (x0 + 3, 15 - x0)], fill=hexc("#3A0A12"))
    elif kind == "butterfly":
        d.ellipse([0, 0, 11, 9], fill=hexc("#FF8FC7")); d.ellipse([1, 8, 9, 15], fill=hexc("#9B6BFF"))
        d.ellipse([4, 2, 7, 5], fill=hexc("#FFF2B3")); d.ellipse([3, 10, 5, 12], fill=hexc("#FFFFFF"))
        for y in range(16):
            for x in range(12):
                if px[x, y][3]:
                    px[x, y] = px[x, y][:3] + (200,)
    elif kind == "crystal":
        for i, (x0, y0, x1, y1) in enumerate([(0, 0, 4, 15), (3, 1, 7, 12), (6, 2, 11, 9)]):
            d.polygon([(x0, y0), (x1, (y0 + y1) // 2), (x0 + 1, y1)], fill=hexc(["#BFE3FF", "#9BD3FF", "#E8F6FF"][i]), outline=hexc("#5AA0D8"))
    elif kind == "krampus":
        d.polygon([(0, 0), (11, 3), (9, 5), (11, 8), (8, 9), (10, 13), (5, 12), (2, 15), (0, 9)], fill=hexc("#1C1C22"))
        for x, y in ((10, 3), (10, 8), (9, 13)):
            px[x, y] = RED_L
    elif kind == "og":
        d.polygon([(0, 0), (11, 2), (11, 6), (9, 9), (10, 12), (6, 15), (3, 13), (0, 8)], fill=hexc("#FFD94A"))
        for y in range(16):
            for x in range(12):
                if px[x, y][3] and (x + y) % 4 == 0:
                    px[x, y] = hexc("#B8860B")
    return img

for wid in ("dragon", "butterfly", "crystal", "krampus", "og"):
    img = Image.new("RGBA", (64, 32), (0, 0, 0, 0)); f = wing_shape(wid)
    img.paste(f, (1, 1)); img.paste(f.transpose(Image.FLIP_LEFT_RIGHT), (14, 1))
    save(img, f"{TEX}/wings/{wid}.png")
    icon = Image.new("RGBA", (32, 32), (0, 0, 0, 0)); big = f.resize((14, 19), Image.NEAREST)
    icon.paste(big.transpose(Image.FLIP_LEFT_RIGHT), (2, 6), big.transpose(Image.FLIP_LEFT_RIGHT)); icon.paste(big, (16, 6), big)
    save(icon, f"{TEX}/gui/wings_{wid}.png")

# ---------- trail icon og
img = Image.new("RGBA", (32, 32), (0, 0, 0, 0)); d = ImageDraw.Draw(img)
for i in range(9):
    d.rectangle([4 + i * 3, 24 - i * 2, 6 + i * 3, 26 - i * 2], fill=hexc("#FFD94A" if i % 2 else "#E0384F"))
save(img, f"{TEX}/gui/trail_og.png")

# ---------- logo glyph for names (8x8) + gear icons
g = Image.new("RGBA", (8, 8), (0, 0, 0, 0)); gp = g.load()
J = ["..####.", "....#..", "....#..", "....#..", "#...#..", "#...#..", ".###...", "......."]
for y, row in enumerate(J):
    for x, ch in enumerate(row):
        if ch == "#":
            gp[x, y] = RED if (x + y) % 2 == 0 else SNOW
save(g, os.path.join(ASSETS, "textures/font/logo.png"))
os.makedirs(os.path.join(ASSETS, "font"), exist_ok=True)
json.dump({"providers": [{"type": "bitmap", "file": "jalyclient:font/logo.png", "ascent": 7, "height": 8, "chars": [""]}]}, open(os.path.join(ASSETS, "font/logo.json"), "w"))
gear = Image.new("RGBA", (20, 20), (0, 0, 0, 0)); gd = ImageDraw.Draw(gear)
for a in range(8):
    ang = a * math.pi / 4
    x, y = 10 + math.cos(ang) * 7.5, 10 + math.sin(ang) * 7.5
    gd.rectangle([x - 2, y - 2, x + 1, y + 1], fill=hexc("#B11A33"))
gd.ellipse([3, 3, 16, 16], fill=hexc("#B11A33"), outline=hexc("#7A0E1E"))
gd.ellipse([7, 7, 12, 12], fill=hexc("#F6F4F7"))
gd.ellipse([1, 1, 7, 5], fill=hexc("#2E7D32")); gd.ellipse([13, 1, 19, 5], fill=hexc("#2E7D32")); gd.rectangle([9, 2, 10, 3], fill=hexc("#E0384F"))
gd.rectangle([4, 15, 15, 17], fill=hexc("#F6F4F7"))
save(gear, f"{TEX}/gui/gear.png")
print("art3 ok")
