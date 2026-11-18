"""Shop room, coin icon, premium wings/capes, icons."""
import math, os, random, sys
sys.path.insert(0, os.path.dirname(__file__))
from gen_art import *  # noqa
from gen_art3 import cape_img, fill, wing_shape  # noqa
from PIL import Image, ImageDraw

W, H = 360, 180
r = random.Random(4)
img = Image.new("RGBA", (W, H)); d = ImageDraw.Draw(img)
for x in range(0, W, 12):  # dark plank wall
    d.rectangle([x, 0, x + 11, 132], fill=hexc("#3A2414") if (x // 12) % 2 else hexc("#43291A")); d.line([(x, 0), (x, 132)], fill=hexc("#2A190E"))
for y in range(132, H, 6):
    d.rectangle([0, y, W, y + 5], fill=hexc("#5C3A20") if (y // 6) % 2 else hexc("#53341C")); d.line([(0, y), (W, y)], fill=hexc("#3E2614"))
# shelves with presents and coin jars
for sx, sy in ((10, 40), (10, 82), (262, 40), (262, 82)):
    d.rectangle([sx, sy, sx + 88, sy + 4], fill=hexc("#7A4E2C"))
    x = sx + 4
    while x < sx + 82:
        w = r.randint(9, 16); h = r.randint(10, 20); col = r.choice([RED, hexc("#2E7D32"), hexc("#2C6FB0"), GOLD, hexc("#7B3FA0")])
        if r.random() < 0.3:
            d.rectangle([x, sy - 16, x + 10, sy - 1], fill=hexc("#BFE3FF", 200)); d.ellipse([x + 2, sy - 9, x + 8, sy - 3], fill=hexc("#FFD94A"))
            w = 10
        else:
            d.rectangle([x, sy - h, x + w, sy - 1], fill=col); d.rectangle([x + w // 2 - 1, sy - h, x + w // 2, sy - 1], fill=GOLD_L)
        x += w + 3
# hanging sign
d.line([(150, 0), (150, 2)], fill=hexc("#2A2A30")); d.line([(210, 0), (210, 2)], fill=hexc("#2A2A30"))
d.rectangle([130, 2, 230, 21], fill=hexc("#7A0E1E")); d.rectangle([132, 4, 228, 19], outline=hexc("#FFD94A"))
GL = {"J": ["..##", "...#", "...#", "#..#", ".##."], "O": [".##.", "#..#", "#..#", "#..#", ".##."], "L": ["#...", "#...", "#...", "#...", "####"],
      "Y": ["#..#", "#..#", ".##.", "..#.", "..#."], "S": [".###", "#...", ".##.", "...#", "###."], "H": ["#..#", "#..#", "####", "#..#", "#..#"], "P": ["###.", "#..#", "###.", "#...", "#..."], " ": ["....", "....", "....", "....", "...."]}
x = 140
for ch in "JOLLY SHOP":
    for yy, row in enumerate(GL[ch]):
        for xx, c in enumerate(row):
            if c == "#":
                d.rectangle([x + xx * 2, 7 + yy * 2, x + xx * 2 + 1, 8 + yy * 2], fill=hexc("#FFD94A"))
    x += 9
# counter
d.rectangle([96, 112, 264, 150], fill=hexc("#6B4426")); d.rectangle([92, 106, 268, 113], fill=hexc("#8C5B32"))
for x in range(100, 262, 16):
    d.rectangle([x, 118, x + 12, 146], outline=hexc("#5A381F"))
d.rectangle([228, 92, 254, 106], fill=hexc("#C99E02")); d.rectangle([232, 86, 250, 92], fill=hexc("#E8C840")); d.rectangle([236, 95, 246, 100], fill=hexc("#1A1A1A"))
d.ellipse([104, 90, 122, 106], fill=hexc("#BFE3FF", 210))
for i in range(6):
    d.rectangle([107 + i * 2, 96 + (i % 3), 108 + i * 2, 97 + (i % 3)], fill=[RED, SNOW][i % 2])
for i in range(5):  # coin stack
    d.ellipse([132, 100 - i * 2, 146, 106 - i * 2], fill=hexc("#FFD94A"), outline=hexc("#B8860B"))
save(img, f"{TEX}/gui/shop.png")
# coin icon 16x16
c = Image.new("RGBA", (16, 16), (0, 0, 0, 0)); cd = ImageDraw.Draw(c)
cd.ellipse([0, 0, 15, 15], fill=hexc("#FFD94A"), outline=hexc("#B8860B")); cd.ellipse([3, 3, 12, 12], outline=hexc("#E8B400"))
for xx, yy in ((9, 4), (9, 5), (9, 6), (9, 7), (9, 8), (8, 10), (7, 10), (6, 9)):
    cd.point((xx, yy), fill=hexc("#B11A33"))
save(c, f"{TEX}/gui/coin.png")
Image.new("RGBA", (1, 1), (255, 255, 255, 255)).save(f"{TEX}/gui/white.png")

# premium capes (animated where marked)
def starlight(im, t=0):
    fill(im, lambda x, y: mix(hexc("#0A0A2A"), hexc("#2A1A5A"), y / im.height)); px = im.load(); rr = random.Random(2)
    for i in range(50):
        x, y = rr.randint(0, im.width - 1), rr.randint(0, im.height - 1)
        if (i + int(t * 8)) % 3:
            px[x, y] = SNOW
            if x + 1 < im.width: px[x + 1, y] = hexc("#FFF2B3")
def velvet(im):
    fill(im, lambda x, y: mix(hexc("#5A0A1A"), hexc("#8E1B2C"), (math.sin(x * 0.3) + 1) / 2))
    dd = ImageDraw.Draw(im); dd.rectangle([0, 0, im.width - 1, im.height - 1], outline=hexc("#FFD94A"), width=2)
    for y in range(6, im.height, 10):
        dd.line([(2, y), (im.width - 3, y)], fill=hexc("#C99E02"))
def ice_king(im, t=0):
    fill(im, lambda x, y: mix(hexc("#E8F6FF"), hexc("#5AA0D8"), y / im.height))
    dd = ImageDraw.Draw(im); w, h = im.size
    sx = int((t * 1.6 - 0.3) * (w + h))
    for k in range(4):
        dd.line([(sx + k - h, h), (sx + k, 0)], fill=(255, 255, 255, 200))
    dd.polygon([(w // 2, 6), (w // 2 + 6, 14), (w // 2, 22), (w // 2 - 6, 14)], fill=hexc("#4CC3FF"))
cape_img(velvet, "royal_velvet")
from gen_art3 import anim_cape
anim_cape("starlight", lambda im, t: starlight(im, t))
anim_cape("ice_king", lambda im, t: ice_king(im, t))

# premium wings
def prem_wing(kind):
    im = Image.new("RGBA", (12, 16), (0, 0, 0, 0)); dd = ImageDraw.Draw(im); px = im.load()
    shape = [(0, 0), (11, 1), (11, 5), (9, 7), (11, 10), (7, 12), (8, 15), (2, 14), (0, 9)]
    dd.polygon(shape, fill=(255, 255, 255, 255))
    for y in range(16):
        for x in range(12):
            if not px[x, y][3]:
                continue
            if kind == "frost_dragon":
                px[x, y] = mix(hexc("#E8F6FF"), hexc("#3A7AC8"), x / 12) if (x + y) % 3 else hexc("#FFFFFF")
            elif kind == "aurora_phoenix":
                px[x, y] = mix(hexc("#3DFFB0"), hexc("#B35CFF"), (x + y) / 26)
            else:
                px[x, y] = hexc("#FFD94A") if (x * 2 + y) % 5 else hexc("#FFF2B3")
    dd.line(shape + [shape[0]], fill={"frost_dragon": hexc("#1E4A8A"), "aurora_phoenix": hexc("#2A1A5A"), "royal_gold": hexc("#8A6A00")}[kind])
    return im
for wid in ("frost_dragon", "aurora_phoenix", "royal_gold"):
    out = Image.new("RGBA", (64, 32), (0, 0, 0, 0)); f = prem_wing(wid)
    out.paste(f, (1, 1)); out.paste(f.transpose(Image.FLIP_LEFT_RIGHT), (14, 1)); save(out, f"{TEX}/wings/{wid}.png")
    icon = Image.new("RGBA", (32, 32), (0, 0, 0, 0)); big = f.resize((14, 19), Image.NEAREST)
    icon.paste(big.transpose(Image.FLIP_LEFT_RIGHT), (2, 6), big.transpose(Image.FLIP_LEFT_RIGHT)); icon.paste(big, (16, 6), big); save(icon, f"{TEX}/gui/wings_{wid}.png")
for tid, (a, b) in {"fireworks": ("#FF4FA0", "#FFD94A"), "galaxy": ("#9B6BFF", "#4CC3FF")}.items():
    im = Image.new("RGBA", (32, 32), (0, 0, 0, 0)); dd = ImageDraw.Draw(im)
    for i in range(10):
        dd.rectangle([3 + i * 3, 25 - i * 2, 5 + i * 3, 27 - i * 2], fill=hexc(a if i % 2 else b))
    save(im, f"{TEX}/gui/trail_{tid}.png")
print("shop art ok")
