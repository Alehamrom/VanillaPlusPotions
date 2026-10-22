package ru.vanillaplus.potions.config;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.vanillaplus.potions.model.CustomPotionType;
import ru.vanillaplus.potions.model.PotionVariant;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ConfigManager {
    private final JavaPlugin plugin;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private FileConfiguration config;

    public ConfigManager(@NotNull JavaPlugin plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfig();
    }

    public ConfigManager(@NotNull FileConfiguration config) {
        this.plugin = null;
        this.config = config;
    }

    public void reload() {
        if (plugin != null) {
            plugin.reloadConfig();
            this.config = plugin.getConfig();
        }
    }

    public boolean isCustomTooltipsEnabled() {
        return config.getBoolean("use-custom-tooltips", true);
    }

    @NotNull
    public String getLevel2Suffix() {
        return config.getString("level-2-suffix", " II");
    }

    public boolean isMagnetismActionBarEnabled() {
        return config.getBoolean("magnetism.action-bar-enabled", true);
    }

    @NotNull
    public Component getMagnetismActionBarMessage(@NotNull String timeFormatted, double radius) {
        String template = config.getString(
                "magnetism.action-bar-message",
                "<gold>⚡ Магнетизм <yellow>%time%</yellow> <gray>(Радиус: <yellow>%radius%</yellow>м)</gray></gold>"
        );
        String formatted = template
                .replace("%time%", timeFormatted)
                .replace("%radius%", String.format(Locale.ROOT, "%.1f", radius));
        return miniMessage.deserialize(formatted);
    }

    public boolean isMagnetismParticleEnabled() {
        return config.getBoolean("magnetism.particle-enabled", true);
    }

    @NotNull
    public Particle getMagnetismParticleType() {
        String name = config.getString("magnetism.particle-type", "WAX_ON");
        try {
            return Particle.valueOf(name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return Particle.WAX_ON;
        }
    }

    public double getMagnetismPullSpeed() {
        return config.getDouble("magnetism.pull-speed", 0.38);
    }

    public long getMagnetismTickRate() {
        return Math.max(1L, config.getLong("magnetism.tick-rate", 2L));
    }

    public int getDuration(
            @NotNull CustomPotionType type,
            int level,
            boolean extended,
            @NotNull PotionVariant variant
    ) {
        String path = "potions." + type.getId() + ".durations.";
        int baseDurationTicks;
        if (level >= 2 && type.supportsAmplifier()) {
            baseDurationTicks = config.getInt(path + "upgraded", 1800);
        } else if (extended && type.supportsExtension()) {
            baseDurationTicks = config.getInt(path + "extended", 9600);
        } else {
            baseDurationTicks = config.getInt(path + "base", 3600);
        }

        double finalDuration = baseDurationTicks * variant.getDurationMultiplier();
        return Math.max(20, (int) Math.round(finalDuration));
    }

    public int getAmplifier(@NotNull CustomPotionType type, int level) {
        if (!type.supportsAmplifier() || level <= 1) {
            return config.getInt("potions." + type.getId() + ".amplifiers.level-1", 0);
        }
        return config.getInt("potions." + type.getId() + ".amplifiers.level-2", 1);
    }

    public int getSecondaryAmplifier(@NotNull CustomPotionType type, int level) {
        if (!type.supportsAmplifier() || level <= 1) {
            return config.getInt("potions." + type.getId() + ".secondary-amplifiers.level-1", 0);
        }
        return config.getInt("potions." + type.getId() + ".secondary-amplifiers.level-2", 1);
    }

    public double getMagnetismRadius(int level) {
        if (level >= 2) {
            return config.getDouble("potions.magnetism.radii.level-2", 10.0);
        }
        return config.getDouble("potions.magnetism.radii.level-1", 6.0);
    }

    @NotNull
    public Color getPotionColor(@NotNull CustomPotionType type) {
        String hex = config.getString("potions." + type.getId() + ".color");
        if (hex != null && hex.startsWith("#")) {
            try {
                int rgb = Integer.parseInt(hex.substring(1), 16);
                return Color.fromRGB(rgb);
            } catch (NumberFormatException ignored) {
            }
        }
        return type.getDefaultColor();
    }

    @NotNull
    public Component getDisplayName(
            @NotNull CustomPotionType type,
            @NotNull PotionVariant variant,
            int level,
            boolean extended
    ) {
        String subKey = switch (variant) {
            case NORMAL -> "normal";
            case SPLASH -> "splash";
            case LINGERING -> "lingering";
            case TIPPED_ARROW -> "arrow";
        };

        String template = config.getString("potions." + type.getId() + ".display-name." + subKey);
        if (template == null) {
            template = "<!italic><white>" + type.getId() + "</white>";
        }

        String levelSuffix = (level >= 2 && type.supportsAmplifier()) ? getLevel2Suffix() : "";
        String parsed = template.replace("%level_suffix%", levelSuffix);
        return miniMessage.deserialize(parsed);
    }

    @NotNull
    public List<Component> getLore(
            @NotNull CustomPotionType type,
            @NotNull PotionVariant variant,
            int level,
            boolean extended
    ) {
        List<String> rawLore = config.getStringList("potions." + type.getId() + ".lore");
        int durationTicks = getDuration(type, level, extended, variant);
        String durationStr = formatDuration(durationTicks);
        String levelSuffix = (level >= 2 && type.supportsAmplifier()) ? getLevel2Suffix() : "";
        double radius = (type == CustomPotionType.MAGNETISM) ? getMagnetismRadius(level) : 0.0;
        String radiusStr = String.format(Locale.ROOT, "%.0f", radius);

        List<Component> components = new ArrayList<>(rawLore.size());
        for (String line : rawLore) {
            String formatted = line
                    .replace("%duration%", durationStr)
                    .replace("%level_suffix%", levelSuffix)
                    .replace("%radius%", radiusStr);
            components.add(miniMessage.deserialize(formatted));
        }
        return components;
    }

    @NotNull
    public static String formatDuration(int ticks) {
        int totalSeconds = ticks / 20;
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format(Locale.ROOT, "%02d:%02d", minutes, seconds);
    }

    @NotNull
    public Component getMessage(@NotNull String key, @NotNull String... replacements) {
        String prefix = config.getString("messages.prefix", "<dark_gray>[<gold>Vanilla<yellow>+</yellow>Potions</dark_gray>] ");
        String text = config.getString("messages." + key, "<red>Message key not found: " + key + "</red>");

        for (int i = 0; i < replacements.length - 1; i += 2) {
            String placeholder = replacements[i];
            String value = replacements[i + 1];
            text = text.replace(placeholder, value);
        }

        return miniMessage.deserialize(prefix + text);
    }

    @NotNull
    public Component getRawMessage(@NotNull String key, @NotNull String... replacements) {
        String text = config.getString("messages." + key, "<red>Message key not found: " + key + "</red>");
        for (int i = 0; i < replacements.length - 1; i += 2) {
            String placeholder = replacements[i];
            String value = replacements[i + 1];
            text = text.replace(placeholder, value);
        }
        return miniMessage.deserialize(text);
    }
}
