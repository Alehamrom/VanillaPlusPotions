package ru.vanillaplus.potions.brewing;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BrewingStand;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BrewingStartEvent;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.vanillaplus.potions.model.CustomPotionType;
import ru.vanillaplus.potions.model.PotionVariant;
import ru.vanillaplus.potions.util.PotionItemFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BrewingListener implements Listener {
    private final JavaPlugin plugin;
    private final PotionItemFactory factory;
    private final Map<Location, BukkitTask> activeBrewingTasks = new HashMap<>();

    public BrewingListener(@NotNull JavaPlugin plugin, @NotNull PotionItemFactory factory) {
        this.plugin = plugin;
        this.factory = factory;
    }

    public void cancelAllTasks() {
        for (BukkitTask task : activeBrewingTasks.values()) {
            task.cancel();
        }
        activeBrewingTasks.clear();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBrewStart(BrewingStartEvent event) {
        // Native Paper brewing has successfully started on this stand
        Location loc = event.getBlock().getLocation();
        BukkitTask task = activeBrewingTasks.remove(loc);
        if (task != null) {
            task.cancel();
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBrew(BrewEvent event) {
        Location loc = event.getBlock().getLocation();
        BukkitTask task = activeBrewingTasks.remove(loc);
        if (task != null) {
            task.cancel();
        }

        // Native brew completed: verify and ensure custom results are properly populated
        BrewerInventory inv = event.getContents();
        ItemStack ingredient = inv.getIngredient();
        if (ingredient != null) {
            List<ItemStack> results = event.getResults();
            for (int i = 0; i < 3 && i < results.size(); i++) {
                ItemStack bottle = inv.getItem(i);
                ItemStack expected = findRecipeResult(ingredient, bottle);
                if (expected != null) {
                    results.set(i, expected);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory().getType() == InventoryType.BREWING) {
            scheduleBrewCheck(event.getInventory());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory().getType() == InventoryType.BREWING) {
            scheduleBrewCheck(event.getInventory());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryMove(InventoryMoveItemEvent event) {
        if (event.getDestination().getType() == InventoryType.BREWING) {
            scheduleBrewCheck(event.getDestination());
        } else if (event.getSource().getType() == InventoryType.BREWING) {
            scheduleBrewCheck(event.getSource());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.getBlock().getType() == Material.BREWING_STAND) {
            BukkitTask task = activeBrewingTasks.remove(event.getBlock().getLocation());
            if (task != null) {
                task.cancel();
            }
        }
    }

    private void scheduleBrewCheck(org.bukkit.inventory.Inventory inventory) {
        if (!(inventory instanceof BrewerInventory brewerInventory)) return;
        // Wait 10 ticks (0.5s) to allow Paper's native PotionMix engine to start first
        Bukkit.getScheduler().runTaskLater(plugin, () -> checkAndStartBrewing(brewerInventory), 10L);
    }

    private void checkAndStartBrewing(@NotNull BrewerInventory inv) {
        BrewingStand stand = inv.getHolder();
        if (stand == null || !stand.isPlaced()) return;

        Location loc = stand.getLocation();
        // If native Paper brewing has already started, do not interfere!
        if (stand.getBrewingTime() > 0 || activeBrewingTasks.containsKey(loc)) {
            return;
        }

        ItemStack ingredient = inv.getIngredient();
        if (ingredient == null || ingredient.getType() == Material.AIR) return;

        boolean canBrewAny = false;
        for (int i = 0; i < 3; i++) {
            ItemStack bottle = inv.getItem(i);
            if (findRecipeResult(ingredient, bottle) != null) {
                canBrewAny = true;
                break;
            }
        }

        if (!canBrewAny) return;

        // Verify fuel
        int fuelLevel = stand.getFuelLevel();
        if (fuelLevel <= 0) {
            ItemStack fuelItem = inv.getFuel();
            if (fuelItem != null && fuelItem.getType() == Material.BLAZE_POWDER && fuelItem.getAmount() > 0) {
                fuelItem.setAmount(fuelItem.getAmount() - 1);
                inv.setFuel(fuelItem.getAmount() > 0 ? fuelItem : null);
                stand.setFuelLevel(20);
                stand.update();
                fuelLevel = 20;
            } else {
                return; // no fuel
            }
        }

        // Start fallback ticking task (400 ticks = 20s)
        final int totalBrewTime = 400;
        stand.setBrewingTime(totalBrewTime);
        stand.update();

        BukkitTask task = new BukkitRunnable() {
            int remainingTicks = totalBrewTime;

            @Override
            public void run() {
                Block block = loc.getBlock();
                if (block.getType() != Material.BREWING_STAND) {
                    cleanup();
                    return;
                }

                if (!(block.getState() instanceof BrewingStand currentStand)) {
                    cleanup();
                    return;
                }

                BrewerInventory curInv = currentStand.getInventory();
                ItemStack curIngredient = curInv.getIngredient();
                if (curIngredient == null || curIngredient.getType() != ingredient.getType()) {
                    currentStand.setBrewingTime(0);
                    currentStand.update();
                    cleanup();
                    return;
                }

                boolean stillValid = false;
                for (int i = 0; i < 3; i++) {
                    if (findRecipeResult(curIngredient, curInv.getItem(i)) != null) {
                        stillValid = true;
                        break;
                    }
                }

                if (!stillValid) {
                    currentStand.setBrewingTime(0);
                    currentStand.update();
                    cleanup();
                    return;
                }

                remainingTicks -= 4;
                currentStand.setBrewingTime(Math.max(0, remainingTicks));

                // Spawn bubbles particle every 20 ticks (1s)
                if (remainingTicks % 20 == 0) {
                    currentStand.update();
                    loc.getWorld().spawnParticle(
                            Particle.DUST,
                            loc.clone().add(0.5, 0.8, 0.5),
                            3,
                            0.15,
                            0.2,
                            0.15,
                            new Particle.DustOptions(org.bukkit.Color.PURPLE, 0.7f)
                    );
                }

                if (remainingTicks <= 0) {
                    // Complete brew
                    for (int i = 0; i < 3; i++) {
                        ItemStack bottle = curInv.getItem(i);
                        ItemStack result = findRecipeResult(curIngredient, bottle);
                        if (result != null) {
                            curInv.setItem(i, result);
                        }
                    }

                    // Consume 1 ingredient
                    curIngredient.setAmount(curIngredient.getAmount() - 1);
                    curInv.setIngredient(curIngredient.getAmount() > 0 ? curIngredient : null);

                    // Consume 1 fuel point
                    int curFuel = currentStand.getFuelLevel();
                    currentStand.setFuelLevel(Math.max(0, curFuel - 1));
                    currentStand.setBrewingTime(0);
                    currentStand.update();

                    // Sound and particle
                    loc.getWorld().playSound(loc, Sound.BLOCK_BREWING_STAND_BREW, 1.0f, 1.0f);
                    loc.getWorld().spawnParticle(
                            Particle.DUST,
                            loc.clone().add(0.5, 0.9, 0.5),
                            12,
                            0.2,
                            0.2,
                            0.2,
                            new Particle.DustOptions(org.bukkit.Color.AQUA, 1.0f)
                    );

                    cleanup();
                }
            }

            private void cleanup() {
                cancel();
                activeBrewingTasks.remove(loc);
            }
        }.runTaskTimer(plugin, 4L, 4L);

        activeBrewingTasks.put(loc, task);
    }

    @Nullable
    public ItemStack findRecipeResult(@Nullable ItemStack ingredient, @Nullable ItemStack potion) {
        if (ingredient == null || potion == null || potion.getType() == Material.AIR) {
            return null;
        }

        // 1. Awkward Potion (any variant: normal, splash, lingering) + Base Ingredient -> Base Potion
        PotionVariant awkwardVariant = getAwkwardPotionVariant(potion);
        if (awkwardVariant != null) {
            for (CustomPotionType type : CustomPotionType.values()) {
                if (!type.isInversion() && ingredient.getType() == type.getBaseIngredient()) {
                    return factory.createPotion(type, awkwardVariant, 1, false);
                }
            }
            return null;
        }

        // 2. Custom Potion progressions
        if (!factory.isCustomPotion(potion)) {
            return null;
        }

        CustomPotionType type = factory.getPotionType(potion);
        if (type == null) return null;

        PotionVariant variant = factory.getPotionVariant(potion);
        int level = factory.getPotionLevel(potion);
        boolean extended = factory.isExtended(potion);

        // Inversion: Fermented Spider Eye
        if (ingredient.getType() == Material.FERMENTED_SPIDER_EYE) {
            for (CustomPotionType candidate : CustomPotionType.values()) {
                if (candidate.isInversion() && candidate.getInvertsFrom() == type) {
                    int targetLevel = (level >= 2 && candidate.supportsAmplifier()) ? 2 : 1;
                    boolean targetExtended = extended && candidate.supportsExtension();
                    return factory.createPotion(candidate, variant, targetLevel, targetExtended);
                }
            }
            return null;
        }

        // Redstone -> Extended (only level 1, non-extended)
        if (ingredient.getType() == Material.REDSTONE) {
            if (type.supportsExtension() && !extended && level == 1) {
                return factory.createPotion(type, variant, 1, true);
            }
            return null;
        }

        // Glowstone -> Upgraded (Level 2, only level 1, non-extended)
        if (ingredient.getType() == Material.GLOWSTONE_DUST) {
            if (type.supportsAmplifier() && level == 1 && !extended) {
                return factory.createPotion(type, variant, 2, false);
            }
            return null;
        }

        // Gunpowder -> Splash
        if (ingredient.getType() == Material.GUNPOWDER) {
            if (variant == PotionVariant.NORMAL) {
                return factory.createPotion(type, PotionVariant.SPLASH, level, extended);
            }
            return null;
        }

        // Dragon's Breath -> Lingering
        if (ingredient.getType() == Material.DRAGON_BREATH) {
            if (variant == PotionVariant.SPLASH) {
                return factory.createPotion(type, PotionVariant.LINGERING, level, extended);
            }
            return null;
        }

        return null;
    }

    @Nullable
    private PotionVariant getAwkwardPotionVariant(@Nullable ItemStack item) {
        if (item == null || factory.isCustomPotion(item)) return null;
        PotionVariant variant = PotionVariant.fromMaterial(item.getType());
        if (variant == null || variant == PotionVariant.TIPPED_ARROW) return null;
        if (item.getItemMeta() instanceof PotionMeta meta) {
            if (meta.getBasePotionType() == PotionType.AWKWARD) {
                return variant;
            }
        }
        return null;
    }
}
