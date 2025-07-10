package org.mindle.protrades.managers;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.mindle.protrades.ProTrades;
import org.mindle.protrades.models.Trade;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.Executor;
import java.util.logging.Level;

/**
 * Streamlined configuration manager for the new resource structure.
 * Handles trade configurations and item serialization with improved organization.
 */
public class ConfigManager {

    private final ProTrades plugin;
    private final File configsDirectory;
    private final File tradesDirectory;
    private final Executor virtualThreadExecutor;

    public ConfigManager(ProTrades plugin) {
        this.plugin = plugin;
        this.configsDirectory = new File(plugin.getDataFolder(), "configs");
        this.tradesDirectory = new File(configsDirectory, "trades");
        this.virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();
        
        createDirectories();
    }

    /**
     * Creates necessary directories for the new structure.
     */
    private void createDirectories() {
        if (!configsDirectory.exists()) {
            configsDirectory.mkdirs();
        }
        if (!tradesDirectory.exists()) {
            tradesDirectory.mkdirs();
        }
    }

    /**
     * Loads a trade configuration from file asynchronously.
     * 
     * @param tradeId The ID of the trade to load
     * @return CompletableFuture containing the trade configuration
     */
    public CompletableFuture<Map<String, Object>> loadTradeConfigAsync(String tradeId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                File file = new File(tradesDirectory, tradeId + ".yml");
                if (!file.exists()) {
                    return loadFromExampleTrades(tradeId);
                }

                FileConfiguration config = YamlConfiguration.loadConfiguration(file);
                Map<String, Object> result = new HashMap<>();
                
                result.put("title", config.getString("title", "&1&lTrade GUI"));
                result.put("rows", config.getInt("rows", 3));
                result.put("trades", config.getConfigurationSection("trades"));
                result.put("order", config.getStringList("order"));
                
                return result;
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Error loading trade config: " + tradeId, e);
                return new HashMap<>();
            }
        }, virtualThreadExecutor);
    }

    /**
     * Loads trade configuration from example trades file.
     */
    private Map<String, Object> loadFromExampleTrades(String tradeId) {
        try {
            File exampleFile = new File(plugin.getDataFolder(), "configs/example_trades.yml");
            if (!exampleFile.exists()) {
                return new HashMap<>();
            }

            FileConfiguration config = YamlConfiguration.loadConfiguration(exampleFile);
            if (config.contains(tradeId)) {
                Map<String, Object> result = new HashMap<>();
                result.put("title", config.getString(tradeId + ".title", "&1&lTrade GUI"));
                result.put("rows", config.getInt(tradeId + ".rows", 3));
                result.put("trades", config.getConfigurationSection(tradeId + ".trades"));
                result.put("order", config.getStringList(tradeId + ".order"));
                return result;
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error loading example trade: " + tradeId, e);
        }
        return new HashMap<>();
    }

    /**
     * Saves a trade configuration to file asynchronously.
     * 
     * @param tradeId The ID of the trade to save
     * @param title The GUI title
     * @param rows The number of rows in the GUI
     * @param trades Map of trades to save
     * @param order List of trade IDs in order
     * @return CompletableFuture indicating completion
     */
    public CompletableFuture<Boolean> saveTradeConfigAsync(String tradeId, String title, int rows, 
                                                          Map<String, Trade> trades, List<String> order) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                File file = new File(tradesDirectory, tradeId + ".yml");
                FileConfiguration config = new YamlConfiguration();
                
                config.set("title", title);
                config.set("rows", rows);
                config.set("created", System.currentTimeMillis());
                config.set("version", plugin.getDescription().getVersion());
                
                // Save trade order
                if (order != null && !order.isEmpty()) {
                    config.set("order", order);
                }
                
                // Save trades with full NBT data
                for (Map.Entry<String, Trade> entry : trades.entrySet()) {
                    String tradePath = "trades." + entry.getKey();
                    Trade trade = entry.getValue();
                    
                    // Save inputs with NBT data
                    List<String> inputStrings = new ArrayList<>();
                    for (ItemStack input : trade.inputs()) {
                        String serializedItem = serializeItemStack(input);
                        if (serializedItem != null) {
                            inputStrings.add(serializedItem);
                        }
                    }
                    config.set(tradePath + ".input", inputStrings);
                    
                    // Save output with NBT data
                    String serializedOutput = serializeItemStack(trade.output());
                    if (serializedOutput != null) {
                        config.set(tradePath + ".output", serializedOutput);
                    }
                    
                    // Save metadata
                    config.set(tradePath + ".metadata.created", System.currentTimeMillis());
                    config.set(tradePath + ".metadata.id", entry.getKey());
                }
                
                config.save(file);
                plugin.getLogger().info("Saved trade configuration: " + tradeId);
                return true;
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Error saving trade config: " + tradeId, e);
                return false;
            }
        }, virtualThreadExecutor);
    }

    /**
     * Overloaded method for backward compatibility.
     */
    public CompletableFuture<Boolean> saveTradeConfigAsync(String tradeId, String title, int rows, Map<String, Trade> trades) {
        return saveTradeConfigAsync(tradeId, title, rows, trades, new ArrayList<>());
    }

    /**
     * Loads all trade files from the trades directory.
     * 
     * @return List of trade file names (without .yml extension)
     */
    public List<String> getAllTradeIds() {
        List<String> tradeIds = new ArrayList<>();
        
        // Load from trades directory
        File[] files = tradesDirectory.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files != null) {
            for (File file : files) {
                String fileName = file.getName();
                tradeIds.add(fileName.substring(0, fileName.length() - 4)); // Remove .yml extension
            }
        }
        
        // Load from example trades
        try {
            File exampleFile = new File(plugin.getDataFolder(), "configs/example_trades.yml");
            if (exampleFile.exists()) {
                FileConfiguration config = YamlConfiguration.loadConfiguration(exampleFile);
                for (String key : config.getKeys(false)) {
                    if (!tradeIds.contains(key)) {
                        tradeIds.add(key);
                    }
                }
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error loading example trade IDs", e);
        }
        
        return tradeIds;
    }

    /**
     * Deletes a trade configuration file.
     * 
     * @param tradeId The ID of the trade to delete
     * @return true if the file was deleted successfully, false otherwise
     */
    public boolean deleteTradeConfig(String tradeId) {
        File file = new File(tradesDirectory, tradeId + ".yml");
        return file.exists() && file.delete();
    }

    /**
     * Creates a default trade configuration file using templates.
     * 
     * @param tradeId The ID of the trade to create
     * @return CompletableFuture indicating completion
     */
    public CompletableFuture<Boolean> createDefaultTradeConfig(String tradeId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                File file = new File(tradesDirectory, tradeId + ".yml");
                if (file.exists()) {
                    return false; // Already exists
                }

                FileConfiguration config = new YamlConfiguration();
                config.set("title", "&1&l" + tradeId + " Shop");
                config.set("rows", 3);
                config.set("trades", new HashMap<>());
                config.set("order", new ArrayList<>());
                config.set("created", System.currentTimeMillis());
                config.set("version", plugin.getDescription().getVersion());
                config.set("auto_generated", true);
                
                config.save(file);
                plugin.getLogger().info("Created default trade config: " + tradeId);
                return true;
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Error creating default trade config: " + tradeId, e);
                return false;
            }
        }, virtualThreadExecutor);
    }

    /**
     * Serializes an ItemStack to a Base64 string with full NBT data preservation.
     * 
     * @param item The ItemStack to serialize
     * @return Base64 encoded string representation of the item, or null if serialization fails
     */
    public String serializeItemStack(ItemStack item) {
        if (item == null) {
            return null;
        }
        
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);
            
            dataOutput.writeObject(item);
            dataOutput.close();
            
            return Base64Coder.encodeLines(outputStream.toByteArray());
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to serialize ItemStack: " + e.getMessage(), e);
            return null;
        }
    }

    /**
     * Deserializes an ItemStack from a Base64 string with full NBT data restoration.
     * 
     * @param data The Base64 encoded string
     * @return The deserialized ItemStack, or null if deserialization fails
     */
    public ItemStack deserializeItemStack(String data) {
        if (data == null || data.trim().isEmpty()) {
            return null;
        }
        
        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64Coder.decodeLines(data));
            BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);
            
            ItemStack item = (ItemStack) dataInput.readObject();
            dataInput.close();
            
            return item;
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to deserialize ItemStack: " + e.getMessage(), e);
            return null;
        }
    }

    /**
     * Enhanced method to parse ItemStack from various formats.
     * 
     * @param itemString The string representation of the item
     * @return The parsed ItemStack, or null if parsing failed
     */
    public ItemStack parseItemStack(String itemString) {
        if (itemString == null || itemString.trim().isEmpty()) {
            return null;
        }
        
        // Check if it's the old format (MATERIAL:AMOUNT)
        if (itemString.contains(":") && !itemString.contains("=") && itemString.split(":").length == 2) {
            return parseLegacyItemStack(itemString);
        }
        
        // Check if it's an ItemX reference (item_id:amount)
        if (itemString.contains(":") && plugin.getItemManager() != null) {
            String[] parts = itemString.split(":");
            if (parts.length == 2) {
                try {
                    int amount = Integer.parseInt(parts[1]);
                    ItemStack itemxItem = plugin.getItemManager().createItemStack(parts[0]);
                    if (itemxItem != null) {
                        itemxItem.setAmount(amount);
                        return itemxItem;
                    }
                } catch (NumberFormatException e) {
                    // Fall through to other parsing methods
                }
            }
        }
        
        // Try to deserialize as Base64 NBT data
        ItemStack nbtItem = deserializeItemStack(itemString);
        if (nbtItem != null) {
            return nbtItem;
        }
        
        // If all else fails, try legacy format
        return parseLegacyItemStack(itemString);
    }

    /**
     * Parses an ItemStack from the legacy string format.
     * Format: "MATERIAL:AMOUNT"
     * 
     * @param itemString The legacy string representation
     * @return The parsed ItemStack, or null if parsing failed
     */
    private ItemStack parseLegacyItemStack(String itemString) {
        try {
            String[] parts = itemString.split(":");
            if (parts.length != 2) {
                return null;
            }
            
            org.bukkit.Material material = org.bukkit.Material.matchMaterial(parts[0]);
            if (material == null) {
                return null;
            }
            
            int amount = Integer.parseInt(parts[1]);
            return new ItemStack(material, amount);
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to parse legacy ItemStack: " + itemString, e);
            return null;
        }
    }

    /**
     * Gets the configs directory.
     */
    public File getConfigsDirectory() {
        return configsDirectory;
    }

    /**
     * Gets the trades directory.
     */
    public File getTradesDirectory() {
        return tradesDirectory;
    }

    /**
     * Validates the configuration structure.
     */
    public boolean validateConfiguration() {
        try {
            // Check if required directories exist
            if (!configsDirectory.exists() || !tradesDirectory.exists()) {
                plugin.getLogger().warning("Required directories missing, creating them...");
                createDirectories();
            }
            
            // Check if example trades file exists
            File exampleFile = new File(plugin.getDataFolder(), "configs/example_trades.yml");
            if (!exampleFile.exists()) {
                plugin.getLogger().warning("Example trades file not found: " + exampleFile.getPath());
                return false;
            }
            
            plugin.getLogger().info("Configuration structure validated successfully");
            return true;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error validating configuration", e);
            return false;
        }
    }
}