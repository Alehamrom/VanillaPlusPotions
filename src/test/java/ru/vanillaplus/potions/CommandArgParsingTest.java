package ru.vanillaplus.potions;

import org.junit.jupiter.api.Test;
import ru.vanillaplus.potions.model.PotionVariant;

import static org.junit.jupiter.api.Assertions.*;

public class CommandArgParsingTest {

    record ParsedCommand(PotionVariant variant, int level, boolean extended, int amount) {}

    ParsedCommand parseGiveArgs(String[] args) {
        PotionVariant variant = PotionVariant.NORMAL;
        int level = 1;
        boolean extended = false;
        int amount = 1;

        int argIndex = 3;
        if (args.length > argIndex) {
            PotionVariant parsed = PotionVariant.fromString(args[argIndex]);
            if (parsed != null) {
                variant = parsed;
                argIndex++;
            }
        }

        if (args.length > argIndex) {
            try {
                level = Integer.parseInt(args[argIndex]);
                argIndex++;
            } catch (NumberFormatException ignored) {}
        }

        if (args.length > argIndex) {
            extended = Boolean.parseBoolean(args[argIndex])
                    || args[argIndex].equalsIgnoreCase("ext")
                    || args[argIndex].equalsIgnoreCase("true");
            argIndex++;
        }

        if (args.length > argIndex) {
            try {
                amount = Math.max(1, Math.min(64, Integer.parseInt(args[argIndex])));
            } catch (NumberFormatException ignored) {}
        }

        return new ParsedCommand(variant, level, extended, amount);
    }

    @Test
    public void testFullArguments() {
        // /vpotions give Alexsey haste splash 2 false 3
        String[] args = new String[]{"give", "Alexsey", "haste", "splash", "2", "false", "3"};
        ParsedCommand parsed = parseGiveArgs(args);
        assertEquals(PotionVariant.SPLASH, parsed.variant());
        assertEquals(2, parsed.level());
        assertFalse(parsed.extended());
        assertEquals(3, parsed.amount());
    }

    @Test
    public void testOmittedVariantWithDirectLevel() {
        // /vpotions give Alexsey haste 2
        String[] args = new String[]{"give", "Alexsey", "haste", "2"};
        ParsedCommand parsed = parseGiveArgs(args);
        assertEquals(PotionVariant.NORMAL, parsed.variant());
        assertEquals(2, parsed.level());
        assertFalse(parsed.extended());
        assertEquals(1, parsed.amount());
    }

    @Test
    public void testOmittedVariantWithExtendedAndAmount() {
        // /vpotions give Alexsey resistance 1 true 16
        String[] args = new String[]{"give", "Alexsey", "resistance", "1", "true", "16"};
        ParsedCommand parsed = parseGiveArgs(args);
        assertEquals(PotionVariant.NORMAL, parsed.variant());
        assertEquals(1, parsed.level());
        assertTrue(parsed.extended());
        assertEquals(16, parsed.amount());
    }

    @Test
    public void testOnlyTypeProvided() {
        // /vpotions give Alexsey blindness
        String[] args = new String[]{"give", "Alexsey", "blindness"};
        ParsedCommand parsed = parseGiveArgs(args);
        assertEquals(PotionVariant.NORMAL, parsed.variant());
        assertEquals(1, parsed.level());
        assertFalse(parsed.extended());
        assertEquals(1, parsed.amount());
    }

    @Test
    public void testNautilusTippedArrow() {
        // /vpotions give Alexsey nautilus arrow 1 true 64
        String[] args = new String[]{"give", "Alexsey", "nautilus", "arrow", "1", "true", "64"};
        ParsedCommand parsed = parseGiveArgs(args);
        assertEquals(PotionVariant.TIPPED_ARROW, parsed.variant());
        assertEquals(1, parsed.level());
        assertTrue(parsed.extended());
        assertEquals(64, parsed.amount());
    }

    @Test
    public void testMiningFatigueSplashLevel2() {
        // /vpotions give Alexsey mining_fatigue splash 2 false 8
        String[] args = new String[]{"give", "Alexsey", "mining_fatigue", "splash", "2", "false", "8"};
        ParsedCommand parsed = parseGiveArgs(args);
        assertEquals(PotionVariant.SPLASH, parsed.variant());
        assertEquals(2, parsed.level());
        assertFalse(parsed.extended());
        assertEquals(8, parsed.amount());
    }
}
