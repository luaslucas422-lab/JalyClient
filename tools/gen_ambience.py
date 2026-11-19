"""Better Sounds ambience (wind, rain on the roof, birds, crickets, cave drips, owl) + custom disc helpers. All synthesized."""
import os, sys, subprocess
import numpy as np
sys.path.insert(0, os.path.dirname(__file__))
from gen_sounds import write_ogg, t_axis, SR, OUT, ROOT  # noqa

r = np.random.default_rng(42)


def lowpass(x, k):
    return np.convolve(x, np.ones(k) / k, mode="same")


def loopable(sig, fade=1.0):
    """Crossfade the end into the start so the loop has no click."""
    n = int(fade * SR)
    out = sig[:-n].copy()
    ramp = np.linspace(0, 1, n)
    out[:n] = sig[:n] * ramp + sig[-n:] * (1 - ramp)
    return out


def wind(sec=24):
    n = int(sec * SR)
    noise = r.normal(0, 1, n)
    body = lowpass(lowpass(noise, 60), 40)
    t = t_axis(sec)
    gust = 0.55 + 0.45 * np.sin(2 * np.pi * t / 8.0) * np.sin(2 * np.pi * t / 5.3 + 1)
    whistle = lowpass(noise, 6) - lowpass(noise, 14)
    sig = body * gust * 3 + whistle * 0.15 * np.clip(gust - 0.6, 0, 1)
    return loopable(sig, 2.0)


def rain_roof(sec=16):
    n = int(sec * SR)
    out = lowpass(r.normal(0, 1, n), 4) * 0.25
    for _ in range(int(sec * 120)):  # individual taps on wood/tiles
        i = r.integers(0, n - 2000)
        f = r.uniform(900, 3200)
        tt = np.arange(1500) / SR
        out[i:i + 1500] += np.sin(2 * np.pi * f * tt) * np.exp(-tt * r.uniform(60, 140)) * r.uniform(0.1, 0.45)
    return loopable(out, 1.0)


def crickets(sec=12):
    n = int(sec * SR)
    out = np.zeros(n)
    for c in range(3):
        f = r.uniform(4300, 5200)
        period = r.uniform(0.6, 0.9)
        start = r.uniform(0, 0.5)
        tt = start
        while tt < sec - 0.3:
            for k in range(3):  # a chirp is 3 quick pulses
                i = int((tt + k * 0.035) * SR)
                seg = np.arange(int(0.025 * SR)) / SR
                out[i:i + len(seg)] += np.sin(2 * np.pi * f * seg) * np.sin(np.pi * seg / 0.025) * (0.4 - c * 0.08)
            tt += period * r.uniform(0.9, 1.1)
    return loopable(out, 0.5)


def bird(seed):
    g = np.random.default_rng(seed)
    out = np.zeros(int(1.6 * SR))
    tt = 0.05
    for _ in range(g.integers(3, 7)):
        d = g.uniform(0.06, 0.16)
        seg = np.arange(int(d * SR)) / SR
        f0, f1 = g.uniform(2500, 4200), g.uniform(3000, 6000)
        f = f0 + (f1 - f0) * (seg / d) + 300 * np.sin(2 * np.pi * 30 * seg)
        ph = 2 * np.pi * np.cumsum(f) / SR
        i = int(tt * SR)
        out[i:i + len(seg)] += np.sin(ph) * np.sin(np.pi * seg / d) ** 2
        tt += d + g.uniform(0.03, 0.12)
        if tt > 1.4:
            break
    return out


def drip(seed):
    g = np.random.default_rng(seed)
    t = t_axis(1.4)
    f = g.uniform(900, 1500) * (1 + 0.6 * np.exp(-t * 40))
    ph = 2 * np.pi * np.cumsum(f) / SR
    s = np.sin(ph) * np.exp(-t * 9)
    echo = np.zeros_like(s)
    for k, a in ((int(0.13 * SR), 0.35), (int(0.27 * SR), 0.18), (int(0.45 * SR), 0.08)):
        echo[k:] += s[:-k] * a
    return s + echo


def owl():
    out = np.zeros(int(2.0 * SR))
    for start, d in ((0.0, 0.25), (0.45, 0.18), (0.7, 0.55)):
        seg = np.arange(int(d * SR)) / SR
        f = 380 - 40 * seg / d
        ph = 2 * np.pi * np.cumsum(f) / SR
        env = np.sin(np.pi * seg / d) ** 0.7
        i = int(start * SR)
        out[i:i + len(seg)] += (np.sin(ph) + 0.25 * np.sin(2 * ph)) * env
    return lowpass(out, 3)


def leaves(sec=10):
    n = int(sec * SR)
    noise = r.normal(0, 1, n)
    hp = noise - lowpass(noise, 3)
    t = t_axis(sec)
    env = 0.4 + 0.6 * np.abs(np.sin(2 * np.pi * t / 3.7)) ** 3
    return loopable(hp * env * 0.6, 1.0)


if __name__ == "__main__":
    write_ogg("amb_wind", wind(), gain=0.8)
    write_ogg("amb_rain_roof", rain_roof(), gain=0.8)
    write_ogg("amb_crickets", crickets(), gain=0.6)
    write_ogg("amb_leaves", leaves(), gain=0.6)
    for i in range(3):
        write_ogg(f"amb_bird{i + 1}", bird(10 + i), gain=0.5)
    for i in range(2):
        write_ogg(f"amb_drip{i + 1}", drip(20 + i), gain=0.6)
    write_ogg("amb_owl", owl(), gain=0.6)
    write_ogg("silence", np.zeros(SR // 10) + 1e-9, gain=0.0001)
    # tiny mp3 for the custom disc smoke test
    test = os.path.join(ROOT, "mod/src/main/resources/assets/jalyclient/test/disc_test.mp3")
    os.makedirs(os.path.dirname(test), exist_ok=True)
    subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-f", "lavfi", "-i", "sine=frequency=523:duration=2", "-ac", "2",
                    "-metadata", "title=Smoke Test Carol", "-metadata", "artist=Jaly Choir", "-b:a", "48k", test], check=True)
    print("ambience ok")
