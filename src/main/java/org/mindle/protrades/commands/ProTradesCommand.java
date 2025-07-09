package org.mindle.protrades.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mindle.protrades.ProTrades;
import org.mindle.protrades.managers.GUIManager;
import org.mindle.protrades.managers.TradeManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Command executor for the main ProTrades admin command.
 * Uses Java 21 switch expressions for cleaner command handling.
 * Enhanced with edit trade functionality and move trade features.
 */
public class ProTradesCommand implements CommandExecutor, TabCompleter {

    private final ProTrades plugin;
    private final TradeManager tradeManager;
    private final GUIManager guiManager;

    public ProTradesCommand(ProTrades plugin, TradeManager tradeManager, GUIManager guiManager) {
        this.plugin = plugin;
        this.tradeManager = tradeManager;
        this.guiManager = guiManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, 
                           @NotNull String label, @NotNull String[] args) {
        
        if (!sender.hasPermission("protrades.admin")) {
            sender.sendMessage(Component.text("You don't have permission to use this command.", NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            sendHelpMessage(sender);
            return true;
        }

        return switch (args[0].toLowerCase()) {
            case "create" -> handleCreate(sender, args);
            case "delete" -> handleDelete(sender, args);
            case "edit" -> handleEdit(sender, args);
            case "edittrade" -> handleEditTrade(sender, args);
            case "movetrade" -> handleMoveTrade(sender, args);
            case "reorder" -> handleReorder(sender, args);
            case "list" -> handleList(sender);
            case "reload" -> handleReload(sender);
            case "help" -> {
                sendHelpMessage(sender);
                yield true;
            }
            default -> {
                sender.sendMessage(Component.text("Unknown subcommand: " + args[0], NamedTextColor.RED));
                sendHelpMessage(sender);
                yield true;
            }
        };
    }

    /**
     * Handles the create subcommand.
     * 
     * @param sender The command sender
     * @param args The command arguments
     * @return true if the command was handled
     */
    private boolean handleCreate(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /protrades create <id>", NamedTextColor.RED));
            return true;
        }

        String tradeId = args[1];
        
        if (tradeManager.hasTradeGUI(tradeId)) {
            sender.sendMessage(Component.text("Trade GUI already exists: " + tradeId, NamedTextColor.RED));
            return true;
        }

        tradeManager.createTradeGUI(tradeId)
                .thenAccept(success -> {
                    if (success) {
                        sender.sendMessage(Component.text("Created trade GUI: " + tradeId, NamedTextColor.GREEN));
                    } else {
                        sender.sendMessage(Component.text("Failed to create trade GUI: " + tradeId, NamedTextColor.RED));
                    }
                });

        return true;
    }

    /**
     * Handles the delete subcommand.
     * 
     * @param sender The command sender
     * @param args The command arguments
     * @return true if the command was handled
     */
    private boolean handleDelete(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /protrades delete <id>", NamedTextColor.RED));
            return true;
        }

        String tradeId = args[1];
        
        if (!tradeManager.hasTradeGUI(tradeId)) {
            sender.sendMessage(Component.text("Trade GUI not found: " + tradeId, NamedTextColor.RED));
            return true;
        }

        boolean success = tradeManager.deleteTradeGUI(tradeId);
        
        if (success) {
            sender.sendMessage(Component.text("Deleted trade GUI: " + tradeId, NamedTextColor.GREEN));
        } else {
            sender.sendMessage(Component.text("Failed to delete trade GUI: " + tradeId, NamedTextColor.RED));
        }

        return true;
    }

    /**
     * Handles the edit subcommand (opens the trade GUI editor).
     * 
     * @param sender The command sender
     * @param args The command arguments
     * @return true if the command was handled
     */
    private boolean handleEdit(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("This command can only be used by players.", NamedTextColor.RED));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /protrades edit <id>", NamedTextColor.RED));
            return true;
        }

        String tradeId = args[1];
        
        if (!tradeManager.hasTradeGUI(tradeId)) {
            sender.sendMessage(Component.text("Trade GUI not found: " + tradeId, NamedTextColor.RED));
            return true;
        }

        guiManager.openEditorGUI(player, tradeId);
        return true;
    }

    /**
     * Handles the edittrade subcommand (opens the specific trade editor).
     * 
     * @param sender The command sender
     * @param args The command arguments
     * @return true if the command was handled
     */
    private boolean handleEditTrade(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("This command can only be used by players.", NamedTextColor.RED));
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage(Component.text("Usage: /protrades edittrade <gui_id> <trade_id>", NamedTextColor.RED));
            return true;
        }

        String guiId = args[1];
        String tradeId = args[2];
        
        if (!tradeManager.hasTradeGUI(guiId)) {
            sender.sendMessage(Component.text("Trade GUI not found: " + guiId, NamedTextColor.RED));
            return true;
        }

        if (tradeManager.getTrade(guiId, tradeId) == null) {
            sender.sendMessage(Component.text("Trade not found: " + tradeId + " in GUI: " + guiId, NamedTextColor.RED));
            return true;
        }

        guiManager.openTradeEditGUI(player, guiId, tradeId);
        return true;
    }

    /**
     * Handles the movetrade subcommand (moves a trade to a specific position).
     * 
     * @param sender The command sender
     * @param args The command arguments
     * @return true if the command was handled
     */
    private boolean handleMoveTrade(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage(Component.text("Usage: /protrades movetrade <gui_id> <trade_id> <position>", NamedTextColor.RED));
            return true;
        }

        String guiId = args[1];
        String tradeId = args[2];
        
        if (!tradeManager.hasTradeGUI(guiId)) {
            sender.sendMessage(Component.text("Trade GUI not found: " + guiId, NamedTextColor.RED));
            return true;
        }

        if (tradeManager.getTrade(guiId, tradeId) == null) {
            sender.sendMessage(Component.text("Trade not found: " + tradeId + " in GUI: " + guiId, NamedTextColor.RED));
            return true;
        }

        try {
            int position = Integer.parseInt(args[3]);
            boolean success = tradeManager.moveTradeToPosition(guiId, tradeId, position);
            
            if (success) {
                sender.sendMessage(Component.text("✅ Moved trade '" + tradeId + "' to position " + position, NamedTextColor.GREEN));
                
                // Refresh any open editor GUIs
                if (sender instanceof Player player) {
                    guiManager.refreshEditorGUIIfOpen(player, guiId);
                }
            } else {
                sender.sendMessage(Component.text("❌ Failed to move trade. Position may be invalid.", NamedTextColor.RED));
            }
        } catch (NumberFormatException e) {
            sender.sendMessage(Component.text("❌ Invalid position number: " + args[3], NamedTextColor.RED));
        }

        return true;
    }

    /**
     * Handles the reorder subcommand (opens the trade reorder GUI).
     * 
     * @param sender The command sender
     * @param args The command arguments
     * @return true if the command was handled
     */
    private boolean handleReorder(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("This command can only be used by players.", NamedTextColor.RED));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /protrades reorder <gui_id>", NamedTextColor.RED));
            return true;
        }

        String guiId = args[1];
        
        if (!tradeManager.hasTradeGUI(guiId)) {
            sender.sendMessage(Component.text("Trade GUI not found: " + guiId, NamedTextColor.RED));
            return true;
        }

        guiManager.openTradeReorderGUI(player, guiId);
        return true;
    }

    /**
     * Handles the list subcommand.
     * 
     * @param sender The command sender
     * @return true if the command was handled
     */
    private boolean handleList(CommandSender sender) {
        Set<String> guiIds = tradeManager.getAllTradeGUIIds();
        
        if (guiIds.isEmpty()) {
            sender.sendMessage(Component.text("No trade GUIs found.", NamedTextColor.YELLOW));
            return true;
        }

        sender.sendMessage(Component.text("Trade GUIs (" + guiIds.size() + "):", NamedTextColor.GREEN));
        
        for (String guiId : guiIds) {
            int tradeCount = tradeManager.getTrades(guiId).size();
            sender.sendMessage(Component.text("- " + guiId + " (" + tradeCount + " trades)", NamedTextColor.GRAY));
        }

        return true;
    }

    /**
     * Handles the reload subcommand.
     * 
     * @param sender The command sender
     * @return true if the command was handled
     */
    private boolean handleReload(CommandSender sender) {
        try {
            tradeManager.loadAllTrades();
            sender.sendMessage(Component.text("ProTrades configurations reloaded successfully!", NamedTextColor.GREEN));
        } catch (Exception e) {
            sender.sendMessage(Component.text("Failed to reload configurations: " + e.getMessage(), NamedTextColor.RED));
            plugin.getLogger().severe("Error reloading configurations: " + e.getMessage());
        }

        return true;
    }

    /**
     * Sends a help message to the command sender.
     * 
     * @param sender The command sender
     */
    private void sendHelpMessage(CommandSender sender) {
        sender.sendMessage(Component.text("=== ProTrades Commands ===", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/protrades create <id> - Create a new trade GUI", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/protrades delete <id> - Delete a trade GUI", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/protrades edit <id> - Edit a trade GUI", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/protrades edittrade <gui_id> <trade_id> - Edit a specific trade", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/protrades movetrade <gui_id> <trade_id> <position> - Move trade to position", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/protrades reorder <gui_id> - Open trade reorder GUI", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/protrades list - List all trade GUIs", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/protrades reload - Reload all configurations", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/protrades help - Show this help message", NamedTextColor.YELLOW));
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, 
                                              @NotNull String label, @NotNull String[] args) {
        
        if (!sender.hasPermission("protrades.admin")) {
            return new ArrayList<>();
        }

        return switch (args.length) {
            case 1 -> {
                List<String> subcommands = List.of("create", "delete", "edit", "edittrade", "movetrade", "reorder", "list", "reload", "help");
                yield subcommands.stream()
                        .filter(sub -> sub.startsWith(args[0].toLowerCase()))
                        .toList();
            }
            case 2 -> {
                if (args[0].equalsIgnoreCase("delete") || args[0].equalsIgnoreCase("edit") || 
                    args[0].equalsIgnoreCase("edittrade") || args[0].equalsIgnoreCase("movetrade") || 
                    args[0].equalsIgnoreCase("reorder")) {
                    yield tradeManager.getAllTradeGUIIds().stream()
                            .filter(id -> id.startsWith(args[1].toLowerCase()))
                            .toList();
                }
                yield new ArrayList<>();
            }
            case 3 -> {
                if (args[0].equalsIgnoreCase("edittrade") || args[0].equalsIgnoreCase("movetrade")) {
                    String guiId = args[1];
                    if (tradeManager.hasTradeGUI(guiId)) {
                        yield tradeManager.getTrades(guiId).keySet().stream()
                                .filter(id -> id.startsWith(args[2].toLowerCase()))
                                .toList();
                    }
                }
                yield new ArrayList<>();
            }
            case 4 -> {
                if (args[0].equalsIgnoreCase("movetrade")) {
                    String guiId = args[1];
                    if (tradeManager.hasTradeGUI(guiId)) {
                        int tradeCount = tradeManager.getTrades(guiId).size();
                        List<String> positions = new ArrayList<>();
                        for (int i = 1; i <= tradeCount; i++) {
                            positions.add(String.valueOf(i));
                        }
                        yield positions.stream()
                                .filter(pos -> pos.startsWith(args[3]))
                                .toList();
                    }
                }
                yield new ArrayList<>();
            }
            default -> new ArrayList<>();
        };
    }
}