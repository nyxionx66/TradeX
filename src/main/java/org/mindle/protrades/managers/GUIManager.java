package org.mindle.protrades.managers;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Merchant;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.ItemFlag;
import org.mindle.protrades.ProTrades;
import org.mindle.protrades.models.Trade;
import org.mindle.protrades.utils.ItemUtils;

import java.util.*;

/**
 * Enhanced GUI Manager with improved visual design, proper inventory handling, full NBT support,
 * comprehensive trade editing capabilities, and trade movement/reordering functionality.
 * Creates beautiful, intuitive trade editor interfaces with fixed slot management and NBT preservation.
 */
public class GUIManager {

    private final ProTrades plugin;
    private final TradeManager tradeManager;
    private final Map<UUID, String> playerEditorSessions = new HashMap<>();
    private final Map<UUID, String> playerTradeGUIs = new HashMap<>();
    private final Map<UUID, String> playerTradeEditSessions = new HashMap<>();
    private final Map<UUID, String> playerReorderSessions = new HashMap<>();
    private final Map<UUID, ItemStack> playerDraggedItems = new HashMap<>();

    // Constants for slot positions
    private static final int INPUT_SLOT_1 = 0;
    private static final int INPUT_SLOT_2 = 1;
    private static final int OUTPUT_SLOT = 2;
    private static final int SAVE_BUTTON_SLOT = 9;
    private static final int CLEAR_BUTTON_SLOT = 10;
    private static final int HELP_BUTTON_SLOT = 11;
    private static final int PREVIEW_BUTTON_SLOT = 12;
    private static final int CANCEL_BUTTON_SLOT = 13;
    private static final int DELETE_BUTTON_SLOT = 14;
    private static final int TRADES_HEADER_SLOT = 18;
    private static final int TRADES_START_SLOT = 19;

    public GUIManager(ProTrades plugin, TradeManager tradeManager) {
        this.plugin = plugin;
        this.tradeManager = tradeManager;
    }

    /**
     * Opens a villager-style trade GUI for a player with NBT support.
     */
    public void openTradeGUI(Player player, String guiId) {
        if (!tradeManager.hasTradeGUI(guiId)) {
            player.sendMessage(Component.text("Trade GUI not found: " + guiId, NamedTextColor.RED));
            return;
        }

        String title = tradeManager.getGUITitle(guiId);
        Map<String, Trade> trades = tradeManager.getTrades(guiId);

        // Create merchant for villager-style trading
        Merchant merchant = Bukkit.createMerchant(Component.text(title.replace("&", "§")));
        List<MerchantRecipe> recipes = new ArrayList<>();

        // Convert trades to merchant recipes with NBT preservation (in order)
        for (Trade trade : trades.values()) {
            MerchantRecipe recipe = createMerchantRecipe(trade);
            if (recipe != null) {
                recipes.add(recipe);
                plugin.getLogger().fine("Created merchant recipe for trade: " + trade.id() + " with NBT data");
            }
        }

        merchant.setRecipes(recipes);
        playerTradeGUIs.put(player.getUniqueId(), guiId);
        player.openMerchant(merchant, true);
        
        plugin.getLogger().info("Opened trade GUI: " + guiId + " for player: " + player.getName() + " with " + recipes.size() + " NBT-enabled trades");
    }

    /**
     * Opens the enhanced trade editor GUI for a player with NBT support.
     */
    public void openEditorGUI(Player player, String guiId) {
        if (!tradeManager.hasTradeGUI(guiId)) {
            player.sendMessage(Component.text("Trade GUI not found: " + guiId, NamedTextColor.RED));
            return;
        }

        playerEditorSessions.put(player.getUniqueId(), guiId);

        // Create enhanced editor inventory (6 rows) with proper colors
        Component title = Component.text("Trade Editor: ", NamedTextColor.DARK_BLUE)
                .decorate(TextDecoration.BOLD)
                .append(Component.text(guiId, NamedTextColor.GOLD))
                .append(Component.text(" (NBT Enabled)", NamedTextColor.GREEN));

        Inventory inventory = Bukkit.createInventory(null, 54, title);
        populateEnhancedEditorGUI(inventory, guiId);

        player.openInventory(inventory);
        plugin.getLogger().info("Opened NBT-enabled editor GUI for player: " + player.getName() + " on GUI: " + guiId);
    }

    /**
     * Opens the trade edit GUI for editing a specific trade.
     */
    public void openTradeEditGUI(Player player, String guiId, String tradeId) {
        if (!tradeManager.hasTradeGUI(guiId)) {
            player.sendMessage(Component.text("Trade GUI not found: " + guiId, NamedTextColor.RED));
            return;
        }

        Trade trade = tradeManager.getTrade(guiId, tradeId);
        if (trade == null) {
            player.sendMessage(Component.text("Trade not found: " + tradeId, NamedTextColor.RED));
            return;
        }

        playerTradeEditSessions.put(player.getUniqueId(), guiId + ":" + tradeId);

        // Create trade edit inventory (6 rows) with proper colors
        Component title = Component.text("Edit Trade: ", NamedTextColor.DARK_PURPLE)
                .decorate(TextDecoration.BOLD)
                .append(Component.text(tradeId, NamedTextColor.GOLD))
                .append(Component.text(" (NBT Enabled)", NamedTextColor.GREEN));

        Inventory inventory = Bukkit.createInventory(null, 54, title);
        populateTradeEditGUI(inventory, trade);

        player.openInventory(inventory);
        plugin.getLogger().info("Opened trade edit GUI for player: " + player.getName() + " editing trade: " + tradeId);
    }

    /**
     * Opens the trade reorder GUI for rearranging trade positions.
     */
    public void openTradeReorderGUI(Player player, String guiId) {
        if (!tradeManager.hasTradeGUI(guiId)) {
            player.sendMessage(Component.text("Trade GUI not found: " + guiId, NamedTextColor.RED));
            return;
        }

        playerReorderSessions.put(player.getUniqueId(), guiId);

        // Create reorder inventory (6 rows) with proper colors
        Component title = Component.text("Reorder Trades: ", NamedTextColor.DARK_GREEN)
                .decorate(TextDecoration.BOLD)
                .append(Component.text(guiId, NamedTextColor.GOLD))
                .append(Component.text(" (Drag & Drop)", NamedTextColor.AQUA));

        Inventory inventory = Bukkit.createInventory(null, 54, title);
        populateTradeReorderGUI(inventory, guiId);

        player.openInventory(inventory);
        player.sendMessage(Component.text("🔄 Trade Reorder Mode", NamedTextColor.GREEN)
                .decorate(TextDecoration.BOLD));
        player.sendMessage(Component.text("• Drag trades to reorder them", NamedTextColor.GRAY));
        player.sendMessage(Component.text("• Click 'Save Order' when finished", NamedTextColor.GRAY));
        player.sendMessage(Component.text("• Click 'Cancel' to discard changes", NamedTextColor.GRAY));
        
        plugin.getLogger().info("Opened trade reorder GUI for player: " + player.getName() + " on GUI: " + guiId);
    }

    /**
     * Populates the trade reorder GUI with draggable trade items.
     */
    private void populateTradeReorderGUI(Inventory inventory, String guiId) {
        // Clear inventory first
        inventory.clear();

        // Header with instructions
        ItemStack header = new ItemStack(Material.COMPASS);
        ItemMeta headerMeta = header.getItemMeta();
        if (headerMeta != null) {
            headerMeta.displayName(Component.text("🔄 Trade Reorder", NamedTextColor.AQUA)
                    .decorate(TextDecoration.BOLD));
            
            List<Component> lore = new ArrayList<>();
            lore.add(Component.empty());
            lore.add(Component.text("Drag and drop trades to reorder them", NamedTextColor.GRAY));
            lore.add(Component.text("The order here will be the order players see", NamedTextColor.GRAY));
            lore.add(Component.empty());
            lore.add(Component.text("💡 Tip: First trade = Top of villager GUI", NamedTextColor.YELLOW));
            lore.add(Component.empty());
            
            headerMeta.lore(lore);
            headerMeta.addEnchant(Enchantment.UNBREAKING, 1, true);
            headerMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            header.setItemMeta(headerMeta);
        }
        inventory.setItem(4, header);

        // Control buttons
        inventory.setItem(45, createReorderControlButton(Material.LIME_CONCRETE, "Save Order", 
                "Click to save the new trade order", NamedTextColor.GREEN));
        inventory.setItem(46, createReorderControlButton(Material.RED_CONCRETE, "Cancel", 
                "Click to cancel and return to editor", NamedTextColor.RED));
        inventory.setItem(47, createReorderControlButton(Material.BOOK, "Help", 
                "Click for reordering help", NamedTextColor.BLUE));
        inventory.setItem(48, createReorderControlButton(Material.ARROW, "Reset Order", 
                "Click to reset to original order", NamedTextColor.YELLOW));

        // Fill separator row
        for (int i = 49; i <= 53; i++) {
            ItemStack separator = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
            ItemMeta meta = separator.getItemMeta();
            if (meta != null) {
                meta.displayName(Component.text(" "));
                separator.setItemMeta(meta);
            }
            inventory.setItem(i, separator);
        }

        // Display trades in current order (starting from slot 9)
        Map<String, Trade> trades = tradeManager.getTrades(guiId);
        int slot = 9;
        
        for (Map.Entry<String, Trade> entry : trades.entrySet()) {
            if (slot >= 45) break; // Don't overlap with control buttons
            
            Trade trade = entry.getValue();
            ItemStack tradeItem = createReorderableTradeItem(trade, tradeManager.getTradePosition(guiId, trade.id()));
            inventory.setItem(slot, tradeItem);
            slot++;
        }
    }

    /**
     * Creates a reorderable trade item for the reorder GUI.
     */
    private ItemStack createReorderableTradeItem(Trade trade, int position) {
        // Create display item using ultra cloning system for perfect preservation
        ItemStack displayItem = ItemUtils.createDisplayClone(trade.output());
        ItemMeta meta = displayItem.getItemMeta();
        if (meta == null) return displayItem;

        meta.displayName(Component.text("Position " + position + ": ", NamedTextColor.GOLD)
                .decorate(TextDecoration.BOLD)
                .append(Component.text(ItemUtils.getDetailedDescription(trade.output()), NamedTextColor.YELLOW)));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text("🔄 Drag to reorder", NamedTextColor.AQUA)
                .decorate(TextDecoration.BOLD));
        lore.add(Component.empty());
        lore.add(Component.text("Current Position: " + position, NamedTextColor.GRAY));
        lore.add(Component.text("Trade ID: " + trade.id(), NamedTextColor.DARK_GRAY));
        lore.add(Component.empty());
        
        lore.add(Component.text("Requirements:", NamedTextColor.GREEN));
        for (ItemStack input : trade.inputs()) {
            if (input != null && input.getType() != Material.AIR) {
                String inputDescription = ItemUtils.getDetailedDescription(input);
                lore.add(Component.text("  • " + input.getAmount() + "x " + inputDescription, NamedTextColor.GRAY));
            }
        }
        
        lore.add(Component.empty());
        lore.add(Component.text("💡 Drag this item to change its position", NamedTextColor.YELLOW));

        meta.lore(lore);
        meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

        displayItem.setItemMeta(meta);
        return displayItem;
    }

    /**
     * Creates control buttons for the reorder GUI.
     */
    private ItemStack createReorderControlButton(Material material, String name, String description, NamedTextColor color) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.displayName(Component.text("⚡ " + name, color)
                .decorate(TextDecoration.BOLD));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text(description, NamedTextColor.GRAY));
        lore.add(Component.empty());
        lore.add(Component.text("▶ Click to activate", NamedTextColor.DARK_GRAY)
                .decorate(TextDecoration.ITALIC));

        meta.lore(lore);
        if (color == NamedTextColor.GREEN || color == NamedTextColor.BLUE) {
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }

        item.setItemMeta(meta);
        return item;
    }

    /**
     * Handles saving the new trade order from the reorder GUI.
     */
    public void handleSaveTradeOrder(Player player, Inventory inventory) {
        String guiId = playerReorderSessions.get(player.getUniqueId());
        if (guiId == null) {
            player.sendMessage(Component.text("No active reorder session", NamedTextColor.RED));
            return;
        }

        List<String> newOrder = new ArrayList<>();
        
        // Extract trade IDs from the inventory in order
        for (int slot = 9; slot < 45; slot++) {
            ItemStack item = inventory.getItem(slot);
            if (item != null && item.getType() != Material.AIR) {
                String tradeId = extractTradeIdFromReorderItem(item);
                if (tradeId != null) {
                    newOrder.add(tradeId);
                }
            }
        }

        if (newOrder.isEmpty()) {
            player.sendMessage(Component.text("❌ No trades found to reorder!", NamedTextColor.RED));
            return;
        }

        boolean success = tradeManager.setTradeOrder(guiId, newOrder);
        
        if (success) {
            player.sendMessage(Component.text("✅ Trade order saved successfully!", NamedTextColor.GREEN)
                    .decorate(TextDecoration.BOLD));
            player.sendMessage(Component.text("  New order has " + newOrder.size() + " trades", NamedTextColor.GRAY));
            
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
            
            // Return to editor
            player.closeInventory();
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) {
                    openEditorGUI(player, guiId);
                }
            }, 1L);
        } else {
            player.sendMessage(Component.text("❌ Failed to save trade order!", NamedTextColor.RED));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
        }
    }

    /**
     * Handles canceling the reorder operation.
     */
    public void handleCancelReorder(Player player) {
        String guiId = playerReorderSessions.get(player.getUniqueId());
        if (guiId == null) {
            player.sendMessage(Component.text("No active reorder session", NamedTextColor.RED));
            return;
        }

        player.closeInventory();
        player.sendMessage(Component.text("❌ Reorder cancelled", NamedTextColor.YELLOW));
        
        // Return to editor
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                openEditorGUI(player, guiId);
            }
        }, 1L);
    }

    /**
     * Handles resetting the trade order to original.
     */
    public void handleResetTradeOrder(Player player, Inventory inventory) {
        String guiId = playerReorderSessions.get(player.getUniqueId());
        if (guiId == null) {
            player.sendMessage(Component.text("No active reorder session", NamedTextColor.RED));
            return;
        }

        // Refresh the GUI with original order
        populateTradeReorderGUI(inventory, guiId);
        
        player.sendMessage(Component.text("🔄 Order reset to original", NamedTextColor.YELLOW));
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.8f, 1.0f);
    }

    /**
     * Handles reorder help.
     */
    public void handleReorderHelp(Player player) {
        player.sendMessage(Component.text("📚 Trade Reorder Help", NamedTextColor.BLUE)
                .decorate(TextDecoration.BOLD));
        player.sendMessage(Component.text(""));
        player.sendMessage(Component.text("How to reorder trades:", NamedTextColor.GOLD));
        player.sendMessage(Component.text("1. ", NamedTextColor.YELLOW)
                .append(Component.text("Drag trade items to new positions", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("2. ", NamedTextColor.YELLOW)
                .append(Component.text("The order here = order in villager GUI", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("3. ", NamedTextColor.YELLOW)
                .append(Component.text("Click 'Save Order' when finished", NamedTextColor.GRAY)));
        player.sendMessage(Component.text(""));
        player.sendMessage(Component.text("Tips:", NamedTextColor.AQUA));
        player.sendMessage(Component.text("• First trade = Top of villager trading GUI", NamedTextColor.GRAY));
        player.sendMessage(Component.text("• Use 'Reset Order' to undo changes", NamedTextColor.GRAY));
        player.sendMessage(Component.text("• Position numbers update automatically", NamedTextColor.GRAY));
        
        player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1.0f, 1.0f);
    }

    /**
     * Extracts trade ID from a reorder item's lore.
     */
    private String extractTradeIdFromReorderItem(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null || meta.lore() == null) {
            return null;
        }

        for (Component line : meta.lore()) {
            String text = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                    .serialize(line);
            if (text.startsWith("Trade ID: ")) {
                return text.substring("Trade ID: ".length()).trim();
            }
        }
        return null;
    }

    /**
     * Refreshes the editor GUI if the player has it open.
     */
    public void refreshEditorGUIIfOpen(Player player, String guiId) {
        if (playerEditorSessions.containsKey(player.getUniqueId()) && 
            playerEditorSessions.get(player.getUniqueId()).equals(guiId)) {
            refreshEditorGUI(player, guiId);
        }
    }

    /**
     * Populates the trade edit GUI with the existing trade data.
     */
    private void populateTradeEditGUI(Inventory inventory, Trade trade) {
        // Clear inventory first
        inventory.clear();

        // Load existing trade data into input/output slots
        List<ItemStack> inputs = trade.inputs();
        if (inputs.size() >= 1 && inputs.get(0) != null) {
            inventory.setItem(INPUT_SLOT_1, ItemUtils.createDisplayClone(inputs.get(0)));
        } else {
            inventory.setItem(INPUT_SLOT_1, createEnhancedInputSlot(1));
        }

        if (inputs.size() >= 2 && inputs.get(1) != null) {
            inventory.setItem(INPUT_SLOT_2, inputs.get(1).clone());
        } else {
            inventory.setItem(INPUT_SLOT_2, createEnhancedInputSlot(2));
        }

        // Load output item
        if (trade.output() != null) {
            inventory.setItem(OUTPUT_SLOT, trade.output().clone());
        } else {
            inventory.setItem(OUTPUT_SLOT, createEnhancedOutputSlot());
        }

        // Fill the rest of first row with decorative separators
        fillSeparatorRow(inventory, 3, 8);

        // Second row: Enhanced control buttons for editing
        inventory.setItem(SAVE_BUTTON_SLOT, createEnhancedControlButton(Material.LIME_CONCRETE,
                "Update Trade", "Click to save changes to this trade", NamedTextColor.GREEN));
        inventory.setItem(CLEAR_BUTTON_SLOT, createEnhancedControlButton(Material.YELLOW_CONCRETE,
                "Reset Trade", "Click to reset trade to original state", NamedTextColor.YELLOW));
        inventory.setItem(HELP_BUTTON_SLOT, createEnhancedControlButton(Material.BOOK,
                "Help Guide", "Click for trade editing tutorial", NamedTextColor.BLUE));
        inventory.setItem(PREVIEW_BUTTON_SLOT, createEnhancedControlButton(Material.SPYGLASS,
                "Preview Changes", "Click to preview how this trade will look", NamedTextColor.YELLOW));
        inventory.setItem(CANCEL_BUTTON_SLOT, createEnhancedControlButton(Material.BARRIER,
                "Cancel Edit", "Click to cancel editing and return", NamedTextColor.RED));
        inventory.setItem(DELETE_BUTTON_SLOT, createEnhancedControlButton(Material.TNT,
                "Delete Trade", "Click to permanently delete this trade", NamedTextColor.DARK_RED));

        // Enhanced separator row with pattern
        fillSeparatorRow(inventory, 15, 17);

        // Information section
        inventory.setItem(TRADES_HEADER_SLOT, createTradeInfoHeader(trade));

        // Fill remaining slots with info about the trade
        displayTradeEditInfo(inventory, trade, TRADES_START_SLOT);
    }

    /**
     * Creates a trade information header for the edit GUI.
     */
    private ItemStack createTradeInfoHeader(Trade trade) {
        ItemStack item = new ItemStack(Material.KNOWLEDGE_BOOK);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.displayName(Component.text("✦ Trade Information", NamedTextColor.LIGHT_PURPLE)
                .decorate(TextDecoration.BOLD));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text("Trade ID: " + trade.id(), NamedTextColor.GRAY));
        lore.add(Component.text("Input Items: " + trade.inputs().size(), NamedTextColor.GRAY));
        lore.add(Component.text("Output Item: " + ItemUtils.getDisplayName(trade.output()), NamedTextColor.GRAY));
        lore.add(Component.empty());
        lore.add(Component.text("Edit the items above and click Update Trade", NamedTextColor.AQUA));
        lore.add(Component.empty());

        meta.lore(lore);
        meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

        item.setItemMeta(meta);
        return item;
    }

    /**
     * Displays trade edit information in the lower section.
     */
    private void displayTradeEditInfo(Inventory inventory, Trade trade, int startSlot) {
        int slot = startSlot;

        // Current inputs info
        for (int i = 0; i < trade.inputs().size() && slot < inventory.getSize(); i++) {
            ItemStack input = trade.inputs().get(i);
            ItemStack infoItem = createTradeItemInfo(input, "Input " + (i + 1), NamedTextColor.GREEN);
            inventory.setItem(slot++, infoItem);
        }

        // Current output info
        if (slot < inventory.getSize()) {
            ItemStack outputInfo = createTradeItemInfo(trade.output(), "Output", NamedTextColor.GOLD);
            inventory.setItem(slot++, outputInfo);
        }

        // Fill remaining slots with decorative items
        while (slot < inventory.getSize()) {
            ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
            ItemMeta meta = filler.getItemMeta();
            if (meta != null) {
                meta.displayName(Component.text(" "));
                filler.setItemMeta(meta);
            }
            inventory.setItem(slot++, filler);
        }
    }

    /**
     * Creates an information item for trade editing.
     */
    private ItemStack createTradeItemInfo(ItemStack item, String type, NamedTextColor color) {
        ItemStack infoItem = new ItemStack(Material.PAPER);
        ItemMeta meta = infoItem.getItemMeta();
        if (meta == null) return infoItem;

        meta.displayName(Component.text("📋 " + type + " Info", color)
                .decorate(TextDecoration.BOLD));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        if (item != null) {
            lore.add(Component.text("Item: " + ItemUtils.getDetailedDescription(item), NamedTextColor.GRAY));
            lore.add(Component.text("Amount: " + item.getAmount(), NamedTextColor.GRAY));
            if (ItemUtils.hasCustomNBT(item)) {
                lore.add(Component.text("⚡ Has custom NBT data", NamedTextColor.AQUA));
            }
        } else {
            lore.add(Component.text("No item set", NamedTextColor.DARK_GRAY));
        }
        lore.add(Component.empty());

        meta.lore(lore);
        item.setItemMeta(meta);
        return infoItem;
    }

    /**
     * Creates a merchant recipe from a trade with proper error handling and enhanced NBT preservation.
     * Now ensures that ProItems and other custom NBT items work correctly in villager trading.
     */
    private MerchantRecipe createMerchantRecipe(Trade trade) {
        try {
            List<ItemStack> inputs = trade.inputs();
            ItemStack output = trade.output();

            if (inputs.isEmpty() || output == null) {
                plugin.getLogger().warning("Invalid trade data for trade: " + trade.id());
                return null;
            }

            // Create a copy of the output to avoid modifying the original
            // Ensure NBT preservation for ProItems and custom items
            ItemStack modifiedOutput = ItemUtils.createTradingSafeCopy(output);

            // If there are more than 2 inputs, add them to the output lore
            if (inputs.size() > 2) {
                ItemMeta meta = modifiedOutput.getItemMeta();
                if (meta != null) {
                    List<Component> lore = meta.lore() != null ? new ArrayList<>(meta.lore()) : new ArrayList<>();
                    lore.add(Component.text("Additional requirements:", NamedTextColor.GRAY));

                    for (int i = 2; i < inputs.size(); i++) {
                        ItemStack input = inputs.get(i);
                        if (input != null && input.getType() != Material.AIR) {
                            String displayName = ItemUtils.getDetailedDescription(input);
                            lore.add(Component.text("- " + input.getAmount() + "x " + displayName, NamedTextColor.YELLOW));
                        }
                    }

                    meta.lore(lore);
                    modifiedOutput.setItemMeta(meta);
                }
            }

            // Create the merchant recipe
            MerchantRecipe recipe = new MerchantRecipe(modifiedOutput, Integer.MAX_VALUE);

            // Set ingredients (up to 2 ingredients supported by villager trading)
            // Create trading-safe copies of ingredients to preserve NBT data
            if (inputs.size() >= 1 && inputs.get(0) != null && inputs.get(0).getType() != Material.AIR) {
                ItemStack safeInput1 = ItemUtils.createTradingSafeCopy(inputs.get(0));
                recipe.addIngredient(safeInput1);
                
                // Log ProItems information
                if (ItemUtils.isProItem(safeInput1)) {
                    String proItemId = ItemUtils.getProItemId(safeInput1);
                    plugin.getLogger().fine("Trade ingredient 1 is ProItem: " + proItemId);
                }
            }
            
            if (inputs.size() >= 2 && inputs.get(1) != null && inputs.get(1).getType() != Material.AIR) {
                ItemStack safeInput2 = ItemUtils.createTradingSafeCopy(inputs.get(1));
                recipe.addIngredient(safeInput2);
                
                // Log ProItems information
                if (ItemUtils.isProItem(safeInput2)) {
                    String proItemId = ItemUtils.getProItemId(safeInput2);
                    plugin.getLogger().fine("Trade ingredient 2 is ProItem: " + proItemId);
                }
            }

            // Log output ProItems information
            if (ItemUtils.isProItem(modifiedOutput)) {
                String proItemId = ItemUtils.getProItemId(modifiedOutput);
                plugin.getLogger().fine("Trade output is ProItem: " + proItemId);
            }

            return recipe;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to create merchant recipe for trade: " + trade.id() + " - " + e.getMessage());
            return null;
        }
    }

    /**
     * Populates the enhanced editor GUI with modern visual design (2 inputs + 1 output) and NBT support.
     */
    private void populateEnhancedEditorGUI(Inventory inventory, String guiId) {
        // Clear inventory first
        inventory.clear();

        // Create enhanced input slots (0-1) with green slime balls
        inventory.setItem(INPUT_SLOT_1, createEnhancedInputSlot(1));
        inventory.setItem(INPUT_SLOT_2, createEnhancedInputSlot(2));

        // Create enhanced output slot (2) with emerald
        inventory.setItem(OUTPUT_SLOT, createEnhancedOutputSlot());

        // Fill the rest of first row with decorative separators
        fillSeparatorRow(inventory, 3, 8);

        // Second row: Enhanced control buttons
        inventory.setItem(SAVE_BUTTON_SLOT, createEnhancedControlButton(Material.LIME_CONCRETE,
                "Save Trade", "Click to save the current trade configuration with NBT data", NamedTextColor.GREEN));
        inventory.setItem(CLEAR_BUTTON_SLOT, createEnhancedControlButton(Material.RED_CONCRETE,
                "Clear All", "Click to clear all input and output slots", NamedTextColor.RED));
        inventory.setItem(HELP_BUTTON_SLOT, createEnhancedControlButton(Material.BOOK,
                "Help Guide", "Click for NBT-enabled trade creation tutorial", NamedTextColor.BLUE));
        inventory.setItem(PREVIEW_BUTTON_SLOT, createEnhancedControlButton(Material.SPYGLASS,
                "Preview Trade", "Click to preview how this trade will look with NBT data", NamedTextColor.YELLOW));

        // Add reorder button
        inventory.setItem(15, createEnhancedControlButton(Material.COMPASS,
                "Reorder Trades", "Click to change the order of existing trades", NamedTextColor.DARK_PURPLE));

        // Enhanced separator row with pattern
        fillSeparatorRow(inventory, 13, 14);
        fillSeparatorRow(inventory, 16, 17);

        // Enhanced existing trades section header
        inventory.setItem(TRADES_HEADER_SLOT, createSectionHeader("Existing Trades (NBT Enabled)",
                "View and manage your current trades with full NBT support"));

        // Display existing trades with enhanced visuals (starting from slot 19)
        displayEnhancedExistingTrades(inventory, guiId, TRADES_START_SLOT);
    }

    /**
     * Creates an enhanced input slot with green slime ball design and NBT information.
     */
    public ItemStack createEnhancedInputSlot(int slotNumber) {
        ItemStack item = new ItemStack(Material.SLIME_BALL);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.displayName(Component.text("Input Slot " + slotNumber, NamedTextColor.GREEN)
                .decorate(TextDecoration.BOLD));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text("• Place items here as trade inputs", NamedTextColor.GRAY));
        lore.add(Component.text("• Supports multiple item types", NamedTextColor.GRAY));
        lore.add(Component.text("• Drag & drop to organize", NamedTextColor.GRAY));
        lore.add(Component.text("• NBT data is fully preserved", NamedTextColor.AQUA));
        lore.add(Component.empty());
        lore.add(Component.text("Status: ", NamedTextColor.AQUA)
                .append(Component.text("Empty", NamedTextColor.DARK_GRAY)));

        meta.lore(lore);
        meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

        item.setItemMeta(meta);
        return item;
    }

    /**
     * Creates an enhanced output slot with emerald design and NBT information.
     */
    public ItemStack createEnhancedOutputSlot() {
        ItemStack item = new ItemStack(Material.EMERALD);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.displayName(Component.text("Output Slot", NamedTextColor.GOLD)
                .decorate(TextDecoration.BOLD));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text("• Place the reward item here", NamedTextColor.GRAY));
        lore.add(Component.text("• This is what players will receive", NamedTextColor.GRAY));
        lore.add(Component.text("• Only one item type allowed", NamedTextColor.GRAY));
        lore.add(Component.text("• NBT data is fully preserved", NamedTextColor.AQUA));
        lore.add(Component.empty());
        lore.add(Component.text("Status: ", NamedTextColor.AQUA)
                .append(Component.text("Empty", NamedTextColor.DARK_GRAY)));

        meta.lore(lore);
        meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

        item.setItemMeta(meta);
        return item;
    }

    /**
     * Creates enhanced control buttons with proper styling and NBT information.
     */
    private ItemStack createEnhancedControlButton(Material material, String name,
                                                  String description, NamedTextColor color) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.displayName(Component.text("⚡ " + name, color)
                .decorate(TextDecoration.BOLD));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text(description, NamedTextColor.GRAY));
        lore.add(Component.empty());
        lore.add(Component.text("▶ Click to activate", NamedTextColor.DARK_GRAY)
                .decorate(TextDecoration.ITALIC));

        meta.lore(lore);
        if (color == NamedTextColor.GREEN || color == NamedTextColor.BLUE || color == NamedTextColor.DARK_PURPLE) {
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }

        item.setItemMeta(meta);
        return item;
    }

    /**
     * Fills the separator row with a decorative pattern.
     */
    private void fillSeparatorRow(Inventory inventory, int startSlot, int endSlot) {
        for (int i = startSlot; i <= endSlot; i++) {
            if (i >= 0 && i < inventory.getSize()) {
                Material material = (i % 2 == 0) ? Material.GRAY_STAINED_GLASS_PANE : Material.LIGHT_GRAY_STAINED_GLASS_PANE;
                ItemStack separator = new ItemStack(material);
                ItemMeta meta = separator.getItemMeta();
                if (meta != null) {
                    meta.displayName(Component.text(" "));
                    separator.setItemMeta(meta);
                }
                inventory.setItem(i, separator);
            }
        }
    }

    /**
     * Creates a section header item with NBT information.
     */
    private ItemStack createSectionHeader(String title, String description) {
        ItemStack item = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.displayName(Component.text("✦ " + title, NamedTextColor.LIGHT_PURPLE)
                .decorate(TextDecoration.BOLD));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text(description, NamedTextColor.GRAY));
        lore.add(Component.empty());

        meta.lore(lore);
        meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

        item.setItemMeta(meta);
        return item;
    }

    /**
     * Displays existing trades with enhanced visuals and NBT information in their defined order.
     */
    private void displayEnhancedExistingTrades(Inventory inventory, String guiId, int startSlot) {
        Map<String, Trade> trades = tradeManager.getTrades(guiId); // This returns trades in order
        int slot = startSlot;

        for (Map.Entry<String, Trade> entry : trades.entrySet()) {
            if (slot >= inventory.getSize()) {
                break;
            }

            Trade trade = entry.getValue();
            int position = tradeManager.getTradePosition(guiId, trade.id());
            ItemStack displayItem = createEnhancedTradeDisplay(trade, position);

            inventory.setItem(slot, displayItem);
            slot++;
        }

        // Fill remaining slots with "Add Trade" placeholders
        while (slot < inventory.getSize()) {
            ItemStack addTradeSlot = createAddTradeSlot();
            inventory.setItem(slot, addTradeSlot);
            slot++;
        }
    }

    /**
     * Creates an enhanced trade display item with proper trade ID in lore, NBT information, and position.
     */
    private ItemStack createEnhancedTradeDisplay(Trade trade, int position) {
        ItemStack displayItem = trade.output().clone();
        ItemMeta meta = displayItem.getItemMeta();
        if (meta == null) return displayItem;

        meta.displayName(Component.text("Trade #" + position + ": ", NamedTextColor.GOLD)
                .decorate(TextDecoration.BOLD)
                .append(Component.text(ItemUtils.getDetailedDescription(trade.output()), NamedTextColor.YELLOW)));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text("Position: " + position, NamedTextColor.AQUA));
        lore.add(Component.empty());
        lore.add(Component.text("Requirements:", NamedTextColor.AQUA)
                .decorate(TextDecoration.BOLD));

        for (ItemStack input : trade.inputs()) {
            if (input != null && input.getType() != Material.AIR) {
                String inputDescription = ItemUtils.getDetailedDescription(input);
                lore.add(Component.text("  • " + input.getAmount() + "x " + inputDescription, NamedTextColor.GREEN));
                
                if (ItemUtils.hasCustomNBT(input)) {
                    lore.add(Component.text("    ⚡ Has custom NBT data", NamedTextColor.DARK_AQUA));
                }
            }
        }

        lore.add(Component.empty());
        lore.add(Component.text("Reward:", NamedTextColor.GOLD)
                .decorate(TextDecoration.BOLD));
        String outputDescription = ItemUtils.getDetailedDescription(trade.output());
        lore.add(Component.text("  • " + trade.output().getAmount() + "x " + outputDescription, NamedTextColor.YELLOW));
        
        if (ItemUtils.hasCustomNBT(trade.output())) {
            lore.add(Component.text("    ⚡ Has custom NBT data", NamedTextColor.DARK_AQUA));
        }

        lore.add(Component.empty());
        // Make sure the trade ID is clearly formatted and easy to extract
        lore.add(Component.text("ID: " + trade.id(), NamedTextColor.DARK_GRAY));
        lore.add(Component.empty());
        lore.add(Component.text("⚠ Right-click to delete", NamedTextColor.RED)
                .decorate(TextDecoration.ITALIC));
        lore.add(Component.text("✏ Middle-click to edit", NamedTextColor.BLUE)
                .decorate(TextDecoration.ITALIC));

        meta.lore(lore);
        meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

        displayItem.setItemMeta(meta);
        return displayItem;
    }

    /**
     * Creates an "Add Trade" placeholder slot with NBT information.
     */
    private ItemStack createAddTradeSlot() {
        ItemStack item = new ItemStack(Material.LIME_STAINED_GLASS);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.displayName(Component.text("+ Add New Trade", NamedTextColor.GREEN)
                .decorate(TextDecoration.BOLD));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text("Create a new trade by:", NamedTextColor.GRAY));
        lore.add(Component.text("1. Adding items to input slots", NamedTextColor.GRAY));
        lore.add(Component.text("2. Adding reward to output slot", NamedTextColor.GRAY));
        lore.add(Component.text("3. Clicking the Save button", NamedTextColor.GRAY));
        lore.add(Component.empty());
        lore.add(Component.text("⚡ NBT data is fully preserved!", NamedTextColor.AQUA));
        lore.add(Component.empty());

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Handles saving a trade from the enhanced editor GUI (2 inputs max) with NBT preservation.
     */
    public void handleTradeSave(Player player, Inventory inventory) {
        String guiId = playerEditorSessions.get(player.getUniqueId());
        if (guiId == null) {
            player.sendMessage(Component.text("No active editor session", NamedTextColor.RED));
            return;
        }

        // Validate inventory size
        if (inventory.getSize() < 3) {
            player.sendMessage(Component.text("Invalid inventory size", NamedTextColor.RED));
            return;
        }

        // Collect input items (slots 0-1 only) with NBT preservation
        List<ItemStack> inputs = new ArrayList<>();

        // Check input slot 1
        ItemStack input1 = inventory.getItem(INPUT_SLOT_1);
        if (input1 != null && input1.getType() != Material.SLIME_BALL && input1.getType() != Material.AIR) {
            inputs.add(ItemUtils.createTradingSafeCopy(input1)); // Create trading-safe copy
        }

        // Check input slot 2
        ItemStack input2 = inventory.getItem(INPUT_SLOT_2);
        if (input2 != null && input2.getType() != Material.SLIME_BALL && input2.getType() != Material.AIR) {
            inputs.add(ItemUtils.createTradingSafeCopy(input2)); // Create trading-safe copy
        }

        // Get output item (slot 2) with NBT preservation
        ItemStack outputItem = inventory.getItem(OUTPUT_SLOT);
        if (outputItem == null || outputItem.getType() == Material.EMERALD || outputItem.getType() == Material.AIR) {
            player.sendMessage(Component.text("⚠ No output item specified!", NamedTextColor.RED)
                    .append(Component.text(" Please place a reward item in the output slot.", NamedTextColor.GRAY)));
            return;
        }

        if (inputs.isEmpty()) {
            player.sendMessage(Component.text("⚠ No input items specified!", NamedTextColor.RED)
                    .append(Component.text(" Please place at least one input item.", NamedTextColor.GRAY)));
            return;
        }

        // Create trade with unique ID and enhanced NBT preservation
        String tradeId = "trade_" + System.currentTimeMillis();
        Trade trade = Trade.of(tradeId, inputs, ItemUtils.createTradingSafeCopy(outputItem)); // Create trading-safe copy

        try {
            tradeManager.addTrade(guiId, trade);

            // Clear input/output slots and restore placeholders
            inventory.setItem(INPUT_SLOT_1, createEnhancedInputSlot(1));
            inventory.setItem(INPUT_SLOT_2, createEnhancedInputSlot(2));
            inventory.setItem(OUTPUT_SLOT, createEnhancedOutputSlot());

            // Refresh the existing trades display
            displayEnhancedExistingTrades(inventory, guiId, TRADES_START_SLOT);

            // Success message with enhanced formatting and NBT information
            player.sendMessage(Component.text("✅ Trade saved successfully with NBT data!", NamedTextColor.GREEN)
                    .decorate(TextDecoration.BOLD));
            player.sendMessage(Component.text("  Trade ID: ", NamedTextColor.GRAY)
                    .append(Component.text(tradeId, NamedTextColor.GOLD)));
            
            // Log NBT information
            boolean hasNBT = false;
            for (ItemStack input : inputs) {
                if (ItemUtils.hasCustomNBT(input)) {
                    hasNBT = true;
                    break;
                }
            }
            if (!hasNBT && ItemUtils.hasCustomNBT(outputItem)) {
                hasNBT = true;
            }
            
            if (hasNBT) {
                player.sendMessage(Component.text("  ⚡ NBT data preserved!", NamedTextColor.AQUA));
            }

        } catch (Exception e) {
            player.sendMessage(Component.text("❌ Failed to save trade: " + e.getMessage(), NamedTextColor.RED));
            plugin.getLogger().severe("Failed to save trade with NBT data: " + e.getMessage());
        }
    }

    /**
     * Handles updating a trade from the trade edit GUI.
     */
    public void handleTradeUpdate(Player player, Inventory inventory) {
        String sessionData = playerTradeEditSessions.get(player.getUniqueId());
        if (sessionData == null) {
            player.sendMessage(Component.text("No active trade edit session", NamedTextColor.RED));
            return;
        }

        String[] parts = sessionData.split(":");
        if (parts.length != 2) {
            player.sendMessage(Component.text("Invalid edit session data", NamedTextColor.RED));
            return;
        }

        String guiId = parts[0];
        String tradeId = parts[1];

        // Validate inventory size
        if (inventory.getSize() < 3) {
            player.sendMessage(Component.text("Invalid inventory size", NamedTextColor.RED));
            return;
        }

        // Collect input items (slots 0-1 only) with NBT preservation
        List<ItemStack> inputs = new ArrayList<>();

        // Check input slot 1
        ItemStack input1 = inventory.getItem(INPUT_SLOT_1);
        if (input1 != null && input1.getType() != Material.SLIME_BALL && input1.getType() != Material.AIR) {
            inputs.add(ItemUtils.createTradingSafeCopy(input1)); // Create trading-safe copy
        }

        // Check input slot 2
        ItemStack input2 = inventory.getItem(INPUT_SLOT_2);
        if (input2 != null && input2.getType() != Material.SLIME_BALL && input2.getType() != Material.AIR) {
            inputs.add(ItemUtils.createTradingSafeCopy(input2)); // Create trading-safe copy
        }

        // Get output item (slot 2) with NBT preservation
        ItemStack outputItem = inventory.getItem(OUTPUT_SLOT);
        if (outputItem == null || outputItem.getType() == Material.EMERALD || outputItem.getType() == Material.AIR) {
            player.sendMessage(Component.text("⚠ No output item specified!", NamedTextColor.RED)
                    .append(Component.text(" Please place a reward item in the output slot.", NamedTextColor.GRAY)));
            return;
        }

        if (inputs.isEmpty()) {
            player.sendMessage(Component.text("⚠ No input items specified!", NamedTextColor.RED)
                    .append(Component.text(" Please place at least one input item.", NamedTextColor.GRAY)));
            return;
        }

        // Create updated trade with same ID and enhanced NBT preservation
        Trade updatedTrade = Trade.of(tradeId, inputs, ItemUtils.createTradingSafeCopy(outputItem)); // Create trading-safe copy

        try {
            // Remove old trade and add updated one
            tradeManager.removeTrade(guiId, tradeId);
            tradeManager.addTrade(guiId, updatedTrade);

            // Success message with enhanced formatting and NBT information
            player.sendMessage(Component.text("✅ Trade updated successfully with NBT data!", NamedTextColor.GREEN)
                    .decorate(TextDecoration.BOLD));
            player.sendMessage(Component.text("  Trade ID: ", NamedTextColor.GRAY)
                    .append(Component.text(tradeId, NamedTextColor.GOLD)));
            
            // Log NBT information
            boolean hasNBT = false;
            for (ItemStack input : inputs) {
                if (ItemUtils.hasCustomNBT(input)) {
                    hasNBT = true;
                    break;
                }
            }
            if (!hasNBT && ItemUtils.hasCustomNBT(outputItem)) {
                hasNBT = true;
            }
            
            if (hasNBT) {
                player.sendMessage(Component.text("  ⚡ NBT data preserved!", NamedTextColor.AQUA));
            }

            // Close the edit GUI and return to main editor
            player.closeInventory();
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) {
                    openEditorGUI(player, guiId);
                }
            }, 1L);

        } catch (Exception e) {
            player.sendMessage(Component.text("❌ Failed to update trade: " + e.getMessage(), NamedTextColor.RED));
            plugin.getLogger().severe("Failed to update trade with NBT data: " + e.getMessage());
        }
    }

    /**
     * Handles resetting a trade to its original state.
     */
    public void handleTradeReset(Player player, Inventory inventory) {
        String sessionData = playerTradeEditSessions.get(player.getUniqueId());
        if (sessionData == null) {
            player.sendMessage(Component.text("No active trade edit session", NamedTextColor.RED));
            return;
        }

        String[] parts = sessionData.split(":");
        if (parts.length != 2) {
            player.sendMessage(Component.text("Invalid edit session data", NamedTextColor.RED));
            return;
        }

        String guiId = parts[0];
        String tradeId = parts[1];

        Trade originalTrade = tradeManager.getTrade(guiId, tradeId);
        if (originalTrade == null) {
            player.sendMessage(Component.text("Original trade not found", NamedTextColor.RED));
            return;
        }

        // Reset the GUI to original trade data
        populateTradeEditGUI(inventory, originalTrade);
        
        player.sendMessage(Component.text("🔄 Trade reset to original state", NamedTextColor.YELLOW));
    }

    /**
     * Handles canceling trade edit and returning to main editor.
     */
    public void handleTradeEditCancel(Player player) {
        String sessionData = playerTradeEditSessions.get(player.getUniqueId());
        if (sessionData == null) {
            player.sendMessage(Component.text("No active trade edit session", NamedTextColor.RED));
            return;
        }

        String[] parts = sessionData.split(":");
        if (parts.length != 2) {
            player.sendMessage(Component.text("Invalid edit session data", NamedTextColor.RED));
            return;
        }

        String guiId = parts[0];

        player.closeInventory();
        player.sendMessage(Component.text("❌ Trade editing cancelled", NamedTextColor.YELLOW));
        
        // Return to main editor
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                openEditorGUI(player, guiId);
            }
        }, 1L);
    }

    /**
     * Handles deleting a trade from the edit GUI.
     */
    public void handleTradeEditDelete(Player player) {
        String sessionData = playerTradeEditSessions.get(player.getUniqueId());
        if (sessionData == null) {
            player.sendMessage(Component.text("No active trade edit session", NamedTextColor.RED));
            return;
        }

        String[] parts = sessionData.split(":");
        if (parts.length != 2) {
            player.sendMessage(Component.text("Invalid edit session data", NamedTextColor.RED));
            return;
        }

        String guiId = parts[0];
        String tradeId = parts[1];

        tradeManager.removeTrade(guiId, tradeId);
        
        player.closeInventory();
        player.sendMessage(Component.text("🗑 Trade deleted: ", NamedTextColor.RED)
                .append(Component.text(tradeId, NamedTextColor.GRAY)));
        
        // Return to main editor
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                openEditorGUI(player, guiId);
            }
        }, 1L);
    }

    /**
     * Handles clearing the enhanced editor GUI input/output slots (2 inputs max).
     */
    public void handleTradeClear(Player player, Inventory inventory) {
        // Validate inventory size
        if (inventory.getSize() < 3) {
            player.sendMessage(Component.text("Invalid inventory size", NamedTextColor.RED));
            return;
        }

        // Clear input/output slots and restore enhanced placeholders
        inventory.setItem(INPUT_SLOT_1, createEnhancedInputSlot(1));
        inventory.setItem(INPUT_SLOT_2, createEnhancedInputSlot(2));
        inventory.setItem(OUTPUT_SLOT, createEnhancedOutputSlot());

        player.sendMessage(Component.text("🧹 Editor cleared", NamedTextColor.YELLOW)
                .append(Component.text(" - All slots have been reset.", NamedTextColor.GRAY)));
    }

    /**
     * Handles the help button click with NBT information.
     */
    public void handleHelpButton(Player player) {
        player.sendMessage(Component.text("📚 NBT-Enabled Trade Editor Help", NamedTextColor.BLUE)
                .decorate(TextDecoration.BOLD));
        player.sendMessage(Component.text(""));
        player.sendMessage(Component.text("How to create trades:", NamedTextColor.GOLD));
        player.sendMessage(Component.text("1. ", NamedTextColor.YELLOW)
                .append(Component.text("Place items in 2 green input slots (what players give)", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("2. ", NamedTextColor.YELLOW)
                .append(Component.text("Place reward item in gold output slot (what players get)", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("3. ", NamedTextColor.YELLOW)
                .append(Component.text("Click the Save button to create the trade", NamedTextColor.GRAY)));
        player.sendMessage(Component.text(""));
        player.sendMessage(Component.text("How to edit trades:", NamedTextColor.GOLD));
        player.sendMessage(Component.text("• Middle-click existing trades to edit them", NamedTextColor.GRAY));
        player.sendMessage(Component.text("• Use /protrades edittrade <gui_id> <trade_id>", NamedTextColor.GRAY));
        player.sendMessage(Component.text("• Edit GUI allows updating, resetting, or deleting trades", NamedTextColor.GRAY));
        player.sendMessage(Component.text(""));
        player.sendMessage(Component.text("How to reorder trades:", NamedTextColor.GOLD));
        player.sendMessage(Component.text("• Click 'Reorder Trades' button in editor", NamedTextColor.GRAY));
        player.sendMessage(Component.text("• Use /protrades reorder <gui_id>", NamedTextColor.GRAY));
        player.sendMessage(Component.text("• Drag trades to new positions", NamedTextColor.GRAY));
        player.sendMessage(Component.text("• Use /protrades movetrade <gui_id> <trade_id> <position>", NamedTextColor.GRAY));
        player.sendMessage(Component.text(""));
        player.sendMessage(Component.text("NBT Support:", NamedTextColor.AQUA));
        player.sendMessage(Component.text("• Custom names, lore, and enchantments are preserved", NamedTextColor.GRAY));
        player.sendMessage(Component.text("• Items with NBT data are marked with ⚡ in trade displays", NamedTextColor.GRAY));
        player.sendMessage(Component.text("• All item properties are exactly matched during trades", NamedTextColor.GRAY));
        player.sendMessage(Component.text(""));
        player.sendMessage(Component.text("Tips:", NamedTextColor.AQUA));
        player.sendMessage(Component.text("• You can use up to 2 different input items (Minecraft limit)", NamedTextColor.GRAY));
        player.sendMessage(Component.text("• Right-click existing trades to delete them", NamedTextColor.GRAY));
        player.sendMessage(Component.text("• Use the Clear button to reset all slots", NamedTextColor.GRAY));
        player.sendMessage(Component.text("• Trade order affects villager GUI display order", NamedTextColor.GRAY));
    }

    /**
     * Handles the preview button click (2 inputs max) with NBT information.
     */
    public void handlePreviewButton(Player player, Inventory inventory) {
        // Validate inventory size
        if (inventory.getSize() < 3) {
            player.sendMessage(Component.text("Invalid inventory size", NamedTextColor.RED));
            return;
        }

        // Collect current trade setup
        List<ItemStack> inputs = new ArrayList<>();

        ItemStack input1 = inventory.getItem(INPUT_SLOT_1);
        if (input1 != null && input1.getType() != Material.SLIME_BALL && input1.getType() != Material.AIR) {
            inputs.add(ItemUtils.createTradingSafeCopy(input1)); // Create trading-safe copy
        }

        ItemStack input2 = inventory.getItem(INPUT_SLOT_2);
        if (input2 != null && input2.getType() != Material.SLIME_BALL && input2.getType() != Material.AIR) {
            inputs.add(ItemUtils.createTradingSafeCopy(input2)); // Create trading-safe copy
        }

        ItemStack outputItem = inventory.getItem(OUTPUT_SLOT);

        if (inputs.isEmpty() || outputItem == null || outputItem.getType() == Material.EMERALD || outputItem.getType() == Material.AIR) {
            player.sendMessage(Component.text("⚠ Preview unavailable", NamedTextColor.RED)
                    .append(Component.text(" - Set up inputs and output first!", NamedTextColor.GRAY)));
            return;
        }

        // Show preview with NBT information
        player.sendMessage(Component.text("🔍 NBT-Enabled Trade Preview", NamedTextColor.YELLOW)
                .decorate(TextDecoration.BOLD));
        player.sendMessage(Component.text(""));
        player.sendMessage(Component.text("Players will need:", NamedTextColor.AQUA));

        for (ItemStack input : inputs) {
            String inputDescription = ItemUtils.getDetailedDescription(input);
            player.sendMessage(Component.text("  • " + input.getAmount() + "x " + inputDescription, NamedTextColor.GREEN));
            
            if (ItemUtils.hasCustomNBT(input)) {
                player.sendMessage(Component.text("    ⚡ Has custom NBT data", NamedTextColor.DARK_AQUA));
            }
        }

        player.sendMessage(Component.text(""));
        player.sendMessage(Component.text("Players will receive:", NamedTextColor.GOLD));
        String outputDescription = ItemUtils.getDetailedDescription(outputItem);
        player.sendMessage(Component.text("  • " + outputItem.getAmount() + "x " + outputDescription, NamedTextColor.YELLOW));
        
        if (ItemUtils.hasCustomNBT(outputItem)) {
            player.sendMessage(Component.text("    ⚡ Has custom NBT data", NamedTextColor.DARK_AQUA));
        }
    }

    /**
     * Utility method to check if a slot is a valid input slot.
     */
    public boolean isInputSlot(int slot) {
        return slot == INPUT_SLOT_1 || slot == INPUT_SLOT_2;
    }

    /**
     * Utility method to check if a slot is the output slot.
     */
    public boolean isOutputSlot(int slot) {
        return slot == OUTPUT_SLOT;
    }

    /**
     * Utility method to check if a slot is a control button slot.
     */
    public boolean isControlSlot(int slot) {
        return slot == SAVE_BUTTON_SLOT || slot == CLEAR_BUTTON_SLOT ||
                slot == HELP_BUTTON_SLOT || slot == PREVIEW_BUTTON_SLOT ||
                slot == CANCEL_BUTTON_SLOT || slot == DELETE_BUTTON_SLOT ||
                slot == 15; // Reorder button
    }

    /**
     * Utility method to check if a slot is in the trades area.
     */
    public boolean isTradesAreaSlot(int slot) {
        return slot >= TRADES_START_SLOT;
    }

    /**
     * Gets the GUI ID for a player's current trade session.
     */
    public String getPlayerTradeGUI(UUID playerId) {
        return playerTradeGUIs.get(playerId);
    }

    /**
     * Gets the GUI ID for a player's current editor session.
     */
    public String getPlayerEditorGUI(UUID playerId) {
        return playerEditorSessions.get(playerId);
    }

    /**
     * Gets the session data for a player's current trade edit session.
     */
    public String getPlayerTradeEditSession(UUID playerId) {
        return playerTradeEditSessions.get(playerId);
    }

    /**
     * Gets the GUI ID for a player's current reorder session.
     */
    public String getPlayerReorderSession(UUID playerId) {
        return playerReorderSessions.get(playerId);
    }

    /**
     * Clears a player's trade session.
     */
    public void clearPlayerTradeSession(UUID playerId) {
        playerTradeGUIs.remove(playerId);
    }

    /**
     * Clears a player's editor session.
     */
    public void clearPlayerEditorSession(UUID playerId) {
        playerEditorSessions.remove(playerId);
    }

    /**
     * Clears a player's trade edit session.
     */
    public void clearPlayerTradeEditSession(UUID playerId) {
        playerTradeEditSessions.remove(playerId);
    }

    /**
     * Clears a player's reorder session.
     */
    public void clearPlayerReorderSession(UUID playerId) {
        playerReorderSessions.remove(playerId);
    }

    /**
     * Creates an enhanced trade display item (public method for InventoryClickListener).
     */
    public ItemStack createPublicEnhancedTradeDisplay(Trade trade) {
        int position = 1; // Default position if not found
        return createEnhancedTradeDisplay(trade, position);
    }

    /**
     * Creates an "Add Trade" placeholder slot (public method for InventoryClickListener).
     */
    public ItemStack createPublicAddTradeSlot() {
        return createAddTradeSlot();
    }

    /**
     * Refreshes the editor GUI for a player with proper session management and NBT support.
     */
    public void refreshEditorGUI(Player player, String guiId) {
        if (playerEditorSessions.containsKey(player.getUniqueId())) {
            // Check if player still has the inventory open
            if (player.getOpenInventory().getTopInventory().getSize() == 54) {
                // Refresh just the trades area to avoid disrupting player's current work
                displayEnhancedExistingTrades(player.getOpenInventory().getTopInventory(), guiId, TRADES_START_SLOT);
                plugin.getLogger().fine("Refreshed NBT-enabled editor GUI for player: " + player.getName());
            } else {
                // Re-open the editor GUI completely
                openEditorGUI(player, guiId);
            }
        }
    }
}