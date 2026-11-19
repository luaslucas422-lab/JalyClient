package fr.darkerdawn.jaly.disc;

import fr.darkerdawn.jaly.Sounds;
import fr.darkerdawn.jaly.screen.JalyScreen;
import fr.darkerdawn.jaly.ui.Gfx;
import fr.darkerdawn.jaly.ui.JalyButton;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import java.util.List;

/** H: turn a song into a music disc. Link or file -> name -> disc look -> it's in your inventory. */
public class CustomDiscScreen extends JalyScreen {
    enum Step { SOURCE, LOADING, NAME, DONE }

    private Step step = Step.SOURCE;
    private TextFieldWidget field;
    private String message = "", keepText = "";
    private boolean error;
    private CustomDiscs.Imported song;
    private int icon;

    public CustomDiscScreen(Screen parent) {
        super("Custom Music Disc", parent);
    }

    @Override
    protected int maxPanelHeight() { return 250; }

    @Override
    protected void build() {
        int x = cx() + 8, w = cw() - 16;
        switch (step) {
            case SOURCE -> {
                field = new TextFieldWidget(textRenderer, x, cy() + 34, w, 18, Text.literal("Song link"));
                field.setMaxLength(1000);
                field.setPlaceholder(Text.literal("https://.../song.mp3  or a file on your PC"));
                field.setText(keepText);
                addDrawableChild(field);
                setInitialFocus(field);
                addDrawableChild(new JalyButton(x, cy() + 58, 120, 18, Text.literal("Pick a song file..."), b -> pickFile()));
                addDrawableChild(new JalyButton(x + w - 90, cy() + 58, 90, 18, Text.literal("Next"), b -> next()));
                List<CustomDiscs.Disc> mine = CustomDiscs.all();
                for (int i = 0; i < Math.min(4, mine.size()); i++) {
                    CustomDiscs.Disc d = mine.get(i);
                    addDrawableChild(new JalyButton(x + w - 60, cy() + 112 + i * 20, 60, 16, Text.literal("Get again"), b -> {
                        message = CustomDiscs.give(d);
                        error = !message.contains("inventory!");
                        Sounds.play(Sounds.UNWRAP, 1f, 0.6f);
                    }));
                }
            }
            case NAME -> {
                field = new TextFieldWidget(textRenderer, x, cy() + 34, w, 18, Text.literal("Disc name"));
                field.setMaxLength(48);
                field.setText(keepText);
                addDrawableChild(field);
                setInitialFocus(field);
                addDrawableChild(new JalyButton(x + w - 110, ph + py - 30, 110, 20, Text.literal("Create disc"), b -> create()));
                addDrawableChild(new JalyButton(x, ph + py - 30, 70, 20, Text.literal("Back"), b -> { step = Step.SOURCE; keepText = ""; clearAndInit(); }));
            }
            case DONE -> {
                addDrawableChild(new JalyButton(x, ph + py - 30, 120, 20, Text.literal("Make another"), b -> { step = Step.SOURCE; keepText = ""; message = ""; clearAndInit(); }));
                addDrawableChild(new JalyButton(x + w - 80, ph + py - 30, 80, 20, Text.literal("Done"), b -> close()));
            }
            default -> { }
        }
    }

    private void pickFile() {
        Thread t = new Thread(() -> {
            String picked;
            try (MemoryStack stack = MemoryStack.stackPush()) {
                PointerBuffer f = stack.mallocPointer(3);
                f.put(stack.UTF8("*.mp3"));
                f.put(stack.UTF8("*.ogg"));
                f.put(stack.UTF8("*.wav"));
                f.flip();
                picked = TinyFileDialogs.tinyfd_openFileDialog("Pick a song", "", f, "Songs (mp3, ogg, wav)", false);
            }
            if (picked != null) client.execute(() -> {
                keepText = picked;
                if (field != null) field.setText(picked);
                next();
            });
        }, "Jaly file picker");
        t.setDaemon(true);
        t.start();
    }

    private void next() {
        String src = field == null ? keepText : field.getText();
        keepText = src;
        if (CustomDiscs.isYouTube(src)) {
            message = "YouTube links can't be used: YouTube doesn't allow downloading its videos. Use a song file or a direct .mp3/.ogg link.";
            error = true;
            return;
        }
        step = Step.LOADING;
        message = src.startsWith("http") ? "Downloading the song..." : "Copying the song...";
        error = false;
        clearAndInit();
        CustomDiscs.importSong(src).whenComplete((s, ex) -> client.execute(() -> {
            if (ex != null) {
                Throwable c = ex.getCause() != null ? ex.getCause() : ex;
                message = c.getMessage() == null ? "Couldn't get that song" : c.getMessage();
                error = true;
                step = Step.SOURCE;
            } else {
                song = s;
                keepText = s.suggestedName();
                message = "";
                error = false;
                step = Step.NAME;
                Sounds.play(Sounds.CHIME, 1.2f, 0.6f);
            }
            if (client.currentScreen == this) clearAndInit();
        }));
    }

    private void create() {
        if (song == null) return;
        message = CustomDiscs.create(song, field.getText(), icon);
        error = !message.contains("inventory!");
        Sounds.play(Sounds.UNWRAP, 1f, 0.8f);
        step = Step.DONE;
        clearAndInit();
    }

    /** Smoke test: run the whole flow without the UI. */
    public static String createForTest(CustomDiscs.Imported s, String name, int icon) { return CustomDiscs.create(s, name, icon); }

    @Override
    protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float delta) {
        int x = cx() + 8, w = cw() - 16;
        long now = Gfx.now();
        switch (step) {
            case SOURCE -> {
                Gfx.text(ctx, "1. Paste a link to a song, or pick a file", x, cy() + 6, Gfx.GOLD_LIGHT, true);
                Gfx.text(ctx, "mp3, ogg or wav. It plays in any jukebox!", x, cy() + 19, Gfx.MUTED, false);
                List<CustomDiscs.Disc> mine = CustomDiscs.all();
                if (!mine.isEmpty()) {
                    Gfx.text(ctx, "Your discs", x, cy() + 98, Gfx.GOLD_LIGHT, true);
                    for (int i = 0; i < Math.min(4, mine.size()); i++) {
                        CustomDiscs.Disc d = mine.get(i);
                        int yy = cy() + 112 + i * 20;
                        Gfx.tex(ctx, icon(d.icon), x, yy, 16, 16, 0, 0, 16, 16, 16, 16, open);
                        Gfx.text(ctx, Gfx.trim(d.name, w - 90), x + 20, yy + 4, Gfx.SNOW, false);
                    }
                }
            }
            case LOADING -> {
                float spin = (now % 1200) / 1200f * 360f;
                Gfx.push(ctx);
                Gfx.translate(ctx, px + pw / 2f, cy() + 70);
                Gfx.rotate(ctx, spin);
                Gfx.tex(ctx, icon((int) (now / 600 % 8)), -16, -16, 32, 32, 0, 0, 16, 16, 16, 16, open);
                Gfx.pop(ctx);
            }
            case NAME -> {
                Gfx.text(ctx, "2. Name your disc", x, cy() + 6, Gfx.GOLD_LIGHT, true);
                Gfx.text(ctx, "We filled in the song title and artist. Change it if you like.", x, cy() + 19, Gfx.MUTED, false);
                Gfx.text(ctx, "3. Pick how it looks", x, cy() + 62, Gfx.GOLD_LIGHT, true);
                int tw = Math.min(46, (w - 7 * 6) / 8);
                for (int i = 0; i < CustomDiscs.ICONS.length; i++) {
                    int tx = x + i * (tw + 6), ty = cy() + 76;
                    boolean hov = mouseX >= tx && mouseX < tx + tw && mouseY >= ty && mouseY < ty + tw + 12;
                    Gfx.round(ctx, tx, ty, tw, tw + 12, i == icon ? 0xF0244A1C : hov ? 0xF0203A18 : 0xE0162A12);
                    Gfx.outline(ctx, tx, ty, tw, tw + 12, i == icon ? Gfx.GOLD_LIGHT : 0x55C99E02);
                    float bob = i == icon ? (float) Math.sin(now / 200.0) * 1.5f : 0f;
                    Gfx.tex(ctx, icon(i), tx + (tw - 32) / 2, ty + 4 + Math.round(bob), 32, 32, 0, 0, 16, 16, 16, 16, open);
                    Gfx.centered(ctx, Gfx.trim(CustomDiscs.ICON_NAMES[i], tw), tx + tw / 2, ty + tw + 2, i == icon ? Gfx.GOLD_LIGHT : Gfx.MUTED);
                }
            }
            case DONE -> {
                Gfx.tex(ctx, icon(icon), px + pw / 2 - 24, cy() + 20, 48, 48, 0, 0, 16, 16, 16, 16, open);
                Gfx.centered(ctx, "Put it in a jukebox to play it!", px + pw / 2, cy() + 80, Gfx.SNOW);
            }
        }
        if (!message.isEmpty()) {
            int yy = py + ph - 52;
            for (var line : textRenderer.wrapLines(Text.literal(message), w)) {
                ctx.drawText(textRenderer, line, x, yy, error ? 0xFFFF9A9A : Gfx.GOLD_LIGHT, true);
                yy += 10;
            }
        }
    }

    private static Identifier icon(int i) {
        return Identifier.of("jalyclient", "textures/item/disc_" + CustomDiscs.ICONS[Math.max(0, Math.min(7, i))] + ".png");
    }

    @Override
//#if >=1.21.9
    public boolean mouseClicked(net.minecraft.client.gui.Click click, boolean doubled) {
       double mx = click.x(), my = click.y(); int button = click.button();
//#else
    public boolean mouseClicked(double mx, double my, int button) {
//#endif
//#if >=1.21.9
        if (super.mouseClicked(click, doubled)) return true;
//#else
        if (super.mouseClicked(mx, my, button)) return true;
//#endif
        if (step == Step.NAME) {
            int x = cx() + 8, w = cw() - 16, tw = Math.min(46, (w - 7 * 6) / 8);
            for (int i = 0; i < CustomDiscs.ICONS.length; i++) {
                int tx = x + i * (tw + 6), ty = cy() + 76;
                if (mx >= tx && mx < tx + tw && my >= ty && my < ty + tw + 12) {
                    icon = i;
                    Sounds.play(Sounds.CLICK, 1.2f, 0.6f);
                    return true;
                }
            }
        }
        return false;
    }

    @Override
//#if >=1.21.9
    public boolean keyPressed(net.minecraft.client.input.KeyInput input) {
       int keyCode = input.key(), scanCode = input.scancode(), modifiers = input.modifiers();
//#else
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
//#endif
        if ((keyCode == 257 || keyCode == 335) && field != null && field.isFocused()) {
            if (step == Step.SOURCE) next();
            else if (step == Step.NAME) create();
            return true;
        }
//#if >=1.21.9
        if (field != null && field.isFocused() && keyCode != 256) return field.keyPressed(input);
//#else
        if (field != null && field.isFocused() && keyCode != 256) return field.keyPressed(keyCode, scanCode, modifiers);
//#endif
//#if >=1.21.9
        return super.keyPressed(input);
//#else
        return super.keyPressed(keyCode, scanCode, modifiers);
//#endif
    }
}
