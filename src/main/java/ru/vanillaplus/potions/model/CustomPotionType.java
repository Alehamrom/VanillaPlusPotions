package ru.vanillaplus.potions.model;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public enum CustomPotionType {
    HASTE(
            "haste",
            NamespacedKey.minecraft("haste"),
            NamespacedKey.minecraft("hunger"),
            Material.AMETHYST_SHARD,
            null,
            Color.fromRGB(255, 200, 30),
            true,
            true
    ),
    RESISTANCE(
            "resistance",
            NamespacedKey.minecraft("resistance"),
            NamespacedKey.minecraft("slowness"),
            Material.ARMADILLO_SCUTE,
            null,
            Color.fromRGB(153, 163, 179),
            false,
            true
    ),
    BLINDNESS(
            "blindness",
            NamespacedKey.minecraft("blindness"),
            null,
            Material.INK_SAC,
            null,
            Color.fromRGB(31, 31, 35),
            false,
            true
    ),
    MAGNETISM(
            "magnetism",
            null,
            null,
            Material.COPPER_INGOT,
            null,
            Color.fromRGB(184, 115, 51),
            true,
            true
    ),
    NAUTILUS(
            "nautilus",
            NamespacedKey.minecraft("dolphins_grace"),
            null,
            Material.NAUTILUS_SHELL,
            null,
            Color.fromRGB(0, 168, 150),
            false,
            true
    ),
    MINING_FATIGUE(
            "mining_fatigue",
            NamespacedKey.minecraft("mining_fatigue"),
            null,
            Material.FERMENTED_SPIDER_EYE,
            HASTE,
            Color.fromRGB(74, 84, 67),
            true,
            true
    );

    private final String id;
    private final NamespacedKey effectKey;
    private final NamespacedKey secondaryEffectKey;
    private final Material baseIngredient;
    private final CustomPotionType invertsFrom;
    private final Color defaultColor;
    private final boolean supportsAmplifier;
    private final boolean supportsExtension;

    CustomPotionType(
            @NotNull String id,
            @Nullable NamespacedKey effectKey,
            @Nullable NamespacedKey secondaryEffectKey,
            @NotNull Material baseIngredient,
            @Nullable CustomPotionType invertsFrom,
            @NotNull Color defaultColor,
            boolean supportsAmplifier,
            boolean supportsExtension
    ) {
        this.id = id;
        this.effectKey = effectKey;
        this.secondaryEffectKey = secondaryEffectKey;
        this.baseIngredient = baseIngredient;
        this.invertsFrom = invertsFrom;
        this.defaultColor = defaultColor;
        this.supportsAmplifier = supportsAmplifier;
        this.supportsExtension = supportsExtension;
    }

    @NotNull
    public String getId() {
        return id;
    }

    @Nullable
    public NamespacedKey getEffectKey() {
        return effectKey;
    }

    @Nullable
    public NamespacedKey getSecondaryEffectKey() {
        return secondaryEffectKey;
    }

    public boolean hasSecondaryEffect() {
        return secondaryEffectKey != null;
    }

    @Nullable
    public PotionEffectType getVanillaEffect() {
        if (effectKey == null) return null;
        try {
            PotionEffectType reg = org.bukkit.Registry.POTION_EFFECT_TYPE.get(effectKey);
            if (reg != null) return reg;
        } catch (Throwable ignored) {
        }
        return PotionEffectType.getByKey(effectKey);
    }

    @Nullable
    public PotionEffectType getSecondaryVanillaEffect() {
        if (secondaryEffectKey == null) return null;
        try {
            PotionEffectType reg = org.bukkit.Registry.POTION_EFFECT_TYPE.get(secondaryEffectKey);
            if (reg != null) return reg;
        } catch (Throwable ignored) {
        }
        return PotionEffectType.getByKey(secondaryEffectKey);
    }

    public boolean isCustomMechanic() {
        return effectKey == null;
    }

    @NotNull
    public Material getBaseIngredient() {
        return baseIngredient;
    }

    public boolean isInversion() {
        return invertsFrom != null;
    }

    @Nullable
    public CustomPotionType getInvertsFrom() {
        return invertsFrom;
    }

    @NotNull
    public Color getDefaultColor() {
        return defaultColor;
    }

    public boolean supportsAmplifier() {
        return supportsAmplifier;
    }

    public boolean supportsExtension() {
        return supportsExtension;
    }

    @Nullable
    public static CustomPotionType fromId(@Nullable String id) {
        if (id == null) return null;
        String clean = id.trim().toLowerCase(Locale.ROOT).replace(' ', '_').replace('-', '_');
        for (CustomPotionType type : values()) {
            if (type.id.equalsIgnoreCase(clean) || type.name().equalsIgnoreCase(clean)) {
                return type;
            }
        }
        if (clean.equals("fatigue") || clean.equals("miningfatigue")) {
            return MINING_FATIGUE;
        }
        if (clean.equals("dolphin") || clean.equals("dolphins_grace") || clean.equals("grace")) {
            return NAUTILUS;
        }
        return null;
    }
}
