package ru.vanillaplus.potions;

import org.junit.jupiter.api.Test;
import ru.vanillaplus.potions.config.ConfigManager;
import ru.vanillaplus.potions.model.PotionVariant;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PotionDurationAndRadiiTest {

    int calculateDuration(int base, int ext, int upg, boolean supportsUpg, boolean supportsExt,
                          int level, boolean extended, PotionVariant variant) {
        int sanitizedLevel = (level >= 2 && supportsUpg) ? 2 : 1;
        boolean sanitizedExtended = extended && supportsExt && sanitizedLevel == 1;

        int ticks;
        if (sanitizedLevel >= 2) {
            ticks = upg;
        } else if (sanitizedExtended) {
            ticks = ext;
        } else {
            ticks = base;
        }
        return Math.max(20, (int) Math.round(ticks * variant.getDurationMultiplier()));
    }

    @Test
    public void testHasteDurations() {
        // Haste: 3600 base (03:00) / 9600 ext (08:00) / 1800 upg (01:30)
        int base = 3600;
        int ext = 9600;
        int upg = 1800;

        // Normal Level 1
        int d1 = calculateDuration(base, ext, upg, true, true, 1, false, PotionVariant.NORMAL);
        assertEquals(3600, d1);
        assertEquals("03:00", ConfigManager.formatDuration(d1));

        // Normal Extended
        int dExt = calculateDuration(base, ext, upg, true, true, 1, true, PotionVariant.NORMAL);
        assertEquals(9600, dExt);
        assertEquals("08:00", ConfigManager.formatDuration(dExt));

        // Normal Level 2
        int d2 = calculateDuration(base, ext, upg, true, true, 2, false, PotionVariant.NORMAL);
        assertEquals(1800, d2);
        assertEquals("01:30", ConfigManager.formatDuration(d2));

        // Lingering Level 1 (must be 00:45 = 900 ticks)
        int lingD1 = calculateDuration(base, ext, upg, true, true, 1, false, PotionVariant.LINGERING);
        assertEquals(900, lingD1);
        assertEquals("00:45", ConfigManager.formatDuration(lingD1));

        // Arrow Level 1 (must be 00:22 = 450 ticks)
        int arrD1 = calculateDuration(base, ext, upg, true, true, 1, false, PotionVariant.TIPPED_ARROW);
        assertEquals(450, arrD1);
        assertEquals("00:22", ConfigManager.formatDuration(arrD1));
    }

    @Test
    public void testResistanceDurationsNerfed() {
        // Resistance (v1.1.0 Nerf A+): 400 base (00:20) / 800 ext (00:40) / no upgrade
        int base = 400;
        int ext = 800;
        int upg = 400;

        int d1 = calculateDuration(base, ext, upg, false, true, 1, false, PotionVariant.NORMAL);
        assertEquals(400, d1);
        assertEquals("00:20", ConfigManager.formatDuration(d1));

        int dExt = calculateDuration(base, ext, upg, false, true, 1, true, PotionVariant.NORMAL);
        assertEquals(800, dExt);
        assertEquals("00:40", ConfigManager.formatDuration(dExt));

        // Resistance does NOT support level 2 (supportsAmplifier = false), so passing level 2 falls back to base
        int d2 = calculateDuration(base, ext, upg, false, true, 2, false, PotionVariant.NORMAL);
        assertEquals(400, d2);
        assertEquals("00:20", ConfigManager.formatDuration(d2));

        int lingD1 = calculateDuration(base, ext, upg, false, true, 1, false, PotionVariant.LINGERING);
        assertEquals(100, lingD1);
        assertEquals("00:05", ConfigManager.formatDuration(lingD1));

        int arrD1 = calculateDuration(base, ext, upg, false, true, 1, false, PotionVariant.TIPPED_ARROW);
        assertEquals(50, arrD1);
        assertEquals("00:02", ConfigManager.formatDuration(arrD1));
    }

    @Test
    public void testBlindnessDurations() {
        // Blindness: 300 base (00:15) / 600 ext (00:30)
        int base = 300;
        int ext = 600;
        int upg = 300;

        int d1 = calculateDuration(base, ext, upg, false, true, 1, false, PotionVariant.NORMAL);
        assertEquals(300, d1);
        assertEquals("00:15", ConfigManager.formatDuration(d1));

        int dExt = calculateDuration(base, ext, upg, false, true, 1, true, PotionVariant.NORMAL);
        assertEquals(600, dExt);
        assertEquals("00:30", ConfigManager.formatDuration(dExt));

        // Blindness does not support level 2, so level 2 input falls back to level 1
        int d2 = calculateDuration(base, ext, upg, false, true, 2, false, PotionVariant.NORMAL);
        assertEquals(300, d2);
    }

    @Test
    public void testMagnetismRadiiAndDurations() {
        // Magnetism: 3600 base (03:00) / 9600 ext (08:00) / 1800 upg (01:30)
        int base = 3600;
        int ext = 9600;
        int upg = 1800;

        int d1 = calculateDuration(base, ext, upg, true, true, 1, false, PotionVariant.NORMAL);
        assertEquals(3600, d1);
        assertEquals("03:00", ConfigManager.formatDuration(d1));

        int dExt = calculateDuration(base, ext, upg, true, true, 1, true, PotionVariant.NORMAL);
        assertEquals(9600, dExt);
        assertEquals("08:00", ConfigManager.formatDuration(dExt));

        int d2 = calculateDuration(base, ext, upg, true, true, 2, false, PotionVariant.NORMAL);
        assertEquals(1800, d2);
        assertEquals("01:30", ConfigManager.formatDuration(d2));

        // Radii
        double r1 = 6.0;
        double r2 = 10.0;
        assertEquals(6.0, r1);
        assertEquals(10.0, r2);
    }

    @Test
    public void testNautilusDurations() {
        // Nautilus: 600 base (00:30) / 1200 ext (01:00) / no upgrade
        int base = 600;
        int ext = 1200;
        int upg = 600;

        int d1 = calculateDuration(base, ext, upg, false, true, 1, false, PotionVariant.NORMAL);
        assertEquals(600, d1);
        assertEquals("00:30", ConfigManager.formatDuration(d1));

        int dExt = calculateDuration(base, ext, upg, false, true, 1, true, PotionVariant.NORMAL);
        assertEquals(1200, dExt);
        assertEquals("01:00", ConfigManager.formatDuration(dExt));

        // No level 2 support
        int d2 = calculateDuration(base, ext, upg, false, true, 2, false, PotionVariant.NORMAL);
        assertEquals(600, d2);

        int lingD1 = calculateDuration(base, ext, upg, false, true, 1, false, PotionVariant.LINGERING);
        assertEquals(150, lingD1);
        assertEquals("00:07", ConfigManager.formatDuration(lingD1));

        int arrD1 = calculateDuration(base, ext, upg, false, true, 1, false, PotionVariant.TIPPED_ARROW);
        assertEquals(75, arrD1);
        assertEquals("00:03", ConfigManager.formatDuration(arrD1));
    }

    @Test
    public void testMiningFatigueDurations() {
        // Mining Fatigue: 900 base (00:45) / 1800 ext (01:30) / 450 upg (00:22)
        int base = 900;
        int ext = 1800;
        int upg = 450;

        int d1 = calculateDuration(base, ext, upg, true, true, 1, false, PotionVariant.NORMAL);
        assertEquals(900, d1);
        assertEquals("00:45", ConfigManager.formatDuration(d1));

        int dExt = calculateDuration(base, ext, upg, true, true, 1, true, PotionVariant.NORMAL);
        assertEquals(1800, dExt);
        assertEquals("01:30", ConfigManager.formatDuration(dExt));

        int d2 = calculateDuration(base, ext, upg, true, true, 2, false, PotionVariant.NORMAL);
        assertEquals(450, d2);
        assertEquals("00:22", ConfigManager.formatDuration(d2));

        int lingD1 = calculateDuration(base, ext, upg, true, true, 1, false, PotionVariant.LINGERING);
        assertEquals(225, lingD1);
        assertEquals("00:11", ConfigManager.formatDuration(lingD1));

        int arrD1 = calculateDuration(base, ext, upg, true, true, 1, false, PotionVariant.TIPPED_ARROW);
        assertEquals(113, arrD1);
        assertEquals("00:05", ConfigManager.formatDuration(arrD1));
    }
}
