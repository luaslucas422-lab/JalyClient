"""Synthesizes every JalyClient sound (no samples, all generated -> no licensing issues).
Jingle Bells (James Lord Pierpont, 1857) is public domain; this arrangement is original.
Run: python3 tools/gen_sounds.py  (needs numpy + ffmpeg with libvorbis)
"""
import os
import subprocess
import tempfile
import wave
import numpy as np

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, "mod/src/main/resources/assets/jalyclient/sounds")
SR = 44100
rng = np.random.default_rng(7)


def t_axis(sec):
    return np.arange(int(sec * SR)) / SR


def bell(freq, sec=1.2, decay=3.0, partials=((1, 1.0), (2.0, 0.35), (2.76, 0.25), (5.4, 0.08)), amp=0.5):
    t = t_axis(sec)
    s = np.zeros_like(t)
    for mult, a in partials:
        s += a * np.sin(2 * np.pi * freq * mult * t) * np.exp(-t * decay * (0.6 + mult * 0.4))
    att = np.minimum(1, t / 0.004)
    return amp * s * att


def jingle_shake(sec=0.35, n=6, amp=0.35, seed=0):
    r = np.random.default_rng(seed)
    out = np.zeros(int(sec * SR))
    for _ in range(n):
        f = r.uniform(2600, 5200)
        b = bell(f, sec=sec, decay=r.uniform(10, 18), partials=((1, 1.0), (1.51, 0.5), (2.3, 0.3)), amp=amp / n * 2)
        off = int(r.uniform(0, 0.04) * SR)
        out[off:] += b[: len(out) - off]
    noise = r.normal(0, 1, len(out)) * np.exp(-t_axis(sec)[: len(out)] * 30) * 0.06
    hp = noise - np.convolve(noise, np.ones(8) / 8, mode="same")
    return out + hp


def write_ogg(name, sig, gain=0.9):
    sig = sig / max(1e-6, np.max(np.abs(sig))) * gain
    pcm = (sig * 32767).astype(np.int16)
    with tempfile.NamedTemporaryFile(suffix=".wav", delete=False) as tmp:
        with wave.open(tmp.name, "w") as w:
            w.setnchannels(1)
            w.setsampwidth(2)
            w.setframerate(SR)
            w.writeframes(pcm.tobytes())
        os.makedirs(OUT, exist_ok=True)
        subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", tmp.name, "-ac", "1", "-c:a", "libvorbis", "-q:a", "4", os.path.join(OUT, name + ".ogg")], check=True)
    os.unlink(tmp.name)


NOTE = {"C4": 261.63, "D4": 293.66, "E4": 329.63, "F4": 349.23, "G4": 392.0, "A4": 440.0, "B4": 493.88,
        "C5": 523.25, "D5": 587.33, "E5": 659.25, "F5": 698.46, "G5": 783.99, "A5": 880.0, "C6": 1046.5,
        "C3": 130.81, "F3": 174.61, "G3": 196.0, "D3": 146.83, "G2": 98.0, "A3": 220.0}


def music_box(f, dur):
    return bell(f, sec=max(dur, 0.9) + 0.6, decay=2.4, partials=((1, 1.0), (2, 0.28), (3, 0.08), (4.2, 0.05)), amp=0.45)


def soft_bass(f, dur):
    t = t_axis(dur + 0.3)
    s = (np.sin(2 * np.pi * f * t) + 0.3 * np.sin(4 * np.pi * f * t)) * np.exp(-t * 3.5)
    return 0.25 * s * np.minimum(1, t / 0.01)


def jingle_bells():
    bpm = 116
    beat = 60 / bpm
    chorus = [("E5", 1), ("E5", 1), ("E5", 2), ("E5", 1), ("E5", 1), ("E5", 2), ("E5", 1), ("G5", 1), ("C5", 1.5), ("D5", .5), ("E5", 4),
              ("F5", 1), ("F5", 1), ("F5", 1.5), ("F5", .5), ("F5", 1), ("E5", 1), ("E5", 1), ("E5", .5), ("E5", .5),
              ("E5", 1), ("D5", 1), ("D5", 1), ("E5", 1), ("D5", 2), ("G5", 2),
              ("E5", 1), ("E5", 1), ("E5", 2), ("E5", 1), ("E5", 1), ("E5", 2), ("E5", 1), ("G5", 1), ("C5", 1.5), ("D5", .5), ("E5", 4),
              ("F5", 1), ("F5", 1), ("F5", 1.5), ("F5", .5), ("F5", 1), ("E5", 1), ("E5", 1), ("E5", .5), ("E5", .5),
              ("G5", 1), ("G5", 1), ("F5", 1), ("D5", 1), ("C5", 4)]
    chords = ["C3", "C3", "C3", "C3", "F3", "C3", "D3", "G2", "C3", "C3", "C3", "C3", "F3", "C3", "G2", "C3"]
    total_beats = sum(d for _, d in chorus)
    passes = 2
    length = int((total_beats * passes * beat + 3) * SR)
    out = np.zeros(length)

    def add(sig, at):
        i = int(at * SR)
        n = min(len(sig), length - i)
        if n > 0:
            out[i:i + n] += sig[:n]
    for p in range(passes):
        base = p * total_beats * beat
        pos = base
        for n, d in chorus:
            f = NOTE[n] * (2 if p == 1 and n in ("C5", "D5") else 1)
            add(music_box(f, d * beat), pos)
            if p == 1:
                add(0.35 * music_box(f / 2, d * beat), pos)
            pos += d * beat
        for bar, root in enumerate(chords):
            for b in range(4):
                f = NOTE[root] * (1.5 if b % 2 else 1)
                add(soft_bass(f, beat), base + (bar * 4 + b) * beat)
        for b in range(int(total_beats)):
            if b % 2 == 1:
                add(jingle_shake(0.3, n=5, amp=0.16, seed=b), base + b * beat)
    fade = np.ones(length)
    fade[-int(2.5 * SR):] = np.linspace(1, 0, int(2.5 * SR))
    return out * fade


def main():
    write_ogg("jingle", jingle_shake(0.4, n=7, amp=0.5, seed=1))
    write_ogg("sleigh", np.concatenate([jingle_shake(0.22, n=6, seed=s) for s in range(5)]) * 0.8)
    write_ogg("chime", bell(NOTE["C6"], 1.4, 2.6))
    write_ogg("click", bell(2093, 0.18, 22, partials=((1, 1.0), (2.76, 0.3)), amp=0.4), gain=0.6)
    write_ogg("hover", bell(3136, 0.14, 30, partials=((1, 1.0), (2.0, 0.2)), amp=0.3), gain=0.35)
    arp = np.zeros(int(1.4 * SR))
    for i, n in enumerate(["C5", "E5", "G5", "C6"]):
        s = bell(NOTE[n], 1.2, 3.0)
        o = int(i * 0.09 * SR)
        arp[o:o + len(s)] += s[: len(arp) - o]
    write_ogg("kill", arp + np.pad(jingle_shake(0.35, seed=4), (0, len(arp) - int(0.35 * SR))))
    noise = rng.normal(0, 1, int(0.45 * SR))
    env = np.exp(-t_axis(0.45) * 9) * np.abs(np.sin(t_axis(0.45) * 60))
    rustle = np.convolve(noise * env, np.ones(6) / 6, mode="same") * 0.5
    sparkle = np.zeros(int(1.2 * SR))
    for i, n in enumerate(["G5", "C6", "E5", "G5"]):
        s = bell(NOTE[n] * (2 if i == 3 else 1), 0.9, 4.0, amp=0.35)
        o = int((0.3 + i * 0.07) * SR)
        sparkle[o:o + len(s)] += s[: len(sparkle) - o]
    sparkle[: len(rustle)] += rustle
    write_ogg("unwrap", sparkle)
    write_ogg("menu_music", jingle_bells(), gain=0.8)
    print("sounds:", sorted(os.listdir(OUT)))


if __name__ == "__main__":
    main()
