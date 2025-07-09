package org.mindle.protrades.nbt;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.mindle.protrades.ProTrades;
import org.mindle.protrades.nbt.data.NBTCompoundData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Enhanced NBT Manager for ProTrades that preserves ProItems NBT data and provides
 * comprehensive NBT handling for trading systems without conflicts.
 */
public class NBTManager {
    
    private final ProTrades plugin;
    private final Map<String, NBTCompoundData> nbtCache;
    private final NamespacedKey proTradesKey;
    private final NamespacedKey nbtPreservedKey;
    private final NamespacedKey originalNBTKey;
    private final NamespacedKey tradeMetadataKey;
    
    // ProItems integration keys
    private final NamespacedKey proItemsKey;
    private final NamespacedKey proItemsIdKey;
    
    public NBTManager(ProTrades plugin) {
        this.plugin = plugin;
        this.nbtCache = new ConcurrentHashMap<>();
        
        // ProTrades specific keys
        this.proTradesKey = new NamespacedKey(plugin, "protrades");
        this.nbtPreservedKey = new NamespacedKey(plugin, "nbt_preserved");
        this.originalNBTKey = new NamespacedKey(plugin, "original_nbt");
        this.tradeMetadataKey = new NamespacedKey(plugin, "trade_metadata");
        
        // ProItems integration keys (read-only, never modify)
        this.proItemsKey = new NamespacedKey("proitems", "custom_item_id");
        this.proItemsIdKey = new NamespacedKey("proitems", "custom_item_id");
    }
    
    /**
     * Creates a trading-safe copy of an ItemStack with complete NBT preservation.
     * This method ensures that ProItems NBT data is never modified or lost during trades.
     */
    @NotNull
    public ItemStack createTradingSafeCopy(@NotNull ItemStack original) {
        if (original.getType() == Material.AIR) {
            return new ItemStack(Material.AIR);
        }
        
        // Create a deep clone to avoid any reference issues
        ItemStack copy = original.clone();
        
        if (!copy.hasItemMeta()) {
            return copy;
        }
        
        ItemMeta meta = copy.getItemMeta();
        if (meta == null) {
            return copy;
        }
        
        PersistentDataContainer container = meta.getPersistentDataContainer();
        
        // Extract complete NBT data for preservation
        NBTCompoundData nbtData = extractCompleteNBTData(original);
        
        if (nbtData != null && !nbtData.isEmpty()) {
            // Mark as NBT preserved
            container.set(nbtPreservedKey, PersistentDataType.BYTE, (byte) 1);
            
            // Store original NBT data in compressed format
            String serializedNBT = serializeNBTData(nbtData);
            if (serializedNBT != null) {
                container.set(originalNBTKey, PersistentDataType.STRING, serializedNBT);
            }
            
            // Add trade metadata
            long timestamp = System.currentTimeMillis();
            container.set(tradeMetadataKey, PersistentDataType.LONG, timestamp);
            
            copy.setItemMeta(meta);
            
            plugin.getLogger().fine("Created trading-safe copy with preserved NBT data for: " + 
                    original.getType() + " (ProItems compatible)");
        }
        
        return copy;
    }
    
    /**
     * Extracts complete NBT data from an ItemStack, including ProItems data.
     */
    @Nullable
    public NBTCompoundData extractCompleteNBTData(@NotNull ItemStack item) {
        if (item.getType() == Material.AIR || !item.hasItemMeta()) {
            return null;
        }
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return null;
        }
        
        NBTCompoundData nbtData = new NBTCompoundData();
        PersistentDataContainer container = meta.getPersistentDataContainer();
        
        // Extract all persistent data, preserving ProItems data
        for (NamespacedKey key : container.getKeys()) {
            try {
                extractPersistentDataByType(container, key, nbtData);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to extract NBT data for key: " + key + " - " + e.getMessage());
            }
        }
        
        // Store item metadata
        storeItemMetadata(item, nbtData);
        
        return nbtData.isEmpty() ? null : nbtData;
    }
    
    /**
     * Extracts persistent data by trying different data types.
     */
    private void extractPersistentDataByType(PersistentDataContainer container, NamespacedKey key, NBTCompoundData nbtData) {
        String keyString = key.toString();
        
        // Try different data types in order of most common to least common
        if (container.has(key, PersistentDataType.STRING)) {
            String value = container.get(key, PersistentDataType.STRING);
            nbtData.setString(keyString, value);
        } else if (container.has(key, PersistentDataType.INTEGER)) {
            Integer value = container.get(key, PersistentDataType.INTEGER);
            nbtData.setInteger(keyString, value);
        } else if (container.has(key, PersistentDataType.DOUBLE)) {
            Double value = container.get(key, PersistentDataType.DOUBLE);
            nbtData.setDouble(keyString, value);
        } else if (container.has(key, PersistentDataType.BYTE)) {
            Byte value = container.get(key, PersistentDataType.BYTE);
            nbtData.setByte(keyString, value);
        } else if (container.has(key, PersistentDataType.LONG)) {
            Long value = container.get(key, PersistentDataType.LONG);
            nbtData.setLong(keyString, value);
        } else if (container.has(key, PersistentDataType.FLOAT)) {
            Float value = container.get(key, PersistentDataType.FLOAT);
            nbtData.setFloat(keyString, value);
        } else if (container.has(key, PersistentDataType.SHORT)) {
            Short value = container.get(key, PersistentDataType.SHORT);
            nbtData.setShort(keyString, value);
        } else if (container.has(key, PersistentDataType.BYTE_ARRAY)) {
            byte[] value = container.get(key, PersistentDataType.BYTE_ARRAY);
            nbtData.setByteArray(keyString, value);
        } else if (container.has(key, PersistentDataType.INTEGER_ARRAY)) {
            int[] value = container.get(key, PersistentDataType.INTEGER_ARRAY);
            nbtData.setIntegerArray(keyString, value);
        } else if (container.has(key, PersistentDataType.LONG_ARRAY)) {
            long[] value = container.get(key, PersistentDataType.LONG_ARRAY);
            nbtData.setLongArray(keyString, value);
        }
    }
    
    /**
     * Stores additional item metadata in NBT data.
     */
    private void storeItemMetadata(ItemStack item, NBTCompoundData nbtData) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        
        // Store display name
        if (meta.hasDisplayName()) {
            nbtData.setString("display_name", meta.getDisplayName());
        }
        
        // Store lore
        if (meta.hasLore() && meta.getLore() != null) {
            List<String> lore = meta.getLore();
            nbtData.setStringList("lore", lore);
        }
        
        // Store enchantments
        if (meta.hasEnchants()) {
            Map<String, Integer> enchants = new HashMap<>();
            meta.getEnchants().forEach((enchant, level) -> 
                enchants.put(enchant.getKey().toString(), level));
            nbtData.setStringIntegerMap("enchantments", enchants);
        }
        
        // Store item flags
        if (!meta.getItemFlags().isEmpty()) {
            Set<String> flags = new HashSet<>();
            meta.getItemFlags().forEach(flag -> flags.add(flag.name()));
            nbtData.setStringSet("item_flags", flags);
        }
        
        // Store other metadata
        if (meta.hasCustomModelData()) {
            nbtData.setInteger("custom_model_data", meta.getCustomModelData());
        }
        
        if (meta.isUnbreakable()) {
            nbtData.setBoolean("unbreakable", true);
        }
    }
    
    /**
     * Applies complete NBT data to an ItemStack, restoring all properties.
     */
    public boolean applyCompleteNBTData(@NotNull ItemStack item, @NotNull NBTCompoundData nbtData) {
        if (item.getType() == Material.AIR) {
            return false;
        }
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }
        
        try {
            // Apply persistent data
            PersistentDataContainer container = meta.getPersistentDataContainer();
            applyPersistentData(container, nbtData);
            
            // Apply item metadata
            applyItemMetadata(meta, nbtData);
            
            item.setItemMeta(meta);
            return true;
            
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to apply complete NBT data: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Applies persistent data to the container.
     */
    private void applyPersistentData(PersistentDataContainer container, NBTCompoundData nbtData) {
        for (Map.Entry<String, Object> entry : nbtData.getAllData().entrySet()) {
            String keyStr = entry.getKey();
            Object value = entry.getValue();
            
            // Skip metadata keys as they're handled separately
            if (isMetadataKey(keyStr)) {
                continue;
            }
            
            // Parse the key
            NamespacedKey key = parseNamespacedKey(keyStr);
            if (key == null) continue;
            
            // Apply based on value type
            applyValueByType(container, key, value);
        }
    }
    
    /**
     * Checks if a key is a metadata key (not persistent data).
     */
    private boolean isMetadataKey(String key) {
        return key.equals("display_name") || key.equals("lore") || 
               key.equals("enchantments") || key.equals("item_flags") ||
               key.equals("custom_model_data") || key.equals("unbreakable");
    }
    
    /**
     * Parses a string key into a NamespacedKey.
     */
    @Nullable
    private NamespacedKey parseNamespacedKey(String keyStr) {
        try {
            if (keyStr.contains(":")) {
                String[] parts = keyStr.split(":", 2);
                return new NamespacedKey(parts[0], parts[1]);
            } else {
                return new NamespacedKey(plugin, keyStr);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to parse NamespacedKey: " + keyStr);
            return null;
        }
    }
    
    /**
     * Applies a value to the container based on its type.
     */
    private void applyValueByType(PersistentDataContainer container, NamespacedKey key, Object value) {
        try {
            if (value instanceof String) {
                container.set(key, PersistentDataType.STRING, (String) value);
            } else if (value instanceof Integer) {
                container.set(key, PersistentDataType.INTEGER, (Integer) value);
            } else if (value instanceof Double) {
                container.set(key, PersistentDataType.DOUBLE, (Double) value);
            } else if (value instanceof Byte) {
                container.set(key, PersistentDataType.BYTE, (Byte) value);
            } else if (value instanceof Long) {
                container.set(key, PersistentDataType.LONG, (Long) value);
            } else if (value instanceof Float) {
                container.set(key, PersistentDataType.FLOAT, (Float) value);
            } else if (value instanceof Short) {
                container.set(key, PersistentDataType.SHORT, (Short) value);
            } else if (value instanceof byte[]) {
                container.set(key, PersistentDataType.BYTE_ARRAY, (byte[]) value);
            } else if (value instanceof int[]) {
                container.set(key, PersistentDataType.INTEGER_ARRAY, (int[]) value);
            } else if (value instanceof long[]) {
                container.set(key, PersistentDataType.LONG_ARRAY, (long[]) value);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to apply value for key " + key + ": " + e.getMessage());
        }
    }
    
    /**
     * Applies item metadata from NBT data.
     */
    private void applyItemMetadata(ItemMeta meta, NBTCompoundData nbtData) {
        // Apply display name
        String displayName = nbtData.getString("display_name");
        if (displayName != null) {
            meta.setDisplayName(displayName);
        }
        
        // Apply lore
        List<String> lore = nbtData.getStringList("lore");
        if (lore != null && !lore.isEmpty()) {
            meta.setLore(lore);
        }
        
        // Apply enchantments
        Map<String, Integer> enchants = nbtData.getStringIntegerMap("enchantments");
        if (enchants != null) {
            for (Map.Entry<String, Integer> entry : enchants.entrySet()) {
                try {
                    org.bukkit.enchantments.Enchantment enchant = 
                        org.bukkit.enchantments.Enchantment.getByKey(
                            org.bukkit.NamespacedKey.fromString(entry.getKey()));
                    if (enchant != null) {
                        meta.addEnchant(enchant, entry.getValue(), true);
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to apply enchantment: " + entry.getKey());
                }
            }
        }
        
        // Apply item flags
        Set<String> flags = nbtData.getStringSet("item_flags");
        if (flags != null) {
            for (String flagName : flags) {
                try {
                    org.bukkit.inventory.ItemFlag flag = org.bukkit.inventory.ItemFlag.valueOf(flagName);
                    meta.addItemFlags(flag);
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to apply item flag: " + flagName);
                }
            }
        }
        
        // Apply custom model data
        Integer customModelData = nbtData.getInteger("custom_model_data");
        if (customModelData != null) {
            meta.setCustomModelData(customModelData);
        }
        
        // Apply unbreakable
        Boolean unbreakable = nbtData.getBoolean("unbreakable");
        if (unbreakable != null && unbreakable) {
            meta.setUnbreakable(true);
        }
    }
    
    /**
     * Checks if an ItemStack has preserved NBT data.
     */
    public boolean hasPreservedNBT(@NotNull ItemStack item) {
        if (item.getType() == Material.AIR || !item.hasItemMeta()) {
            return false;
        }
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }
        
        PersistentDataContainer container = meta.getPersistentDataContainer();
        return container.has(nbtPreservedKey, PersistentDataType.BYTE) && 
               container.has(originalNBTKey, PersistentDataType.STRING);
    }
    
    /**
     * Restores complete NBT data from a preserved ItemStack.
     */
    @Nullable
    public ItemStack restoreCompleteNBTData(@NotNull ItemStack item) {
        if (!hasPreservedNBT(item)) {
            return item; // Return as-is if no preserved data
        }
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        
        PersistentDataContainer container = meta.getPersistentDataContainer();
        String serializedData = container.get(originalNBTKey, PersistentDataType.STRING);
        
        if (serializedData == null) {
            return item;
        }
        
        NBTCompoundData nbtData = deserializeNBTData(serializedData);
        if (nbtData == null) {
            return item;
        }
        
        // Create a fresh copy of the item
        ItemStack restored = new ItemStack(item.getType(), item.getAmount());
        
        if (applyCompleteNBTData(restored, nbtData)) {
            plugin.getLogger().fine("Restored complete NBT data for: " + item.getType());
            return restored;
        }
        
        return item;
    }
    
    /**
     * Checks if an ItemStack is a ProItem.
     */
    public boolean isProItem(@NotNull ItemStack item) {
        if (item.getType() == Material.AIR || !item.hasItemMeta()) {
            return false;
        }
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }
        
        PersistentDataContainer container = meta.getPersistentDataContainer();
        return container.has(proItemsKey, PersistentDataType.STRING) ||
               container.has(proItemsIdKey, PersistentDataType.STRING);
    }
    
    /**
     * Gets the ProItem ID from an ItemStack.
     */
    @Nullable
    public String getProItemId(@NotNull ItemStack item) {
        if (!isProItem(item)) {
            return null;
        }
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return null;
        }
        
        PersistentDataContainer container = meta.getPersistentDataContainer();
        
        // Try both possible keys
        String id = container.get(proItemsKey, PersistentDataType.STRING);
        if (id == null) {
            id = container.get(proItemsIdKey, PersistentDataType.STRING);
        }
        
        return id;
    }
    
    /**
     * Checks if two ItemStacks are NBT-equivalent for trading purposes.
     * This method provides exact NBT matching for ProItems and other custom items.
     */
    public boolean areNBTEquivalent(@NotNull ItemStack item1, @NotNull ItemStack item2) {
        if (item1.getType() != item2.getType()) {
            return false;
        }
        
        // Extract NBT data from both items
        NBTCompoundData nbt1 = extractCompleteNBTData(item1);
        NBTCompoundData nbt2 = extractCompleteNBTData(item2);
        
        // If both have no NBT data, they're equivalent
        if ((nbt1 == null || nbt1.isEmpty()) && (nbt2 == null || nbt2.isEmpty())) {
            return true;
        }
        
        // If only one has NBT data, they're not equivalent
        if ((nbt1 == null || nbt1.isEmpty()) || (nbt2 == null || nbt2.isEmpty())) {
            return false;
        }
        
        // Compare NBT data
        return nbt1.equals(nbt2);
    }
    
    /**
     * Serializes NBT data to a compressed string.
     */
    @Nullable
    private String serializeNBTData(@NotNull NBTCompoundData nbtData) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             GZIPOutputStream gzipOut = new GZIPOutputStream(baos);
             ObjectOutputStream oos = new ObjectOutputStream(gzipOut)) {
            
            oos.writeObject(nbtData);
            oos.flush();
            gzipOut.finish();
            
            return Base64.getEncoder().encodeToString(baos.toByteArray());
            
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to serialize NBT data: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Deserializes NBT data from a compressed string.
     */
    @Nullable
    private NBTCompoundData deserializeNBTData(@NotNull String serializedData) {
        try {
            byte[] data = Base64.getDecoder().decode(serializedData);
            
            try (ByteArrayInputStream bais = new ByteArrayInputStream(data);
                 GZIPInputStream gzipIn = new GZIPInputStream(bais);
                 ObjectInputStream ois = new ObjectInputStream(gzipIn)) {
                
                return (NBTCompoundData) ois.readObject();
                
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to deserialize NBT data: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Caches NBT data for performance.
     */
    public void cacheNBTData(@NotNull String id, @NotNull NBTCompoundData data) {
        nbtCache.put(id, data);
    }
    
    /**
     * Retrieves cached NBT data.
     */
    @Nullable
    public NBTCompoundData getCachedNBTData(@NotNull String id) {
        return nbtCache.get(id);
    }
    
    /**
     * Clears NBT cache.
     */
    public void clearCache() {
        nbtCache.clear();
        plugin.getLogger().info("NBT cache cleared");
    }
    
    /**
     * Gets cache statistics.
     */
    public Map<String, Object> getCacheStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("cache_size", nbtCache.size());
        stats.put("cache_keys", new ArrayList<>(nbtCache.keySet()));
        return stats;
    }
}