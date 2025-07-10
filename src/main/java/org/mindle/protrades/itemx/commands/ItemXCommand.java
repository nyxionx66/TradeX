package org.mindle.protrades.itemx.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.mindle.protrades.ProTrades;
import org.mindle.protrades.itemx.ColorUtil;
import org.mindle.protrades.itemx.ItemDefinition;
import org.mindle.protrades.itemx.ItemManager;
import org.mindle.protrades.utils.ItemUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Command executor for ItemX commands with full tab completion support.
 */
public class ItemXCommand implements CommandExecutor, TabCompleter {
    
    private final ProTrades plugin;
    private final ItemManager itemManager;
    
    public ItemXCommand(ProTrades plugin, ItemManager itemManager) {
        this.plugin = plugin;
        this.itemManager = itemManager;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendMessage(sender, plugin.getConfig().getString("itemx.messages.invalid-usage", 
                    "<red>Usage: /itemx <give|get|reload>"));
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "give":
                return handleGiveCommand(sender, args);
            case "get":
                return handleGetCommand(sender, args);
            case "reload":
                return handleReloadCommand(sender);
            default:
                sendMessage(sender, plugin.getConfig().getString("itemx.messages.invalid-usage", 
                        "<red>Usage: /itemx <give|get|reload>"));
                return true;
        }
    }
    
    /**
     * Handles the give command: /itemx give <item-id> [player]
     */
    private boolean handleGiveCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("itemx.give")) {
            sendMessage(sender, plugin.getConfig().getString("itemx.messages.no-permission",
                    "<red>You don't have permission to use this command."));
            return true;
        }
        
        if (args.length < 2) {
            sendMessage(sender, "<red>Usage: /itemx give <item-id> [player]");
            return true;
        }
        
        String itemId = args[1];
        ItemDefinition itemDef = itemManager.getItemDefinition(itemId);
        
        if (itemDef == null) {
            sendMessage(sender, ColorUtil.processPlaceholders(
                    plugin.getConfig().getString("itemx.messages.item-not-found", 
                            "<red>Item <yellow>%item%</yellow> not found."),
                    "%item%", itemId));
            return true;
        }
        
        Player target;
        if (args.length >= 3) {
            target = Bukkit.getPlayer(args[2]);
            if (target == null) {
                sendMessage(sender, ColorUtil.processPlaceholders(
                        plugin.getConfig().getString("itemx.messages.player-not-found", 
                                "<red>Player <yellow>%player%</yellow> not found."),
                        "%player%", args[2]));
                return true;
            }
        } else {
            if (!(sender instanceof Player)) {
                sendMessage(sender, "<red>You must specify a player when running from console.");
                return true;
            }
            target = (Player) sender;
        }
        
        ItemStack item = itemManager.createItemStack(itemDef);
        if (item == null) {
            sendMessage(sender, "<red>Failed to create item: " + itemId);
            return true;
        }
        
        ItemUtils.giveItem(target, item);
        
        // Send success message
        String giveMessage = plugin.getConfig().getString("itemx.messages.give-message", 
                "<green>Gave <yellow>%item%</yellow> to <blue>%player%</blue>");
        sendMessage(sender, ColorUtil.processPlaceholders(giveMessage, 
                "%item%", itemDef.getDisplayName(), "%player%", target.getName()));
        
        if (target != sender) {
            String getMessage = plugin.getConfig().getString("itemx.messages.get-message", 
                    "<green>You received <yellow>%item%</yellow>");
            sendMessage(target, ColorUtil.processPlaceholders(getMessage, 
                    "%item%", itemDef.getDisplayName()));
        }
        
        return true;
    }
    
    /**
     * Handles the get command: /itemx get category:<category-name>
     */
    private boolean handleGetCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("itemx.get")) {
            sendMessage(sender, plugin.getConfig().getString("itemx.messages.no-permission",
                    "<red>You don't have permission to use this command."));
            return true;
        }
        
        if (!(sender instanceof Player)) {
            sendMessage(sender, "<red>This command can only be used by players.");
            return true;
        }
        
        Player player = (Player) sender;
        
        if (args.length < 2) {
            sendMessage(sender, "<red>Usage: /itemx get category:<category-name>");
            return true;
        }
        
        String argument = args[1];
        
        // Check if it's a category request
        if (argument.startsWith("category:")) {
            return handleCategoryGet(player, argument.substring(9));
        } else {
            sendMessage(sender, "<red>You can only get items by category. Usage: /itemx get category:<category-name>");
            return true;
        }
    }
    
    /**
     * Handles getting all items from a category.
     */
    private boolean handleCategoryGet(Player player, String categoryName) {
        Set<String> itemIds = itemManager.getItemsInCategory(categoryName);
        
        if (itemIds.isEmpty()) {
            sendMessage(player, "<red>Category '" + categoryName + "' not found or empty.");
            return true;
        }
        
        int givenCount = 0;
        for (String itemId : itemIds) {
            ItemDefinition itemDef = itemManager.getItemDefinition(itemId);
            if (itemDef != null) {
                ItemStack item = itemManager.createItemStack(itemDef);
                if (item != null) {
                    ItemUtils.giveItem(player, item);
                    givenCount++;
                }
            }
        }
        
        String getMessage = plugin.getConfig().getString("itemx.messages.get-category-message", 
                "<green>You received <yellow>%count%</yellow> items from category <aqua>%category%</aqua>");
        sendMessage(player, ColorUtil.processPlaceholders(getMessage, 
                "%count%", String.valueOf(givenCount), "%category%", categoryName));
        
        return true;
    }
    
    /**
     * Handles the reload command: /itemx reload
     */
    private boolean handleReloadCommand(CommandSender sender) {
        if (!sender.hasPermission("itemx.reload")) {
            sendMessage(sender, plugin.getConfig().getString("itemx.messages.no-permission",
                    "<red>You don't have permission to use this command."));
            return true;
        }
        
        try {
            itemManager.reload();
            sendMessage(sender, plugin.getConfig().getString("itemx.messages.reload-success", 
                    "<green>ItemX has been reloaded successfully!"));
        } catch (Exception e) {
            sendMessage(sender, "<red>Error reloading ItemX: " + e.getMessage());
            plugin.getLogger().severe("Error reloading ItemX: " + e.getMessage());
        }
        
        return true;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            // First argument: subcommands
            List<String> subCommands = Arrays.asList("give", "get", "reload");
            completions.addAll(subCommands.stream()
                    .filter(sub -> sub.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList()));
        } else if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            
            switch (subCommand) {
                case "give":
                case "get":
                    // Second argument: item IDs
                    completions.addAll(itemManager.getAllItemIds().stream()
                            .filter(id -> id.startsWith(args[1].toLowerCase()))
                            .collect(Collectors.toList()));
                    
                    // For get command, also add category options
                    if ("get".equals(subCommand)) {
                        completions.addAll(itemManager.getAllCategories().stream()
                                .map(cat -> "category:" + cat)
                                .filter(cat -> cat.startsWith(args[1].toLowerCase()))
                                .collect(Collectors.toList()));
                    }
                    break;
            }
        } else if (args.length == 3 && "give".equals(args[0].toLowerCase())) {
            // Third argument for give: player names
            completions.addAll(Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[2].toLowerCase()))
                    .collect(Collectors.toList()));
        }
        
        return completions;
    }
    
    /**
     * Sends a colored message to a command sender.
     */
    private void sendMessage(CommandSender sender, String message) {
        if (message == null || message.isEmpty()) {
            return;
        }
        
        String prefix = plugin.getConfig().getString("itemx.prefix", "<gray>[<aqua>ItemX</aqua>]</gray> ");
        String fullMessage = prefix + message;
        
        if (sender instanceof Player) {
            ((Player) sender).sendMessage(ColorUtil.colorize(fullMessage));
        } else {
            sender.sendMessage(ColorUtil.stripColor(fullMessage));
        }
    }
}