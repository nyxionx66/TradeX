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

/**
 * Command executor for the trade command.
 * Allows players to open trade GUIs.
 */
public class TradeCommand implements CommandExecutor, TabCompleter {

    private final ProTrades plugin;
    private final TradeManager tradeManager;
    private final GUIManager guiManager;

    public TradeCommand(ProTrades plugin, TradeManager tradeManager, GUIManager guiManager) {
        this.plugin = plugin;
        this.tradeManager = tradeManager;
        this.guiManager = guiManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, 
                           @NotNull String label, @NotNull String[] args) {
        
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("This command can only be used by players.", NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(Component.text("Usage: /trade <id>", NamedTextColor.RED));
            return true;
        }

        String tradeId = args[0];
        
        // Check base permission
        if (!player.hasPermission("protrades.trade")) {
            player.sendMessage(Component.text("You don't have permission to use trades.", NamedTextColor.RED));
            return true;
        }

        // Check specific trade permission
        if (!player.hasPermission("protrades.trade." + tradeId) && 
            !player.hasPermission("protrades.trade.*")) {
            player.sendMessage(Component.text("You don't have permission to access this trade GUI.", NamedTextColor.RED));
            return true;
        }

        if (!tradeManager.hasTradeGUI(tradeId)) {
            player.sendMessage(Component.text("Trade GUI not found: " + tradeId, NamedTextColor.RED));
            return true;
        }

        guiManager.openTradeGUI(player, tradeId);
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, 
                                              @NotNull String label, @NotNull String[] args) {
        
        if (!(sender instanceof Player player)) {
            return new ArrayList<>();
        }

        if (!player.hasPermission("protrades.trade")) {
            return new ArrayList<>();
        }

        if (args.length == 1) {
            return tradeManager.getAllTradeGUIIds().stream()
                    .filter(id -> player.hasPermission("protrades.trade." + id) || 
                                player.hasPermission("protrades.trade.*"))
                    .filter(id -> id.startsWith(args[0].toLowerCase()))
                    .toList();
        }

        return new ArrayList<>();
    }
}