# Borderless glass textures for "Jaly Connected Glass" (drawn from scratch: clear with soft streaks).
import os, json, random
from PIL import Image
root = "/home/claude/jalyclient/mod/src/main/resources/resourcepacks/jaly_glass_ctm"
tex = os.path.join(root, "assets/minecraft/textures/block")
os.makedirs(tex, exist_ok=True)
json.dump({"pack": {"pack_format": 34, "description": "§9Jaly §fConnected Glass §7(Tools > Visuals)"}}, open(os.path.join(root, "pack.mcmeta"), "w"), indent=1)
DYES = {"white": (249,255,254), "orange": (249,128,29), "magenta": (199,78,189), "light_blue": (58,179,218), "yellow": (254,216,61),
        "lime": (128,199,31), "pink": (243,139,170), "gray": (71,79,82), "light_gray": (157,157,151), "cyan": (22,156,156),
        "purple": (137,50,184), "blue": (60,68,170), "brown": (131,84,50), "green": (94,124,22), "red": (176,46,38), "black": (29,29,33)}
def streaks(img, rgb, base_a, streak_a, seed):
    rnd = random.Random(seed)
    px = img.load()
    for y in range(16):
        for x in range(16):
            px[x, y] = rgb + (base_a,)
    for start in (3, 9, 13):  # three soft diagonal glints
        for k in range(rnd.randint(3, 5)):
            x, y = start + k, 2 + k + rnd.randint(0, 1)
            if 0 <= x < 16 and 0 <= y < 16:
                c = tuple(min(255, v + 70) for v in rgb)
                px[x, y] = c + (streak_a,)
img = Image.new("RGBA", (16, 16)); streaks(img, (220, 236, 242), 0, 70, 1); img.save(os.path.join(tex, "glass.png"))
for i, (name, rgb) in enumerate(DYES.items()):
    img = Image.new("RGBA", (16, 16)); streaks(img, rgb, 110, 150, i + 2); img.save(os.path.join(tex, f"{name}_stained_glass.png"))
img = Image.new("RGBA", (16, 16)); streaks(img, (44, 34, 52), 200, 220, 99); img.save(os.path.join(tex, "tinted_glass.png"))
print("ok", len(os.listdir(tex)))
