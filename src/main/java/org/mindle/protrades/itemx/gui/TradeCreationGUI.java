package org.mindle.protrades.itemx.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.mindle.protrades.ProTrades;
import org.mindle.protrades.itemx.ColorUtil;
import org.mindle.protrades.itemx.ItemManager;
import org.mindle.protrades.models.Trade;

import java.util.*;

/**
 * GUI for dynamically creating trades with ItemX items.
 * Provides an intuitive interface for setting up custom trades.
 */
public class TradeCreationGUI implements Listener {
    
    private final ProTrades plugin;
    private final ItemManager itemManager;
    private final Map<Player, TradeCreationSession> sessions;
    
    // GUI Layout Constants
    private static final int GUI_SIZE = 54; // 6 rows
    private static final int[] INPUT_SLOTS = {10, 11, 12, 19, 20, 21}; // Max 6 inputs
    private static final int OUTPUT_SLOT = 24;
    private static final int PREVIEW_SLOT = 40;
    private static final int SAVE_SLOT = 45;
    private static final int CANCEL_SLOT = 53;
    private static final int ITEMX_BROWSER_SLOT = 16;
    private static final int TEMPLATE_BROWSER_SLOT = 34;
    
    public TradeCreationGUI(ProTrades plugin, ItemManager itemManager) {
        this.plugin = plugin;
        this.itemManager = itemManager;
        this.sessions = new HashMap<>();
        
        // Register as event listener
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }
    
    /**
     * Opens the trade creation GUI for a player.
     */
    public void openTradeCreationGUI(Player player, String targetGuiId) {
        TradeCreationSession session = new TradeCreationSession(targetGuiId);
        sessions.put(player, session);
        
        Inventory gui = createTradeCreationInventory(session);
        player.openInventory(gui);
        
        plugin.getLogger().fine("Opened trade creation GUI for " + player.getName() + " targeting GUI: " + targetGuiId);
    }
    
    /**
     * Creates the trade creation inventory.
     */
    private Inventory createTradeCreationInventory(TradeCreationSession session) {
        Inventory gui = Bukkit.createInventory(null, GUI_SIZE, 
                ColorUtil.colorize("<gradient:#FFD700:#FFA500>Trade Creator</gradient>"));
        
        // Fill background
        fillBackground(gui);
        
        // Add control buttons
        addControlButtons(gui);
        
        // Add current items from session
        updateSessionItems(gui, session);
        
        return gui;
    }
    
    /**
     * Fills the GUI background with decorative items.
     */
    private void fillBackground(Inventory gui) {
        ItemStack background = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = background.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(" "));
            background.setItemMeta(meta);
        }
        
        // Fill specific slots with background
        for (int i = 0; i < GUI_SIZE; i++) {
            if (!isInteractableSlot(i)) {
                gui.setItem(i, background);
            }
        }
    }
    
    /**
     * Adds control buttons to the GUI.
     */
    private void addControlButtons(Inventory gui) {
        // ItemX Browser button
        ItemStack itemxBrowser = new ItemStack(Material.ENCHANTED_BOOK);
        ItemMeta itemxMeta = itemxBrowser.getItemMeta();
        if (itemxMeta != null) {
            itemxMeta.displayName(ColorUtil.colorize("<aqua>ItemX Browser</aqua>"));
            itemxMeta.lore(Arrays.asList(
                    ColorUtil.colorize("<gray>Click to browse ItemX items"),
                    ColorUtil.colorize("<gray>Right-click to browse by category")
            ));
            itemxBrowser.setItemMeta(itemxMeta);
        }
        gui.setItem(ITEMX_BROWSER_SLOT, itemxBrowser);
        
        // Template Browser button
        ItemStack templateBrowser = new ItemStack(Material.WRITABLE_BOOK);
        ItemMeta templateMeta = templateBrowser.getItemMeta();
        if (templateMeta != null) {
            templateMeta.displayName(ColorUtil.colorize("<yellow>Template Browser</yellow>"));
            templateMeta.lore(Arrays.asList(
                    ColorUtil.colorize("<gray>Click to browse trade templates"),
                    ColorUtil.colorize("<gray>Quick-start your trade creation")
            ));
            templateBrowser.setItemMeta(templateMeta);
        }
        gui.setItem(TEMPLATE_BROWSER_SLOT, templateBrowser);
        
        // Preview button
        ItemStack preview = new ItemStack(Material.SPYGLASS);
        ItemMeta previewMeta = preview.getItemMeta();
        if (previewMeta != null) {
            previewMeta.displayName(ColorUtil.colorize("<green>Preview Trade</green>"));
            previewMeta.lore(Arrays.asList(
                    ColorUtil.colorize("<gray>Click to preview your trade"),
                    ColorUtil.colorize("<gray>Shows how it will appear in-game")
            ));
            preview.setItemMeta(previewMeta);
        }
        gui.setItem(PREVIEW_SLOT, preview);
        
        // Save button
        ItemStack save = new ItemStack(Material.EMERALD);
        ItemMeta saveMeta = save.getItemMeta();
        if (saveMeta != null) {
            saveMeta.displayName(ColorUtil.colorize("<green>Save Trade</green>"));
            saveMeta.lore(Arrays.asList(
                    ColorUtil.colorize("<gray>Click to save this trade"),
                    ColorUtil.colorize("<gray>Will be added to the target GUI")
            ));
            save.setItemMeta(saveMeta);
        }
        gui.setItem(SAVE_SLOT, save);
        
        // Cancel button
        ItemStack cancel = new ItemStack(Material.BARRIER);
        ItemMeta cancelMeta = cancel.getItemMeta();
        if (cancelMeta != null) {
            cancelMeta.displayName(ColorUtil.colorize("<red>Cancel</red>"));
            cancelMeta.lore(Arrays.asList(
                    ColorUtil.colorize("<gray>Click to cancel trade creation"),
                    ColorUtil.colorize("<gray>All progress will be lost")
            ));
            cancel.setItemMeta(cancelMeta);
        }
        gui.setItem(CANCEL_SLOT, cancel);
        
        // Add slot indicators
        addSlotIndicators(gui);
    }
    
    /**
     * Adds slot indicators to show where items should be placed.
     */
    private void addSlotIndicators(Inventory gui) {
        // Input slot indicators
        for (int i = 0; i < INPUT_SLOTS.length; i++) {
            int slot = INPUT_SLOTS[i];
            if (gui.getItem(slot) == null) {
                ItemStack indicator = new ItemStack(Material.LIGHT_BLUE_STAINED_GLASS_PANE);
                ItemMeta meta = indicator.getItemMeta();
                if (meta != null) {
                    meta.displayName(ColorUtil.colorize("<blue>Input Slot " + (i + 1) + "</blue>"));
                    meta.lore(Arrays.asList(
                            ColorUtil.colorize("<gray>Place input items here"),
                            ColorUtil.colorize("<gray>Items players must provide")
                    ));
                    indicator.setItemMeta(meta);
                }
                gui.setItem(slot, indicator);
            }
        }
        
        // Output slot indicator
        if (gui.getItem(OUTPUT_SLOT) == null) {
            ItemStack indicator = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
            ItemMeta meta = indicator.getItemMeta();
            if (meta != null) {
                meta.displayName(ColorUtil.colorize("<green>Output Slot</green>"));
                meta.lore(Arrays.asList(
                        ColorUtil.colorize("<gray>Place output item here"),
                        ColorUtil.colorize("<gray>Item players will receive")
                ));
                indicator.setItemMeta(meta);
            }
            gui.setItem(OUTPUT_SLOT, indicator);
        }
    }
    
    /**
     * Updates the GUI with items from the session.
     */
    private void updateSessionItems(Inventory gui, TradeCreationSession session) {
        // Place input items
        for (int i = 0; i < session.getInputItems().size() && i < INPUT_SLOTS.length; i++) {
            gui.setItem(INPUT_SLOTS[i], session.getInputItems().get(i));
        }
        
        // Place output item
        if (session.getOutputItem() != null) {
            gui.setItem(OUTPUT_SLOT, session.getOutputItem());
        }
    }
    
    /**
     * Checks if a slot is interactable (not background).
     */
    private boolean isInteractableSlot(int slot) {
        // Input slots
        for (int inputSlot : INPUT_SLOTS) {
            if (slot == inputSlot) return true;
        }
        
        // Special slots
        return slot == OUTPUT_SLOT || slot == PREVIEW_SLOT || slot == SAVE_SLOT || 
               slot == CANCEL_SLOT || slot == ITEMX_BROWSER_SLOT || slot == TEMPLATE_BROWSER_SLOT;
    }
    
    /**
     * Handles inventory click events.
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        Player player = (Player) event.getWhoClicked();
        TradeCreationSession session = sessions.get(player);
        
        if (session == null) return;
        
        // Check if this is our GUI
        if (!event.getView().getTitle().contains("Trade Creator")) return;
        
        event.setCancelled(true);
        
        int slot = event.getRawSlot();
        ItemStack clickedItem = event.getCurrentItem();
        ItemStack cursorItem = event.getCursor();
        
        // Handle different slot types
        if (isInputSlot(slot)) {
            handleInputSlotClick(player, session, slot, clickedItem, cursorItem);
        } else if (slot == OUTPUT_SLOT) {
            handleOutputSlotClick(player, session, clickedItem, cursorItem);
        } else if (slot == ITEMX_BROWSER_SLOT) {
            handleItemXBrowserClick(player, session, event.isRightClick());
        } else if (slot == TEMPLATE_BROWSER_SLOT) {
            handleTemplateBrowserClick(player, session);
        } else if (slot == PREVIEW_SLOT) {
            handlePreviewClick(player, session);
        } else if (slot == SAVE_SLOT) {
            handleSaveClick(player, session);
        } else if (slot == CANCEL_SLOT) {
            handleCancelClick(player, session);
        }
    }
    
    /**
     * Handles input slot clicks.
     */
    private void handleInputSlotClick(Player player, TradeCreationSession session, int slot, 
                                    ItemStack clickedItem, ItemStack cursorItem) {
        int inputIndex = getInputIndex(slot);
        if (inputIndex == -1) return;
        
        if (cursorItem != null && cursorItem.getType() != Material.AIR) {
            // Placing item
            session.setInputItem(inputIndex, cursorItem.clone());
            player.setItemOnCursor(null);
            player.sendMessage(ColorUtil.colorize("<green>Added input item " + (inputIndex + 1)));
        } else if (clickedItem != null && !isIndicator(clickedItem)) {
            // Removing item
            session.setInputItem(inputIndex, null);
            player.setItemOnCursor(clickedItem);
            player.sendMessage(ColorUtil.colorize("<yellow>Removed input item " + (inputIndex + 1)));
        }
        
        // Refresh GUI
        refreshGUI(player, session);
    }
    
    /**
     * Handles output slot clicks.
     */
    private void handleOutputSlotClick(Player player, TradeCreationSession session, 
                                     ItemStack clickedItem, ItemStack cursorItem) {
        if (cursorItem != null && cursorItem.getType() != Material.AIR) {
            // Placing item
            session.setOutputItem(cursorItem.clone());
            player.setItemOnCursor(null);
            player.sendMessage(ColorUtil.colorize("<green>Set output item"));
        } else if (clickedItem != null && !isIndicator(clickedItem)) {
            // Removing item
            session.setOutputItem(null);
            player.setItemOnCursor(clickedItem);
            player.sendMessage(ColorUtil.colorize("<yellow>Removed output item"));
        }
        
        // Refresh GUI
        refreshGUI(player, session);
    }
    
    /**
     * Handles ItemX browser clicks.
     */
    private void handleItemXBrowserClick(Player player, TradeCreationSession session, boolean rightClick) {
        if (rightClick) {
            // Open category browser
            openCategoryBrowser(player, session);
        } else {
            // Open item browser
            openItemBrowser(player, session);
        }
    }
    
    /**
     * Handles template browser clicks.
     */
    private void handleTemplateBrowserClick(Player player, TradeCreationSession session) {
        openTemplateBrowser(player, session);
    }
    
    /**
     * Handles preview clicks.
     */
    private void handlePreviewClick(Player player, TradeCreationSession session) {
        if (!session.isValid()) {
            player.sendMessage(ColorUtil.colorize("<red>Trade is not complete! Add inputs and output."));
            return;
        }
        
        // Show preview message
        player.sendMessage(ColorUtil.colorize("<yellow>=== Trade Preview ==="));
        player.sendMessage(ColorUtil.colorize("<blue>Inputs Required:"));
        
        for (int i = 0; i < session.getInputItems().size(); i++) {
            ItemStack item = session.getInputItems().get(i);
            if (item != null) {
                String name = item.hasItemMeta() && item.getItemMeta().hasDisplayName() 
                        ? item.getItemMeta().getDisplayName() 
                        : item.getType().name();
                player.sendMessage(ColorUtil.colorize("<gray>  " + (i + 1) + ". " + name + " x" + item.getAmount()));
            }
        }
        
        player.sendMessage(ColorUtil.colorize("<green>Output Given:"));
        ItemStack output = session.getOutputItem();
        if (output != null) {
            String name = output.hasItemMeta() && output.getItemMeta().hasDisplayName() 
                    ? output.getItemMeta().getDisplayName() 
                    : output.getType().name();
            player.sendMessage(ColorUtil.colorize("<gray>  " + name + " x" + output.getAmount()));
        }
        
        player.sendMessage(ColorUtil.colorize("<yellow>================"));
    }
    
    /**
     * Handles save clicks.
     */
    private void handleSaveClick(Player player, TradeCreationSession session) {
        if (!session.isValid()) {
            player.sendMessage(ColorUtil.colorize("<red>Cannot save incomplete trade! Add inputs and output."));
            return;
        }
        
        try {
            // Generate unique trade ID
            String tradeId = "custom_" + System.currentTimeMillis();
            
            // Create trade
            List<ItemStack> inputs = new ArrayList<>();
            for (ItemStack item : session.getInputItems()) {
                if (item != null) {
                    inputs.add(item);
                }
            }
            
            Trade trade = Trade.of(tradeId, inputs, session.getOutputItem());
            
            // Add to target GUI
            plugin.getTradeManager().addTrade(session.getTargetGuiId(), trade);
            
            player.sendMessage(ColorUtil.colorize("<green>Trade saved successfully!"));
            player.sendMessage(ColorUtil.colorize("<yellow>Added to GUI: " + session.getTargetGuiId()));
            
            // Close GUI
            player.closeInventory();
            
        } catch (Exception e) {
            player.sendMessage(ColorUtil.colorize("<red>Error saving trade: " + e.getMessage()));
            plugin.getLogger().severe("Error saving trade from GUI: " + e.getMessage());
        }
    }
    
    /**
     * Handles cancel clicks.
     */
    private void handleCancelClick(Player player, TradeCreationSession session) {
        player.sendMessage(ColorUtil.colorize("<yellow>Trade creation cancelled."));
        player.closeInventory();
    }
    
    /**
     * Opens the item browser (placeholder for now).
     */
    private void openItemBrowser(Player player, TradeCreationSession session) {
        player.sendMessage(ColorUtil.colorize("<yellow>ItemX Browser - Coming Soon!"));
        player.sendMessage(ColorUtil.colorize("<gray>For now, use /itemx give to get items and place them manually."));
    }
    
    /**
     * Opens the category browser (placeholder for now).
     */
    private void openCategoryBrowser(Player player, TradeCreationSession session) {
        player.sendMessage(ColorUtil.colorize("<yellow>Category Browser - Coming Soon!"));
        player.sendMessage(ColorUtil.colorize("<gray>Available categories: " + String.join(", ", itemManager.getAllCategories())));
    }
    
    /**
     * Opens the template browser (placeholder for now).
     */
    private void openTemplateBrowser(Player player, TradeCreationSession session) {
        player.sendMessage(ColorUtil.colorize("<yellow>Template Browser - Coming Soon!"));
        player.sendMessage(ColorUtil.colorize("<gray>Templates will allow quick trade setup."));
    }
    
    /**
     * Refreshes the GUI for a player.
     */
    private void refreshGUI(Player player, TradeCreationSession session) {
        Inventory gui = createTradeCreationInventory(session);
        player.getOpenInventory().getTopInventory().setContents(gui.getContents());
    }
    
    /**
     * Gets the input index for a slot.
     */
    private int getInputIndex(int slot) {
        for (int i = 0; i < INPUT_SLOTS.length; i++) {
            if (INPUT_SLOTS[i] == slot) {
                return i;
            }
        }
        return -1;
    }
    
    /**
     * Checks if a slot is an input slot.
     */
    private boolean isInputSlot(int slot) {
        return getInputIndex(slot) != -1;
    }
    
    /**
     * Checks if an item is an indicator (glass pane).
     */
    private boolean isIndicator(ItemStack item) {
        return item != null && (item.getType() == Material.LIGHT_BLUE_STAINED_GLASS_PANE || 
                               item.getType() == Material.LIME_STAINED_GLASS_PANE ||
                               item.getType() == Material.GRAY_STAINED_GLASS_PANE);
    }
    
    /**
     * Handles inventory close events.
     */
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        
        Player player = (Player) event.getPlayer();
        
        if (event.getView().getTitle().contains("Trade Creator")) {
            // Clean up session
            sessions.remove(player);
            plugin.getLogger().fine("Cleaned up trade creation session for " + player.getName());
        }
    }
    
    /**
     * Represents a trade creation session.
     */
    private static class TradeCreationSession {
        private final String targetGuiId;
        private final List<ItemStack> inputItems;
        private ItemStack outputItem;
        
        public TradeCreationSession(String targetGuiId) {
            this.targetGuiId = targetGuiId;
            this.inputItems = new ArrayList<>(Collections.nCopies(6, null));
        }
        
        public String getTargetGuiId() { return targetGuiId; }
        public List<ItemStack> getInputItems() { return inputItems; }
        public ItemStack getOutputItem() { return outputItem; }
        
        public void setInputItem(int index, ItemStack item) {
            if (index >= 0 && index < inputItems.size()) {
                inputItems.set(index, item);
            }
        }
        
        public void setOutputItem(ItemStack item) {
            this.outputItem = item;
        }
        
        public boolean isValid() {
            boolean hasInput = inputItems.stream().anyMatch(Objects::nonNull);
            return hasInput && outputItem != null;
        }
    }
}