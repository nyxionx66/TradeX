package org.mindle.protrades.managers;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.mindle.protrades.ProTrades;
import org.mindle.protrades.models.Trade;
import org.mindle.protrades.utils.ItemUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/**
 * Enhanced TradeManager with full NBT support for items and trade positioning.
 * Uses concurrent data structures for thread safety.
 */
public class TradeManager {

    private final ProTrades plugin;
    private final ConfigManager configManager;
    private final Map<String, Map<String, Trade>> tradeGUIs = new ConcurrentHashMap<>();
    private final Map<String, String> guiTitles = new ConcurrentHashMap<>();
    private final Map<String, Integer> guiRows = new ConcurrentHashMap<>();
    private final Map<String, List<String>> tradeOrders = new ConcurrentHashMap<>();

    public TradeManager(ProTrades plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    /**
     * Loads all trades from configuration files with NBT support.
     */
    public void loadAllTrades() {
        List<String> tradeIds = configManager.getAllTradeIds();
        
        for (String tradeId : tradeIds) {
            loadTradeGUI(tradeId);
        }
        
        plugin.getLogger().info("Loaded " + tradeIds.size() + " trade GUIs with NBT support");
    }

    /**
     * Loads a specific trade GUI from configuration with NBT support.
     * 
     * @param tradeId The ID of the trade GUI to load
     */
    public void loadTradeGUI(String tradeId) {
        configManager.loadTradeConfigAsync(tradeId)
                .thenAccept(config -> {
                    try {
                        String title = (String) config.get("title");
                        int rows = (Integer) config.get("rows");
                        ConfigurationSection tradesSection = (ConfigurationSection) config.get("trades");
                        
                        guiTitles.put(tradeId, title);
                        guiRows.put(tradeId, rows);
                        
                        Map<String, Trade> trades = new HashMap<>();
                        List<String> order = new ArrayList<>();
                        
                        if (tradesSection != null) {
                            // Load trade order if it exists
                            List<String> savedOrder = (List<String>) config.get("order");
                            if (savedOrder != null) {
                                order.addAll(savedOrder);
                            }
                            
                            for (String key : tradesSection.getKeys(false)) {
                                ConfigurationSection tradeSection = tradesSection.getConfigurationSection(key);
                                if (tradeSection != null) {
                                    Trade trade = loadTradeFromSection(key, tradeSection);
                                    if (trade != null) {
                                        trades.put(key, trade);
                                        // Add to order if not already present
                                        if (!order.contains(key)) {
                                            order.add(key);
                                        }
                                        plugin.getLogger().fine("Loaded trade: " + key + " with NBT data");
                                    }
                                }
                            }
                            
                            // Remove any trades from order that no longer exist
                            order.removeIf(tradeKey -> !trades.containsKey(tradeKey));
                        }
                        
                        tradeGUIs.put(tradeId, trades);
                        tradeOrders.put(tradeId, order);
                        plugin.getLogger().info("Loaded trade GUI: " + tradeId + " with " + trades.size() + " trades (NBT enabled)");
                    } catch (Exception e) {
                        plugin.getLogger().log(Level.SEVERE, "Error loading trade GUI: " + tradeId, e);
                    }
                });
    }

    /**
     * Saves all trades to configuration files with NBT support.
     */
    public void saveAllTrades() {
        for (String tradeId : tradeGUIs.keySet()) {
            saveTradeGUI(tradeId);
        }
    }

    /**
     * Saves a specific trade GUI to configuration with NBT support and trade order.
     * 
     * @param tradeId The ID of the trade GUI to save
     */
    public void saveTradeGUI(String tradeId) {
        String title = guiTitles.getOrDefault(tradeId, "&1&l" + tradeId + " Shop");
        int rows = guiRows.getOrDefault(tradeId, 3);
        Map<String, Trade> trades = tradeGUIs.getOrDefault(tradeId, new HashMap<>());
        List<String> order = tradeOrders.getOrDefault(tradeId, new ArrayList<>());
        
        configManager.saveTradeConfigAsync(tradeId, title, rows, trades, order)
                .thenAccept(success -> {
                    if (success) {
                        plugin.getLogger().info("Saved trade GUI: " + tradeId + " with NBT data and order");
                    } else {
                        plugin.getLogger().warning("Failed to save trade GUI: " + tradeId);
                    }
                });
    }

    /**
     * Creates a new trade GUI.
     * 
     * @param tradeId The ID of the new trade GUI
     * @return CompletableFuture indicating success
     */
    public CompletableFuture<Boolean> createTradeGUI(String tradeId) {
        return configManager.createDefaultTradeConfig(tradeId)
                .thenApply(success -> {
                    if (success) {
                        tradeGUIs.put(tradeId, new HashMap<>());
                        guiTitles.put(tradeId, "&1&l" + tradeId + " Shop");
                        guiRows.put(tradeId, 3);
                        tradeOrders.put(tradeId, new ArrayList<>());
                        return true;
                    }
                    return false;
                });
    }

    /**
     * Deletes a trade GUI.
     * 
     * @param tradeId The ID of the trade GUI to delete
     * @return true if deleted successfully, false otherwise
     */
    public boolean deleteTradeGUI(String tradeId) {
        tradeGUIs.remove(tradeId);
        guiTitles.remove(tradeId);
        guiRows.remove(tradeId);
        tradeOrders.remove(tradeId);
        return configManager.deleteTradeConfig(tradeId);
    }

    /**
     * Adds a trade to a specific GUI with enhanced NBT validation and preservation.
     * Uses the ultra cloning system for maximum data preservation.
     * 
     * @param guiId The ID of the GUI
     * @param trade The trade to add
     */
    public void addTrade(String guiId, Trade trade) {
        // Use ultra cloning system for perfect item preservation
        List<ItemStack> safeInputs = ItemUtils.createPerfectCloneList(trade.inputs());
        ItemStack safeOutput = ItemUtils.createPerfectClone(trade.output());
        
        // Create a new trade with ultra-cloned items
        Trade safeTrade = Trade.of(trade.id(), safeInputs, safeOutput);
        
        // Validate trade items have proper NBT data
        validateTradeItems(safeTrade);
        
        // Verify clone integrity
        if (!verifyTradeIntegrity(trade, safeTrade)) {
            plugin.getLogger().warning("Trade clone integrity verification failed for trade: " + trade.id());
        }
        
        tradeGUIs.computeIfAbsent(guiId, k -> new HashMap<>()).put(safeTrade.id(), safeTrade);
        
        // Add to order if not already present
        List<String> order = tradeOrders.computeIfAbsent(guiId, k -> new ArrayList<>());
        if (!order.contains(safeTrade.id())) {
            order.add(safeTrade.id());
        }
        
        saveTradeGUI(guiId);
        
        plugin.getLogger().info("Added trade: " + safeTrade.id() + " to GUI: " + guiId + " with ultra cloning system");
    }

    /**
     * Removes a trade from a specific GUI.
     * 
     * @param guiId The ID of the GUI
     * @param tradeId The ID of the trade to remove
     */
    public void removeTrade(String guiId, String tradeId) {
        Map<String, Trade> trades = tradeGUIs.get(guiId);
        if (trades != null) {
            Trade removedTrade = trades.remove(tradeId);
            if (removedTrade != null) {
                // Remove from order as well
                List<String> order = tradeOrders.get(guiId);
                if (order != null) {
                    order.remove(tradeId);
                }
                
                saveTradeGUI(guiId);
                plugin.getLogger().info("Removed trade: " + tradeId + " from GUI: " + guiId);
            }
        }
    }

    /**
     * Moves a trade to a specific position in the order.
     * 
     * @param guiId The ID of the GUI
     * @param tradeId The ID of the trade to move
     * @param position The new position (1-based index)
     * @return true if the trade was moved successfully, false otherwise
     */
    public boolean moveTradeToPosition(String guiId, String tradeId, int position) {
        List<String> order = tradeOrders.get(guiId);
        if (order == null || !order.contains(tradeId)) {
            return false;
        }
        
        // Convert to 0-based index and validate
        int index = position - 1;
        if (index < 0 || index >= order.size()) {
            return false;
        }
        
        // Remove from current position and insert at new position
        order.remove(tradeId);
        order.add(index, tradeId);
        
        saveTradeGUI(guiId);
        plugin.getLogger().info("Moved trade: " + tradeId + " to position " + position + " in GUI: " + guiId);
        
        return true;
    }

    /**
     * Swaps the positions of two trades.
     * 
     * @param guiId The ID of the GUI
     * @param tradeId1 The ID of the first trade
     * @param tradeId2 The ID of the second trade
     * @return true if the trades were swapped successfully, false otherwise
     */
    public boolean swapTrades(String guiId, String tradeId1, String tradeId2) {
        List<String> order = tradeOrders.get(guiId);
        if (order == null || !order.contains(tradeId1) || !order.contains(tradeId2)) {
            return false;
        }
        
        int index1 = order.indexOf(tradeId1);
        int index2 = order.indexOf(tradeId2);
        
        // Swap the positions
        Collections.swap(order, index1, index2);
        
        saveTradeGUI(guiId);
        plugin.getLogger().info("Swapped trades: " + tradeId1 + " and " + tradeId2 + " in GUI: " + guiId);
        
        return true;
    }

    /**
     * Sets the complete order of trades for a GUI.
     * 
     * @param guiId The ID of the GUI
     * @param newOrder The new order of trade IDs
     * @return true if the order was set successfully, false otherwise
     */
    public boolean setTradeOrder(String guiId, List<String> newOrder) {
        Map<String, Trade> trades = tradeGUIs.get(guiId);
        if (trades == null) {
            return false;
        }
        
        // Validate that all trade IDs in the new order exist
        for (String tradeId : newOrder) {
            if (!trades.containsKey(tradeId)) {
                return false;
            }
        }
        
        // Ensure all existing trades are included in the new order
        Set<String> existingTrades = new HashSet<>(trades.keySet());
        Set<String> orderedTrades = new HashSet<>(newOrder);
        
        if (!existingTrades.equals(orderedTrades)) {
            return false;
        }
        
        tradeOrders.put(guiId, new ArrayList<>(newOrder));
        saveTradeGUI(guiId);
        
        plugin.getLogger().info("Updated trade order for GUI: " + guiId);
        return true;
    }

    /**
     * Gets all trades for a specific GUI in their defined order.
     * 
     * @param guiId The ID of the GUI
     * @return LinkedHashMap of trades in order, or empty map if not found
     */
    public Map<String, Trade> getTrades(String guiId) {
        Map<String, Trade> trades = tradeGUIs.get(guiId);
        List<String> order = tradeOrders.get(guiId);
        
        if (trades == null || trades.isEmpty()) {
            return new HashMap<>();
        }
        
        if (order == null || order.isEmpty()) {
            return new HashMap<>(trades);
        }
        
        // Return trades in the specified order
        Map<String, Trade> orderedTrades = new LinkedHashMap<>();
        for (String tradeId : order) {
            Trade trade = trades.get(tradeId);
            if (trade != null) {
                orderedTrades.put(tradeId, trade);
            }
        }
        
        // Add any trades not in the order (shouldn't happen, but safety check)
        for (Map.Entry<String, Trade> entry : trades.entrySet()) {
            if (!orderedTrades.containsKey(entry.getKey())) {
                orderedTrades.put(entry.getKey(), entry.getValue());
            }
        }
        
        return orderedTrades;
    }

    /**
     * Gets the order of trades for a specific GUI.
     * 
     * @param guiId The ID of the GUI
     * @return List of trade IDs in order, or empty list if not found
     */
    public List<String> getTradeOrder(String guiId) {
        return new ArrayList<>(tradeOrders.getOrDefault(guiId, new ArrayList<>()));
    }

    /**
     * Gets the position of a trade in the order (1-based index).
     * 
     * @param guiId The ID of the GUI
     * @param tradeId The ID of the trade
     * @return The position of the trade, or -1 if not found
     */
    public int getTradePosition(String guiId, String tradeId) {
        List<String> order = tradeOrders.get(guiId);
        if (order == null) {
            return -1;
        }
        
        int index = order.indexOf(tradeId);
        return index == -1 ? -1 : index + 1; // Convert to 1-based index
    }

    /**
     * Gets a specific trade.
     * 
     * @param guiId The ID of the GUI
     * @param tradeId The ID of the trade
     * @return The trade, or null if not found
     */
    public Trade getTrade(String guiId, String tradeId) {
        Map<String, Trade> trades = tradeGUIs.get(guiId);
        return trades != null ? trades.get(tradeId) : null;
    }

    /**
     * Gets the title of a GUI.
     * 
     * @param guiId The ID of the GUI
     * @return The title, or a default title if not found
     */
    public String getGUITitle(String guiId) {
        return guiTitles.getOrDefault(guiId, "&1&l" + guiId + " Shop");
    }

    /**
     * Gets the number of rows for a GUI.
     * 
     * @param guiId The ID of the GUI
     * @return The number of rows, or 3 if not found
     */
    public int getGUIRows(String guiId) {
        return guiRows.getOrDefault(guiId, 3);
    }

    /**
     * Gets all trade GUI IDs.
     * 
     * @return Set of all GUI IDs
     */
    public Set<String> getAllTradeGUIIds() {
        return new HashSet<>(tradeGUIs.keySet());
    }

    /**
     * Checks if a trade GUI exists.
     * 
     * @param guiId The ID of the GUI to check
     * @return true if the GUI exists, false otherwise
     */
    public boolean hasTradeGUI(String guiId) {
        return tradeGUIs.containsKey(guiId);
    }

    /**
     * Validates trade items and logs NBT information.
     * Now uses enhanced NBT system for ProItems compatibility.
     * 
     * @param trade The trade to validate
     */
    private void validateTradeItems(Trade trade) {
        plugin.getLogger().fine("Validating trade: " + trade.id());
        
        // Log input items with NBT data
        for (int i = 0; i < trade.inputs().size(); i++) {
            ItemStack input = trade.inputs().get(i);
            plugin.getLogger().fine("Input " + (i + 1) + ": " + ItemUtils.getNBTSummary(input));
            
            if (ItemUtils.hasCustomNBT(input)) {
                plugin.getLogger().fine("Input " + (i + 1) + " has custom NBT data");
                
                // Log ProItems information if applicable
                if (ItemUtils.isProItem(input)) {
                    String proItemId = ItemUtils.getProItemId(input);
                    plugin.getLogger().fine("Input " + (i + 1) + " is ProItem: " + proItemId);
                }
            }
        }
        
        // Log output item with NBT data
        plugin.getLogger().fine("Output: " + ItemUtils.getNBTSummary(trade.output()));
        if (ItemUtils.hasCustomNBT(trade.output())) {
            plugin.getLogger().fine("Output has custom NBT data");
            
            // Log ProItems information if applicable
            if (ItemUtils.isProItem(trade.output())) {
                String proItemId = ItemUtils.getProItemId(trade.output());
                plugin.getLogger().fine("Output is ProItem: " + proItemId);
            }
        }
    }

    /**
     * Loads a trade from a configuration section with enhanced NBT support.
     * Uses the ultra cloning system for maximum data preservation.
     * 
     * @param tradeId The ID of the trade
     * @param section The configuration section
     * @return The loaded trade, or null if loading failed
     */
    private Trade loadTradeFromSection(String tradeId, ConfigurationSection section) {
        try {
            List<String> inputStrings = section.getStringList("input");
            String outputString = section.getString("output");
            
            if (inputStrings.isEmpty() || outputString == null) {
                plugin.getLogger().warning("Trade " + tradeId + " has missing input or output data");
                return null;
            }
            
            List<ItemStack> inputs = new ArrayList<>();
            for (String inputString : inputStrings) {
                ItemStack input = configManager.parseItemStack(inputString);
                if (input != null) {
                    // Use ultra cloning system for perfect preservation
                    ItemStack safeCopy = ItemUtils.createPerfectClone(input);
                    inputs.add(safeCopy);
                    plugin.getLogger().fine("Loaded input item: " + ItemUtils.getNBTSummary(safeCopy));
                } else {
                    plugin.getLogger().warning("Failed to parse input item: " + inputString);
                }
            }
            
            ItemStack output = configManager.parseItemStack(outputString);
            if (output == null) {
                plugin.getLogger().warning("Failed to parse output item for trade: " + tradeId);
                return null;
            }
            
            if (inputs.isEmpty()) {
                plugin.getLogger().warning("Trade " + tradeId + " has no valid input items");
                return null;
            }
            
            // Use ultra cloning system for perfect preservation of the output
            ItemStack safeOutput = ItemUtils.createPerfectClone(output);
            plugin.getLogger().fine("Loaded output item: " + ItemUtils.getNBTSummary(safeOutput));
            
            Trade trade = Trade.of(tradeId, inputs, safeOutput);
            
            // Verify trade integrity
            if (!verifyTradeIntegrity(trade, trade)) {
                plugin.getLogger().warning("Trade integrity verification failed for: " + tradeId);
            }
            
            return trade;
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error loading trade: " + tradeId, e);
            return null;
        }
    }

    /**
     * Verifies the integrity of a trade by comparing original and processed versions.
     * 
     * @param original The original trade
     * @param processed The processed trade
     * @return true if the trades are identical, false otherwise
     */
    private boolean verifyTradeIntegrity(Trade original, Trade processed) {
        if (original == null || processed == null) {
            return false;
        }
        
        // Verify inputs
        if (original.inputs().size() != processed.inputs().size()) {
            return false;
        }
        
        for (int i = 0; i < original.inputs().size(); i++) {
            ItemStack originalInput = original.inputs().get(i);
            ItemStack processedInput = processed.inputs().get(i);
            
            if (!ItemUtils.verifyCloneIntegrity(originalInput, processedInput)) {
                return false;
            }
        }
        
        // Verify output
        return ItemUtils.verifyCloneIntegrity(original.output(), processed.output());
    }

    /**
     * Gets statistics about ultra cloning usage in trades.
     * 
     * @return Map containing ultra cloning statistics
     */
    public Map<String, Integer> getUltraCloningStatistics() {
        Map<String, Integer> stats = new HashMap<>();
        int totalTrades = 0;
        int tradesWithUltraClones = 0;
        int totalUltraClones = 0;
        
        for (Map<String, Trade> guiTrades : tradeGUIs.values()) {
            for (Trade trade : guiTrades.values()) {
                totalTrades++;
                boolean tradeHasUltraClones = false;
                
                // Check input items
                for (ItemStack input : trade.inputs()) {
                    if (ItemUtils.isUltraClone(input)) {
                        totalUltraClones++;
                        tradeHasUltraClones = true;
                    }
                }
                
                // Check output item
                if (ItemUtils.isUltraClone(trade.output())) {
                    totalUltraClones++;
                    tradeHasUltraClones = true;
                }
                
                if (tradeHasUltraClones) {
                    tradesWithUltraClones++;
                }
            }
        }
        
        stats.put("totalTrades", totalTrades);
        stats.put("tradesWithUltraClones", tradesWithUltraClones);
        stats.put("totalUltraClones", totalUltraClones);
        
        return stats;
    }

    /**
     * Gets statistics about NBT usage in trades.
     * Enhanced with ultra cloning statistics.
     * 
     * @return Map containing NBT usage statistics
     */
    public Map<String, Integer> getNBTStatistics() {
        Map<String, Integer> stats = new HashMap<>();
        int totalTrades = 0;
        int tradesWithNBT = 0;
        int itemsWithNBT = 0;
        int totalItems = 0;
        int ultraClones = 0;
        
        for (Map<String, Trade> guiTrades : tradeGUIs.values()) {
            for (Trade trade : guiTrades.values()) {
                totalTrades++;
                boolean tradeHasNBT = false;
                
                // Check input items
                for (ItemStack input : trade.inputs()) {
                    totalItems++;
                    if (ItemUtils.hasCustomNBT(input)) {
                        itemsWithNBT++;
                        tradeHasNBT = true;
                    }
                    if (ItemUtils.isUltraClone(input)) {
                        ultraClones++;
                    }
                }
                
                // Check output item
                totalItems++;
                if (ItemUtils.hasCustomNBT(trade.output())) {
                    itemsWithNBT++;
                    tradeHasNBT = true;
                }
                if (ItemUtils.isUltraClone(trade.output())) {
                    ultraClones++;
                }
                
                if (tradeHasNBT) {
                    tradesWithNBT++;
                }
            }
        }
        
        stats.put("totalTrades", totalTrades);
        stats.put("tradesWithNBT", tradesWithNBT);
        stats.put("totalItems", totalItems);
        stats.put("itemsWithNBT", itemsWithNBT);
        stats.put("ultraClones", ultraClones);
        
        return stats;
    }
}