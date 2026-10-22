package ru.vanillaplus.potions.brewing;

import io.papermc.paper.potion.PotionMix;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionBrewer;
import org.jetbrains.annotations.NotNull;
import ru.vanillaplus.potions.config.ConfigManager;
import ru.vanillaplus.potions.model.CustomPotionType;
import ru.vanillaplus.potions.model.PotionVariant;
import ru.vanillaplus.potions.util.PotionItemFactory;

import java.util.ArrayList;
import java.util.List;

public class BrewingRegistry {
    private final JavaPlugin plugin;
    private final PotionItemFactory factory;
    private final ConfigManager configManager;

    private final List<NamespacedKey> registeredPotionMixKeys = new ArrayList<>();
    private final List<NamespacedKey> registeredCraftingKeys = new ArrayList<>();

    public BrewingRegistry(
            @NotNull JavaPlugin plugin,
            @NotNull PotionItemFactory factory,
            @NotNull ConfigManager configManager
    ) {
        this.plugin = plugin;
        this.factory = factory;
        this.configManager = configManager;
    }

    public void registerAll() {
        unregisterAll();
        registerPotionMixes();
        registerTippedArrowRecipes();
        plugin.getLogger().info("Registered " + registeredPotionMixKeys.size() + " brewing mixes and "
                + registeredCraftingKeys.size() + " crafting recipes.");
    }

    public void unregisterAll() {
        PotionBrewer brewer = Bukkit.getPotionBrewer();
        for (NamespacedKey key : registeredPotionMixKeys) {
            try {
                brewer.removePotionMix(key);
            } catch (Exception ignored) {
            }
        }
        registeredPotionMixKeys.clear();

        for (NamespacedKey key : registeredCraftingKeys) {
            try {
                Bukkit.removeRecipe(key);
            } catch (Exception ignored) {
            }
        }
        registeredCraftingKeys.clear();
    }

    private void registerPotionMixes() {
        PotionBrewer brewer = Bukkit.getPotionBrewer();

        for (CustomPotionType type : CustomPotionType.values()) {
            for (PotionVariant variant : new PotionVariant[]{PotionVariant.NORMAL, PotionVariant.SPLASH, PotionVariant.LINGERING}) {
                String vName = variant.name().toLowerCase(java.util.Locale.ROOT);
                if (!type.isInversion()) {
                    // 1. Base Potion: Awkward + Base Ingredient -> Variant Base (Level 1, Non-extended)
                    ItemStack awkwardItem = PotionItemFactory.getAwkwardPotion(variant);
                    ItemStack basePotion = factory.createPotion(type, variant, 1, false);
                    NamespacedKey baseKey = new NamespacedKey(plugin, "brew_" + type.getId() + "_base_" + vName);
                    PotionMix baseMix = new PotionMix(
                            baseKey,
                            basePotion,
                            new RecipeChoice.ExactChoice(awkwardItem),
                            new RecipeChoice.MaterialChoice(type.getBaseIngredient())
                    );
                    addMixSafe(brewer, baseMix, baseKey);
                } else {
                    // Inversion: Parent Potion (any variant/tier) + Inversion Ingredient -> Inverted Potion
                    CustomPotionType parentType = type.getInvertsFrom();
                    if (parentType != null) {
                        // 1. Base parent -> Base inverted
                        ItemStack parentBase = factory.createPotion(parentType, variant, 1, false);
                        ItemStack invertedBase = factory.createPotion(type, variant, 1, false);
                        NamespacedKey invBaseKey = new NamespacedKey(plugin, "brew_" + type.getId() + "_inv_base_" + vName);
                        PotionMix invBaseMix = new PotionMix(
                                invBaseKey,
                                invertedBase,
                                new RecipeChoice.ExactChoice(parentBase),
                                new RecipeChoice.MaterialChoice(type.getBaseIngredient())
                        );
                        addMixSafe(brewer, invBaseMix, invBaseKey);

                        // 2. Extended parent -> Extended inverted
                        if (parentType.supportsExtension() && type.supportsExtension()) {
                            ItemStack parentExt = factory.createPotion(parentType, variant, 1, true);
                            ItemStack invertedExt = factory.createPotion(type, variant, 1, true);
                            NamespacedKey invExtKey = new NamespacedKey(plugin, "brew_" + type.getId() + "_inv_ext_" + vName);
                            PotionMix invExtMix = new PotionMix(
                                    invExtKey,
                                    invertedExt,
                                    new RecipeChoice.ExactChoice(parentExt),
                                    new RecipeChoice.MaterialChoice(type.getBaseIngredient())
                            );
                            addMixSafe(brewer, invExtMix, invExtKey);
                        }

                        // 3. Upgraded parent -> Upgraded inverted
                        if (parentType.supportsAmplifier() && type.supportsAmplifier()) {
                            ItemStack parentUpg = factory.createPotion(parentType, variant, 2, false);
                            ItemStack invertedUpg = factory.createPotion(type, variant, 2, false);
                            NamespacedKey invUpgKey = new NamespacedKey(plugin, "brew_" + type.getId() + "_inv_upg_" + vName);
                            PotionMix invUpgMix = new PotionMix(
                                    invUpgKey,
                                    invertedUpg,
                                    new RecipeChoice.ExactChoice(parentUpg),
                                    new RecipeChoice.MaterialChoice(type.getBaseIngredient())
                            );
                            addMixSafe(brewer, invUpgMix, invUpgKey);
                        }
                    }
                }

                ItemStack basePotion = factory.createPotion(type, variant, 1, false);

                // 2. Extended: Variant Base + Redstone -> Variant Extended (Level 1, Extended)
                if (type.supportsExtension()) {
                    ItemStack extPotion = factory.createPotion(type, variant, 1, true);
                    NamespacedKey extKey = new NamespacedKey(plugin, "brew_" + type.getId() + "_ext_" + vName);
                    PotionMix extMix = new PotionMix(
                            extKey,
                            extPotion,
                            new RecipeChoice.ExactChoice(basePotion),
                            new RecipeChoice.MaterialChoice(Material.REDSTONE)
                    );
                    addMixSafe(brewer, extMix, extKey);
                }

                // 3. Upgraded: Variant Base + Glowstone -> Variant Upgraded (Level 2, Non-extended)
                if (type.supportsAmplifier()) {
                    ItemStack upgPotion = factory.createPotion(type, variant, 2, false);
                    NamespacedKey upgKey = new NamespacedKey(plugin, "brew_" + type.getId() + "_upg_" + vName);
                    PotionMix upgMix = new PotionMix(
                            upgKey,
                            upgPotion,
                            new RecipeChoice.ExactChoice(basePotion),
                            new RecipeChoice.MaterialChoice(Material.GLOWSTONE_DUST)
                    );
                    addMixSafe(brewer, upgMix, upgKey);
                }
            }

            // Conversions between forms (Normal -> Splash with Gunpowder, Splash -> Lingering with Dragon's Breath)
            registerConversions(brewer, type, 1, false, "base");
            if (type.supportsExtension()) {
                registerConversions(brewer, type, 1, true, "ext");
            }
            if (type.supportsAmplifier()) {
                registerConversions(brewer, type, 2, false, "upg");
            }
        }
    }

    private void registerConversions(
            @NotNull PotionBrewer brewer,
            @NotNull CustomPotionType type,
            int level,
            boolean extended,
            @NotNull String suffix
    ) {
        ItemStack normalPotion = factory.createPotion(type, PotionVariant.NORMAL, level, extended);
        ItemStack splashPotion = factory.createPotion(type, PotionVariant.SPLASH, level, extended);
        ItemStack lingeringPotion = factory.createPotion(type, PotionVariant.LINGERING, level, extended);

        // Normal -> Splash
        NamespacedKey splashKey = new NamespacedKey(plugin, "brew_" + type.getId() + "_splash_" + suffix);
        PotionMix splashMix = new PotionMix(
                splashKey,
                splashPotion,
                new RecipeChoice.ExactChoice(normalPotion),
                new RecipeChoice.MaterialChoice(Material.GUNPOWDER)
        );
        addMixSafe(brewer, splashMix, splashKey);

        // Splash -> Lingering
        NamespacedKey lingeringKey = new NamespacedKey(plugin, "brew_" + type.getId() + "_lingering_" + suffix);
        PotionMix lingeringMix = new PotionMix(
                lingeringKey,
                lingeringPotion,
                new RecipeChoice.ExactChoice(splashPotion),
                new RecipeChoice.MaterialChoice(Material.DRAGON_BREATH)
        );
        addMixSafe(brewer, lingeringMix, lingeringKey);
    }

    private void addMixSafe(PotionBrewer brewer, PotionMix mix, NamespacedKey key) {
        try {
            brewer.addPotionMix(mix);
            registeredPotionMixKeys.add(key);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to register potion mix " + key + ": " + e.getMessage());
        }
    }

    private void registerTippedArrowRecipes() {
        for (CustomPotionType type : CustomPotionType.values()) {
            registerArrowCrafting(type, 1, false, "base");
            if (type.supportsExtension()) {
                registerArrowCrafting(type, 1, true, "ext");
            }
            if (type.supportsAmplifier()) {
                registerArrowCrafting(type, 2, false, "upg");
            }
        }
    }

    private void registerArrowCrafting(
            @NotNull CustomPotionType type,
            int level,
            boolean extended,
            @NotNull String suffix
    ) {
        ItemStack lingeringPotion = factory.createPotion(type, PotionVariant.LINGERING, level, extended, 1);
        ItemStack arrowResult = factory.createPotion(type, PotionVariant.TIPPED_ARROW, level, extended, 8);

        NamespacedKey key = new NamespacedKey(plugin, "craft_arrow_" + type.getId() + "_" + suffix);
        ShapedRecipe recipe = new ShapedRecipe(key, arrowResult);
        recipe.shape("AAA", "APA", "AAA");
        recipe.setIngredient('A', Material.ARROW);
        recipe.setIngredient('P', new RecipeChoice.ExactChoice(lingeringPotion));

        try {
            Bukkit.addRecipe(recipe);
            registeredCraftingKeys.add(key);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to register arrow recipe for " + key + ": " + e.getMessage());
        }
    }

    public List<NamespacedKey> getRegisteredPotionMixKeys() {
        return registeredPotionMixKeys;
    }

    public List<NamespacedKey> getRegisteredCraftingKeys() {
        return registeredCraftingKeys;
    }
}
