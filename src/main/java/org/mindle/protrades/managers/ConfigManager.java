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
 * Manages YML configuration files for trades with full NBT support and trade ordering.
 * Uses Java 21 virtual threads for I/O operations and Base64 serialization for ItemStacks.
 */
public class ConfigManager {

    private final ProTrades plugin;
    private final File tradesDirectory;
    private final Executor virtualThreadExecutor;

    public ConfigManager(ProTrades plugin) {
        this.plugin = plugin;
        this.tradesDirectory = new File(plugin.getDataFolder(), "trades");
        this.virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();
        
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
                    return new HashMap<>();
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
     * Saves a trade configuration to file asynchronously with full NBT support and trade ordering.
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
                }
                
                config.save(file);
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
        
        File[] files = tradesDirectory.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files != null) {
            for (File file : files) {
                String fileName = file.getName();
                tradeIds.add(fileName.substring(0, fileName.length() - 4)); // Remove .yml extension
            }
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
     * Creates a default trade configuration file.
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
                
                config.save(file);
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
     * Legacy method for backward compatibility - attempts to parse old format first,
     * then falls back to NBT deserialization.
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
        
        // Try to deserialize as Base64 NBT data
        ItemStack nbtItem = deserializeItemStack(itemString);
        if (nbtItem != null) {
            return nbtItem;
        }
        
        // If both fail, try legacy format as fallback
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
     * Converts an ItemStack to a string representation with full NBT support.
     * Uses Base64 serialization for complete data preservation.
     * 
     * @param item The ItemStack to convert
     * @return String representation of the item with NBT data
     */
    private String itemStackToString(ItemStack item) {
        return serializeItemStack(item);
    }
}