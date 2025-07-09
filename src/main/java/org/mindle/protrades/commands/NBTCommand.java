package org.mindle.protrades.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.mindle.protrades.ProTrades;
import org.mindle.protrades.managers.TradeManager;
import org.mindle.protrades.nbt.data.NBTCompoundData;
import org.mindle.protrades.utils.ItemUtils;
import org.mindle.protrades.utils.NBTUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Command handler for NBT-related operations in ProTrades.
 * Provides debugging and management tools for NBT data.
 */
public class NBTCommand implements CommandExecutor, TabCompleter {
    
    private final ProTrades plugin;
    private final TradeManager tradeManager;
    
    public NBTCommand(ProTrades plugin, TradeManager tradeManager) {
        this.plugin = plugin;
        this.tradeManager = tradeManager;
    }
    
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("protrades.admin")) {
            sender.sendMessage(Component.text("You don't have permission to use this command.", NamedTextColor.RED));
            return true;
        }
        
        if (args.length == 0) {
            sendHelpMessage(sender);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "info":
                return handleInfoCommand(sender, args);
            case "debug":
                return handleDebugCommand(sender, args);
            case "stats":
                return handleStatsCommand(sender);
            case "cache":
                return handleCacheCommand(sender, args);
            case "validate":
                return handleValidateCommand(sender, args);
            case "help":
                sendHelpMessage(sender);
                return true;
            default:
                sender.sendMessage(Component.text("Unknown subcommand: " + subCommand, NamedTextColor.RED));
                sendHelpMessage(sender);
                return true;
        }
    }
    
    /**
     * Handles the info subcommand - shows NBT info for held item.
     */
    private boolean handleInfoCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Component.text("This command can only be used by players.", NamedTextColor.RED));
            return true;
        }
        
        Player player = (Player) sender;
        ItemStack item = player.getInventory().getItemInMainHand();
        
        if (item.getType().isAir()) {
            sender.sendMessage(Component.text("You must be holding an item.", NamedTextColor.RED));
            return true;
        }
        
        sender.sendMessage(Component.text("=== NBT Item Information ===", NamedTextColor.GOLD)
                .decorate(TextDecoration.BOLD));
        sender.sendMessage(Component.text(""));
        
        // Basic item info
        sender.sendMessage(Component.text("Material: ", NamedTextColor.GRAY)
                .append(Component.text(item.getType().name(), NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("Amount: ", NamedTextColor.GRAY)
                .append(Component.text(item.getAmount(), NamedTextColor.WHITE)));
        
        // ProItems info
        if (ItemUtils.isProItem(item)) {
            String proItemId = ItemUtils.getProItemId(item);
            sender.sendMessage(Component.text("ProItem ID: ", NamedTextColor.AQUA)
                    .append(Component.text(proItemId != null ? proItemId : "Unknown", NamedTextColor.YELLOW)));
        }
        
        // NBT info
        if (ItemUtils.hasCustomNBT(item)) {
            sender.sendMessage(Component.text("Has Custom NBT: ", NamedTextColor.GREEN)
                    .append(Component.text("Yes", NamedTextColor.WHITE)));
            
            NBTCompoundData nbtData = NBTUtils.extractNBTData(item);
            if (nbtData != null) {
                sender.sendMessage(Component.text("NBT Entries: ", NamedTextColor.GREEN)
                        .append(Component.text(nbtData.size(), NamedTextColor.WHITE)));
                
                // Show key summary
                Set<String> keys = nbtData.getKeys();
                if (keys.size() <= 10) {
                    sender.sendMessage(Component.text("NBT Keys: ", NamedTextColor.GREEN)
                            .append(Component.text(String.join(", ", keys), NamedTextColor.GRAY)));
                } else {
                    sender.sendMessage(Component.text("NBT Keys: ", NamedTextColor.GREEN)
                            .append(Component.text(keys.size() + " entries (too many to display)", NamedTextColor.GRAY)));
                }
            }
        } else {
            sender.sendMessage(Component.text("Has Custom NBT: ", NamedTextColor.RED)
                    .append(Component.text("No", NamedTextColor.WHITE)));
        }
        
        // Preserved NBT info
        if (NBTUtils.hasPreservedNBT(item)) {
            sender.sendMessage(Component.text("Has Preserved NBT: ", NamedTextColor.YELLOW)
                    .append(Component.text("Yes", NamedTextColor.WHITE)));
        }
        
        return true;
    }
    
    /**
     * Handles the debug subcommand - shows detailed NBT debug info.
     */
    private boolean handleDebugCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Component.text("This command can only be used by players.", NamedTextColor.RED));
            return true;
        }
        
        Player player = (Player) sender;
        ItemStack item = player.getInventory().getItemInMainHand();
        
        if (item.getType().isAir()) {
            sender.sendMessage(Component.text("You must be holding an item.", NamedTextColor.RED));
            return true;
        }
        
        String debugInfo = ItemUtils.getDebugNBTInfo(item);
        String[] lines = debugInfo.split("\n");
        
        for (String line : lines) {
            sender.sendMessage(Component.text(line, NamedTextColor.GRAY));
        }
        
        return true;
    }
    
    /**
     * Handles the stats subcommand - shows NBT usage statistics.
     */
    private boolean handleStatsCommand(CommandSender sender) {
        sender.sendMessage(Component.text("=== NBT Statistics ===", NamedTextColor.GOLD)
                .decorate(TextDecoration.BOLD));
        sender.sendMessage(Component.text(""));
        
        // Trade NBT statistics
        Map<String, Integer> tradeStats = tradeManager.getNBTStatistics();
        sender.sendMessage(Component.text("Trade Statistics:", NamedTextColor.AQUA));
        sender.sendMessage(Component.text("  Total Trades: ", NamedTextColor.GRAY)
                .append(Component.text(tradeStats.getOrDefault("totalTrades", 0), NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("  Trades with NBT: ", NamedTextColor.GRAY)
                .append(Component.text(tradeStats.getOrDefault("tradesWithNBT", 0), NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("  Total Items: ", NamedTextColor.GRAY)
                .append(Component.text(tradeStats.getOrDefault("totalItems", 0), NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("  Items with NBT: ", NamedTextColor.GRAY)
                .append(Component.text(tradeStats.getOrDefault("itemsWithNBT", 0), NamedTextColor.WHITE)));
        
        // Cache statistics
        Map<String, Object> cacheStats = plugin.getNBTManager().getCacheStatistics();
        sender.sendMessage(Component.text(""));
        sender.sendMessage(Component.text("Cache Statistics:", NamedTextColor.AQUA));
        sender.sendMessage(Component.text("  Cache Size: ", NamedTextColor.GRAY)
                .append(Component.text(cacheStats.getOrDefault("cache_size", 0).toString(), NamedTextColor.WHITE)));
        
        // ProItems integration
        sender.sendMessage(Component.text(""));
        sender.sendMessage(Component.text("ProItems Integration:", NamedTextColor.AQUA));
        sender.sendMessage(Component.text("  Status: ", NamedTextColor.GRAY)
                .append(Component.text(plugin.isProItemsInstalled() ? "Enabled" : "Disabled", 
                        plugin.isProItemsInstalled() ? NamedTextColor.GREEN : NamedTextColor.RED)));
        
        return true;
    }
    
    /**
     * Handles the cache subcommand.
     */
    private boolean handleCacheCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /ptnbt cache <clear|info>", NamedTextColor.RED));
            return true;
        }
        
        String action = args[1].toLowerCase();
        
        switch (action) {
            case "clear":
                plugin.getNBTManager().clearCache();
                sender.sendMessage(Component.text("NBT cache cleared.", NamedTextColor.GREEN));
                break;
            case "info":
                Map<String, Object> cacheStats = plugin.getNBTManager().getCacheStatistics();
                sender.sendMessage(Component.text("Cache Size: " + cacheStats.get("cache_size"), NamedTextColor.GRAY));
                break;
            default:
                sender.sendMessage(Component.text("Unknown cache action: " + action, NamedTextColor.RED));
                break;
        }
        
        return true;
    }
    
    /**
     * Handles the validate subcommand.
     */
    private boolean handleValidateCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /ptnbt validate <gui_id>", NamedTextColor.RED));
            return true;
        }
        
        String guiId = args[1];
        
        if (!tradeManager.hasTradeGUI(guiId)) {
            sender.sendMessage(Component.text("Trade GUI not found: " + guiId, NamedTextColor.RED));
            return true;
        }
        
        sender.sendMessage(Component.text("Validating NBT data for GUI: " + guiId, NamedTextColor.YELLOW));
        
        // Validate all trades in the GUI
        Map<String, org.mindle.protrades.models.Trade> trades = tradeManager.getTrades(guiId);
        int validTrades = 0;
        int nbtTrades = 0;
        
        for (org.mindle.protrades.models.Trade trade : trades.values()) {
            validTrades++;
            
            boolean hasNBT = false;
            for (ItemStack input : trade.inputs()) {
                if (ItemUtils.hasCustomNBT(input)) {
                    hasNBT = true;
                    break;
                }
            }
            
            if (!hasNBT && ItemUtils.hasCustomNBT(trade.output())) {
                hasNBT = true;
            }
            
            if (hasNBT) {
                nbtTrades++;
            }
        }
        
        sender.sendMessage(Component.text("Validation complete:", NamedTextColor.GREEN));
        sender.sendMessage(Component.text("  Total trades: " + validTrades, NamedTextColor.GRAY));
        sender.sendMessage(Component.text("  Trades with NBT: " + nbtTrades, NamedTextColor.GRAY));
        
        return true;
    }
    
    /**
     * Sends the help message.
     */
    private void sendHelpMessage(CommandSender sender) {
        sender.sendMessage(Component.text("=== ProTrades NBT Commands ===", NamedTextColor.GOLD)
                .decorate(TextDecoration.BOLD));
        sender.sendMessage(Component.text(""));
        sender.sendMessage(Component.text("/ptnbt info", NamedTextColor.YELLOW)
                .append(Component.text(" - Show NBT info for held item", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/ptnbt debug", NamedTextColor.YELLOW)
                .append(Component.text(" - Show detailed NBT debug info", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/ptnbt stats", NamedTextColor.YELLOW)
                .append(Component.text(" - Show NBT usage statistics", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/ptnbt cache <clear|info>", NamedTextColor.YELLOW)
                .append(Component.text(" - Manage NBT cache", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/ptnbt validate <gui_id>", NamedTextColor.YELLOW)
                .append(Component.text(" - Validate NBT data in trades", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/ptnbt help", NamedTextColor.YELLOW)
                .append(Component.text(" - Show this help message", NamedTextColor.GRAY)));
    }
    
    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (!sender.hasPermission("protrades.admin")) {
            return Collections.emptyList();
        }
        
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            List<String> subCommands = Arrays.asList("info", "debug", "stats", "cache", "validate", "help");
            for (String subCommand : subCommands) {
                if (subCommand.toLowerCase().startsWith(args[0].toLowerCase())) {
                    completions.add(subCommand);
                }
            }
        } else if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "cache":
                    List<String> cacheActions = Arrays.asList("clear", "info");
                    for (String action : cacheActions) {
                        if (action.toLowerCase().startsWith(args[1].toLowerCase())) {
                            completions.add(action);
                        }
                    }
                    break;
                case "validate":
                    // Add GUI IDs for tab completion
                    Set<String> guiIds = tradeManager.getAllTradeGUIIds();
                    for (String guiId : guiIds) {
                        if (guiId.toLowerCase().startsWith(args[1].toLowerCase())) {
                            completions.add(guiId);
                        }
                    }
                    break;
            }
        }
        
        return completions;
    }
}