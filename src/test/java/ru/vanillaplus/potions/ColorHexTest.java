package ru.vanillaplus.potions;

import org.bukkit.Color;
import org.junit.jupiter.api.Test;
import ru.vanillaplus.potions.model.CustomPotionType;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ColorHexTest {

    private Color parseHex(String hex) {
        int rgb = Integer.parseInt(hex.substring(1), 16);
        return Color.fromRGB(rgb);
    }

    @Test
    public void testHexParsing() {
        Color hasteColor = parseHex("#FFC81E");
        assertEquals(255, hasteColor.getRed());
        assertEquals(200, hasteColor.getGreen());
        assertEquals(30, hasteColor.getBlue());
        assertEquals(hasteColor, CustomPotionType.HASTE.getDefaultColor());

        Color resColor = parseHex("#99A3B3");
        assertEquals(153, resColor.getRed());
        assertEquals(163, resColor.getGreen());
        assertEquals(179, resColor.getBlue());
        assertEquals(resColor, CustomPotionType.RESISTANCE.getDefaultColor());

        Color blindColor = parseHex("#1F1F23");
        assertEquals(31, blindColor.getRed());
        assertEquals(31, blindColor.getGreen());
        assertEquals(35, blindColor.getBlue());
        assertEquals(blindColor, CustomPotionType.BLINDNESS.getDefaultColor());

        Color magColor = parseHex("#B87333");
        assertEquals(184, magColor.getRed());
        assertEquals(115, magColor.getGreen());
        assertEquals(51, magColor.getBlue());
        assertEquals(magColor, CustomPotionType.MAGNETISM.getDefaultColor());

        Color nautColor = parseHex("#00A896");
        assertEquals(0, nautColor.getRed());
        assertEquals(168, nautColor.getGreen());
        assertEquals(150, nautColor.getBlue());
        assertEquals(nautColor, CustomPotionType.NAUTILUS.getDefaultColor());

        Color fatigueColor = parseHex("#4A5443");
        assertEquals(74, fatigueColor.getRed());
        assertEquals(84, fatigueColor.getGreen());
        assertEquals(67, fatigueColor.getBlue());
        assertEquals(fatigueColor, CustomPotionType.MINING_FATIGUE.getDefaultColor());
    }
}
