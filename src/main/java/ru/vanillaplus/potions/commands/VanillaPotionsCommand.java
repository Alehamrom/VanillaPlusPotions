package ru.vanillaplus.potions.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.vanillaplus.potions.VanillaPlusPotions;
import ru.vanillaplus.potions.config.ConfigManager;
import ru.vanillaplus.potions.model.CustomPotionType;
import ru.vanillaplus.potions.model.PotionVariant;
import ru.vanillaplus.potions.util.PotionItemFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class VanillaPotionsCommand implements CommandExecutor, TabCompleter {
    private final VanillaPlusPotions plugin;
    private final PotionItemFactory factory;
    private final ConfigManager configManager;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public VanillaPotionsCommand(
            @NotNull VanillaPlusPotions plugin,
            @NotNull PotionItemFactory factory,
            @NotNull ConfigManager configManager
    ) {
        this.plugin = plugin;
        this.factory = factory;
        this.configManager = configManager;
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args
    ) {
        if (!sender.hasPermission("vanillapluspotions.admin")) {
            sender.sendMessage(configManager.getMessage("no-permission"));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(configManager.getMessage("usage"));
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "reload" -> {
                plugin.reloadPlugin();
                sender.sendMessage(configManager.getMessage("reload-success"));
                return true;
            }
            case "list" -> {
                sender.sendMessage(miniMessage.deserialize("<gold>--- Список доступных зелий VanillaPlus ---</gold>"));
                for (CustomPotionType type : CustomPotionType.values()) {
                    String extStr = type.supportsExtension() ? "<green>удлинение</green>" : "<red>нет</red>";
                    String upgStr = type.supportsAmplifier() ? "<green>усиление (II)</green>" : "<red>нет</red>";
                    String ingStr = type.isInversion() && type.getInvertsFrom() != null
                            ? "инверсия " + type.getInvertsFrom().getId() + " + " + type.getBaseIngredient().name().toLowerCase(Locale.ROOT)
                            : type.getBaseIngredient().name().toLowerCase(Locale.ROOT);
                    sender.sendMessage(miniMessage.deserialize(
                            "<yellow>• " + type.getId() + "</yellow> <gray>(Ингредиент: "
                                    + ingStr
                                    + ", " + extStr + ", " + upgStr + ")</gray>"
                    ));
                }
                return true;
            }
            case "give" -> {
                if (args.length < 3) {
                    sender.sendMessage(configManager.getMessage("usage"));
                    return true;
                }

                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage(configManager.getMessage("player-not-found", "%player%", args[1]));
                    return true;
                }

                CustomPotionType type = CustomPotionType.fromId(args[2]);
                if (type == null) {
                    sender.sendMessage(configManager.getMessage("unknown-potion", "%type%", args[2]));
                    return true;
                }

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

                ItemStack potion = factory.createPotion(type, variant, level, extended, amount);
                java.util.Map<Integer, ItemStack> leftover = target.getInventory().addItem(potion);
                if (!leftover.isEmpty()) {
                    for (ItemStack drop : leftover.values()) {
                        target.getWorld().dropItemNaturally(target.getLocation(), drop);
                    }
                }

                Component dispName = configManager.getDisplayName(type, variant, level, extended);
                String itemName = miniMessage.serialize(dispName);
                sender.sendMessage(configManager.getMessage(
                        "give-success",
                        "%amount%", String.valueOf(amount),
                        "%item%", itemName,
                        "%player%", target.getName()
                ));
                return true;
            }
            default -> {
                sender.sendMessage(configManager.getMessage("usage"));
                return true;
            }
        }
    }

    @Override
    @NotNull
    public List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String alias,
            @NotNull String[] args
    ) {
        if (!sender.hasPermission("vanillapluspotions.admin")) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            return filter(Arrays.asList("give", "reload", "list"), args[0]);
        }

        if (args[0].equalsIgnoreCase("give")) {
            if (args.length == 2) {
                List<String> names = new ArrayList<>();
                for (Player p : Bukkit.getOnlinePlayers()) {
                    names.add(p.getName());
                }
                return filter(names, args[1]);
            }
            if (args.length == 3) {
                List<String> types = new ArrayList<>();
                for (CustomPotionType t : CustomPotionType.values()) {
                    types.add(t.getId());
                }
                return filter(types, args[2]);
            }
            if (args.length == 4) {
                return filter(Arrays.asList("normal", "splash", "lingering", "arrow"), args[3]);
            }
            if (args.length == 5) {
                CustomPotionType type = CustomPotionType.fromId(args[2]);
                if (type != null && !type.supportsAmplifier()) {
                    return filter(Collections.singletonList("1"), args[4]);
                }
                return filter(Arrays.asList("1", "2"), args[4]);
            }
            if (args.length == 6) {
                if ("2".equals(args[4])) {
                    return filter(Collections.singletonList("false"), args[5]);
                }
                return filter(Arrays.asList("false", "true"), args[5]);
            }
            if (args.length == 7) {
                return filter(Arrays.asList("1", "16", "64"), args[6]);
            }
        }

        return Collections.emptyList();
    }

    private List<String> filter(List<String> options, String prefix) {
        String lower = prefix.toLowerCase(Locale.ROOT);
        List<String> result = new ArrayList<>();
        for (String opt : options) {
            if (opt.toLowerCase(Locale.ROOT).startsWith(lower)) {
                result.add(opt);
            }
        }
        return result;
    }
}
