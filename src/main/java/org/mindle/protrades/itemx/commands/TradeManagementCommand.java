package org.mindle.protrades.itemx.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.mindle.protrades.ProTrades;
import org.mindle.protrades.itemx.ColorUtil;
import org.mindle.protrades.itemx.gui.TradeCreationGUI;
import org.mindle.protrades.itemx.templates.TradeTemplate;
import org.mindle.protrades.itemx.templates.TradeTemplateManager;
import org.mindle.protrades.models.Trade;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Enhanced command executor for trade management with templates and GUI creation.
 * Handles /trademgmt commands for advanced trade operations.
 */
public class TradeManagementCommand implements CommandExecutor, TabCompleter {
    
    private final ProTrades plugin;
    private final TradeTemplateManager templateManager;
    private final TradeCreationGUI tradeGUI;
    
    public TradeManagementCommand(ProTrades plugin, TradeTemplateManager templateManager, 
                                 TradeCreationGUI tradeGUI) {
        this.plugin = plugin;
        this.templateManager = templateManager;
        this.tradeGUI = tradeGUI;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelpMessage(sender);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "create":
                return handleCreateCommand(sender, args);
            case "template":
                return handleTemplateCommand(sender, args);
            case "gui":
                return handleGUICommand(sender, args);
            case "list":
                return handleListCommand(sender, args);
            case "apply":
                return handleApplyCommand(sender, args);
            case "reload":
                return handleReloadCommand(sender);
            case "stats":
                return handleStatsCommand(sender);
            default:
                sendHelpMessage(sender);
                return true;
        }
    }
    
    /**
     * Handles create command: /trademgmt create <gui-id>
     */
    private boolean handleCreateCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("protrades.admin")) {
            sendMessage(sender, "<red>You don't have permission to use this command.");
            return true;
        }
        
        if (!(sender instanceof Player)) {
            sendMessage(sender, "<red>This command can only be used by players.");
            return true;
        }
        
        if (args.length < 2) {
            sendMessage(sender, "<red>Usage: /trademgmt create <gui-id>");
            return true;
        }
        
        Player player = (Player) sender;
        String guiId = args[1];
        
        // Check if GUI exists
        if (!plugin.getTradeManager().hasTradeGUI(guiId)) {
            sendMessage(sender, "<red>Trade GUI '" + guiId + "' does not exist.");
            sendMessage(sender, "<yellow>Available GUIs: " + 
                    String.join(", ", plugin.getTradeManager().getAllTradeGUIIds()));
            return true;
        }
        
        // Open trade creation GUI
        tradeGUI.openTradeCreationGUI(player, guiId);
        sendMessage(sender, "<green>Opened trade creation GUI for: " + guiId);
        
        return true;
    }
    
    /**
     * Handles template command: /trademgmt template <list|info|reload> [args]
     */
    private boolean handleTemplateCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("protrades.admin")) {
            sendMessage(sender, "<red>You don't have permission to use this command.");
            return true;
        }
        
        if (args.length < 2) {
            sendMessage(sender, "<red>Usage: /trademgmt template <list|info|reload> [args]");
            return true;
        }
        
        String templateAction = args[1].toLowerCase();
        
        switch (templateAction) {
            case "list":
                return handleTemplateList(sender, args);
            case "info":
                return handleTemplateInfo(sender, args);
            case "reload":
                return handleTemplateReload(sender);
            default:
                sendMessage(sender, "<red>Unknown template action: " + templateAction);
                return true;
        }
    }
    
    /**
     * Handles template list command.
     */
    private boolean handleTemplateList(CommandSender sender, String[] args) {
        String category = args.length > 2 ? args[2] : null;
        
        Set<String> templateIds;
        if (category != null) {
            templateIds = templateManager.getTemplatesInCategory(category);
            if (templateIds.isEmpty()) {
                sendMessage(sender, "<red>No templates found in category: " + category);
                sendMessage(sender, "<yellow>Available categories: " + 
                        String.join(", ", templateManager.getAllCategories()));
                return true;
            }
        } else {
            templateIds = templateManager.getAllTemplateIds();
        }
        
        sendMessage(sender, "<yellow>=== Trade Templates" + 
                (category != null ? " (" + category + ")" : "") + " ===");
        
        for (String templateId : templateIds) {
            TradeTemplate template = templateManager.getTemplate(templateId);
            if (template != null) {
                String status = template.isEnabled() ? "<green>✓</green>" : "<red>✗</red>";
                sendMessage(sender, status + " <aqua>" + templateId + "</aqua> - " + 
                        template.getName() + " <gray>(" + template.getCategory() + ")</gray>");
            }
        }
        
        sendMessage(sender, "<yellow>Total: " + templateIds.size() + " templates");
        return true;
    }
    
    /**
     * Handles template info command.
     */
    private boolean handleTemplateInfo(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sendMessage(sender, "<red>Usage: /trademgmt template info <template-id>");
            return true;
        }
        
        String templateId = args[2];
        TradeTemplate template = templateManager.getTemplate(templateId);
        
        if (template == null) {
            sendMessage(sender, "<red>Template not found: " + templateId);
            return true;
        }
        
        sendMessage(sender, "<yellow>=== Template Info: " + templateId + " ===");
        sendMessage(sender, "<aqua>Name:</aqua> " + template.getName());
        sendMessage(sender, "<aqua>Description:</aqua> " + template.getDescription());
        sendMessage(sender, "<aqua>Category:</aqua> " + template.getCategory());
        sendMessage(sender, "<aqua>Enabled:</aqua> " + (template.isEnabled() ? "<green>Yes</green>" : "<red>No</red>"));
        
        sendMessage(sender, "<aqua>Inputs:</aqua>");
        for (int i = 0; i < template.getInputItems().size(); i++) {
            TradeTemplate.TradeTemplateItem item = template.getInputItems().get(i);
            String type = item.isItemX() ? "<blue>[ItemX]</blue>" : "<gray>[Regular]</gray>";
            sendMessage(sender, "  " + (i + 1) + ". " + type + " " + item.getItemId() + " x" + item.getAmount());
        }
        
        TradeTemplate.TradeTemplateItem output = template.getOutputItem();
        String outputType = output.isItemX() ? "<blue>[ItemX]</blue>" : "<gray>[Regular]</gray>";
        sendMessage(sender, "<aqua>Output:</aqua> " + outputType + " " + output.getItemId() + " x" + output.getAmount());
        
        return true;
    }
    
    /**
     * Handles template reload command.
     */
    private boolean handleTemplateReload(CommandSender sender) {
        try {
            templateManager.reload();
            sendMessage(sender, "<green>Trade templates reloaded successfully!");
        } catch (Exception e) {
            sendMessage(sender, "<red>Error reloading templates: " + e.getMessage());
        }
        return true;
    }
    
    /**
     * Handles GUI command: /trademgmt gui <list|create|delete> [args]
     */
    private boolean handleGUICommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("protrades.admin")) {
            sendMessage(sender, "<red>You don't have permission to use this command.");
            return true;
        }
        
        if (args.length < 2) {
            sendMessage(sender, "<red>Usage: /trademgmt gui <list|create|delete> [args]");
            return true;
        }
        
        String guiAction = args[1].toLowerCase();
        
        switch (guiAction) {
            case "list":
                return handleGUIList(sender);
            case "create":
                return handleGUICreate(sender, args);
            case "delete":
                return handleGUIDelete(sender, args);
            default:
                sendMessage(sender, "<red>Unknown GUI action: " + guiAction);
                return true;
        }
    }
    
    /**
     * Handles GUI list command.
     */
    private boolean handleGUIList(CommandSender sender) {
        Set<String> guiIds = plugin.getTradeManager().getAllTradeGUIIds();
        
        if (guiIds.isEmpty()) {
            sendMessage(sender, "<yellow>No trade GUIs found.");
            return true;
        }
        
        sendMessage(sender, "<yellow>=== Trade GUIs ===");
        for (String guiId : guiIds) {
            int tradeCount = plugin.getTradeManager().getTrades(guiId).size();
            String title = plugin.getTradeManager().getGUITitle(guiId);
            sendMessage(sender, "<aqua>" + guiId + "</aqua> - " + title + " <gray>(" + tradeCount + " trades)</gray>");
        }
        
        return true;
    }
    
    /**
     * Handles GUI create command.
     */
    private boolean handleGUICreate(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sendMessage(sender, "<red>Usage: /trademgmt gui create <gui-id>");
            return true;
        }
        
        String guiId = args[2];
        
        if (plugin.getTradeManager().hasTradeGUI(guiId)) {
            sendMessage(sender, "<red>Trade GUI already exists: " + guiId);
            return true;
        }
        
        plugin.getTradeManager().createTradeGUI(guiId).thenAccept(success -> {
            if (success) {
                sendMessage(sender, "<green>Created trade GUI: " + guiId);
            } else {
                sendMessage(sender, "<red>Failed to create trade GUI: " + guiId);
            }
        });
        
        return true;
    }
    
    /**
     * Handles GUI delete command.
     */
    private boolean handleGUIDelete(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sendMessage(sender, "<red>Usage: /trademgmt gui delete <gui-id>");
            return true;
        }
        
        String guiId = args[2];
        
        if (!plugin.getTradeManager().hasTradeGUI(guiId)) {
            sendMessage(sender, "<red>Trade GUI does not exist: " + guiId);
            return true;
        }
        
        boolean success = plugin.getTradeManager().deleteTradeGUI(guiId);
        if (success) {
            sendMessage(sender, "<green>Deleted trade GUI: " + guiId);
        } else {
            sendMessage(sender, "<red>Failed to delete trade GUI: " + guiId);
        }
        
        return true;
    }
    
    /**
     * Handles apply command: /trademgmt apply <template-id> <gui-id>
     */
    private boolean handleApplyCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("protrades.admin")) {
            sendMessage(sender, "<red>You don't have permission to use this command.");
            return true;
        }
        
        if (args.length < 3) {
            sendMessage(sender, "<red>Usage: /trademgmt apply <template-id> <gui-id>");
            return true;
        }
        
        String templateId = args[1];
        String guiId = args[2];
        
        // Validate template
        TradeTemplate template = templateManager.getTemplate(templateId);
        if (template == null) {
            sendMessage(sender, "<red>Template not found: " + templateId);
            return true;
        }
        
        if (!template.isEnabled()) {
            sendMessage(sender, "<red>Template is disabled: " + templateId);
            return true;
        }
        
        // Validate GUI
        if (!plugin.getTradeManager().hasTradeGUI(guiId)) {
            sendMessage(sender, "<red>Trade GUI does not exist: " + guiId);
            return true;
        }
        
        // Apply template
        boolean success = templateManager.applyTemplateToGUI(templateId, guiId);
        if (success) {
            sendMessage(sender, "<green>Applied template '" + templateId + "' to GUI '" + guiId + "'");
        } else {
            sendMessage(sender, "<red>Failed to apply template to GUI");
        }
        
        return true;
    }
    
    /**
     * Handles list command: /trademgmt list [gui-id]
     */
    private boolean handleListCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            // List all GUIs
            return handleGUIList(sender);
        } else {
            // List trades in specific GUI
            String guiId = args[1];
            
            if (!plugin.getTradeManager().hasTradeGUI(guiId)) {
                sendMessage(sender, "<red>Trade GUI does not exist: " + guiId);
                return true;
            }
            
            Map<String, Trade> trades = plugin.getTradeManager().getTrades(guiId);
            
            sendMessage(sender, "<yellow>=== Trades in " + guiId + " ===");
            if (trades.isEmpty()) {
                sendMessage(sender, "<gray>No trades found.");
            } else {
                for (Map.Entry<String, Trade> entry : trades.entrySet()) {
                    Trade trade = entry.getValue();
                    sendMessage(sender, "<aqua>" + trade.id() + "</aqua> - " + 
                            trade.inputs().size() + " inputs → " + 
                            (trade.output().hasItemMeta() && trade.output().getItemMeta().hasDisplayName() 
                                    ? trade.output().getItemMeta().getDisplayName() 
                                    : trade.output().getType().name()));
                }
            }
            
            return true;
        }
    }
    
    /**
     * Handles reload command.
     */
    private boolean handleReloadCommand(CommandSender sender) {
        if (!sender.hasPermission("protrades.admin")) {
            sendMessage(sender, "<red>You don't have permission to use this command.");
            return true;
        }
        
        try {
            templateManager.reload();
            plugin.getItemManager().reload();
            sendMessage(sender, "<green>Reloaded templates and ItemX items successfully!");
        } catch (Exception e) {
            sendMessage(sender, "<red>Error during reload: " + e.getMessage());
        }
        
        return true;
    }
    
    /**
     * Handles stats command.
     */
    private boolean handleStatsCommand(CommandSender sender) {
        sendMessage(sender, "<yellow>=== Trade Management Statistics ===");
        
        // Template stats
        Map<String, Object> templateStats = templateManager.getStatistics();
        sendMessage(sender, "<aqua>Templates:</aqua>");
        sendMessage(sender, "  Total: " + templateStats.get("total_templates"));
        sendMessage(sender, "  Enabled: " + templateStats.get("enabled_templates"));
        sendMessage(sender, "  Categories: " + templateStats.get("template_categories"));
        sendMessage(sender, "  ItemX Templates: " + templateStats.get("itemx_templates"));
        sendMessage(sender, "  Mixed Templates: " + templateStats.get("mixed_templates"));
        
        // Trade GUI stats
        Set<String> guiIds = plugin.getTradeManager().getAllTradeGUIIds();
        int totalTrades = 0;
        for (String guiId : guiIds) {
            totalTrades += plugin.getTradeManager().getTrades(guiId).size();
        }
        
        sendMessage(sender, "<aqua>Trade GUIs:</aqua>");
        sendMessage(sender, "  Total GUIs: " + guiIds.size());
        sendMessage(sender, "  Total Trades: " + totalTrades);
        
        // ItemX stats
        Map<String, Object> itemStats = plugin.getItemManager().getStatistics();
        sendMessage(sender, "<aqua>ItemX Items:</aqua>");
        sendMessage(sender, "  Total Items: " + itemStats.get("total_items"));
        sendMessage(sender, "  Categories: " + itemStats.get("categories"));
        
        return true;
    }
    
    /**
     * Sends help message.
     */
    private void sendHelpMessage(CommandSender sender) {
        sendMessage(sender, "<yellow>=== Trade Management Commands ===");
        sendMessage(sender, "<aqua>/trademgmt create <gui-id></aqua> - Open trade creation GUI");
        sendMessage(sender, "<aqua>/trademgmt template list [category]</aqua> - List templates");
        sendMessage(sender, "<aqua>/trademgmt template info <template-id></aqua> - Template details");
        sendMessage(sender, "<aqua>/trademgmt apply <template-id> <gui-id></aqua> - Apply template");
        sendMessage(sender, "<aqua>/trademgmt gui list</aqua> - List trade GUIs");
        sendMessage(sender, "<aqua>/trademgmt gui create <gui-id></aqua> - Create new GUI");
        sendMessage(sender, "<aqua>/trademgmt list [gui-id]</aqua> - List trades in GUI");
        sendMessage(sender, "<aqua>/trademgmt reload</aqua> - Reload configurations");
        sendMessage(sender, "<aqua>/trademgmt stats</aqua> - Show statistics");
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            completions.addAll(Arrays.asList("create", "template", "gui", "list", "apply", "reload", "stats"));
        } else if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            
            switch (subCommand) {
                case "create":
                case "list":
                    completions.addAll(plugin.getTradeManager().getAllTradeGUIIds());
                    break;
                case "template":
                    completions.addAll(Arrays.asList("list", "info", "reload"));
                    break;
                case "gui":
                    completions.addAll(Arrays.asList("list", "create", "delete"));
                    break;
                case "apply":
                    completions.addAll(templateManager.getAllTemplateIds());
                    break;
            }
        } else if (args.length == 3) {
            String subCommand = args[0].toLowerCase();
            String action = args[1].toLowerCase();
            
            if ("template".equals(subCommand)) {
                if ("list".equals(action)) {
                    completions.addAll(templateManager.getAllCategories());
                } else if ("info".equals(action)) {
                    completions.addAll(templateManager.getAllTemplateIds());
                }
            } else if ("gui".equals(subCommand) && ("create".equals(action) || "delete".equals(action))) {
                if ("delete".equals(action)) {
                    completions.addAll(plugin.getTradeManager().getAllTradeGUIIds());
                }
            } else if ("apply".equals(subCommand)) {
                completions.addAll(plugin.getTradeManager().getAllTradeGUIIds());
            }
        }
        
        return completions.stream()
                .filter(completion -> completion.toLowerCase().startsWith(args[args.length - 1].toLowerCase()))
                .collect(Collectors.toList());
    }
    
    /**
     * Sends a colored message to a command sender.
     */
    private void sendMessage(CommandSender sender, String message) {
        if (message == null || message.isEmpty()) {
            return;
        }
        
        String prefix = "<gray>[<green>TradeMgmt</green>]</gray> ";
        String fullMessage = prefix + message;
        
        if (sender instanceof Player) {
            ((Player) sender).sendMessage(ColorUtil.colorize(fullMessage));
        } else {
            sender.sendMessage(ColorUtil.stripColor(fullMessage));
        }
    }
}