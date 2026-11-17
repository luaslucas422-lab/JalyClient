# JalyClient

The jolliest **free** Minecraft client. Christmas all year. By **DarkerDawnFR**.

## Install (players)
1. Download **JalyClient-Setup.exe** from the Releases page and run it (Next > Install > Finish).
2. Open **JalyClient** from the Start menu or your desktop.
3. Press the big **PLAY** button. It installs Fabric, JalyClient and your chosen mods, adds a JalyClient profile to the official Minecraft Launcher and opens it. Press Play there.

Minecraft Java 1.21.1 (Fabric). Windows 10/11.

## What's inside
- **Animated Christmas main menu**: a blocky snowy valley with parallax hills, an aurora, chimney smoke, a sleigh crossing the moon and Jingle Bells on a music box. The buttons light up, lift and stripe on hover.
- **Custom loading screen** with a candy cane progress bar.
- **The Jaly Lodge** (press **J**): a cozy cabin. Click the notice board (Mods), mirror (Cosmetics), presents (Gifts), fireplace (Packs), painting (Edit HUD) or clock (Settings).
- **30 built-in mods**: FPS, CPS, Keystrokes, Coordinates, Christmas Countdown, Armor, Potions, Direction, Ping, Clock, Memory, Server, Speed, Sprint Status, Zoom (C), Toggle Sprint, Festive Crosshair, Fullbright, Low Fire, Snowfall, Twinkle Lights, Frost Vignette, Christmas Chests, Jingle Hits, Snow Burst, Mob Hats and more. Each has an **Advanced** page (size, colours, background, brackets, shadow) and a drag-and-drop **HUD editor**.
- **Spotify**: **Now Playing** card with live lyrics, and **Sky Lyrics** that float the current line in the sky. Uses the Windows media controls (no login) and LRCLIB lyrics.
- **Free cosmetics**: Santa hat, reindeer antlers, elf hat, snowman top hat, star halo and 6 HD capes.
- **25 daily gifts**: one present a day with a joke, tip or fact card.
- **2 Christmas resource packs**: *Jaly Christmas* (on by default) and *Jaly Winter Wonderland*.
- **Launcher mods**: Sodium, Lithium, FerriteCore, Entity Culling, ImmediatelyFast, ModernFix, Dynamic FPS, Snowy Spirit, Winterly, Wintery Atmosphere, Mod Menu, AppleSkin, Mouse Tweaks, Continuity, Iris, Xaero's Minimap, Jade (switch each on/off).

## Build
- `mod/`: Fabric mod (`gradle build`). `tools/gen_art.py` and `tools/gen_sounds.py` regenerate all art and sounds (original pixel art, synthesized audio; Jingle Bells is public domain).
- `launcher/`: Go + WebView2 Windows launcher. `installer/JalyClient.iss`: Inno Setup wizard.
- GitHub Actions builds everything, runs unit tests, launches the game for an in-game test with screenshots, and installs/uninstalls the setup on Windows. Run the **Release** workflow to publish.
