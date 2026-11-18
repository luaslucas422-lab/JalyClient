package fr.darkerdawn.jaly.screen;

import fr.darkerdawn.jaly.Sounds;
import fr.darkerdawn.jaly.net.Modrinth;
import fr.darkerdawn.jaly.net.Shaders;
import fr.darkerdawn.jaly.net.UserContent;
import fr.darkerdawn.jaly.ui.Gfx;
import fr.darkerdawn.jaly.ui.JalyButton;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Get mods, resource packs and shaders from Modrinth without leaving the game. */
public class ModrinthScreen extends JalyScreen {
    private static final int ROW = 34;
    public static final List<String> FEATURED = List.of("snowy-spirit", "winterly", "particle-rain", "3dskinlayers", "wavey-capes", "lambdynamiclights", "xaeros-minimap", "emi");
    private Modrinth.Kind kind;
    private TextFieldWidget search;
    private String lastQuery = "";
    private final List<Modrinth.Project> featured = new ArrayList<>();
    private final List<Modrinth.Project> results = new ArrayList<>();
    private boolean loading, failed, installedOnly;
    private long typedAt;
    private double scroll;
    private int requestId;

    public ModrinthScreen(Screen parent, Modrinth.Kind kind) {
        super(title(kind), parent);
        this.kind = kind;
    }

    private static String title(Modrinth.Kind k) {
        return switch (k) { case MOD -> "Mods"; case RESOURCEPACK -> "Resource Packs"; case SHADER -> "Shaders"; };
    }

    @Override
    protected int maxPanelWidth() { return 500; }

    @Override
    protected void build() {
        String old = search != null ? search.getText() : "";
        search = new TextFieldWidget(textRenderer, px + pw - 136, py + 7, 128, 14, Text.literal("Search"));
        search.setPlaceholder(Text.literal("Search Modrinth..."));
        search.setText(old);
        search.setChangedListener(s -> typedAt = Gfx.now());
        addDrawableChild(search);
        int x = cx();
        for (Modrinth.Kind k : Modrinth.Kind.values()) {
            String label = title(k);
            int w = textRenderer.getWidth(label) + 14;
            addDrawableChild(new JalyButton(x, cy(), w, 14, Text.literal(label), b -> switchTo(k)).selected(kind == k));
            x += w + 4;
        }
        int iw = textRenderer.getWidth("Installed") + 14;
        addDrawableChild(new JalyButton(x + 6, cy(), iw, 14, Text.literal("Installed"), b -> { installedOnly = !installedOnly; scroll = 0; clearAndInit(); }).selected(installedOnly));
        if (UserContent.restartNeeded()) {
            addDrawableChild(new JalyButton(px + pw - 96, py + ph - 22, 88, 16, Text.literal("Restart now"), b -> UserContent.restart()));
        }
        if (kind == Modrinth.Kind.SHADER && Shaders.irisLoaded()) {
            addDrawableChild(new JalyButton(px + pw - 196, py + ph - 22, 96, 16, Text.literal("Iris settings"), b -> {
                Screen s = Shaders.settingsScreen(this);
                if (s != null) client.setScreen(s);
            }));
        }
        if (results.isEmpty() && !loading) load(false);
    }

    private void switchTo(Modrinth.Kind k) {
        kind = k;
        setTitle(k);
        results.clear();
        featured.clear();
        scroll = 0;
        clearAndInit();
    }

    private void setTitle(Modrinth.Kind k) {}

    @Override
    public Text getTitle() { return Text.literal(title(kind)); }

    private boolean irisGate() { return kind == Modrinth.Kind.SHADER && !Shaders.irisLoaded(); }

    private void load(boolean more) {
        if (irisGate() || installedOnly) return;
        String q = search == null ? "" : search.getText().trim();
        lastQuery = q;
        loading = true;
        failed = false;
        int my = ++requestId;
        Modrinth.Kind k = kind;
        if (!more) results.clear();
        if (!more && q.isEmpty() && k == Modrinth.Kind.MOD && featured.isEmpty()) {
            Modrinth.projects(FEATURED).thenAccept(l -> client.execute(() -> { if (my == requestId) { featured.clear(); featured.addAll(l); } }));
        }
        Modrinth.search(k, q, more ? results.size() : 0).whenComplete((l, err) -> client.execute(() -> {
            if (my != requestId) return;
            loading = false;
            if (err != null) failed = true;
            else results.addAll(l);
        }));
    }

    @Override
    public void tick() {
        super.tick();
        if (search != null && typedAt > 0 && Gfx.now() - typedAt > 450 && !search.getText().trim().equals(lastQuery)) {
            typedAt = 0;
            featured.clear();
            scroll = 0;
            load(false);
        }
    }

    private List<Modrinth.Project> rows() {
        List<Modrinth.Project> out = new ArrayList<>();
        if (installedOnly) {
            for (UserContent.Entry e : UserContent.map(kind).values()) out.add(new Modrinth.Project(e.id(), e.slug(), e.title(), "Installed from the Mods tab", "", 0));
            return out;
        }
        if (lastQuery.isEmpty() && kind == Modrinth.Kind.MOD) out.addAll(featured);
        for (Modrinth.Project p : results) if (out.stream().noneMatch(o -> o.id().equals(p.id()))) out.add(p);
        return out;
    }

    private int listTop() { return cy() + 20; }

    private int listBottom() { return py + ph - (UserContent.restartNeeded() || kind == Modrinth.Kind.SHADER ? 28 : 8); }

    @Override
    protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float delta) {
        if (irisGate()) {
            int bx = cx() + 20, by = listTop() + 10, bw = cw() - 40;
            Gfx.round(ctx, bx, by, bw, 90, 0xE0162A12);
            Gfx.outline(ctx, bx, by, bw, 90, Gfx.GOLD);
            Gfx.centered(ctx, "Shaders need Iris", width / 2, by + 12, Gfx.GOLD_LIGHT);
            Gfx.centered(ctx, "Install Iris + Sodium, then restart to pick a shader pack.", width / 2, by + 28, 0xFFB8C2B0);
            boolean busy = UserContent.busy.containsKey("YL57xq9U");
            boolean hov = mouseX >= width / 2 - 60 && mouseX < width / 2 + 60 && mouseY >= by + 48 && mouseY < by + 68;
            Gfx.round(ctx, width / 2 - 60, by + 48, 120, 20, hov ? 0xFFB11A33 : 0xFF2E7D32);
            Gfx.outline(ctx, width / 2 - 60, by + 48, 120, 20, Gfx.GOLD_LIGHT);
            String label = UserContent.restartNeeded() ? "Restart to finish" : busy ? UserContent.busy.get("YL57xq9U") : "Install Iris Shaders";
            Gfx.centered(ctx, label, width / 2, by + 54, Gfx.SNOW);
            return;
        }
        List<Modrinth.Project> list = rows();
        int top = listTop(), bottom = listBottom();
        int max = Math.max(0, list.size() * ROW - (bottom - top));
        scroll = Math.max(0, Math.min(scroll, max));
        ctx.enableScissor(cx(), top, cx() + cw(), bottom);
        for (int i = 0; i < list.size(); i++) {
            Modrinth.Project p = list.get(i);
            int y = top + i * ROW - (int) scroll;
            if (y + ROW < top || y > bottom) continue;
            boolean feat = lastQuery.isEmpty() && kind == Modrinth.Kind.MOD && !installedOnly && i < featured.size();
            boolean hov = mouseX >= cx() && mouseX < cx() + cw() && mouseY >= Math.max(y, top) && mouseY < Math.min(y + ROW - 3, bottom);
            Gfx.round(ctx, cx(), y, cw(), ROW - 3, hov ? 0xF0203A18 : feat ? 0xE0241E0A : 0xE0162A12);
            if (feat) Gfx.outline(ctx, cx(), y, cw(), ROW - 3, 0x88C99E02);
            int tile = 0xFF000000 | (p.title().hashCode() & 0x7F7F7F) | 0x303030;
            Gfx.round(ctx, cx() + 4, y + 3, 25, 25, tile);
            Gfx.centered(ctx, p.title().isEmpty() ? "?" : p.title().substring(0, 1).toUpperCase(Locale.ROOT), cx() + 17, y + 11, 0xFFFFFFFF);
            String head = Gfx.trim(p.title(), cw() - 150) + (feat ? "  * Featured" : "");
            Gfx.text(ctx, head, cx() + 35, y + 4, feat ? Gfx.GOLD_LIGHT : Gfx.SNOW, true);
            String sub = (p.author().isEmpty() ? "" : "by " + p.author() + "  ") + (p.downloads() > 0 ? fmt(p.downloads()) + " downloads" : "");
            if (!sub.isEmpty()) Gfx.text(ctx, sub, cx() + 35 + textRenderer.getWidth(head) + 6, y + 4, 0xFF6F8068, false);
            Gfx.text(ctx, Gfx.trim(p.description(), cw() - 130), cx() + 35, y + 16, 0xFF9AA894, false);
            drawAction(ctx, p, cx() + cw() - 90, y + 7, mouseX, mouseY);
        }
        ctx.disableScissor();
        if (list.isEmpty()) {
            String msg = loading ? "Loading..." : failed ? "Couldn't reach Modrinth. Check your internet." : installedOnly ? "Nothing installed from here yet" : "No results";
            Gfx.centered(ctx, msg, width / 2, top + 30, 0xFF8FA088);
        } else if (loading) {
            Gfx.centered(ctx, "Loading more...", width / 2, bottom - 10, 0xFF8FA088);
        }
        if (UserContent.restartNeeded()) {
            Gfx.text(ctx, "Restart to finish " + UserContent.pendingCount() + " mod change" + (UserContent.pendingCount() == 1 ? "" : "s"), cx(), py + ph - 18, 0xFFFFB3B3, true);
        }
    }

    private void drawAction(DrawContext ctx, Modrinth.Project p, int x, int y, int mx, int my) {
        UserContent.State st = UserContent.state(kind, p);
        String label;
        int col;
        switch (st) {
            case INSTALLED -> { label = kind == Modrinth.Kind.MOD && UserContent.data().pendingInstalls.stream().anyMatch(s -> s.endsWith(UserContent.map(kind).get(p.id()).file())) ? "Remove (new)" : "Remove"; col = 0xFF7A1424; }
            case PRE_INSTALLED -> { label = "Pre-installed"; col = 0xFF3A3A3A; }
            case PENDING_REMOVE -> { label = "Undo remove"; col = 0xFF8A6A00; }
            case BUSY -> { label = Gfx.trim(UserContent.busy.get(p.id()), 80); col = 0xFF2C4F6E; }
            default -> { label = "Install"; col = 0xFF2E7D32; }
        }
        boolean hov = mx >= x && mx < x + 84 && my >= y && my < y + 16;
        Gfx.round(ctx, x, y, 84, 16, hov && st != UserContent.State.PRE_INSTALLED ? Gfx.lerpColor(col, 0xFFFFFFFF, 0.15f) : col);
        Gfx.outline(ctx, x, y, 84, 16, hov ? Gfx.GOLD_LIGHT : 0x66FFFFFF);
        Gfx.centered(ctx, label, x + 42, y + 4, Gfx.SNOW);
    }

    private static String fmt(long n) {
        return n >= 1_000_000 ? String.format("%.1fM", n / 1e6) : n >= 1000 ? String.format("%.0fK", n / 1e3) : String.valueOf(n);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) return true;
        if (irisGate()) {
            int by = listTop() + 10;
            if (mouseX >= width / 2 - 60 && mouseX < width / 2 + 60 && mouseY >= by + 48 && mouseY < by + 68 && !UserContent.restartNeeded()) {
                installIris();
                return true;
            }
            return false;
        }
        List<Modrinth.Project> list = rows();
        int top = listTop(), bottom = listBottom();
        if (mouseY < top || mouseY >= bottom) return false;
        for (int i = 0; i < list.size(); i++) {
            int y = top + i * ROW - (int) scroll;
            int x = cx() + cw() - 90;
            if (mouseX >= x && mouseX < x + 84 && mouseY >= y + 7 && mouseY < y + 23) {
                act(list.get(i));
                return true;
            }
        }
        return false;
    }

    /** Install / remove / undo for a row. Public for the smoke test. */
    public void act(Modrinth.Project p) {
        switch (UserContent.state(kind, p)) {
            case NONE -> {
                Sounds.play(Sounds.UNWRAP, 1.2f, 0.6f);
                UserContent.install(kind, p).thenRun(() -> client.execute(this::clearAndInit));
            }
            case INSTALLED -> {
                Sounds.play(Sounds.CLICK, 0.8f, 0.6f);
                UserContent.uninstall(kind, p.id());
                if (kind == Modrinth.Kind.MOD && UserContent.restartNeeded()) client.setScreen(new RestartPrompt(this));
                else clearAndInit();
            }
            case PENDING_REMOVE -> { UserContent.keep(kind, p.id()); clearAndInit(); }
            default -> { }
        }
    }

    private void installIris() {
        Sounds.play(Sounds.UNWRAP, 1.2f, 0.6f);
        Modrinth.Project iris = new Modrinth.Project("YL57xq9U", "iris", "Iris Shaders", "", "", 0);
        UserContent.install(Modrinth.Kind.MOD, iris)
            .thenCompose(v -> fr.darkerdawn.jaly.net.UserContent.install(Modrinth.Kind.MOD, new Modrinth.Project("AANobbMI", "sodium", "Sodium", "", "", 0)))
            .thenRun(() -> client.execute(this::clearAndInit));
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        scroll -= verticalAmount * 24;
        int max = Math.max(0, rows().size() * ROW - (listBottom() - listTop()));
        if (scroll > max - 10 && !loading && !installedOnly && results.size() >= 20 && results.size() % 20 == 0) load(true);
        return true;
    }

    /** "Restart now?" after removing a mod. */
    static final class RestartPrompt extends JalyScreen {
        RestartPrompt(Screen parent) { super("Restart Minecraft?", parent); }

        @Override protected int maxPanelWidth() { return 260; }
        @Override protected int maxPanelHeight() { return 120; }

        @Override
        protected void build() {
            addDrawableChild(new JalyButton(px + 14, py + ph - 28, 110, 18, Text.literal("Restart now"), b -> UserContent.restart()));
            addDrawableChild(new JalyButton(px + pw - 124, py + ph - 28, 110, 18, Text.literal("Later"), b -> close()));
        }

        @Override
        protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float delta) {
            Gfx.centered(ctx, "Windows keeps loaded mods locked.", width / 2, cy() + 6, 0xFFDCE6F2);
            Gfx.centered(ctx, "Restart to finish removing it.", width / 2, cy() + 18, 0xFFDCE6F2);
        }
    }
}
