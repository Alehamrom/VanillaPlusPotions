package ru.vanillaplus.potions.listeners;

import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.jetbrains.annotations.NotNull;
import ru.vanillaplus.potions.config.ConfigManager;
import ru.vanillaplus.potions.magnetism.MagnetismManager;
import ru.vanillaplus.potions.model.CustomPotionType;
import ru.vanillaplus.potions.util.PotionItemFactory;

public class PotionConsumeListener implements Listener {
    private final PotionItemFactory factory;
    private final ConfigManager configManager;
    private final MagnetismManager magnetismManager;

    public PotionConsumeListener(
            @NotNull PotionItemFactory factory,
            @NotNull ConfigManager configManager,
            @NotNull MagnetismManager magnetismManager
    ) {
        this.factory = factory;
        this.configManager = configManager;
        this.magnetismManager = magnetismManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onConsume(PlayerItemConsumeEvent event) {
        ItemStack item = event.getItem();
        if (!factory.isCustomPotion(item)) return;

        CustomPotionType type = factory.getPotionType(item);
        if (type == null) return;

        Player player = event.getPlayer();
        int level = factory.getPotionLevel(item);
        int durationTicks = factory.getDuration(item);

        if (type == CustomPotionType.MAGNETISM) {
            magnetismManager.applyMagnetism(player, durationTicks, level);
            player.playSound(player.getLocation(), Sound.BLOCK_COPPER_BULB_TURN_ON, 1.0f, 1.2f);
        } else if (type.getVanillaEffect() != null) {
            int amplifier = configManager.getAmplifier(type, level);
            player.addPotionEffect(new PotionEffect(type.getVanillaEffect(), durationTicks, amplifier));
            if (type.getSecondaryVanillaEffect() != null) {
                int secAmp = configManager.getSecondaryAmplifier(type, level);
                player.addPotionEffect(new PotionEffect(type.getSecondaryVanillaEffect(), durationTicks, secAmp));
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onConsumeMilk(PlayerItemConsumeEvent event) {
        if (event.getItem().getType() == org.bukkit.Material.MILK_BUCKET) {
            magnetismManager.removeMagnetism(event.getPlayer());
        }
    }
}
