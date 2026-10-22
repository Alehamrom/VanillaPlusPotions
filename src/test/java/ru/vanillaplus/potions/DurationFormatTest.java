package ru.vanillaplus.potions;

import org.junit.jupiter.api.Test;
import ru.vanillaplus.potions.config.ConfigManager;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DurationFormatTest {

    @Test
    public void testDurationFormatting() {
        assertEquals("03:00", ConfigManager.formatDuration(3600)); // 180 sec
        assertEquals("08:00", ConfigManager.formatDuration(9600)); // 480 sec
        assertEquals("01:30", ConfigManager.formatDuration(1800)); // 90 sec
        assertEquals("00:45", ConfigManager.formatDuration(900));  // 45 sec
        assertEquals("00:15", ConfigManager.formatDuration(300));  // 15 sec
        assertEquals("00:30", ConfigManager.formatDuration(600));  // 30 sec
        assertEquals("00:00", ConfigManager.formatDuration(0));
    }

    @Test
    public void testMultipliers() {
        // Lingering 1/4 of 3:00 (3600 -> 900 -> 45s)
        int lingeringTicks = (int) Math.round(3600 * 0.25);
        assertEquals(900, lingeringTicks);
        assertEquals("00:45", ConfigManager.formatDuration(lingeringTicks));

        // Arrow 1/8 of 3:00 (3600 -> 450 -> 22s)
        int arrowTicks = (int) Math.round(3600 * 0.125);
        assertEquals(450, arrowTicks);
        assertEquals("00:22", ConfigManager.formatDuration(arrowTicks));
    }
}
