import os
"""Downloads every default-on launcher mod (plus required dependencies) for the in-game CI test.
Usage: python3 tools/fetch_modpack.py <mods dir>   (skips Fabric API: the dev run already has it)"""
import json, os, sys, urllib.parse, urllib.request

MC = os.environ.get("JALY_MC", "1.21.1")
UA = {"User-Agent": "DarkerDawnFR/JalyClient-CI"}
dest = sys.argv[1]
os.makedirs(dest, exist_ok=True)
cat = json.load(open(os.path.join(os.path.dirname(__file__), "..", "launcher", "catalog.json")))
want = [m["slug"] for m in cat if m["default"]]
with_api = "--with-fabric-api" in sys.argv
seen, done, skipped = (set() if with_api else {"P7dR8mSH"}), [], {}
if with_api:
    want.insert(0, "fabric-api")


def get(url):
    return json.load(urllib.request.urlopen(urllib.request.Request(url, headers=UA), timeout=60))


i = 0
while i < len(want):
    slug = want[i]; i += 1
    q = urllib.parse.urlencode({"loaders": '["fabric"]', "game_versions": f'["{MC}"]'})
    try:
        vs = get(f"https://api.modrinth.com/v2/project/{slug}/version?{q}")
    except Exception as e:
        skipped[slug] = str(e); continue
    if not vs:
        skipped[slug] = f"no {MC} version"; continue
    v = vs[0]
    if v["project_id"] in seen:
        continue
    seen.add(v["project_id"])
    f = next((f for f in v["files"] if f.get("primary")), v["files"][0])
    req = urllib.request.Request(f["url"], headers=UA)
    open(os.path.join(dest, f["filename"]), "wb").write(urllib.request.urlopen(req, timeout=120).read())
    done.append(f["filename"])
    for d in v.get("dependencies", []):
        if d.get("dependency_type") == "required" and d.get("project_id") and d["project_id"] not in seen:
            want.append(d["project_id"])
print("MODPACK installed", len(done), "mods:", ", ".join(sorted(done)))
print("MODPACK skipped", json.dumps(skipped))
