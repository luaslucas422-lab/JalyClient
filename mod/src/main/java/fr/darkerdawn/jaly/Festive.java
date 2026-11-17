package fr.darkerdawn.jaly;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/** Date logic for the countdown and the daily gifts. Pure Java so it can be unit tested. */
public final class Festive {
    private Festive() {}

    /** Days until the next Christmas Day. 0 on Christmas Day itself. */
    public static long daysUntilChristmas(LocalDate today) {
        LocalDate xmas = LocalDate.of(today.getYear(), 12, 25);
        if (today.isAfter(xmas)) xmas = xmas.plusYears(1);
        return ChronoUnit.DAYS.between(today, xmas);
    }

    public static String countdownText(LocalDate today) {
        long d = daysUntilChristmas(today);
        if (d == 0) return "Merry Christmas!";
        if (d == 1) return "Christmas is tomorrow!";
        return d + " days until Christmas";
    }

    /**
     * How many gift doors can be opened. Everyone gets one door per day since they first launched
     * JalyClient (so it works all year), and in December the real date also counts.
     */
    public static int doorsAvailable(LocalDate today, long firstEpochDay) {
        long since = today.toEpochDay() - firstEpochDay + 1;
        long dec = today.getMonthValue() == 12 ? Math.min(25, today.getDayOfMonth()) : 0;
        return (int) Math.max(1, Math.min(25, Math.max(since, dec)));
    }

    /** Seconds until the next door unlocks (next local midnight), or -1 when all 25 are open. */
    public static long secondsToNextDoor(LocalDateTime now, long firstEpochDay) {
        if (doorsAvailable(now.toLocalDate(), firstEpochDay) >= 25) return -1;
        LocalDateTime midnight = now.toLocalDate().plusDays(1).atStartOfDay();
        return ChronoUnit.SECONDS.between(now, midnight);
    }

    public static String formatDuration(long seconds) {
        if (seconds < 0) return "";
        long h = seconds / 3600, m = (seconds % 3600) / 60;
        return h > 0 ? h + "h " + m + "m" : m + "m " + (seconds % 60) + "s";
    }

    /** Is a door both unlocked and not yet opened. */
    public static boolean canOpen(int door, LocalDate today, long firstEpochDay, List<Integer> opened) {
        return door >= 1 && door <= 25 && door <= doorsAvailable(today, firstEpochDay) && !opened.contains(door);
    }

    /** The 25 gift cards: a joke, a fact or a tip for each door. All original text. */
    public static final String[][] CARDS = {
        {"Joke", "What do snowmen eat for breakfast?", "Frosted flakes!"},
        {"Tip", "Hold C to zoom.", "Scroll while zooming to zoom even further."},
        {"Joke", "Why was the Christmas tree bad at knitting?", "It kept dropping its needles."},
        {"Fact", "Christmas chests are real Minecraft.", "Vanilla shows them on Dec 24-26. JalyClient can show them all year."},
        {"Tip", "Press J anywhere to open the Lodge.", "Click the fireplace for the Christmas resource packs."},
        {"Joke", "What do you call an elf who sings?", "A wrapper!"},
        {"Tip", "Drag your HUD around.", "Click the painting in the Lodge to open the HUD editor."},
        {"Joke", "How does a snowman get around?", "By riding an icicle!"},
        {"Fact", "Snow in Minecraft only falls in cold biomes.", "Turn on Snowfall and it snows wherever you are."},
        {"Joke", "What is a reindeer's favourite game?", "Stable tennis!"},
        {"Tip", "Every HUD mod has an Advanced page.", "Change its size, colours and background style."},
        {"Joke", "Why did the gingerbread man go to the doctor?", "He was feeling crummy."},
        {"Fact", "Jingle Bells was written in 1857.", "It plays on the JalyClient main menu as a music box."},
        {"Joke", "What do you get if you cross a snowman and a vampire?", "Frostbite!"},
        {"Tip", "Try a new cape.", "The mirror in the Lodge opens your wardrobe. Everything is free."},
        {"Joke", "What falls but never gets hurt?", "Snow!"},
        {"Tip", "Hit something for a jingle.", "Hit sounds and kill effects are in Mods > Festive."},
        {"Joke", "Why are Christmas trees so fond of the past?", "Because the present's beneath them."},
        {"Fact", "Lights twinkle on your screen too.", "Turn on Twinkle Lights for a string of bulbs above your HUD."},
        {"Joke", "What says Oh Oh Oh?", "Santa walking backwards."},
        {"Tip", "Too much snow?", "Settings > Festive level: Full, Cozy or Calm."},
        {"Joke", "What do elves learn at school?", "The elf-abet."},
        {"Tip", "Santa hats on mobs!", "Turn on Mob Hats and every zombie gets festive."},
        {"Joke", "What do you call Santa's helpers?", "Subordinate Clauses."},
        {"Gift", "Merry Christmas from JalyClient!", "Thanks for playing. Have the jolliest day."},
    };
}
