package org.mindle.protrades.listeners;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.mindle.protrades.ProTrades;
import org.mindle.protrades.managers.GUIManager;
import org.mindle.protrades.managers.TradeManager;
import org.mindle.protrades.models.Trade;
import org.mindle.protrades.utils.ItemUtils;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Enhanced inventory click listener with support for trade editing, full NBT handling, and trade reordering.
 * Handles inventory click events for trade GUIs, editor GUIs, trade edit GUIs, and reorder GUIs.
 */
public class InventoryClickListener implements Listener {

    private final ProTrades plugin;
    private final TradeManager tradeManager;
    private final GUIManager guiManager;

    public InventoryClickListener(ProTrades plugin, TradeManager tradeManager, GUIManager guiManager) {
        this.plugin = plugin;
        this.tradeManager = tradeManager;
        this.guiManager = guiManager;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        UUID playerId = player.getUniqueId();
        String tradeGuiId = guiManager.getPlayerTradeGUI(playerId);
        String editorGuiId = guiManager.getPlayerEditorGUI(playerId);
        String tradeEditSession = guiManager.getPlayerTradeEditSession(playerId);
        String reorderSession = guiManager.getPlayerReorderSession(playerId);

        // Handle merchant trading (villager-style GUI)
        if (tradeGuiId != null && event.getInventory() instanceof MerchantInventory) {
            handleMerchantTradeClick(event, player, tradeGuiId);
        }
        // Handle trade reorder GUI
        else if (reorderSession != null && event.getInventory().equals(player.getOpenInventory().getTopInventory())) {
            handleReorderGUIClick(event, player, reorderSession);
        }
        // Handle trade edit GUI
        else if (tradeEditSession != null && event.getInventory().equals(player.getOpenInventory().getTopInventory())) {
            handleTradeEditGUIClick(event, player, tradeEditSession);
        }
        // Handle editor GUI
        else if (editorGuiId != null && event.getInventory().equals(player.getOpenInventory().getTopInventory())) {
            handleEditorGUIClick(event, player, editorGuiId);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        UUID playerId = player.getUniqueId();
        String editorGuiId = guiManager.getPlayerEditorGUI(playerId);
        String tradeEditSession = guiManager.getPlayerTradeEditSession(playerId);
        String reorderSession = guiManager.getPlayerReorderSession(playerId);

        // Handle editor GUI drag events
        if (editorGuiId != null && event.getInventory().equals(player.getOpenInventory().getTopInventory())) {
            handleEditorGUIDrag(event, player);
        }
        // Handle trade edit GUI drag events
        else if (tradeEditSession != null && event.getInventory().equals(player.getOpenInventory().getTopInventory())) {
            handleEditorGUIDrag(event, player); // Same logic for drag handling
        }
        // Handle reorder GUI drag events
        else if (reorderSession != null && event.getInventory().equals(player.getOpenInventory().getTopInventory())) {
            handleReorderGUIDrag(event, player);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }

        UUID playerId = player.getUniqueId();
        guiManager.clearPlayerTradeSession(playerId);
        guiManager.clearPlayerEditorSession(playerId);
        guiManager.clearPlayerTradeEditSession(playerId);
        guiManager.clearPlayerReorderSession(playerId);
    }

    /**
     * Handles clicks in merchant trading GUIs (villager-style).
     */
    private void handleMerchantTradeClick(InventoryClickEvent event, Player player, String guiId) {
        MerchantInventory merchantInventory = (MerchantInventory) event.getInventory();

        // Check if clicking on result slot (slot 2 in merchant inventory)
        if (event.getSlot() == 2) {
            ItemStack resultItem = event.getCurrentItem();
            if (resultItem != null && resultItem.getType() != Material.AIR) {

                // Check if this trade has additional requirements (3+ inputs)
                if (hasAdditionalRequirements(resultItem)) {
                    // Find the original trade to check all requirements
                    Trade originalTrade = findTradeByResult(guiId, resultItem);
                    if (originalTrade != null && originalTrade.inputs().size() > 2) {

                        // Check if player has ALL required items (including additional ones)
                        if (!ItemUtils.hasRequiredItems(player, originalTrade.inputs())) {
                            event.setCancelled(true);
                            player.sendMessage(Component.text("You don't have all the required items!", NamedTextColor.RED));
                            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                            return;
                        }

                        // Remove additional items (beyond the first 2)
                        List<ItemStack> additionalItems = originalTrade.inputs().subList(2, originalTrade.inputs().size());
                        if (!ItemUtils.removeRequiredItems(player, additionalItems)) {
                            event.setCancelled(true);
                            player.sendMessage(Component.text("Failed to remove required items!", NamedTextColor.RED));
                            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                            return;
                        }
                    }
                }

                // Play success sound
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_YES, 1.0f, 1.0f);
            }
        }
    }

    /**
     * Handles clicks in trade reorder GUIs.
     */
    private void handleReorderGUIClick(InventoryClickEvent event, Player player, String guiId) {
        int slot = event.getSlot();
        ItemStack clickedItem = event.getCurrentItem();

        // Handle different inventory areas
        if (slot == -999) {
            // Clicking outside inventory - allow (for dropping items)
            return;
        }

        // Check if clicking in player's inventory (bottom inventory)
        if (event.getClickedInventory() == player.getInventory()) {
            // Allow normal interaction with player inventory
            return;
        }

        // Handle control buttons
        switch (slot) {
            case 45 -> {
                // Save Order button
                event.setCancelled(true);
                if (clickedItem != null && clickedItem.getType() == Material.LIME_CONCRETE) {
                    player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
                    guiManager.handleSaveTradeOrder(player, event.getInventory());
                }
            }
            case 46 -> {
                // Cancel button
                event.setCancelled(true);
                if (clickedItem != null && clickedItem.getType() == Material.RED_CONCRETE) {
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 0.8f);
                    guiManager.handleCancelReorder(player);
                }
            }
            case 47 -> {
                // Help button
                event.setCancelled(true);
                if (clickedItem != null && clickedItem.getType() == Material.BOOK) {
                    player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1.0f, 1.0f);
                    guiManager.handleReorderHelp(player);
                }
            }
            case 48 -> {
                // Reset Order button
                event.setCancelled(true);
                if (clickedItem != null && clickedItem.getType() == Material.ARROW) {
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.8f, 1.0f);
                    guiManager.handleResetTradeOrder(player, event.getInventory());
                }
            }
            case 4 -> {
                // Header item - prevent interaction
                event.setCancelled(true);
            }
            case 49, 50, 51, 52, 53 -> {
                // Separator items - prevent interaction
                event.setCancelled(true);
            }
            default -> {
                // Trade items area (slots 9-44) - allow dragging for reordering
                if (slot >= 9 && slot < 45) {
                    // Allow dragging of trade items for reordering
                    // The actual reordering logic is handled in the drag event
                    if (clickedItem != null && clickedItem.getType() != Material.AIR) {
                        // This is a trade item - allow picking up for reordering
                        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
                    }
                } else {
                    // Other slots - prevent interaction
                    event.setCancelled(true);
                }
            }
        }
    }

    /**
     * Handles drag events in reorder GUI for trade repositioning.
     */
    private void handleReorderGUIDrag(InventoryDragEvent event, Player player) {
        // Check if dragging to GUI slots
        boolean dragToGUI = false;
        for (int slot : event.getRawSlots()) {
            if (slot >= 0 && slot < event.getInventory().getSize()) {
                dragToGUI = true;
                break;
            }
        }

        if (!dragToGUI) {
            // Only dragging in player inventory - allow
            return;
        }

        // Check if dragging to valid reorder area (slots 9-44)
        boolean validDrag = true;
        for (int slot : event.getRawSlots()) {
            if (slot >= 0 && slot < event.getInventory().getSize()) {
                if (slot < 9 || slot >= 45) {
                    validDrag = false;
                    break;
                }
            }
        }

        if (!validDrag) {
            // Dragging to invalid area - cancel
            event.setCancelled(true);
            player.sendMessage(Component.text("❌ You can only reorder trades in the main area!", NamedTextColor.RED));
            return;
        }

        // Allow the drag - this will reposition the trade items
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
    }

    /**
     * Handles clicks in trade edit GUIs.
     */
    private void handleTradeEditGUIClick(InventoryClickEvent event, Player player, String sessionData) {
        int slot = event.getSlot();
        ItemStack clickedItem = event.getCurrentItem();
        ItemStack cursorItem = event.getCursor();
        ClickType clickType = event.getClick();

        // Handle different inventory areas
        if (slot == -999) {
            // Clicking outside inventory - allow (for dropping items)
            return;
        }

        // Check if clicking in player's inventory (bottom inventory)
        if (event.getClickedInventory() == player.getInventory()) {
            // Allow normal interaction with player inventory
            return;
        }

        // We're in the trade edit GUI inventory (top inventory)
        switch (slot) {
            case 0, 1 -> {
                // Input slots - allow item placement/removal with proper handling
                handleInputSlotClick(event, player, slot, clickedItem, cursorItem, clickType);
            }
            case 2 -> {
                // Output slot - allow item placement/removal with proper handling
                handleOutputSlotClick(event, player, slot, clickedItem, cursorItem, clickType);
            }
            case 3, 4, 5, 6, 7, 8 -> {
                // Decorative separators in first row - prevent interaction
                event.setCancelled(true);
            }
            case 9 -> {
                // Update button (Save in edit mode)
                event.setCancelled(true);
                if (clickedItem != null && clickedItem.getType() == Material.LIME_CONCRETE) {
                    player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
                    guiManager.handleTradeUpdate(player, event.getInventory());
                }
            }
            case 10 -> {
                // Reset button (Clear in edit mode)
                event.setCancelled(true);
                if (clickedItem != null && clickedItem.getType() == Material.YELLOW_CONCRETE) {
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.8f, 1.0f);
                    guiManager.handleTradeReset(player, event.getInventory());
                }
            }
            case 11 -> {
                // Help button
                event.setCancelled(true);
                if (clickedItem != null && clickedItem.getType() == Material.BOOK) {
                    player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1.0f, 1.0f);
                    guiManager.handleHelpButton(player);
                }
            }
            case 12 -> {
                // Preview button
                event.setCancelled(true);
                if (clickedItem != null && clickedItem.getType() == Material.SPYGLASS) {
                    player.playSound(player.getLocation(), Sound.ITEM_SPYGLASS_USE, 1.0f, 1.0f);
                    guiManager.handlePreviewButton(player, event.getInventory());
                }
            }
            case 13 -> {
                // Cancel button
                event.setCancelled(true);
                if (clickedItem != null && clickedItem.getType() == Material.BARRIER) {
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 0.8f);
                    guiManager.handleTradeEditCancel(player);
                }
            }
            case 14 -> {
                // Delete button
                event.setCancelled(true);
                if (clickedItem != null && clickedItem.getType() == Material.TNT) {
                    player.playSound(player.getLocation(), Sound.ENTITY_TNT_PRIMED, 1.0f, 1.0f);
                    guiManager.handleTradeEditDelete(player);
                }
            }
            default -> {
                // Other slots - prevent interaction
                event.setCancelled(true);
            }
        }
    }

    /**
     * Handles clicks in enhanced editor GUIs (2 inputs + 1 output).
     */
    private void handleEditorGUIClick(InventoryClickEvent event, Player player, String guiId) {
        int slot = event.getSlot();
        ItemStack clickedItem = event.getCurrentItem();
        ItemStack cursorItem = event.getCursor();
        ClickType clickType = event.getClick();

        // Handle different inventory areas
        if (slot == -999) {
            // Clicking outside inventory - allow (for dropping items)
            return;
        }

        // Check if clicking in player's inventory (bottom inventory)
        if (event.getClickedInventory() == player.getInventory()) {
            // Allow normal interaction with player inventory
            return;
        }

        // We're in the GUI inventory (top inventory)
        switch (slot) {
            case 0, 1 -> {
                // Input slots - allow item placement/removal with proper handling
                handleInputSlotClick(event, player, slot, clickedItem, cursorItem, clickType);
            }
            case 2 -> {
                // Output slot - allow item placement/removal with proper handling
                handleOutputSlotClick(event, player, slot, clickedItem, cursorItem, clickType);
            }
            case 3, 4, 5, 6, 7, 8 -> {
                // Decorative separators in first row - prevent interaction
                event.setCancelled(true);
            }
            case 9 -> {
                // Save button
                event.setCancelled(true);
                if (clickedItem != null && clickedItem.getType() == Material.LIME_CONCRETE) {
                    player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
                    guiManager.handleTradeSave(player, event.getInventory());
                }
            }
            case 10 -> {
                // Clear button
                event.setCancelled(true);
                if (clickedItem != null && clickedItem.getType() == Material.RED_CONCRETE) {
                    player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.8f, 1.0f);
                    guiManager.handleTradeClear(player, event.getInventory());
                }
            }
            case 11 -> {
                // Help button
                event.setCancelled(true);
                if (clickedItem != null && clickedItem.getType() == Material.BOOK) {
                    player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1.0f, 1.0f);
                    guiManager.handleHelpButton(player);
                }
            }
            case 12 -> {
                // Preview button
                event.setCancelled(true);
                if (clickedItem != null && clickedItem.getType() == Material.SPYGLASS) {
                    player.playSound(player.getLocation(), Sound.ITEM_SPYGLASS_USE, 1.0f, 1.0f);
                    guiManager.handlePreviewButton(player, event.getInventory());
                }
            }
            case 15 -> {
                // Reorder button
                event.setCancelled(true);
                if (clickedItem != null && clickedItem.getType() == Material.COMPASS) {
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.2f);
                    
                    // Close current GUI and open reorder GUI
                    player.closeInventory();
                    plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                        if (player.isOnline()) {
                            guiManager.openTradeReorderGUI(player, guiId);
                        }
                    }, 1L);
                }
            }
            case 13, 14, 16, 17 -> {
                // Separator row - prevent interaction
                event.setCancelled(true);
            }
            case 18 -> {
                // Section header - prevent interaction
                event.setCancelled(true);
            }
            default -> {
                // Existing trades area and add trade slots (slot 19+)
                if (slot >= 19) {
                    event.setCancelled(true);
                    handleTradeAreaClick(event, player, guiId, clickedItem);
                }
            }
        }
    }

    /**
     * Handles clicks on input slots (slots 0-1) with proper item management.
     */
    private void handleInputSlotClick(InventoryClickEvent event, Player player, int slot,
                                      ItemStack clickedItem, ItemStack cursorItem, ClickType clickType) {
        event.setCancelled(true); // Cancel first, then handle manually

        boolean isPlaceholder = clickedItem != null && clickedItem.getType() == Material.SLIME_BALL;
        boolean hasValidItem = clickedItem != null && clickedItem.getType() != Material.AIR && !isPlaceholder;
        boolean hasCursorItem = cursorItem != null && cursorItem.getType() != Material.AIR;

        if (isPlaceholder) {
            // Clicking on placeholder
            if (hasCursorItem) {
                // Place cursor item in slot
                event.getInventory().setItem(slot, cursorItem.clone());
                event.getWhoClicked().setItemOnCursor(null);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            }
        } else if (hasValidItem) {
            // Clicking on real item
            switch (clickType) {
                case LEFT -> {
                    if (hasCursorItem) {
                        // Swap items
                        ItemStack tempItem = clickedItem.clone();
                        event.getInventory().setItem(slot, cursorItem.clone());
                        event.getWhoClicked().setItemOnCursor(tempItem);
                    } else {
                        // Pick up item
                        event.getWhoClicked().setItemOnCursor(clickedItem.clone());
                        event.getInventory().setItem(slot, guiManager.createEnhancedInputSlot(slot + 1));
                    }
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
                }
                case RIGHT -> {
                    if (!hasCursorItem) {
                        // Take half the stack
                        ItemStack halfStack = clickedItem.clone();
                        int halfAmount = Math.max(1, halfStack.getAmount() / 2);
                        halfStack.setAmount(halfAmount);
                        event.getWhoClicked().setItemOnCursor(halfStack);

                        if (clickedItem.getAmount() - halfAmount > 0) {
                            ItemStack remainingStack = clickedItem.clone();
                            remainingStack.setAmount(clickedItem.getAmount() - halfAmount);
                            event.getInventory().setItem(slot, remainingStack);
                        } else {
                            event.getInventory().setItem(slot, guiManager.createEnhancedInputSlot(slot + 1));
                        }
                        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
                    }
                }
                case SHIFT_LEFT, SHIFT_RIGHT -> {
                    // Remove item and replace with placeholder
                    event.getInventory().setItem(slot, guiManager.createEnhancedInputSlot(slot + 1));
                    // Try to add to player inventory
                    if (player.getInventory().firstEmpty() != -1) {
                        player.getInventory().addItem(clickedItem.clone());
                    } else {
                        player.getWorld().dropItem(player.getLocation(), clickedItem.clone());
                    }
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
                }
            }
        }
    }

    /**
     * Handles clicks on output slot (slot 2) with proper item management.
     */
    private void handleOutputSlotClick(InventoryClickEvent event, Player player, int slot,
                                       ItemStack clickedItem, ItemStack cursorItem, ClickType clickType) {
        event.setCancelled(true); // Cancel first, then handle manually

        boolean isPlaceholder = clickedItem != null && clickedItem.getType() == Material.EMERALD;
        boolean hasValidItem = clickedItem != null && clickedItem.getType() != Material.AIR && !isPlaceholder;
        boolean hasCursorItem = cursorItem != null && cursorItem.getType() != Material.AIR;

        if (isPlaceholder) {
            // Clicking on placeholder
            if (hasCursorItem) {
                // Place cursor item in slot
                event.getInventory().setItem(slot, cursorItem.clone());
                event.getWhoClicked().setItemOnCursor(null);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            }
        } else if (hasValidItem) {
            // Clicking on real item
            switch (clickType) {
                case LEFT -> {
                    if (hasCursorItem) {
                        // Swap items
                        ItemStack tempItem = clickedItem.clone();
                        event.getInventory().setItem(slot, cursorItem.clone());
                        event.getWhoClicked().setItemOnCursor(tempItem);
                    } else {
                        // Pick up item
                        event.getWhoClicked().setItemOnCursor(clickedItem.clone());
                        event.getInventory().setItem(slot, guiManager.createEnhancedOutputSlot());
                    }
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
                }
                case RIGHT -> {
                    if (!hasCursorItem) {
                        // Take half the stack
                        ItemStack halfStack = clickedItem.clone();
                        int halfAmount = Math.max(1, halfStack.getAmount() / 2);
                        halfStack.setAmount(halfAmount);
                        event.getWhoClicked().setItemOnCursor(halfStack);

                        if (clickedItem.getAmount() - halfAmount > 0) {
                            ItemStack remainingStack = clickedItem.clone();
                            remainingStack.setAmount(clickedItem.getAmount() - halfAmount);
                            event.getInventory().setItem(slot, remainingStack);
                        } else {
                            event.getInventory().setItem(slot, guiManager.createEnhancedOutputSlot());
                        }
                        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
                    }
                }
                case SHIFT_LEFT, SHIFT_RIGHT -> {
                    // Remove item and replace with placeholder
                    event.getInventory().setItem(slot, guiManager.createEnhancedOutputSlot());
                    // Try to add to player inventory
                    if (player.getInventory().firstEmpty() != -1) {
                        player.getInventory().addItem(clickedItem.clone());
                    } else {
                        player.getWorld().dropItem(player.getLocation(), clickedItem.clone());
                    }
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
                }
            }
        }
    }

    /**
     * Handles drag events in editor GUI to properly handle item placement.
     */
    private void handleEditorGUIDrag(InventoryDragEvent event, Player player) {
        // Check if dragging to GUI slots
        boolean dragToGUI = false;
        for (int slot : event.getRawSlots()) {
            if (slot >= 0 && slot < event.getInventory().getSize()) {
                dragToGUI = true;
                break;
            }
        }

        if (!dragToGUI) {
            // Only dragging in player inventory - allow
            return;
        }

        // Cancel all drag events to GUI and handle manually
        event.setCancelled(true);

        ItemStack draggedItem = event.getOldCursor();
        if (draggedItem == null || draggedItem.getType() == Material.AIR) {
            return;
        }

        // Handle dragging to each valid slot
        for (int slot : event.getRawSlots()) {
            if (slot >= 0 && slot <= 2) { // Only input/output slots
                ItemStack currentItem = event.getInventory().getItem(slot);

                // Check if it's a placeholder
                boolean isPlaceholder = (slot <= 1 && currentItem != null && currentItem.getType() == Material.SLIME_BALL) ||
                        (slot == 2 && currentItem != null && currentItem.getType() == Material.EMERALD);

                if (isPlaceholder) {
                    // Place one item in the slot
                    ItemStack itemToPlace = draggedItem.clone();
                    itemToPlace.setAmount(1);
                    event.getInventory().setItem(slot, itemToPlace);

                    // Reduce cursor amount
                    draggedItem.setAmount(Math.max(0, draggedItem.getAmount() - 1));

                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);

                    // If no more items, stop
                    if (draggedItem.getAmount() == 0) {
                        event.getWhoClicked().setItemOnCursor(null);
                        break;
                    }
                }
            }
        }

        // Update cursor with remaining items
        if (draggedItem.getAmount() > 0) {
            event.getWhoClicked().setItemOnCursor(draggedItem);
        } else {
            event.getWhoClicked().setItemOnCursor(null);
        }
    }

    /**
     * Handles clicks on the trade area (existing trades and add slots).
     */
    private void handleTradeAreaClick(InventoryClickEvent event, Player player, String guiId, ItemStack clickedItem) {
        if (clickedItem == null || clickedItem.getType() == Material.AIR) {
            return;
        }

        // Handle "Add Trade" slots
        if (clickedItem.getType() == Material.LIME_STAINED_GLASS) {
            player.sendMessage(Component.text("💡 Tip: ", NamedTextColor.YELLOW)
                    .append(Component.text("Set up your trade in the input/output slots above, then click Save!", NamedTextColor.GRAY)));
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.8f, 1.5f);
            return;
        }

        // Middle-click to edit existing trades
        if (event.getClick() == ClickType.MIDDLE) {
            ItemMeta meta = clickedItem.getItemMeta();
            if (meta != null && meta.lore() != null) {
                String tradeId = extractTradeIdFromLore(meta.lore());
                if (tradeId != null) {
                    if (tradeManager.getTrade(guiId, tradeId) != null) {
                        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.2f);
                        
                        // Close current GUI and open trade edit GUI
                        player.closeInventory();
                        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                            if (player.isOnline()) {
                                guiManager.openTradeEditGUI(player, guiId, tradeId);
                            }
                        }, 1L);
                    } else {
                        player.sendMessage(Component.text("❌ Trade not found or already deleted!", NamedTextColor.RED));
                    }
                } else {
                    player.sendMessage(Component.text("❌ Cannot edit: Invalid trade data!", NamedTextColor.RED));
                }
            }
            return;
        }

        // Right-click to delete existing trades
        if (event.isRightClick()) {
            ItemMeta meta = clickedItem.getItemMeta();
            if (meta != null && meta.lore() != null) {
                String tradeId = extractTradeIdFromLore(meta.lore());
                if (tradeId != null) {
                    if (tradeManager.getTrade(guiId, tradeId) != null) {
                        tradeManager.removeTrade(guiId, tradeId);

                        player.sendMessage(Component.text("🗑 Trade deleted: ", NamedTextColor.RED)
                                .append(Component.text(tradeId, NamedTextColor.GRAY)));
                        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 0.8f);

                        // Refresh the GUI
                        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                            if (player.isOnline() && guiManager.getPlayerEditorGUI(player.getUniqueId()) != null) {
                                guiManager.refreshEditorGUI(player, guiId);
                            }
                        }, 1L);
                    } else {
                        player.sendMessage(Component.text("❌ Trade not found or already deleted!", NamedTextColor.RED));
                    }
                } else {
                    player.sendMessage(Component.text("❌ Cannot delete: Invalid trade data!", NamedTextColor.RED));
                }
            }
        }
        // Left-click to show trade details
        else if (event.isLeftClick()) {
            showTradeDetails(player, clickedItem);
        }
    }

    /**
     * Shows detailed information about a trade.
     */
    private void showTradeDetails(Player player, ItemStack tradeItem) {
        ItemMeta meta = tradeItem.getItemMeta();
        if (meta == null || meta.lore() == null) {
            return;
        }

        player.sendMessage(Component.text("📋 Trade Details", NamedTextColor.BLUE)
                .decorate(TextDecoration.BOLD));
        player.sendMessage(Component.text(""));

        boolean inRequirements = false;
        boolean inReward = false;

        for (Component line : meta.lore()) {
            String text = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                    .serialize(line);

            if (text.contains("Requirements:")) {
                inRequirements = true;
                player.sendMessage(Component.text("Required Items:", NamedTextColor.AQUA));
                continue;
            } else if (text.contains("Reward:")) {
                inRequirements = false;
                inReward = true;
                player.sendMessage(Component.text("Player Receives:", NamedTextColor.GOLD));
                continue;
            }

            if (inRequirements && text.trim().startsWith("•")) {
                player.sendMessage(Component.text(text, NamedTextColor.GREEN));
            } else if (inReward && text.trim().startsWith("•")) {
                player.sendMessage(Component.text(text, NamedTextColor.YELLOW));
            }
        }

        player.sendMessage(Component.text(""));
        player.sendMessage(Component.text("💡 Right-click to delete this trade", NamedTextColor.GRAY)
                .decorate(TextDecoration.ITALIC));
        player.sendMessage(Component.text("✏ Middle-click to edit this trade", NamedTextColor.BLUE)
                .decorate(TextDecoration.ITALIC));

        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.2f);
    }

    /**
     * Checks if an item has additional requirements in its lore.
     */
    private boolean hasAdditionalRequirements(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null || meta.lore() == null) {
            return false;
        }

        for (Component line : meta.lore()) {
            String text = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                    .serialize(line);
            if (text.contains("Additional requirements:")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Finds the original trade by comparing result items.
     */
    private Trade findTradeByResult(String guiId, ItemStack resultItem) {
        Map<String, Trade> trades = tradeManager.getTrades(guiId);

        for (Trade trade : trades.values()) {
            if (ItemUtils.isSimilar(trade.output(), resultItem)) {
                return trade;
            }
        }
        return null;
    }

    /**
     * Extracts trade ID from item lore with improved parsing.
     */
    private String extractTradeIdFromLore(java.util.List<Component> lore) {
        for (Component line : lore) {
            String text = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                    .serialize(line);

            if (text.startsWith("ID: ")) {
                String id = text.substring("ID: ".length()).trim();
                return id;
            } else if (text.startsWith("Trade ID: ")) {
                String id = text.substring("Trade ID: ".length()).trim();
                return id;
            } else if (text.contains("trade_")) {
                int start = text.indexOf("trade_");
                if (start != -1) {
                    int end = text.indexOf(' ', start);
                    if (end == -1) end = text.length();
                    String id = text.substring(start, end).trim();
                    return id;
                }
            }
        }
        return null;
    }
}