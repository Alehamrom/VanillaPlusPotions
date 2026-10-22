package ru.vanillaplus.potions.listeners;

import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.AreaEffectCloudApplyEvent;
import org.bukkit.event.entity.LingeringPotionSplashEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.jetbrains.annotations.NotNull;
import ru.vanillaplus.potions.config.ConfigManager;
import ru.vanillaplus.potions.magnetism.MagnetismManager;
import ru.vanillaplus.potions.model.CustomPotionType;
import ru.vanillaplus.potions.util.PotionItemFactory;

public class PotionSplashListener implements Listener {
    private final PotionItemFactory factory;
    private final ConfigManager configManager;
    private final MagnetismManager magnetismManager;

    public PotionSplashListener(
            @NotNull PotionItemFactory factory,
            @NotNull ConfigManager configManager,
            @NotNull MagnetismManager magnetismManager
    ) {
        this.factory = factory;
        this.configManager = configManager;
        this.magnetismManager = magnetismManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSplash(PotionSplashEvent event) {
        ThrownPotion thrown = event.getPotion();
        ItemStack item = thrown.getItem();
        if (!factory.isCustomPotion(item)) return;

        CustomPotionType type = factory.getPotionType(item);
        if (type == null) return;

        int level = factory.getPotionLevel(item);
        int baseDuration = factory.getDuration(item);
        int amplifier = configManager.getAmplifier(type, level);

        for (LivingEntity entity : event.getAffectedEntities()) {
            double intensity = event.getIntensity(entity);
            int duration = (int) Math.round(baseDuration * intensity);
            if (duration < 20) continue;

            if (type == CustomPotionType.MAGNETISM) {
                if (entity instanceof Player player) {
                    magnetismManager.applyMagnetism(player, duration, level);
                }
            } else if (type.getVanillaEffect() != null) {
                entity.addPotionEffect(new PotionEffect(type.getVanillaEffect(), duration, amplifier));
                if (type.getSecondaryVanillaEffect() != null) {
                    int secAmp = configManager.getSecondaryAmplifier(type, level);
                    entity.addPotionEffect(new PotionEffect(type.getSecondaryVanillaEffect(), duration, secAmp));
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onLingeringSplash(LingeringPotionSplashEvent event) {
        ThrownPotion thrown = event.getEntity();
        ItemStack item = thrown.getItem();
        if (!factory.isCustomPotion(item)) return;

        CustomPotionType type = factory.getPotionType(item);
        if (type == null) return;

        int level = factory.getPotionLevel(item);
        int duration = factory.getDuration(item);

        AreaEffectCloud cloud = event.getAreaEffectCloud();
        cloud.setColor(configManager.getPotionColor(type));

        PersistentDataContainer pdc = cloud.getPersistentDataContainer();
        pdc.set(factory.getKeyType(), PersistentDataType.STRING, type.getId());
        pdc.set(factory.getKeyLevel(), PersistentDataType.INTEGER, level);
        pdc.set(factory.getKeyDuration(), PersistentDataType.INTEGER, duration);

        if (type.getVanillaEffect() != null) {
            int amplifier = configManager.getAmplifier(type, level);
            cloud.addCustomEffect(new PotionEffect(type.getVanillaEffect(), Math.max(20, duration), amplifier), true);
            if (type.getSecondaryVanillaEffect() != null) {
                int secAmp = configManager.getSecondaryAmplifier(type, level);
                cloud.addCustomEffect(new PotionEffect(type.getSecondaryVanillaEffect(), Math.max(20, duration), secAmp), true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onAreaCloudApply(AreaEffectCloudApplyEvent event) {
        AreaEffectCloud cloud = event.getEntity();
        PersistentDataContainer pdc = cloud.getPersistentDataContainer();
        if (!pdc.has(factory.getKeyType(), PersistentDataType.STRING)) return;

        String typeId = pdc.get(factory.getKeyType(), PersistentDataType.STRING);
        CustomPotionType type = CustomPotionType.fromId(typeId);
        if (type != CustomPotionType.MAGNETISM) return;

        int level = pdc.getOrDefault(factory.getKeyLevel(), PersistentDataType.INTEGER, 1);
        int duration = pdc.getOrDefault(factory.getKeyDuration(), PersistentDataType.INTEGER, 900);

        for (LivingEntity entity : event.getAffectedEntities()) {
            if (entity instanceof Player player) {
                magnetismManager.applyMagnetism(player, Math.max(20, duration), level);
            }
        }
    }
}
