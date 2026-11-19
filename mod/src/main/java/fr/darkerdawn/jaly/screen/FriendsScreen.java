package fr.darkerdawn.jaly.screen;

import fr.darkerdawn.jaly.JalyConfig;
import fr.darkerdawn.jaly.Sounds;
import fr.darkerdawn.jaly.net.Friends;
import fr.darkerdawn.jaly.net.Heads;
import fr.darkerdawn.jaly.net.ShopApi;
import fr.darkerdawn.jaly.ui.Gfx;
import fr.darkerdawn.jaly.ui.JalyButton;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.text.Text;

import java.util.List;

/** The Jaly friends list: add JalyClient players, see their status and server, set your own status. */
public class FriendsScreen extends JalyScreen {
    private static final int ROW = 30, LEFT = 128;
    static final String[] BUDDY = {
        "..bbbb..",
        ".bbbbbb.",
        ".bwbbwb.",
        ".bbbbbb.",
        "..bwwb..",
        "rbbbbbbr",
        "bbwbbwbb",
        ".bbbbbb.",
        ".bb..bb."};

    private TextFieldWidget nameField;
    private boolean requestsTab;
    private double scroll;
    private String confirmRemove = "";
    private long confirmAt;

    public FriendsScreen(Screen parent) {
        super("Friends", parent);
    }

    @Override
    protected int maxPanelWidth() { return 500; }

    @Override
    protected int maxPanelHeight() { return 300; }

    private int rx() { return px + LEFT + 14; }
    private int rw() { return pw - LEFT - 24; }
    private int listTop() { return cy() + 44; }
    private int listBottom() { return py + ph - 22; }

    @Override
    protected void build() {
        JalyConfig c = JalyConfig.get();
        if (!c.friendsUsed) {
            c.friendsUsed = true;
            JalyConfig.save();
        }
        if (!Boolean.getBoolean("jaly.smoke")) {
            if (ShopApi.signedIn) Friends.sync(client);
            else if (!ShopApi.busy) ShopApi.connect().thenRun(() -> Friends.sync(client));
        }
        // my status
        int y = cy() + 46;
        for (String s : Friends.STATUSES) {
            final String st = s;
            JalyButton b = new JalyButton(cx(), y, LEFT, 16, Text.literal("   " + Friends.label(s)), x -> {
                Friends.setStatus(st);
                Sounds.play(Sounds.CLICK, 1.1f, 0.6f);
                if (ShopApi.signedIn) Friends.sync(client);
                clearAndInit();
            });
            addDrawableChild(b);
            y += 19;
        }
        addDrawableChild(new JalyButton(cx(), y + 6, LEFT, 16, Text.literal(c.friendShareServer ? "Server: shown" : "Server: hidden"), x -> {
            c.friendShareServer = !c.friendShareServer;
            JalyConfig.save();
            if (ShopApi.signedIn) Friends.sync(client);
            clearAndInit();
        }));
        // add a friend
        String keep = nameField == null ? "" : nameField.getText();
        nameField = new TextFieldWidget(textRenderer, rx(), cy() + 1, rw() - 52, 16, Text.literal("Minecraft name"));
        nameField.setMaxLength(16);
        nameField.setPlaceholder(Text.literal("Add a friend by Minecraft name..."));
        nameField.setText(keep);
        addDrawableChild(nameField);
        addDrawableChild(new JalyButton(rx() + rw() - 48, cy(), 48, 18, Text.literal("Add"), x -> addFriend()));
        int tw = rw() / 2 - 2;
        addDrawableChild(new JalyButton(rx(), cy() + 22, tw, 16, Text.literal("Friends (" + Friends.FRIENDS.size() + ")"), x -> { requestsTab = false; scroll = 0; clearAndInit(); }).selected(!requestsTab));
        int req = Friends.INCOMING.size();
        addDrawableChild(new JalyButton(rx() + tw + 4, cy() + 22, tw, 16, Text.literal("Requests" + (req > 0 ? " (" + req + ")" : "")), x -> { requestsTab = true; scroll = 0; clearAndInit(); }).selected(requestsTab));
        if (!ShopApi.signedIn && !ShopApi.busy) {
            addDrawableChild(new JalyButton(rx() + rw() / 2 - 50, listTop() + 70, 100, 18, Text.literal("Connect"), x -> ShopApi.connect().thenRun(() -> Friends.sync(client))));
        }
    }

    private void addFriend() {
        String n = nameField.getText().trim();
        if (n.isEmpty()) return;
        Sounds.play(Sounds.CHIME, 1f, 0.6f);
        nameField.setText("");
        Friends.add(n).thenRun(() -> client.execute(() -> { if (client.currentScreen == this) clearAndInit(); }));
    }

    @Override
    protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float delta) {
        long now = Gfx.now();
        // ---- me
        int lx = cx(), ly = cy();
        Gfx.round(ctx, lx, ly, LEFT, 40, 0xC0091408);
        String me = client.getSession().getUsername();
        String my = JalyConfig.get().friendStatus;
        Heads.draw(ctx, client.getSession().getUuidOrNull() == null ? "" : client.getSession().getUuidOrNull().toString(), lx + 5, ly + 5, 30, open);
        statusDot(ctx, lx + 29, ly + 29, my.equals("invisible") ? "offline" : my, now);
        Gfx.text(ctx, Gfx.trim(me, LEFT - 44), lx + 40, ly + 9, Gfx.alpha(Gfx.SNOW, open), true);
        Gfx.text(ctx, Friends.label(my), lx + 40, ly + 22, Gfx.alpha(Friends.color(my.equals("invisible") ? "offline" : my), open), false);
        // status pill dots + current marker
        int y = cy() + 46;
        for (String s : Friends.STATUSES) {
            ctx.fill(lx + 7, y + 5, lx + 13, y + 11, Gfx.alpha(Friends.color(s.equals("invisible") ? "offline" : s), open));
            if (s.equals("sleeping")) Gfx.text(ctx, "z", lx + 13, y + 1, Gfx.alpha(0xFFBFB6FF, open), false);
            if (s.equals(my)) Gfx.text(ctx, "✔", lx + LEFT - 12, y + 4, Gfx.alpha(Gfx.GOLD_LIGHT, open), false);
            y += 19;
        }
        Gfx.text(ctx, Gfx.trim(JalyConfig.get().friendShareServer ? "Friends see your server" : "Your server is hidden", LEFT), lx, y + 26, Gfx.alpha(Gfx.MUTED, open), false);
        ctx.fill(px + LEFT + 8, cy(), px + LEFT + 9, py + ph - 10, 0x55C99E02);

        // ---- list
        int top = listTop(), bottom = listBottom(), x = rx(), w = rw();
        if (!ShopApi.signedIn) {
            Gfx.sprite(ctx, BUDDY, new int[]{0xFFB5733A, 0xFFF6F4F7, 0xFFE0384F}, "bwr", x + w / 2 - 8, top + 8, 2, open);
            Gfx.centered(ctx, ShopApi.busy ? "Connecting..." : "Connect your Minecraft account to see friends", x + w / 2, top + 34, Gfx.alpha(Gfx.SNOW, open));
            Gfx.centered(ctx, Gfx.trim(ShopApi.status, w), x + w / 2, top + 48, 0xFFFF9A9A);
            return;
        }
        ctx.enableScissor(x, top, x + w, bottom);
        if (!requestsTab) {
            List<Friends.Friend> list = Friends.FRIENDS;
            if (list.isEmpty()) {
                float bob = (float) Math.sin(now / 300.0) * 2;
                Gfx.sprite(ctx, BUDDY, new int[]{0xFFB5733A, 0xFFF6F4F7, 0xFFE0384F}, "bwr", x + w / 2 - 8, top + 10 + Math.round(bob), 2, open);
                Gfx.centered(ctx, Friends.loaded ? "No friends yet... it's cold in here." : "Loading...", x + w / 2, top + 36, Gfx.alpha(Gfx.SNOW, open));
                Gfx.centered(ctx, "Add a JalyClient player by their name above.", x + w / 2, top + 50, Gfx.alpha(Gfx.MUTED, open));
                Gfx.centered(ctx, "Your first friend unlocks the Ginger Buddy pet!", x + w / 2, top + 62, Gfx.alpha(Gfx.GOLD_LIGHT, open));
            }
            for (int i = 0; i < list.size(); i++) {
                Friends.Friend f = list.get(i);
                int ry = top + i * ROW - (int) scroll;
                if (ry + ROW < top || ry > bottom) continue;
                boolean hov = mouseX >= x && mouseX < x + w && mouseY >= ry && mouseY < ry + ROW - 3 && mouseY >= top && mouseY < bottom;
                Gfx.round(ctx, x, ry, w, ROW - 3, hov ? 0xF0203A18 : 0xE0162A12);
                if (f.online()) ctx.fill(x, ry + 3, x + 2, ry + ROW - 6, Gfx.alpha(Friends.color(f.status()), open));
                Heads.draw(ctx, f.uuid(), x + 5, ry + 3, 21, f.online() ? open : open * 0.55f);
                statusDot(ctx, x + 21, ry + 19, f.status(), now);
                Gfx.text(ctx, Gfx.trim(f.name(), w - 110), x + 32, ry + 4, Gfx.alpha(f.online() ? Gfx.SNOW : 0xFF9AA59A, open), true);
                String sub = switch (f.status()) {
                    case "offline" -> "Offline - last seen " + Friends.ago(f.lastSeen());
                    case "sleeping" -> "Sleeping zzz" + (f.server().isEmpty() ? "" : " - " + f.server());
                    case "dnd" -> "Do Not Disturb" + (f.server().isEmpty() ? "" : " - " + f.server());
                    default -> f.server().isEmpty() ? "Online" : playingText(f.server());
                };
                Gfx.text(ctx, Gfx.trim(sub, w - 100), x + 32, ry + 15, Gfx.alpha(f.online() ? Friends.color(f.status()) : Gfx.MUTED, open), false);
                // actions
                int bx = x + w - 20;
                drawSmallButton(ctx, bx, ry + 6, 16, confirmRemove.equals(f.uuid()) && now - confirmAt < 3000 ? "?" : "x", mouseX, mouseY, 0xFF7A1424);
                if (canJoin(f)) drawSmallButton(ctx, bx - 38, ry + 6, 34, "Join", mouseX, mouseY, 0xFF2E7D32);
            }
        } else {
            int i = 0;
            if (Friends.INCOMING.isEmpty() && Friends.OUTGOING.isEmpty()) {
                Gfx.centered(ctx, "No friend requests right now.", x + w / 2, top + 30, Gfx.alpha(Gfx.MUTED, open));
            }
            for (Friends.Person p : Friends.INCOMING) {
                int ry = top + i++ * ROW - (int) scroll;
                Gfx.round(ctx, x, ry, w, ROW - 3, 0xE0241E0A);
                Gfx.outline(ctx, x, ry, w, ROW - 3, 0x88C99E02);
                Heads.draw(ctx, p.uuid(), x + 5, ry + 3, 21, open);
                Gfx.text(ctx, Gfx.trim(p.name(), w - 130), x + 32, ry + 4, Gfx.alpha(Gfx.SNOW, open), true);
                Gfx.text(ctx, "wants to be friends!", x + 32, ry + 15, Gfx.alpha(Gfx.GOLD_LIGHT, open), false);
                drawSmallButton(ctx, x + w - 96, ry + 6, 46, "Accept", mouseX, mouseY, 0xFF2E7D32);
                drawSmallButton(ctx, x + w - 46, ry + 6, 42, "Decline", mouseX, mouseY, 0xFF7A1424);
            }
            for (Friends.Person p : Friends.OUTGOING) {
                int ry = top + i++ * ROW - (int) scroll;
                Gfx.round(ctx, x, ry, w, ROW - 3, 0xE0162A12);
                Heads.draw(ctx, p.uuid(), x + 5, ry + 3, 21, open * 0.7f);
                Gfx.text(ctx, Gfx.trim(p.name(), w - 110), x + 32, ry + 4, Gfx.alpha(0xFFB8C2B0, open), true);
                Gfx.text(ctx, "Request sent - waiting...", x + 32, ry + 15, Gfx.alpha(Gfx.MUTED, open), false);
                drawSmallButton(ctx, x + w - 46, ry + 6, 42, "Cancel", mouseX, mouseY, 0xFF5A5A5A);
            }
        }
        ctx.disableScissor();
        String msg = Friends.message;
        if (!msg.isEmpty()) Gfx.centered(ctx, Gfx.trim(msg, w), x + w / 2, py + ph - 16, Friends.busy ? Gfx.GOLD_LIGHT : 0xFFDCE6F2);
        else {
            long on = Friends.FRIENDS.stream().filter(Friends.Friend::online).count();
            Gfx.centered(ctx, on + " of " + Friends.FRIENDS.size() + " friends online", x + w / 2, py + ph - 16, Gfx.alpha(Gfx.MUTED, open));
        }
    }

    private static String playingText(String server) {
        return switch (server) {
            case "In menus" -> "Online - in the menus";
            case "Singleplayer" -> "Playing singleplayer";
            case "Realms" -> "Playing on Realms";
            case "Multiplayer" -> "Playing multiplayer";
            default -> "Playing on " + server;
        };
    }

    private boolean canJoin(Friends.Friend f) {
        String s = f.server();
        return f.online() && client.world == null && !s.isEmpty() && !s.equals("In menus") && !s.equals("Singleplayer") && !s.equals("Realms")
            && !s.equals("Multiplayer") && ServerAddress.isValid(s);
    }

    private void statusDot(DrawContext ctx, int x, int y, String status, long now) {
        ctx.fill(x - 1, y - 1, x + 8, y + 8, Gfx.alpha(0xFF0B160A, open));
        int c = Friends.color(status);
        if (status.equals("online")) {
            float pulse = 0.5f + 0.5f * (float) Math.sin(now / 400.0);
            ctx.fill(x - 2, y - 2, x + 9, y + 9, Gfx.alpha(c, 0.25f * pulse * open));
        }
        ctx.fill(x, y, x + 7, y + 7, Gfx.alpha(c, open));
        if (status.equals("dnd")) ctx.fill(x + 1, y + 3, x + 6, y + 4, Gfx.alpha(0xFFFFFFFF, open));
        if (status.equals("sleeping")) ctx.fill(x + 3, y + 1, x + 6, y + 4, Gfx.alpha(0xFF0B160A, open));
    }

    private void drawSmallButton(DrawContext ctx, int x, int y, int w, String label, int mx, int my, int color) {
        boolean hov = mx >= x && mx < x + w && my >= y && my < y + 15;
        Gfx.round(ctx, x, y, w, 15, hov ? Gfx.alpha(0xFFFFFFFF, 0.25f) | (color & 0x00FFFFFF) | 0xFF000000 : color);
        if (hov) Gfx.outline(ctx, x, y, w, 15, Gfx.GOLD_LIGHT);
        Gfx.centered(ctx, label, x + w / 2, y + 4, Gfx.SNOW);
    }

    private static boolean in(double mx, double my, int x, int y, int w, int h) { return mx >= x && mx < x + w && my >= y && my < y + h; }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (super.mouseClicked(mx, my, button)) return true;
        int top = listTop(), bottom = listBottom(), x = rx(), w = rw();
        if (button != 0 || my < top || my >= bottom || !ShopApi.signedIn) return false;
        if (!requestsTab) {
            List<Friends.Friend> list = Friends.FRIENDS;
            for (int i = 0; i < list.size(); i++) {
                Friends.Friend f = list.get(i);
                int ry = top + i * ROW - (int) scroll;
                if (in(mx, my, x + w - 20, ry + 6, 16, 15)) {
                    if (confirmRemove.equals(f.uuid()) && Gfx.now() - confirmAt < 3000) {
                        confirmRemove = "";
                        Friends.remove(f.uuid());
                        Sounds.play(Sounds.CLICK, 0.6f, 0.6f);
                    } else {
                        confirmRemove = f.uuid();
                        confirmAt = Gfx.now();
                        Friends.message = "Click ? again to remove " + f.name();
                    }
                    return true;
                }
                if (canJoin(f) && in(mx, my, x + w - 58, ry + 6, 34, 15)) {
                    Sounds.play(Sounds.JINGLE, 1.1f, 0.7f);
                    ConnectScreen.connect(this, client, ServerAddress.parse(f.server()), new ServerInfo(f.name() + "'s server", f.server(), ServerInfo.ServerType.OTHER), false, null);
                    return true;
                }
            }
        } else {
            int i = 0;
            for (Friends.Person p : Friends.INCOMING) {
                int ry = top + i++ * ROW - (int) scroll;
                if (in(mx, my, x + w - 96, ry + 6, 46, 15)) { Sounds.play(Sounds.UNWRAP, 1f, 0.7f); Friends.accept(p.uuid()).thenRun(this::refresh); return true; }
                if (in(mx, my, x + w - 46, ry + 6, 42, 15)) { Friends.decline(p.uuid()).thenRun(this::refresh); return true; }
            }
            for (Friends.Person p : Friends.OUTGOING) {
                int ry = top + i++ * ROW - (int) scroll;
                if (in(mx, my, x + w - 46, ry + 6, 42, 15)) { Friends.decline(p.uuid()).thenRun(this::refresh); return true; }
            }
        }
        return false;
    }

    private void refresh() {
        client.execute(() -> { if (client.currentScreen == this) clearAndInit(); });
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (nameField != null && nameField.isFocused() && (keyCode == 257 || keyCode == 335)) {
            addFriend();
            return true;
        }
        if (nameField != null && nameField.isFocused() && keyCode != 256) return nameField.keyPressed(keyCode, scanCode, modifiers);
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double h, double v) {
        int rows = requestsTab ? Friends.INCOMING.size() + Friends.OUTGOING.size() : Friends.FRIENDS.size();
        scroll = Math.max(0, Math.min(Math.max(0, rows * ROW - (listBottom() - listTop())), scroll - v * 20));
        return true;
    }
}
