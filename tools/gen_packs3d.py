"""PvP + Crystal packs, then 3D (voxel, puffy) item models for every Jaly pack."""
import json, os, sys
sys.path.insert(0, os.path.dirname(__file__))
from gen_art import *  # noqa
from PIL import Image, ImageDraw

BASE = os.path.join(RES, "resourcepacks")

def mcmeta(p, desc):
    os.makedirs(p, exist_ok=True)
    json.dump({"pack": {"pack_format": 34, "description": desc}}, open(os.path.join(p, "pack.mcmeta"), "w"), indent=2)

SHORT = ["................", "................", "..........AB....", ".........ABA....", "........ABA.....", ".......ABA......",
         "......ABA.......", "...GGABA........", "...GGGA.........", "....GGB.........", ".....HGG........", "....HHGGG.......",
         "...HH..GG.......", "..HH............", ".K..............", "................"]

def short_sword(c1, c2, guard, handle):
    img = tex16(SHORT, {"A": c1, "B": c2, "G": guard, "H": handle, "K": shade(guard, 0.8)})
    return img

# ---- Jaly PvP: short clean swords, dark hotbar, crisp items
p = os.path.join(BASE, "jaly_pvp"); tx = os.path.join(p, "assets/minecraft/textures")
mcmeta(p, "§cJaly §fPvP §7- short swords, clean HUD, crisp items")
for k, (a, b) in {"wooden": ("#B0824F", "#E8C9A0"), "stone": ("#9AA0A8", "#D8DCE2"), "iron": ("#E8ECF2", "#B8C0CC"), "golden": ("#FFD94A", "#FFF2B3"),
                  "diamond": ("#5CE1E6", "#E0FFFF"), "netherite": ("#5A4A5E", "#A898AA")}.items():
    save(short_sword(hexc(a), hexc(b), hexc("#B11A33"), hexc("#2A2A30")), f"{tx}/item/{k}_sword.png")
hb = Image.new("RGBA", (182, 22), (0, 0, 0, 0)); d = ImageDraw.Draw(hb)
d.rectangle([0, 0, 181, 21], fill=(10, 10, 12, 190)); d.rectangle([0, 0, 181, 21], outline=hexc("#B11A33"))
for i in range(9):
    d.rectangle([2 + i * 20, 2, 19 + i * 20, 19], outline=(60, 60, 66, 255))
save(hb, f"{tx}/gui/sprites/hud/hotbar.png")
sel = Image.new("RGBA", (24, 23), (0, 0, 0, 0)); ImageDraw.Draw(sel).rectangle([0, 0, 23, 22], outline=hexc("#E0384F"), width=2); save(sel, f"{tx}/gui/sprites/hud/hotbar_selection.png")
for name, c1, c2 in (("golden_apple", "#FFC93A", "#FFF2B3"), ("ender_pearl", "#1F8A70", "#8FE3CF")):
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0)); dd = ImageDraw.Draw(im)
    dd.ellipse([2, 3, 13, 14], fill=hexc(c1), outline=shade(hexc(c1), 0.6)); dd.ellipse([4, 5, 7, 8], fill=hexc(c2))
    save(im, f"{tx}/item/{name}.png")
icon = build_icon(64).convert("RGBA"); ImageDraw.Draw(icon).rectangle([0, 0, 63, 63], outline=hexc("#E0384F"), width=3)
save(icon, os.path.join(p, "pack.png")); save(icon, f"{TEX}/gui/pack_jaly_pvp.png")

# ---- Jaly Crystal: end crystals, obsidian, anchors-ready items, totems
p = os.path.join(BASE, "jaly_crystal"); tx = os.path.join(p, "assets/minecraft/textures")
mcmeta(p, "§dJaly §fCrystal §7- festive end crystals, obsidian, totems")
ec = Image.new("RGBA", (64, 32), (0, 0, 0, 0)); ep = ec.load()
for y in range(32):
    for x in range(64):
        if y < 16 and x < 32:
            ep[x, y] = (155, 211, 255, 120) if (x + y) % 4 else (230, 246, 255, 170)
        elif y < 16:
            ep[x, y] = RED if ((x + y) // 2) % 2 == 0 else hexc("#FFD94A")
        elif x < 48:
            ep[x, y] = hexc("#2A1A3A") if (x // 2 + y // 2) % 2 else hexc("#3A2450")
save(ec, f"{tx}/entity/end_crystal/end_crystal.png")
ob = Image.new("RGBA", (16, 16)); r = random.Random(8); op = ob.load()
for y in range(16):
    for x in range(16):
        v = r.random()
        op[x, y] = hexc("#14081E") if v < 0.5 else hexc("#22103A") if v < 0.85 else hexc("#4A2A7A") if v < 0.96 else SNOW
save(ob, f"{tx}/block/obsidian.png")
items = {"end_crystal": ("crystal", "#FF6FAE", "#FFFFFF"), "totem_of_undying": ("crystal", "#FFD94A", "#E0384F"),
         "golden_apple": ("orb", "#FFC93A", "#FFFFFF"), "ender_pearl": ("orb", "#9B6BFF", "#E8DAFF"), "experience_bottle": ("orb", "#5CFF7A", "#E0FFE8")}
for name, (kind, c1, c2) in items.items():
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0)); dd = ImageDraw.Draw(im)
    if kind == "orb":
        dd.ellipse([2, 3, 13, 14], fill=hexc(c1), outline=shade(hexc(c1), 0.6)); dd.ellipse([4, 5, 7, 8], fill=hexc(c2))
    else:
        dd.polygon([(8, 0), (14, 6), (8, 15), (2, 6)], fill=hexc(c1), outline=shade(hexc(c1), 0.6)); dd.line([(8, 2), (8, 12)], fill=hexc(c2)); dd.line([(5, 6), (11, 6)], fill=hexc(c2))
    save(im, f"{tx}/item/{name}.png")
icon = build_icon(64).convert("RGBA"); ImageDraw.Draw(icon).rectangle([0, 0, 63, 63], outline=hexc("#FF6FAE"), width=3)
save(icon, os.path.join(p, "pack.png")); save(icon, f"{TEX}/gui/pack_jaly_crystal.png")

# ---- 3D voxel models for every Jaly pack item
HANDHELD = {"thirdperson_righthand": {"rotation": [0, -90, 55], "translation": [0, 4.0, 0.5], "scale": [0.85, 0.85, 0.85]},
            "thirdperson_lefthand": {"rotation": [0, 90, -55], "translation": [0, 4.0, 0.5], "scale": [0.85, 0.85, 0.85]},
            "firstperson_righthand": {"rotation": [0, -90, 25], "translation": [1.13, 3.2, 1.13], "scale": [0.68, 0.68, 0.68]},
            "firstperson_lefthand": {"rotation": [0, 90, -25], "translation": [1.13, 3.2, 1.13], "scale": [0.68, 0.68, 0.68]},
            "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.5, 0.5, 0.5]},
            "head": {"rotation": [0, 180, 0], "translation": [0, 13, 7], "scale": [1, 1, 1]},
            "fixed": {"rotation": [0, 180, 0], "scale": [1, 1, 1]}}
GENERATED = dict(HANDHELD)
GENERATED["thirdperson_righthand"] = {"rotation": [0, 0, 0], "translation": [0, 3, 1], "scale": [0.55, 0.55, 0.55]}
GENERATED["thirdperson_lefthand"] = {"rotation": [0, 0, 0], "translation": [0, 3, 1], "scale": [0.55, 0.55, 0.55]}

def voxel_model(png, tex_ref, sword):
    im = Image.open(png).convert("RGBA"); px = im.load()
    solid = [[px[x, y][3] > 20 for x in range(16)] for y in range(16)]
    def dist(x, y):
        best = 3
        for dy in range(-3, 4):
            for dx in range(-3, 4):
                xx, yy = x + dx, y + dy
                if not (0 <= xx < 16 and 0 <= yy < 16) or not solid[yy][xx]:
                    best = min(best, max(abs(dx), abs(dy)))
        return best
    els = []
    for y in range(16):
        x = 0
        while x < 16:
            if not solid[y][x]:
                x += 1; continue
            d = dist(x, y); start = x
            while x < 16 and solid[y][x] and dist(x, y) == d:
                x += 1
            depth = {1: 1.0, 2: 2.0, 3: 3.0}.get(d, 3.0) if not sword else {1: 1.0, 2: 1.6, 3: 2.4}.get(d, 2.4)
            z0, z1 = 8 - depth / 2, 8 + depth / 2
            u0, u1, v0, v1 = start, x, y, y + 1
            faces = {"north": {"uv": [u1, v0, u0, v1], "texture": "#t"}, "south": {"uv": [u0, v0, u1, v1], "texture": "#t"},
                     "up": {"uv": [u0, v0, u1, v1], "texture": "#t"}, "down": {"uv": [u0, v0, u1, v1], "texture": "#t"},
                     "west": {"uv": [u0, v0, u0 + 1, v1], "texture": "#t"}, "east": {"uv": [u1 - 1, v0, u1, v1], "texture": "#t"}}
            els.append({"from": [start, 15 - y, z0], "to": [x, 16 - y, z1], "faces": faces})
    return {"textures": {"t": tex_ref, "particle": tex_ref}, "gui_light": "front",
            "display": HANDHELD if sword else GENERATED, "elements": els}

count = 0
for pack in os.listdir(BASE):
    item_dir = os.path.join(BASE, pack, "assets/minecraft/textures/item")
    if not os.path.isdir(item_dir):
        continue
    mdir = os.path.join(BASE, pack, "assets/minecraft/models/item"); os.makedirs(mdir, exist_ok=True)
    for f in os.listdir(item_dir):
        name = f[:-4]
        model = voxel_model(os.path.join(item_dir, f), "minecraft:item/" + name, name.endswith("_sword"))
        json.dump(model, open(os.path.join(mdir, name + ".json"), "w"))
        count += 1
        if name == "golden_apple":
            json.dump(model, open(os.path.join(mdir, "enchanted_golden_apple.json"), "w"))
print("3D models:", count, "packs:", sorted(os.listdir(BASE)))
