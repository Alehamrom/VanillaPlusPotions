package ru.vanillaplus.potions.util;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.vanillaplus.potions.config.ConfigManager;
import ru.vanillaplus.potions.model.CustomPotionType;
import ru.vanillaplus.potions.model.PotionVariant;

public class PotionItemFactory {
    private final JavaPlugin plugin;
    private final ConfigManager configManager;

    private final NamespacedKey keyType;
    private final NamespacedKey keyVariant;
    private final NamespacedKey keyLevel;
    private final NamespacedKey keyExtended;
    private final NamespacedKey keyDuration;

    public PotionItemFactory(@NotNull JavaPlugin plugin, @NotNull ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;

        this.keyType = new NamespacedKey(plugin, "potion_type");
        this.keyVariant = new NamespacedKey(plugin, "potion_variant");
        this.keyLevel = new NamespacedKey(plugin, "potion_level");
        this.keyExtended = new NamespacedKey(plugin, "potion_extended");
        this.keyDuration = new NamespacedKey(plugin, "potion_duration");
    }

    @NotNull
    public ItemStack createPotion(
            @NotNull CustomPotionType type,
            @NotNull PotionVariant variant,
            int level,
            boolean extended
    ) {
        return createPotion(type, variant, level, extended, 1);
    }

    @NotNull
    public ItemStack createPotion(
            @NotNull CustomPotionType type,
            @NotNull PotionVariant variant,
            int level,
            boolean extended,
            int amount
    ) {
        int sanitizedLevel = (level >= 2 && type.supportsAmplifier()) ? 2 : 1;
        boolean sanitizedExtended = extended && type.supportsExtension() && sanitizedLevel == 1;

        ItemStack item = new ItemStack(variant.getMaterial(), Math.max(1, amount));
        if (!(item.getItemMeta() instanceof PotionMeta meta)) {
            return item;
        }

        // 1. Color
        Color color = configManager.getPotionColor(type);
        meta.setColor(color);
        meta.setBasePotionType(PotionType.AWKWARD);

        // 2. Custom Vanilla Potion Effect (if not purely custom mechanic)
        int durationTicks = configManager.getDuration(type, sanitizedLevel, sanitizedExtended, variant);
        if (type.getVanillaEffect() != null) {
            int amplifier = configManager.getAmplifier(type, sanitizedLevel);
            meta.addCustomEffect(new PotionEffect(type.getVanillaEffect(), durationTicks, amplifier), true);
        }
        if (type.getSecondaryVanillaEffect() != null) {
            int secondaryAmplifier = configManager.getSecondaryAmplifier(type, sanitizedLevel);
            meta.addCustomEffect(new PotionEffect(type.getSecondaryVanillaEffect(), durationTicks, secondaryAmplifier), true);
        }

        // 3. Display name and Lore
        meta.displayName(configManager.getDisplayName(type, variant, sanitizedLevel, sanitizedExtended));
        if (configManager.isCustomTooltipsEnabled()) {
            meta.addItemFlags(ItemFlag.HIDE_ADDITIONAL_TOOLTIP, ItemFlag.HIDE_ITEM_SPECIFICS);
            meta.lore(configManager.getLore(type, variant, sanitizedLevel, sanitizedExtended));
        } else if (type.isCustomMechanic()) {
            meta.lore(configManager.getLore(type, variant, sanitizedLevel, sanitizedExtended));
        }

        // 4. PersistentDataContainer tags
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(keyType, PersistentDataType.STRING, type.getId());
        pdc.set(keyVariant, PersistentDataType.STRING, variant.name());
        pdc.set(keyLevel, PersistentDataType.INTEGER, sanitizedLevel);
        pdc.set(keyExtended, PersistentDataType.BYTE, (byte) (sanitizedExtended ? 1 : 0));
        pdc.set(keyDuration, PersistentDataType.INTEGER, durationTicks);

        item.setItemMeta(meta);
        return item;
    }

    public boolean isCustomPotion(@Nullable ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        return pdc.has(keyType, PersistentDataType.STRING);
    }

    @Nullable
    public CustomPotionType getPotionType(@Nullable ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        String id = pdc.get(keyType, PersistentDataType.STRING);
        return CustomPotionType.fromId(id);
    }

    @NotNull
    public PotionVariant getPotionVariant(@Nullable ItemStack item) {
        if (item == null || !item.hasItemMeta()) return PotionVariant.NORMAL;
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        String varName = pdc.get(keyVariant, PersistentDataType.STRING);
        PotionVariant parsed = PotionVariant.fromString(varName);
        if (parsed != null) return parsed;
        PotionVariant fromMat = PotionVariant.fromMaterial(item.getType());
        return fromMat != null ? fromMat : PotionVariant.NORMAL;
    }

    public int getPotionLevel(@Nullable ItemStack item) {
        if (item == null || !item.hasItemMeta()) return 1;
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        Integer level = pdc.get(keyLevel, PersistentDataType.INTEGER);
        return level != null ? level : 1;
    }

    public boolean isExtended(@Nullable ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        Byte ext = pdc.get(keyExtended, PersistentDataType.BYTE);
        return ext != null && ext == 1;
    }

    public int getDuration(@Nullable ItemStack item) {
        if (item == null || !item.hasItemMeta()) return 0;
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        Integer duration = pdc.get(keyDuration, PersistentDataType.INTEGER);
        if (duration != null) return duration;
        CustomPotionType type = getPotionType(item);
        if (type == null) return 0;
        return configManager.getDuration(type, getPotionLevel(item), isExtended(item), getPotionVariant(item));
    }

    @NotNull
    public static ItemStack getAwkwardPotion() {
        return getAwkwardPotion(PotionVariant.NORMAL);
    }

    @NotNull
    public static ItemStack getAwkwardPotion(@NotNull PotionVariant variant) {
        ItemStack item = new ItemStack(variant.getMaterial());
        if (item.getItemMeta() instanceof PotionMeta meta) {
            meta.setBasePotionType(PotionType.AWKWARD);
            item.setItemMeta(meta);
        }
        return item;
    }

    @NotNull
    public NamespacedKey getKeyType() {
        return keyType;
    }

    @NotNull
    public NamespacedKey getKeyVariant() {
        return keyVariant;
    }

    @NotNull
    public NamespacedKey getKeyLevel() {
        return keyLevel;
    }

    @NotNull
    public NamespacedKey getKeyExtended() {
        return keyExtended;
    }

    @NotNull
    public NamespacedKey getKeyDuration() {
        return keyDuration;
    }
}
