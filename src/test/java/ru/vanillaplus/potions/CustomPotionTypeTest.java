package ru.vanillaplus.potions;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.junit.jupiter.api.Test;
import ru.vanillaplus.potions.model.CustomPotionType;
import ru.vanillaplus.potions.model.PotionVariant;

import static org.junit.jupiter.api.Assertions.*;

public class CustomPotionTypeTest {

    @Test
    public void testFromId() {
        assertEquals(CustomPotionType.HASTE, CustomPotionType.fromId("haste"));
        assertEquals(CustomPotionType.HASTE, CustomPotionType.fromId("HASTE"));
        assertEquals(CustomPotionType.RESISTANCE, CustomPotionType.fromId("resistance"));
        assertEquals(CustomPotionType.BLINDNESS, CustomPotionType.fromId("blindness"));
        assertEquals(CustomPotionType.MAGNETISM, CustomPotionType.fromId("magnetism"));
        assertEquals(CustomPotionType.NAUTILUS, CustomPotionType.fromId("nautilus"));
        assertEquals(CustomPotionType.MINING_FATIGUE, CustomPotionType.fromId("mining_fatigue"));
        assertEquals(CustomPotionType.MINING_FATIGUE, CustomPotionType.fromId("fatigue"));
        assertEquals(CustomPotionType.MINING_FATIGUE, CustomPotionType.fromId("miningfatigue"));
        assertEquals(CustomPotionType.MINING_FATIGUE, CustomPotionType.fromId("mining-fatigue"));
        assertEquals(CustomPotionType.MINING_FATIGUE, CustomPotionType.fromId("mining fatigue"));
        assertEquals(CustomPotionType.NAUTILUS, CustomPotionType.fromId("dolphin"));
        assertEquals(CustomPotionType.NAUTILUS, CustomPotionType.fromId("dolphins_grace"));
        assertEquals(CustomPotionType.NAUTILUS, CustomPotionType.fromId("grace"));
        assertNull(CustomPotionType.fromId("invalid_id"));
        assertNull(CustomPotionType.fromId(null));
    }

    @Test
    public void testIngredients() {
        assertEquals(Material.AMETHYST_SHARD, CustomPotionType.HASTE.getBaseIngredient());
        assertEquals(Material.ARMADILLO_SCUTE, CustomPotionType.RESISTANCE.getBaseIngredient());
        assertEquals(Material.INK_SAC, CustomPotionType.BLINDNESS.getBaseIngredient());
        assertEquals(Material.COPPER_INGOT, CustomPotionType.MAGNETISM.getBaseIngredient());
        assertEquals(Material.NAUTILUS_SHELL, CustomPotionType.NAUTILUS.getBaseIngredient());
        assertEquals(Material.FERMENTED_SPIDER_EYE, CustomPotionType.MINING_FATIGUE.getBaseIngredient());
    }

    @Test
    public void testInversions() {
        assertFalse(CustomPotionType.HASTE.isInversion());
        assertFalse(CustomPotionType.RESISTANCE.isInversion());
        assertFalse(CustomPotionType.BLINDNESS.isInversion());
        assertFalse(CustomPotionType.MAGNETISM.isInversion());
        assertFalse(CustomPotionType.NAUTILUS.isInversion());
        assertTrue(CustomPotionType.MINING_FATIGUE.isInversion());
        assertEquals(CustomPotionType.HASTE, CustomPotionType.MINING_FATIGUE.getInvertsFrom());
    }

    @Test
    public void testAmplifierAndExtensionSupport() {
        assertTrue(CustomPotionType.HASTE.supportsAmplifier());
        assertFalse(CustomPotionType.RESISTANCE.supportsAmplifier()); // Nerfed in 1.1.0!
        assertFalse(CustomPotionType.BLINDNESS.supportsAmplifier());
        assertTrue(CustomPotionType.MAGNETISM.supportsAmplifier());
        assertFalse(CustomPotionType.NAUTILUS.supportsAmplifier());
        assertTrue(CustomPotionType.MINING_FATIGUE.supportsAmplifier());

        assertTrue(CustomPotionType.HASTE.supportsExtension());
        assertTrue(CustomPotionType.RESISTANCE.supportsExtension());
        assertTrue(CustomPotionType.BLINDNESS.supportsExtension());
        assertTrue(CustomPotionType.MAGNETISM.supportsExtension());
        assertTrue(CustomPotionType.NAUTILUS.supportsExtension());
        assertTrue(CustomPotionType.MINING_FATIGUE.supportsExtension());
    }

    @Test
    public void testEffectKeys() {
        assertEquals(NamespacedKey.minecraft("haste"), CustomPotionType.HASTE.getEffectKey());
        assertEquals(NamespacedKey.minecraft("hunger"), CustomPotionType.HASTE.getSecondaryEffectKey());
        assertTrue(CustomPotionType.HASTE.hasSecondaryEffect());

        assertEquals(NamespacedKey.minecraft("resistance"), CustomPotionType.RESISTANCE.getEffectKey());
        assertEquals(NamespacedKey.minecraft("slowness"), CustomPotionType.RESISTANCE.getSecondaryEffectKey());
        assertTrue(CustomPotionType.RESISTANCE.hasSecondaryEffect());

        assertEquals(NamespacedKey.minecraft("blindness"), CustomPotionType.BLINDNESS.getEffectKey());
        assertNull(CustomPotionType.BLINDNESS.getSecondaryEffectKey());
        assertFalse(CustomPotionType.BLINDNESS.hasSecondaryEffect());

        assertNull(CustomPotionType.MAGNETISM.getEffectKey());
        assertNull(CustomPotionType.MAGNETISM.getSecondaryEffectKey());
        assertTrue(CustomPotionType.MAGNETISM.isCustomMechanic());
        assertFalse(CustomPotionType.HASTE.isCustomMechanic());

        assertEquals(NamespacedKey.minecraft("dolphins_grace"), CustomPotionType.NAUTILUS.getEffectKey());
        assertNull(CustomPotionType.NAUTILUS.getSecondaryEffectKey());
        assertFalse(CustomPotionType.NAUTILUS.hasSecondaryEffect());

        assertEquals(NamespacedKey.minecraft("mining_fatigue"), CustomPotionType.MINING_FATIGUE.getEffectKey());
        assertNull(CustomPotionType.MINING_FATIGUE.getSecondaryEffectKey());
        assertFalse(CustomPotionType.MINING_FATIGUE.hasSecondaryEffect());
    }

    @Test
    public void testVariants() {
        assertEquals(PotionVariant.NORMAL, PotionVariant.fromString("normal"));
        assertEquals(PotionVariant.SPLASH, PotionVariant.fromString("splash"));
        assertEquals(PotionVariant.LINGERING, PotionVariant.fromString("lingering"));
        assertEquals(PotionVariant.TIPPED_ARROW, PotionVariant.fromString("arrow"));

        assertEquals(Material.POTION, PotionVariant.NORMAL.getMaterial());
        assertEquals(Material.SPLASH_POTION, PotionVariant.SPLASH.getMaterial());
        assertEquals(Material.LINGERING_POTION, PotionVariant.LINGERING.getMaterial());
        assertEquals(Material.TIPPED_ARROW, PotionVariant.TIPPED_ARROW.getMaterial());

        assertEquals(1.0, PotionVariant.NORMAL.getDurationMultiplier());
        assertEquals(1.0, PotionVariant.SPLASH.getDurationMultiplier());
        assertEquals(0.25, PotionVariant.LINGERING.getDurationMultiplier());
        assertEquals(0.125, PotionVariant.TIPPED_ARROW.getDurationMultiplier());
    }
}
