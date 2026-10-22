package ru.vanillaplus.potions;

import org.bukkit.Color;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ru.vanillaplus.potions.model.CustomPotionType;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ConfigFileTest {

    private static FileConfiguration config;

    @BeforeAll
    public static void loadConfig() {
        File configFile = new File("src/main/resources/config.yml");
        assertTrue(configFile.exists(), "config.yml must exist in src/main/resources");
        config = YamlConfiguration.loadConfiguration(configFile);
        assertNotNull(config, "config.yml must parse cleanly into YamlConfiguration");
    }

    @Test
    public void testVersionAndGlobals() {
        assertEquals("1.1.0", config.getString("version"));
        assertTrue(config.getBoolean("use-custom-tooltips"));
        assertEquals(" II", config.getString("level-2-suffix"));
    }

    @Test
    public void testAllPotionsPresent() {
        for (CustomPotionType type : CustomPotionType.values()) {
            assertTrue(config.contains("potions." + type.getId()),
                    "config.yml must contain configuration for potion type: " + type.getId());
        }
    }

    @Test
    public void testColorsValidHex() {
        for (CustomPotionType type : CustomPotionType.values()) {
            String hex = config.getString("potions." + type.getId() + ".color");
            assertNotNull(hex, "Color must be defined for " + type.getId());
            assertTrue(hex.startsWith("#") && hex.length() == 7,
                    "Color must be a valid 6-char hex string starting with # for " + type.getId());

            int rgb = Integer.parseInt(hex.substring(1), 16);
            Color parsed = Color.fromRGB(rgb);
            assertEquals(type.getDefaultColor(), parsed,
                    "Configured color for " + type.getId() + " must match CustomPotionType defaultColor");
        }
    }

    @Test
    public void testResistanceNerfDurationsAndDebuff() {
        int base = config.getInt("potions.resistance.durations.base");
        int ext = config.getInt("potions.resistance.durations.extended");

        // 20s (400 ticks) base, 40s (800 ticks) extended
        assertEquals(400, base);
        assertEquals(800, ext);

        // Secondary debuff configured
        assertTrue(config.contains("potions.resistance.secondary-amplifiers.level-1"));
        assertEquals(0, config.getInt("potions.resistance.secondary-amplifiers.level-1"));

        // Lore includes Slowness
        List<String> lore = config.getStringList("potions.resistance.lore");
        assertFalse(lore.isEmpty());
        assertTrue(lore.stream().anyMatch(l -> l.contains("Замедление")),
                "Resistance lore must mention Замедление debuff");
    }

    @Test
    public void testHasteDebuffConfiguration() {
        assertEquals(3600, config.getInt("potions.haste.durations.base"));
        assertEquals(9600, config.getInt("potions.haste.durations.extended"));
        assertEquals(1800, config.getInt("potions.haste.durations.upgraded"));

        // Secondary debuff amplifiers (Hunger I for Haste I, Hunger II for Haste II)
        assertEquals(0, config.getInt("potions.haste.secondary-amplifiers.level-1"));
        assertEquals(1, config.getInt("potions.haste.secondary-amplifiers.level-2"));

        List<String> lore = config.getStringList("potions.haste.lore");
        assertTrue(lore.stream().anyMatch(l -> l.contains("Голод")),
                "Haste lore must mention Голод debuff");
    }

    @Test
    public void testNautilusConfiguration() {
        // 30s base (600 ticks), 60s ext (1200 ticks)
        assertEquals(600, config.getInt("potions.nautilus.durations.base"));
        assertEquals(1200, config.getInt("potions.nautilus.durations.extended"));
        assertEquals("#00A896", config.getString("potions.nautilus.color"));

        List<String> lore = config.getStringList("potions.nautilus.lore");
        assertTrue(lore.stream().anyMatch(l -> l.contains("Грация дельфина")),
                "Nautilus lore must mention Грация дельфина");
    }

    @Test
    public void testMiningFatigueConfiguration() {
        // 45s base (900 ticks), 1:30 ext (1800 ticks), 22s upg (450 ticks)
        assertEquals(900, config.getInt("potions.mining_fatigue.durations.base"));
        assertEquals(1800, config.getInt("potions.mining_fatigue.durations.extended"));
        assertEquals(450, config.getInt("potions.mining_fatigue.durations.upgraded"));
        assertEquals("#4A5443", config.getString("potions.mining_fatigue.color"));

        assertEquals(0, config.getInt("potions.mining_fatigue.amplifiers.level-1"));
        assertEquals(1, config.getInt("potions.mining_fatigue.amplifiers.level-2"));

        List<String> lore = config.getStringList("potions.mining_fatigue.lore");
        assertTrue(lore.stream().anyMatch(l -> l.contains("Утомление")),
                "Mining Fatigue lore must mention Утомление");
    }

    @Test
    public void testMessagesIncludeAllPotions() {
        String unknownMsg = config.getString("messages.unknown-potion");
        assertNotNull(unknownMsg);
        for (CustomPotionType type : CustomPotionType.values()) {
            assertTrue(unknownMsg.contains(type.getId()),
                    "unknown-potion message must mention type: " + type.getId());
        }
    }
}
