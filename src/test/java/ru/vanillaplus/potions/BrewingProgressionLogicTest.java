package ru.vanillaplus.potions;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;
import ru.vanillaplus.potions.model.CustomPotionType;
import ru.vanillaplus.potions.model.PotionVariant;

import static org.junit.jupiter.api.Assertions.*;

public class BrewingProgressionLogicTest {

    record MockPotion(CustomPotionType type, PotionVariant variant, int level, boolean extended) {}

    MockPotion simulateProgression(Material ingredient, MockPotion current) {
        if (current == null) return null;
        CustomPotionType type = current.type();
        PotionVariant variant = current.variant();
        int level = current.level();
        boolean extended = current.extended();

        // Inversion: Fermented Spider Eye
        if (ingredient == Material.FERMENTED_SPIDER_EYE) {
            for (CustomPotionType candidate : CustomPotionType.values()) {
                if (candidate.isInversion() && candidate.getInvertsFrom() == type) {
                    int targetLevel = (level >= 2 && candidate.supportsAmplifier()) ? 2 : 1;
                    boolean targetExtended = extended && candidate.supportsExtension();
                    return new MockPotion(candidate, variant, targetLevel, targetExtended);
                }
            }
            return null;
        }

        // Redstone -> Extended (only level 1, non-extended)
        if (ingredient == Material.REDSTONE) {
            if (type.supportsExtension() && !extended && level == 1) {
                return new MockPotion(type, variant, 1, true);
            }
            return null;
        }

        // Glowstone -> Upgraded (Level 2, only level 1, non-extended)
        if (ingredient == Material.GLOWSTONE_DUST) {
            if (type.supportsAmplifier() && level == 1 && !extended) {
                return new MockPotion(type, variant, 2, false);
            }
            return null;
        }

        // Gunpowder -> Splash
        if (ingredient == Material.GUNPOWDER) {
            if (variant == PotionVariant.NORMAL) {
                return new MockPotion(type, PotionVariant.SPLASH, level, extended);
            }
            return null;
        }

        // Dragon's Breath -> Lingering
        if (ingredient == Material.DRAGON_BREATH) {
            if (variant == PotionVariant.SPLASH) {
                return new MockPotion(type, PotionVariant.LINGERING, level, extended);
            }
            return null;
        }

        return null;
    }

    @Test
    public void testValidHasteProgressions() {
        MockPotion base = new MockPotion(CustomPotionType.HASTE, PotionVariant.NORMAL, 1, false);

        // Base -> Extended
        MockPotion ext = simulateProgression(Material.REDSTONE, base);
        assertNotNull(ext);
        assertTrue(ext.extended());
        assertEquals(1, ext.level());
        assertEquals(PotionVariant.NORMAL, ext.variant());

        // Base -> Upgraded (II)
        MockPotion upg = simulateProgression(Material.GLOWSTONE_DUST, base);
        assertNotNull(upg);
        assertFalse(upg.extended());
        assertEquals(2, upg.level());

        // Base -> Splash -> Lingering
        MockPotion splash = simulateProgression(Material.GUNPOWDER, base);
        assertNotNull(splash);
        assertEquals(PotionVariant.SPLASH, splash.variant());

        MockPotion lingering = simulateProgression(Material.DRAGON_BREATH, splash);
        assertNotNull(lingering);
        assertEquals(PotionVariant.LINGERING, lingering.variant());
    }

    @Test
    public void testMiningFatigueInversionFromHaste() {
        // 1. Haste Base + Spider Eye -> Mining Fatigue Base
        MockPotion hasteBase = new MockPotion(CustomPotionType.HASTE, PotionVariant.NORMAL, 1, false);
        MockPotion fatigueBase = simulateProgression(Material.FERMENTED_SPIDER_EYE, hasteBase);
        assertNotNull(fatigueBase);
        assertEquals(CustomPotionType.MINING_FATIGUE, fatigueBase.type());
        assertEquals(1, fatigueBase.level());
        assertFalse(fatigueBase.extended());
        assertEquals(PotionVariant.NORMAL, fatigueBase.variant());

        // 2. Haste Ext + Spider Eye -> Mining Fatigue Ext
        MockPotion hasteExt = new MockPotion(CustomPotionType.HASTE, PotionVariant.NORMAL, 1, true);
        MockPotion fatigueExt = simulateProgression(Material.FERMENTED_SPIDER_EYE, hasteExt);
        assertNotNull(fatigueExt);
        assertEquals(CustomPotionType.MINING_FATIGUE, fatigueExt.type());
        assertEquals(1, fatigueExt.level());
        assertTrue(fatigueExt.extended());

        // 3. Haste II + Spider Eye -> Mining Fatigue II
        MockPotion hasteUpg = new MockPotion(CustomPotionType.HASTE, PotionVariant.NORMAL, 2, false);
        MockPotion fatigueUpg = simulateProgression(Material.FERMENTED_SPIDER_EYE, hasteUpg);
        assertNotNull(fatigueUpg);
        assertEquals(CustomPotionType.MINING_FATIGUE, fatigueUpg.type());
        assertEquals(2, fatigueUpg.level());
        assertFalse(fatigueUpg.extended());

        // 4. Haste Splash II + Spider Eye -> Mining Fatigue Splash II
        MockPotion hasteSplash2 = new MockPotion(CustomPotionType.HASTE, PotionVariant.SPLASH, 2, false);
        MockPotion fatigueSplash2 = simulateProgression(Material.FERMENTED_SPIDER_EYE, hasteSplash2);
        assertNotNull(fatigueSplash2);
        assertEquals(CustomPotionType.MINING_FATIGUE, fatigueSplash2.type());
        assertEquals(PotionVariant.SPLASH, fatigueSplash2.variant());
        assertEquals(2, fatigueSplash2.level());
    }

    @Test
    public void testMiningFatigueDirectProgression() {
        MockPotion fatigueBase = new MockPotion(CustomPotionType.MINING_FATIGUE, PotionVariant.NORMAL, 1, false);

        // Mining Fatigue Base + Redstone -> Mining Fatigue Extended
        MockPotion ext = simulateProgression(Material.REDSTONE, fatigueBase);
        assertNotNull(ext);
        assertTrue(ext.extended());
        assertEquals(1, ext.level());

        // Mining Fatigue Base + Glowstone -> Mining Fatigue II
        MockPotion upg = simulateProgression(Material.GLOWSTONE_DUST, fatigueBase);
        assertNotNull(upg);
        assertEquals(2, upg.level());
        assertFalse(upg.extended());

        // Mining Fatigue Normal + Gunpowder -> Splash
        MockPotion splash = simulateProgression(Material.GUNPOWDER, fatigueBase);
        assertNotNull(splash);
        assertEquals(PotionVariant.SPLASH, splash.variant());

        // Mining Fatigue Splash + Dragon's Breath -> Lingering
        MockPotion ling = simulateProgression(Material.DRAGON_BREATH, splash);
        assertNotNull(ling);
        assertEquals(PotionVariant.LINGERING, ling.variant());
    }

    @Test
    public void testNautilusProgressions() {
        MockPotion nautBase = new MockPotion(CustomPotionType.NAUTILUS, PotionVariant.NORMAL, 1, false);

        // Nautilus Base + Redstone -> Extended
        MockPotion ext = simulateProgression(Material.REDSTONE, nautBase);
        assertNotNull(ext);
        assertTrue(ext.extended());
        assertEquals(1, ext.level());

        // Nautilus Base + Glowstone -> rejected (no amplifier)
        assertNull(simulateProgression(Material.GLOWSTONE_DUST, nautBase));

        // Nautilus Normal + Gunpowder -> Splash
        MockPotion splash = simulateProgression(Material.GUNPOWDER, nautBase);
        assertNotNull(splash);
        assertEquals(PotionVariant.SPLASH, splash.variant());

        // Nautilus Splash + Dragon's Breath -> Lingering
        MockPotion ling = simulateProgression(Material.DRAGON_BREATH, splash);
        assertNotNull(ling);
        assertEquals(PotionVariant.LINGERING, ling.variant());
    }

    @Test
    public void testResistanceNerfNoAmplifier() {
        MockPotion resBase = new MockPotion(CustomPotionType.RESISTANCE, PotionVariant.NORMAL, 1, false);

        // Resistance Base + Glowstone -> rejected in 1.1.0!
        assertNull(simulateProgression(Material.GLOWSTONE_DUST, resBase));

        // Resistance Base + Redstone -> Extended (supported)
        MockPotion ext = simulateProgression(Material.REDSTONE, resBase);
        assertNotNull(ext);
        assertTrue(ext.extended());
        assertEquals(1, ext.level());
    }

    @Test
    public void testIllegalProgressionsRejected() {
        // 1. Cannot extend an already extended potion
        MockPotion extHaste = new MockPotion(CustomPotionType.HASTE, PotionVariant.NORMAL, 1, true);
        assertNull(simulateProgression(Material.REDSTONE, extHaste));

        // 2. Cannot upgrade an already extended potion
        assertNull(simulateProgression(Material.GLOWSTONE_DUST, extHaste));

        // 3. Cannot extend an upgraded potion
        MockPotion upgHaste = new MockPotion(CustomPotionType.HASTE, PotionVariant.NORMAL, 2, false);
        assertNull(simulateProgression(Material.REDSTONE, upgHaste));

        // 4. Cannot upgrade an already upgraded potion
        assertNull(simulateProgression(Material.GLOWSTONE_DUST, upgHaste));

        // 5. Blindness does not support amplifier (glowstone)
        MockPotion blindBase = new MockPotion(CustomPotionType.BLINDNESS, PotionVariant.NORMAL, 1, false);
        assertNull(simulateProgression(Material.GLOWSTONE_DUST, blindBase));

        // 6. Gunpowder on already splash potion is rejected
        MockPotion splash = new MockPotion(CustomPotionType.HASTE, PotionVariant.SPLASH, 1, false);
        assertNull(simulateProgression(Material.GUNPOWDER, splash));

        // 7. Dragon's breath on normal potion is rejected (must be splash first)
        assertNull(simulateProgression(Material.DRAGON_BREATH, extHaste));

        // 8. Spider eye on Resistance (no inversion defined) is rejected
        MockPotion resBase = new MockPotion(CustomPotionType.RESISTANCE, PotionVariant.NORMAL, 1, false);
        assertNull(simulateProgression(Material.FERMENTED_SPIDER_EYE, resBase));

        // 9. Spider eye on Mining Fatigue (no inversion defined) is rejected
        MockPotion fatigueBase = new MockPotion(CustomPotionType.MINING_FATIGUE, PotionVariant.NORMAL, 1, false);
        assertNull(simulateProgression(Material.FERMENTED_SPIDER_EYE, fatigueBase));
    }
}
