package org.mindle.protrades.itemx;

import org.bukkit.inventory.ItemStack;
import org.mindle.protrades.ProTrades;
import org.mindle.protrades.models.Trade;
import org.mindle.protrades.utils.ItemUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Integration utility class for ItemX with ProTrades.
 * Provides helper methods for integrating ItemX items with the trade system.
 */
public class ItemXIntegration {
    
    private final ProTrades plugin;
    private final ItemManager itemManager;
    
    public ItemXIntegration(ProTrades plugin) {
        this.plugin = plugin;
        this.itemManager = plugin.getItemManager();
    }
    
    /**
     * Creates a trade using ItemX items.
     * 
     * @param tradeId The ID of the trade
     * @param inputItemIds List of input ItemX item IDs
     * @param outputItemId The output ItemX item ID
     * @return A Trade object with ItemX items, or null if any items are invalid
     */
    public Trade createTradeWithItemX(String tradeId, List<String> inputItemIds, String outputItemId) {
        try {
            // Create input items
            List<ItemStack> inputs = new ArrayList<>();
            for (String inputId : inputItemIds) {
                ItemStack item = itemManager.createItemStack(inputId);
                if (item == null) {
                    plugin.getLogger().warning("Failed to create input item: " + inputId);
                    return null;
                }
                inputs.add(item);
            }
            
            // Create output item
            ItemStack output = itemManager.createItemStack(outputItemId);
            if (output == null) {
                plugin.getLogger().warning("Failed to create output item: " + outputItemId);
                return null;
            }
            
            return Trade.of(tradeId, inputs, output);
            
        } catch (Exception e) {
            plugin.getLogger().severe("Error creating trade with ItemX items: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Checks if a trade contains ItemX items.
     * 
     * @param trade The trade to check
     * @return true if the trade contains at least one ItemX item
     */
    public boolean containsItemXItems(Trade trade) {
        if (trade == null) {
            return false;
        }
        
        // Check input items
        for (ItemStack input : trade.inputs()) {
            if (itemManager.isItemXItem(input)) {
                return true;
            }
        }
        
        // Check output item
        return itemManager.isItemXItem(trade.output());
    }
    
    /**
     * Gets a summary of ItemX items in a trade.
     * 
     * @param trade The trade to analyze
     * @return A summary string of ItemX items
     */
    public String getItemXSummary(Trade trade) {
        if (trade == null) {
            return "No trade provided";
        }
        
        StringBuilder summary = new StringBuilder();
        summary.append("Trade: ").append(trade.id()).append("\n");
        
        // Analyze input items
        summary.append("Inputs:\n");
        for (int i = 0; i < trade.inputs().size(); i++) {
            ItemStack input = trade.inputs().get(i);
            if (itemManager.isItemXItem(input)) {
                String itemId = itemManager.getItemXId(input);
                summary.append("  - ItemX: ").append(itemId).append("\n");
            } else {
                summary.append("  - Regular: ").append(ItemUtils.getDisplayName(input)).append("\n");
            }
        }
        
        // Analyze output item
        summary.append("Output:\n");
        if (itemManager.isItemXItem(trade.output())) {
            String itemId = itemManager.getItemXId(trade.output());
            summary.append("  - ItemX: ").append(itemId).append("\n");
        } else {
            summary.append("  - Regular: ").append(ItemUtils.getDisplayName(trade.output())).append("\n");
        }
        
        return summary.toString();
    }
    
    /**
     * Validates that all ItemX items in a trade are valid.
     * 
     * @param trade The trade to validate
     * @return true if all ItemX items are valid
     */
    public boolean validateItemXTrade(Trade trade) {
        if (trade == null) {
            return false;
        }
        
        // Validate input items
        for (ItemStack input : trade.inputs()) {
            if (itemManager.isItemXItem(input)) {
                String itemId = itemManager.getItemXId(input);
                if (itemId == null || itemManager.getItemDefinition(itemId) == null) {
                    plugin.getLogger().warning("Invalid ItemX item in trade inputs: " + itemId);
                    return false;
                }
            }
        }
        
        // Validate output item
        if (itemManager.isItemXItem(trade.output())) {
            String itemId = itemManager.getItemXId(trade.output());
            if (itemId == null || itemManager.getItemDefinition(itemId) == null) {
                plugin.getLogger().warning("Invalid ItemX item in trade output: " + itemId);
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Upgrades regular items to ItemX items if possible.
     * This method can be used to migrate existing trades to use ItemX items.
     * 
     * @param item The item to upgrade
     * @return The upgraded ItemX item, or the original item if no upgrade is possible
     */
    public ItemStack upgradeToItemX(ItemStack item) {
        if (item == null || itemManager.isItemXItem(item)) {
            return item;
        }
        
        // Simple upgrade logic - this can be expanded based on needs
        String materialName = item.getType().name().toLowerCase();
        
        // Try to find a matching ItemX item
        for (String itemId : itemManager.getAllItemIds()) {
            ItemDefinition definition = itemManager.getItemDefinition(itemId);
            if (definition != null && definition.getMaterial() == item.getType()) {
                // Found a matching material, create ItemX version
                ItemStack itemXVersion = itemManager.createItemStack(definition);
                if (itemXVersion != null) {
                    plugin.getLogger().info("Upgraded item " + materialName + " to ItemX: " + itemId);
                    return itemXVersion;
                }
            }
        }
        
        return item; // Return original if no upgrade possible
    }
    
    /**
     * Gets statistics about ItemX usage in the trade system.
     * 
     * @return A map containing statistics about ItemX integration
     */
    public java.util.Map<String, Object> getIntegrationStatistics() {
        java.util.Map<String, Object> stats = new java.util.HashMap<>();
        
        // Count trades with ItemX items
        int totalTrades = 0;
        int tradesWithItemX = 0;
        
        for (String guiId : plugin.getTradeManager().getAllTradeGUIIds()) {
            java.util.Map<String, Trade> trades = plugin.getTradeManager().getTrades(guiId);
            totalTrades += trades.size();
            
            for (Trade trade : trades.values()) {
                if (containsItemXItems(trade)) {
                    tradesWithItemX++;
                }
            }
        }
        
        stats.put("total_trades", totalTrades);
        stats.put("trades_with_itemx", tradesWithItemX);
        stats.put("itemx_integration_percentage", 
                totalTrades > 0 ? (tradesWithItemX * 100.0 / totalTrades) : 0.0);
        
        // Add ItemX statistics
        stats.putAll(itemManager.getStatistics());
        
        return stats;
    }
}