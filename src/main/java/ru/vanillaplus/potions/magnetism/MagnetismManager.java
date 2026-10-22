package ru.vanillaplus.potions.magnetism;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import ru.vanillaplus.potions.config.ConfigManager;

import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class MagnetismManager implements Listener {
    private final JavaPlugin plugin;
    private final ConfigManager configManager;
    private final Map<UUID, MagnetismData> activePlayers = new ConcurrentHashMap<>();
    private BukkitTask task;
    private int tickCounter = 0;

    public record MagnetismData(long expiryMillis, int level, double radius) {}

    public MagnetismManager(@NotNull JavaPlugin plugin, @NotNull ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    public void start() {
        stop();
        long tickRate = configManager.getMagnetismTickRate();
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, tickRate, tickRate);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        activePlayers.clear();
    }

    public void applyMagnetism(@NotNull Player player, int durationTicks, int level) {
        long current = System.currentTimeMillis();
        long addedMillis = durationTicks * 50L;

        MagnetismData existing = activePlayers.get(player.getUniqueId());
        long newExpiry;
        if (existing != null && existing.expiryMillis() > current) {
            // If already active with same or lower level, extend or upgrade
            if (level >= existing.level()) {
                newExpiry = Math.max(existing.expiryMillis(), current + addedMillis);
            } else {
                newExpiry = existing.expiryMillis();
                level = existing.level();
            }
        } else {
            newExpiry = current + addedMillis;
        }

        double radius = configManager.getMagnetismRadius(level);
        activePlayers.put(player.getUniqueId(), new MagnetismData(newExpiry, level, radius));
    }

    public boolean isMagnetized(@NotNull Player player) {
        MagnetismData data = activePlayers.get(player.getUniqueId());
        return data != null && data.expiryMillis() > System.currentTimeMillis();
    }

    public void removeMagnetism(@NotNull Player player) {
        activePlayers.remove(player.getUniqueId());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        activePlayers.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPlayerDeath(org.bukkit.event.entity.PlayerDeathEvent event) {
        activePlayers.remove(event.getEntity().getUniqueId());
    }

    @EventHandler
    public void onPlayerRespawn(org.bukkit.event.player.PlayerRespawnEvent event) {
        activePlayers.remove(event.getPlayer().getUniqueId());
    }

    private void tick() {
        if (activePlayers.isEmpty()) return;

        tickCounter++;
        long current = System.currentTimeMillis();
        boolean sendActionBar = configManager.isMagnetismActionBarEnabled() && (tickCounter % 5 == 0);

        for (Map.Entry<UUID, MagnetismData> entry : activePlayers.entrySet()) {
            UUID uuid = entry.getKey();
            MagnetismData data = entry.getValue();

            if (current >= data.expiryMillis()) {
                activePlayers.remove(uuid);
                continue;
            }

            Player player = Bukkit.getPlayer(uuid);
            if (player == null || !player.isOnline() || player.isDead() || player.getGameMode() == org.bukkit.GameMode.SPECTATOR) {
                if (player != null && (player.isDead() || !player.isOnline())) {
                    activePlayers.remove(uuid);
                }
                continue;
            }

            double radius = data.radius();
            Location playerLoc = player.getLocation();
            Location pullTarget = playerLoc.clone().add(0, 0.6, 0);

            // Pull nearby dropped items
            Collection<Entity> nearby = player.getWorld().getNearbyEntities(
                    pullTarget,
                    radius,
                    radius,
                    radius,
                    e -> e instanceof Item
            );

            double pullSpeed = configManager.getMagnetismPullSpeed();
            boolean spawnParticles = configManager.isMagnetismParticleEnabled();

            for (Entity entity : nearby) {
                if (!(entity instanceof Item droppedItem)) continue;
                if (!droppedItem.isValid() || droppedItem.isDead() || droppedItem.getPickupDelay() > 0) {
                    continue;
                }

                if (!canPickupItem(player, droppedItem)) {
                    continue;
                }

                Location itemLoc = droppedItem.getLocation();
                Vector delta = pullTarget.toVector().subtract(itemLoc.toVector());
                double dist = delta.length();

                if (dist > radius || dist < 0.25) continue;

                Vector velocity = delta.normalize().multiply(pullSpeed);
                if (droppedItem.isOnGround()) {
                    velocity.setY(Math.max(velocity.getY(), 0.18));
                }
                droppedItem.setVelocity(velocity);

                if (spawnParticles && Math.random() < 0.20) {
                    droppedItem.getWorld().spawnParticle(
                            configManager.getMagnetismParticleType(),
                            itemLoc.add(0, 0.2, 0),
                            1,
                            0.05,
                            0.05,
                            0.05,
                            0.01
                    );
                }
            }

            // Action bar notification every 10 ticks (0.5s)
            if (sendActionBar) {
                long remainingSec = Math.max(0, (data.expiryMillis() - current) / 1000);
                String formatted = String.format(Locale.ROOT, "%02d:%02d", remainingSec / 60, remainingSec % 60);
                player.sendActionBar(configManager.getMagnetismActionBarMessage(formatted, radius));
            }
        }
    }

    private boolean canPickupItem(@NotNull Player player, @NotNull Item item) {
        org.bukkit.inventory.PlayerInventory inv = player.getInventory();
        if (inv.firstEmpty() != -1) return true;
        ItemStack itemStack = item.getItemStack();
        for (ItemStack slot : inv.getStorageContents()) {
            if (slot != null && slot.isSimilar(itemStack) && slot.getAmount() < slot.getMaxStackSize()) {
                return true;
            }
        }
        return false;
    }

    public Map<UUID, MagnetismData> getActivePlayers() {
        return activePlayers;
    }
}
