package fr.darkerdawn.jaly;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FestiveTest {
    @Test
    void countdown() {
        assertEquals(0, Festive.daysUntilChristmas(LocalDate.of(2026, 12, 25)));
        assertEquals(1, Festive.daysUntilChristmas(LocalDate.of(2026, 12, 24)));
        assertEquals(38, Festive.daysUntilChristmas(LocalDate.of(2026, 11, 17)));
        assertEquals(364, Festive.daysUntilChristmas(LocalDate.of(2026, 12, 26)));
        assertEquals("Merry Christmas!", Festive.countdownText(LocalDate.of(2026, 12, 25)));
        assertEquals("Christmas is tomorrow!", Festive.countdownText(LocalDate.of(2026, 12, 24)));
    }

    @Test
    void doorsGrowOnePerDayAllYear() {
        long first = LocalDate.of(2026, 6, 1).toEpochDay();
        assertEquals(1, Festive.doorsAvailable(LocalDate.of(2026, 6, 1), first));
        assertEquals(4, Festive.doorsAvailable(LocalDate.of(2026, 6, 4), first));
        assertEquals(25, Festive.doorsAvailable(LocalDate.of(2026, 9, 1), first));
    }

    @Test
    void decemberUsesTheRealDate() {
        long first = LocalDate.of(2026, 12, 10).toEpochDay();
        assertEquals(10, Festive.doorsAvailable(LocalDate.of(2026, 12, 10), first));
        assertEquals(25, Festive.doorsAvailable(LocalDate.of(2026, 12, 31), first));
    }

    @Test
    void canOpenRules() {
        LocalDate d = LocalDate.of(2026, 3, 5);
        long first = LocalDate.of(2026, 3, 1).toEpochDay();
        assertTrue(Festive.canOpen(5, d, first, List.of()));
        assertFalse(Festive.canOpen(6, d, first, List.of()));
        assertFalse(Festive.canOpen(2, d, first, List.of(2)));
        assertFalse(Festive.canOpen(0, d, first, List.of()));
        assertFalse(Festive.canOpen(26, d, first, List.of()));
    }

    @Test
    void nextDoorTimer() {
        long first = LocalDate.of(2026, 3, 1).toEpochDay();
        assertEquals(3600, Festive.secondsToNextDoor(LocalDateTime.of(2026, 3, 1, 23, 0), first));
        assertEquals(-1, Festive.secondsToNextDoor(LocalDateTime.of(2026, 5, 1, 12, 0), first));
        assertEquals("1h 0m", Festive.formatDuration(3600));
    }

    @Test
    void every_door_has_a_card() {
        assertEquals(25, Festive.CARDS.length);
        for (String[] c : Festive.CARDS) {
            assertEquals(3, c.length);
            for (String s : c) assertFalse(s.isBlank());
        }
    }
}
