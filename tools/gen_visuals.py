"""Visuals: tall smooth grass packs, rain packs, glass rain/frost textures, custom disc icons."""
import json, math, os, random
from PIL import Image, ImageDraw

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(ROOT, "mod/src/main/resources")
ASSETS = os.path.join(RES, "assets/jalyclient")


def save(img, path):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    img.save(path)


def wjson(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as f:
        json.dump(obj, f, indent=1)


def mcmeta(name, desc):
    wjson(os.path.join(RES, "resourcepacks", name, "pack.mcmeta"), {"pack": {"pack_format": 34, "description": desc}})


# ---------------------------------------------------------------- grass
def grass_frames(seed, blades=9, frames=16, tall=False, top=False):
    rnd = random.Random(seed)
    spec = []
    for i in range(blades):
        x = 1 + (14 * i) / (blades - 1) + rnd.uniform(-0.6, 0.6)
        h = rnd.uniform(9, 15.5) if not top else rnd.uniform(6, 15)
        if tall:
            h = min(15.8, h + 2)
        spec.append((x, h, rnd.uniform(0, math.tau), 1, rnd.choice([-1, 1]) * rnd.uniform(0.2, 1.0)))
    strip = Image.new("RGBA", (16, 16 * frames), (0, 0, 0, 0))
    for f in range(frames):
        ph = math.tau * f / frames
        img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
        px = img.load()
        for (x0, h, bp, spd, lean) in sorted(spec, key=lambda s: s[1]):
            steps = int(h * 3)
            for k in range(steps + 1):
                fr = k / steps
                yy = 15.99 - fr * h if not top else 15.99 - fr * h
                sway = 1.7 * fr * fr * math.sin(ph * spd + bp) + lean * fr * fr
                xx = x0 + sway
                ix, iy = int(xx), int(yy)
                if 0 <= ix < 16 and 0 <= iy < 16:
                    g = int(120 + 120 * fr)  # darker at the base, light at the tip (biome-tinted)
                    a = 255 if fr < 0.92 else 200
                    old = px[ix, iy]
                    if old[3] == 0 or old[0] < g:
                        px[ix, iy] = (g, g, g, a)
                    if fr < 0.35 and ix + 1 < 16 and px[ix + 1, iy][3] == 0:  # thicker at the base
                        px[ix + 1, iy] = (g - 15, g - 15, g - 15, 255)
        strip.paste(img, (0, 16 * f))
    return strip


def cross_model(texture, height, extra=True):
    def plane(frm, to, angle, axis_faces):
        faces = {f: {"uv": [0, 0, 16, 16], "texture": "#cross", "tintindex": 0} for f in axis_faces}
        return {"from": frm, "to": to, "rotation": {"origin": [8, height / 2, 8], "axis": "y", "angle": angle, "rescale": True}, "shade": False, "faces": faces}
    els = [plane([0.8, 0, 8], [15.2, height, 8], 45, ["north", "south"]), plane([8, 0, 0.8], [8, height, 15.2], 45, ["west", "east"])]
    if extra:
        els += [plane([1.5, 0, 8], [14.5, height * 0.9, 8], -22.5, ["north", "south"]), plane([8, 0, 1.5], [8, height * 0.85, 14.5], 22.5, ["west", "east"])]
    return {"ambientocclusion": False, "textures": {"particle": texture, "cross": texture}, "elements": els}


def grass_pack(name, height, desc):
    mcmeta(name, desc)
    base = os.path.join(RES, "resourcepacks", name, "assets/minecraft")
    for tex, seed, top in (("short_grass", 1, False), ("fern", 2, False), ("tall_grass_bottom", 3, False), ("tall_grass_top", 4, True)):
        strip = grass_frames(seed, blades=13 if "tall" in tex else 12, tall=height > 22, top=top)
        save(strip, f"{base}/textures/block/{tex}.png")
        wjson(f"{base}/textures/block/{tex}.png.mcmeta", {"animation": {"frametime": 3, "interpolate": True}})
    wjson(f"{base}/models/block/short_grass.json", cross_model("minecraft:block/short_grass", height))
    wjson(f"{base}/models/block/fern.json", cross_model("minecraft:block/fern", height * 0.95))
    wjson(f"{base}/models/block/tall_grass_bottom.json", cross_model("minecraft:block/tall_grass_bottom", 16, extra=True))
    wjson(f"{base}/models/block/tall_grass_top.json", cross_model("minecraft:block/tall_grass_top", min(32, height + 2), extra=True))


grass_pack("jaly_grass", 21, "§aJaly §fTall Smooth Grass §7(Tools > Visuals)")
grass_pack("jaly_grass_xl", 28, "§aJaly §fExtra Tall Grass §7(Tools > Visuals)")
# pack icons
for name, col in (("jaly_grass", (90, 170, 70)), ("jaly_grass_xl", (60, 150, 60)), ("jaly_rain", (110, 150, 200)), ("jaly_rain_blocky", (70, 110, 200))):
    icon = Image.new("RGBA", (64, 64), (14, 26, 12, 255))
    d = ImageDraw.Draw(icon)
    for i in range(14):
        x = 4 + i * 4
        if "grass" in name:
            d.line([(x, 62), (x + random.Random(i).randint(-4, 4), 62 - random.Random(i + 9).randint(20, 50))], fill=col, width=2)
        else:
            y = random.Random(i).randint(0, 50)
            if "blocky" in name:
                d.rectangle([x, y, x + 2, y + 4], fill=col)
            else:
                d.line([(x, y), (x - 2, y + 14)], fill=col, width=1)
    save(icon, os.path.join(RES, "resourcepacks", name, "pack.png"))

# ---------------------------------------------------------------- rain
def rain_texture(style):
    img = Image.new("RGBA", (64, 256), (0, 0, 0, 0))
    px = img.load()
    rnd = random.Random(style)
    if style == "real":
        for _ in range(170):
            x, y, ln = rnd.randint(0, 63), rnd.randint(0, 255), rnd.randint(8, 22)
            a = rnd.randint(55, 125)
            for k in range(ln):
                fa = int(a * (0.35 + 0.65 * k / ln))
                px[x, (y + k) % 256] = (205, 220, 238, fa)
    else:
        for _ in range(90):
            x, y = rnd.randint(0, 62), rnd.randint(0, 252)
            for dx in range(2):
                for dy in range(4):
                    px[x + dx, (y + dy) % 256] = (95, 145, 215, 215)
            px[x, y % 256] = (190, 220, 255, 230)
    return img


mcmeta("jaly_rain", "§9Jaly §fRealistic Rain §7(Tools > Visuals)")
save(rain_texture("real"), os.path.join(RES, "resourcepacks/jaly_rain/assets/minecraft/textures/environment/rain.png"))
mcmeta("jaly_rain_blocky", "§9Jaly §fBlocky Rain §7(Tools > Visuals)")
save(rain_texture("blocky"), os.path.join(RES, "resourcepacks/jaly_rain_blocky/assets/minecraft/textures/environment/rain.png"))

# ---------------------------------------------------------------- glass rain + frost
def drop(px, cx, cy, r, w, h):
    for y in range(int(cy - r - 1), int(cy + r + 2)):
        for x in range(int(cx - r - 1), int(cx + r + 2)):
            d = math.hypot(x - cx, (y - cy) * 0.9)
            if d <= r:
                edge = d / max(r, 0.01)
                a = int(60 + 120 * edge ** 2)
                shade = 235 if (x - cx) < -r * 0.3 and (y - cy) < -r * 0.3 else 190
                px[x % w, y % h] = (shade, min(255, shade + 12), 255, a)


drops = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
p = drops.load()
rnd = random.Random(5)
for _ in range(34):
    drop(p, rnd.uniform(0, 64), rnd.uniform(0, 64), rnd.choice([0.8, 1.2, 1.6, 2.2]), 64, 64)
save(drops, f"{ASSETS}/textures/visual/glass_drops.png")
streaks = Image.new("RGBA", (64, 128), (0, 0, 0, 0))
p = streaks.load()
for i in range(9):
    x = rnd.uniform(2, 62)
    y0 = rnd.uniform(0, 128)
    ln = rnd.uniform(25, 60)
    for k in range(int(ln)):
        xx = x + math.sin(k * 0.15 + i) * 0.8
        p[int(xx) % 64, int(y0 + k) % 128] = (200, 215, 255, int(40 + 50 * k / ln))
    drop(p, x, y0 + ln, 1.8, 64, 128)
save(streaks, f"{ASSETS}/textures/visual/glass_streaks.png")
frost = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
p = frost.load()
for y in range(64):
    for x in range(64):
        e = min(x, y, 63 - x, 63 - y) / 32
        a = max(0, 1 - e * 2.2)
        if a > 0 and rnd.random() < 0.75:
            v = rnd.randint(215, 255)
            p[x, y] = (v, v, 255, int(a * rnd.randint(90, 190)))
d = ImageDraw.Draw(frost)
for _ in range(14):  # frost ferns creeping in from the edges
    side = rnd.randint(0, 3)
    sx, sy = [(rnd.randint(0, 63), 0), (rnd.randint(0, 63), 63), (0, rnd.randint(0, 63)), (63, rnd.randint(0, 63))][side]
    ang = [math.pi / 2, -math.pi / 2, 0, math.pi][side] + rnd.uniform(-0.6, 0.6)
    for k in range(rnd.randint(6, 16)):
        x2, y2 = sx + math.cos(ang) * 2, sy + math.sin(ang) * 2
        d.line([(sx, sy), (x2, y2)], fill=(235, 245, 255, 170))
        if k % 3 == 0:
            d.line([(sx, sy), (sx + math.cos(ang + 0.9) * 3, sy + math.sin(ang + 0.9) * 3)], fill=(235, 245, 255, 130))
        sx, sy = x2, y2
        ang += rnd.uniform(-0.3, 0.3)
save(frost, f"{ASSETS}/textures/visual/glass_frost.png")

# ---------------------------------------------------------------- custom music disc icons
PRESETS = [("red", "#C8102E", "#7A0A1C"), ("candy", "#F6F4F7", "#C8102E"), ("gold", "#FFD94A", "#B8860B"), ("frost", "#BFE3FF", "#4C8BD8"),
           ("holly", "#2E8B47", "#C8102E"), ("midnight", "#2A2A5A", "#FFD94A"), ("ginger", "#B5733A", "#F6F4F7"), ("rainbow", "#FF4FA0", "#4CC3FF")]


def disc(main, accent, kind):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    p = img.load()
    m = tuple(int(main[i:i + 2], 16) for i in (1, 3, 5))
    a = tuple(int(accent[i:i + 2], 16) for i in (1, 3, 5))
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            if d > 7.3:
                continue
            c = (24, 24, 30)
            if d < 1.2:
                c = (10, 10, 12)
            elif d < 3.3:
                if kind == "candy":
                    c = m if (int((math.atan2(y - 7.5, x - 7.5) + math.pi) / (math.pi / 4))) % 2 else a
                elif kind == "rainbow":
                    hue = (math.atan2(y - 7.5, x - 7.5) + math.pi) / math.tau
                    import colorsys
                    c = tuple(int(v * 255) for v in colorsys.hsv_to_rgb(hue, 0.7, 1))
                else:
                    c = m if d < 2.6 else a
            elif int(d) % 2 == 0:
                c = (36, 36, 44)
            if d > 6.5:
                c = tuple(int(v * 0.7) for v in c)
            if 4 < d < 6 and x < 7 and y < 7 and (x + y) % 3 == 0:
                c = (120, 120, 140)
            p[x, y] = c + (255,)
    return img


overrides = []
for i, (pid, main, accent) in enumerate(PRESETS):
    save(disc(main, accent, pid), f"{ASSETS}/textures/item/disc_{pid}.png")
    wjson(f"{ASSETS}/models/item/disc_{pid}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"jalyclient:item/disc_{pid}"}})
    overrides.append({"predicate": {"custom_model_data": 9101 + i}, "model": f"jalyclient:item/disc_{pid}"})
wjson(os.path.join(RES, "assets/minecraft/models/item/music_disc_precipice.json"),
      {"parent": "minecraft:item/template_music_disc", "textures": {"layer0": "minecraft:item/music_disc_precipice"}, "overrides": overrides})
print("visuals ok")
