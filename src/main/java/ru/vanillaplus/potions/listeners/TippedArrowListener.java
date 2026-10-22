package ru.vanillaplus.potions.listeners;

import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.jetbrains.annotations.NotNull;
import ru.vanillaplus.potions.config.ConfigManager;
import ru.vanillaplus.potions.magnetism.MagnetismManager;
import ru.vanillaplus.potions.model.CustomPotionType;
import ru.vanillaplus.potions.util.PotionItemFactory;

public class TippedArrowListener implements Listener {
    private final PotionItemFactory factory;
    private final ConfigManager configManager;
    private final MagnetismManager magnetismManager;

    public TippedArrowListener(
            @NotNull PotionItemFactory factory,
            @NotNull ConfigManager configManager,
            @NotNull MagnetismManager magnetismManager
    ) {
        this.factory = factory;
        this.configManager = configManager;
        this.magnetismManager = magnetismManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onShootBow(EntityShootBowEvent event) {
        ItemStack consumable = event.getConsumable() != null ? event.getConsumable() : event.getArrowItem();
        if (consumable == null || !factory.isCustomPotion(consumable)) return;
        if (!(event.getProjectile() instanceof Arrow arrow)) return;

        CustomPotionType type = factory.getPotionType(consumable);
        if (type == null) return;

        int level = factory.getPotionLevel(consumable);
        int duration = factory.getDuration(consumable);

        arrow.setColor(configManager.getPotionColor(type));

        PersistentDataContainer pdc = arrow.getPersistentDataContainer();
        pdc.set(factory.getKeyType(), PersistentDataType.STRING, type.getId());
        pdc.set(factory.getKeyLevel(), PersistentDataType.INTEGER, level);
        pdc.set(factory.getKeyDuration(), PersistentDataType.INTEGER, duration);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Arrow arrow)) return;
        if (!(event.getHitEntity() instanceof LivingEntity hitEntity)) return;

        PersistentDataContainer pdc = arrow.getPersistentDataContainer();
        String typeId = null;
        int level = 1;
        int duration = 450;

        if (pdc.has(factory.getKeyType(), PersistentDataType.STRING)) {
            typeId = pdc.get(factory.getKeyType(), PersistentDataType.STRING);
            level = pdc.getOrDefault(factory.getKeyLevel(), PersistentDataType.INTEGER, 1);
            duration = pdc.getOrDefault(factory.getKeyDuration(), PersistentDataType.INTEGER, 450);
        } else {
            ItemStack arrowItem = arrow.getItemStack();
            if (factory.isCustomPotion(arrowItem)) {
                CustomPotionType t = factory.getPotionType(arrowItem);
                if (t != null) {
                    typeId = t.getId();
                    level = factory.getPotionLevel(arrowItem);
                    duration = factory.getDuration(arrowItem);
                }
            }
        }

        if (typeId == null) return;
        CustomPotionType type = CustomPotionType.fromId(typeId);
        if (type == null) return;

        if (type == CustomPotionType.MAGNETISM) {
            if (hitEntity instanceof Player player) {
                magnetismManager.applyMagnetism(player, duration, level);
            }
        } else if (type.getVanillaEffect() != null) {
            int amplifier = configManager.getAmplifier(type, level);
            hitEntity.addPotionEffect(new PotionEffect(type.getVanillaEffect(), duration, amplifier));
            if (type.getSecondaryVanillaEffect() != null) {
                int secAmp = configManager.getSecondaryAmplifier(type, level);
                hitEntity.addPotionEffect(new PotionEffect(type.getSecondaryVanillaEffect(), duration, secAmp));
            }
        }
    }
}
