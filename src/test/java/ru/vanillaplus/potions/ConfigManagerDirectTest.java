package ru.vanillaplus.potions;

import net.kyori.adventure.text.Component;
import org.bukkit.Color;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ru.vanillaplus.potions.config.ConfigManager;
import ru.vanillaplus.potions.model.CustomPotionType;
import ru.vanillaplus.potions.model.PotionVariant;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ConfigManagerDirectTest {

    private static ConfigManager configManager;

    @BeforeAll
    public static void setup() {
        File configFile = new File("src/main/resources/config.yml");
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(configFile);
        configManager = new ConfigManager(yaml);
    }

    @Test
    public void testResistanceDurationsDirect() {
        // Base normal (400 ticks / 20 sec)
        int baseDur = configManager.getDuration(CustomPotionType.RESISTANCE, 1, false, PotionVariant.NORMAL);
        assertEquals(400, baseDur);
        assertEquals("00:20", ConfigManager.formatDuration(baseDur));

        // Extended normal (800 ticks / 40 sec)
        int extDur = configManager.getDuration(CustomPotionType.RESISTANCE, 1, true, PotionVariant.NORMAL);
        assertEquals(800, extDur);
        assertEquals("00:40", ConfigManager.formatDuration(extDur));

        // Level 2 should fall back to base because supportsAmplifier is false
        int upgDur = configManager.getDuration(CustomPotionType.RESISTANCE, 2, false, PotionVariant.NORMAL);
        assertEquals(400, upgDur);

        // Lingering base (100 ticks / 5 sec)
        int lingDur = configManager.getDuration(CustomPotionType.RESISTANCE, 1, false, PotionVariant.LINGERING);
        assertEquals(100, lingDur);
        assertEquals("00:05", ConfigManager.formatDuration(lingDur));

        // Arrow base (50 ticks / 2 sec)
        int arrDur = configManager.getDuration(CustomPotionType.RESISTANCE, 1, false, PotionVariant.TIPPED_ARROW);
        assertEquals(50, arrDur);
        assertEquals("00:02", ConfigManager.formatDuration(arrDur));
    }

    @Test
    public void testNautilusDurationsDirect() {
        // Base normal (600 ticks / 30 sec)
        int baseDur = configManager.getDuration(CustomPotionType.NAUTILUS, 1, false, PotionVariant.NORMAL);
        assertEquals(600, baseDur);
        assertEquals("00:30", ConfigManager.formatDuration(baseDur));

        // Extended normal (1200 ticks / 1 min)
        int extDur = configManager.getDuration(CustomPotionType.NAUTILUS, 1, true, PotionVariant.NORMAL);
        assertEquals(1200, extDur);
        assertEquals("01:00", ConfigManager.formatDuration(extDur));

        // Level 2 should fall back to base
        int upgDur = configManager.getDuration(CustomPotionType.NAUTILUS, 2, false, PotionVariant.NORMAL);
        assertEquals(600, upgDur);

        // Lingering (150 ticks / 7 sec)
        int lingDur = configManager.getDuration(CustomPotionType.NAUTILUS, 1, false, PotionVariant.LINGERING);
        assertEquals(150, lingDur);
        assertEquals("00:07", ConfigManager.formatDuration(lingDur));

        // Arrow (75 ticks / 3 sec)
        int arrDur = configManager.getDuration(CustomPotionType.NAUTILUS, 1, false, PotionVariant.TIPPED_ARROW);
        assertEquals(75, arrDur);
        assertEquals("00:03", ConfigManager.formatDuration(arrDur));
    }

    @Test
    public void testMiningFatigueDurationsDirect() {
        // Base normal (900 ticks / 45 sec)
        int baseDur = configManager.getDuration(CustomPotionType.MINING_FATIGUE, 1, false, PotionVariant.NORMAL);
        assertEquals(900, baseDur);
        assertEquals("00:45", ConfigManager.formatDuration(baseDur));

        // Extended normal (1800 ticks / 1:30)
        int extDur = configManager.getDuration(CustomPotionType.MINING_FATIGUE, 1, true, PotionVariant.NORMAL);
        assertEquals(1800, extDur);
        assertEquals("01:30", ConfigManager.formatDuration(extDur));

        // Upgraded normal (450 ticks / 22 sec)
        int upgDur = configManager.getDuration(CustomPotionType.MINING_FATIGUE, 2, false, PotionVariant.NORMAL);
        assertEquals(450, upgDur);
        assertEquals("00:22", ConfigManager.formatDuration(upgDur));

        // Lingering base (225 ticks / 11 sec)
        int lingDur = configManager.getDuration(CustomPotionType.MINING_FATIGUE, 1, false, PotionVariant.LINGERING);
        assertEquals(225, lingDur);
        assertEquals("00:11", ConfigManager.formatDuration(lingDur));

        // Arrow base (113 ticks / 5 sec)
        int arrDur = configManager.getDuration(CustomPotionType.MINING_FATIGUE, 1, false, PotionVariant.TIPPED_ARROW);
        assertEquals(113, arrDur);
        assertEquals("00:05", ConfigManager.formatDuration(arrDur));
    }

    @Test
    public void testSecondaryAmplifiersDirect() {
        // Haste: Hunger I for Haste I, Hunger II for Haste II
        assertEquals(0, configManager.getSecondaryAmplifier(CustomPotionType.HASTE, 1));
        assertEquals(1, configManager.getSecondaryAmplifier(CustomPotionType.HASTE, 2));

        // Resistance: Slowness I (level 1), level 2 falls back to level 1 because supportsAmplifier is false
        assertEquals(0, configManager.getSecondaryAmplifier(CustomPotionType.RESISTANCE, 1));
        assertEquals(0, configManager.getSecondaryAmplifier(CustomPotionType.RESISTANCE, 2));
    }

    @Test
    public void testColorsDirect() {
        assertEquals(Color.fromRGB(0x00, 0xA8, 0x96), configManager.getPotionColor(CustomPotionType.NAUTILUS));
        assertEquals(Color.fromRGB(0x4A, 0x54, 0x43), configManager.getPotionColor(CustomPotionType.MINING_FATIGUE));
        assertEquals(Color.fromRGB(0x99, 0xA3, 0xB3), configManager.getPotionColor(CustomPotionType.RESISTANCE));
        assertEquals(Color.fromRGB(0xFF, 0xC8, 0x1E), configManager.getPotionColor(CustomPotionType.HASTE));
        assertEquals(Color.fromRGB(0x1F, 0x1F, 0x23), configManager.getPotionColor(CustomPotionType.BLINDNESS));
        assertEquals(Color.fromRGB(0xB8, 0x73, 0x33), configManager.getPotionColor(CustomPotionType.MAGNETISM));
    }

    @Test
    public void testDisplayNamesAndLoreComponents() {
        for (CustomPotionType type : CustomPotionType.values()) {
            for (PotionVariant variant : PotionVariant.values()) {
                Component name = configManager.getDisplayName(type, variant, 1, false);
                assertNotNull(name, "Display name must not be null for " + type + " / " + variant);

                List<Component> lore = configManager.getLore(type, variant, 1, false);
                assertNotNull(lore);
                assertFalse(lore.isEmpty(), "Lore must not be empty for " + type + " / " + variant);
            }
        }
    }
}
