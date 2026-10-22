package ru.vanillaplus.potions;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import ru.vanillaplus.potions.brewing.BrewingListener;
import ru.vanillaplus.potions.brewing.BrewingRegistry;
import ru.vanillaplus.potions.commands.VanillaPotionsCommand;
import ru.vanillaplus.potions.config.ConfigManager;
import ru.vanillaplus.potions.listeners.PotionConsumeListener;
import ru.vanillaplus.potions.listeners.PotionSplashListener;
import ru.vanillaplus.potions.listeners.TippedArrowListener;
import ru.vanillaplus.potions.magnetism.MagnetismManager;
import ru.vanillaplus.potions.util.PotionItemFactory;

public final class VanillaPlusPotions extends JavaPlugin {
    private ConfigManager configManager;
    private PotionItemFactory potionItemFactory;
    private MagnetismManager magnetismManager;
    private BrewingRegistry brewingRegistry;
    private BrewingListener brewingListener;

    @Override
    public void onEnable() {
        // 1. Config
        saveDefaultConfig();
        this.configManager = new ConfigManager(this);

        // 2. Factory & Managers
        this.potionItemFactory = new PotionItemFactory(this, configManager);
        this.magnetismManager = new MagnetismManager(this, configManager);
        this.magnetismManager.start();

        // 3. Brewing & Crafting Registry
        this.brewingRegistry = new BrewingRegistry(this, potionItemFactory, configManager);
        this.brewingRegistry.registerAll();

        // 4. Listeners
        this.brewingListener = new BrewingListener(this, potionItemFactory);
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(brewingListener, this);
        pm.registerEvents(magnetismManager, this);
        pm.registerEvents(new PotionConsumeListener(potionItemFactory, configManager, magnetismManager), this);
        pm.registerEvents(new PotionSplashListener(potionItemFactory, configManager, magnetismManager), this);
        pm.registerEvents(new TippedArrowListener(potionItemFactory, configManager, magnetismManager), this);

        // 5. Commands
        PluginCommand cmd = getCommand("vpotions");
        if (cmd != null) {
            VanillaPotionsCommand executor = new VanillaPotionsCommand(this, potionItemFactory, configManager);
            cmd.setExecutor(executor);
            cmd.setTabCompleter(executor);
        }

        getLogger().info("VanillaPlusPotions v" + getPluginMeta().getVersion() + " successfully enabled!");
    }

    @Override
    public void onDisable() {
        if (brewingListener != null) {
            brewingListener.cancelAllTasks();
        }
        if (magnetismManager != null) {
            magnetismManager.stop();
        }
        if (brewingRegistry != null) {
            brewingRegistry.unregisterAll();
        }
        getLogger().info("VanillaPlusPotions v" + getPluginMeta().getVersion() + " successfully disabled!");
    }

    public void reloadPlugin() {
        configManager.reload();
        if (magnetismManager != null) {
            magnetismManager.start();
        }
        if (brewingRegistry != null) {
            brewingRegistry.registerAll();
        }
        getLogger().info("VanillaPlusPotions v" + getPluginMeta().getVersion() + " reloaded!");
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public PotionItemFactory getPotionItemFactory() {
        return potionItemFactory;
    }

    public MagnetismManager getMagnetismManager() {
        return magnetismManager;
    }

    public BrewingRegistry getBrewingRegistry() {
        return brewingRegistry;
    }
}
