package ru.vanillaplus.potions.model;

import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public enum PotionVariant {
    NORMAL(Material.POTION, 1.0),
    SPLASH(Material.SPLASH_POTION, 1.0),
    LINGERING(Material.LINGERING_POTION, 0.25),
    TIPPED_ARROW(Material.TIPPED_ARROW, 0.125);

    private final Material material;
    private final double durationMultiplier;

    PotionVariant(@NotNull Material material, double durationMultiplier) {
        this.material = material;
        this.durationMultiplier = durationMultiplier;
    }

    @NotNull
    public Material getMaterial() {
        return material;
    }

    public double getDurationMultiplier() {
        return durationMultiplier;
    }

    @Nullable
    public static PotionVariant fromMaterial(@Nullable Material material) {
        if (material == null) return null;
        for (PotionVariant variant : values()) {
            if (variant.material == material) {
                return variant;
            }
        }
        return null;
    }

    @Nullable
    public static PotionVariant fromString(@Nullable String name) {
        if (name == null) return null;
        String clean = name.trim().toLowerCase(Locale.ROOT);
        switch (clean) {
            case "normal", "drinkable", "regular", "bottle", "potion" -> {
                return NORMAL;
            }
            case "splash", "thrown" -> {
                return SPLASH;
            }
            case "lingering", "cloud" -> {
                return LINGERING;
            }
            case "arrow", "tipped_arrow", "tipped" -> {
                return TIPPED_ARROW;
            }
            default -> {
                for (PotionVariant v : values()) {
                    if (v.name().equalsIgnoreCase(clean)) {
                        return v;
                    }
                }
                return null;
            }
        }
    }
}
