package org.mindle.protrades.itemx;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.mindle.protrades.ProTrades;
import org.mindle.protrades.utils.NBTUtils;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * NBT utility class specifically for ItemX items.
 * Extends the existing NBTUtils functionality for ItemX-specific operations.
 */
public class NBTUtil {
    
    private static final String ITEMX_NAMESPACE = "itemx";
    
    /**
     * Creates an ItemX-specific NBT key.
     */
    public static NamespacedKey createItemXKey(String key) {
        return new NamespacedKey(ITEMX_NAMESPACE, key);
    }
    
    /**
     * Creates an ItemX-specific NBT key with custom namespace.
     */
    public static NamespacedKey createItemXKey(String namespace, String key) {
        return new NamespacedKey(namespace, key);
    }
    
    /**
     * Sets ItemX identification NBT data on an ItemStack.
     */
    public static void setItemXId(ItemStack item, String itemId) {
        if (item == null || !item.hasItemMeta()) {
            return;
        }
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }
        
        PersistentDataContainer container = meta.getPersistentDataContainer();
        NamespacedKey key = createItemXKey("id");
        container.set(key, PersistentDataType.STRING, itemId);
        
        item.setItemMeta(meta);
    }
    
    /**
     * Gets the ItemX ID from an ItemStack.
     */
    @Nullable
    public static String getItemXId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return null;
        }
        
        PersistentDataContainer container = meta.getPersistentDataContainer();
        NamespacedKey key = createItemXKey("id");
        
        return container.get(key, PersistentDataType.STRING);
    }
    
    /**
     * Checks if an ItemStack is an ItemX item.
     */
    public static boolean isItemXItem(ItemStack item) {
        return getItemXId(item) != null;
    }
    
    /**
     * Sets custom NBT data on an ItemStack.
     */
    public static void setCustomNBTData(ItemStack item, String key, Object value) {
        if (item == null || !item.hasItemMeta()) {
            return;
        }
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }
        
        PersistentDataContainer container = meta.getPersistentDataContainer();
        NamespacedKey nbtKey = createItemXKey(key);
        
        try {
            if (value instanceof String) {
                container.set(nbtKey, PersistentDataType.STRING, (String) value);
            } else if (value instanceof Integer) {
                container.set(nbtKey, PersistentDataType.INTEGER, (Integer) value);
            } else if (value instanceof Double) {
                container.set(nbtKey, PersistentDataType.DOUBLE, (Double) value);
            } else if (value instanceof Boolean) {
                container.set(nbtKey, PersistentDataType.BYTE, (Boolean) value ? (byte) 1 : (byte) 0);
            } else if (value instanceof Long) {
                container.set(nbtKey, PersistentDataType.LONG, (Long) value);
            } else if (value instanceof Float) {
                container.set(nbtKey, PersistentDataType.FLOAT, (Float) value);
            } else {
                container.set(nbtKey, PersistentDataType.STRING, value.toString());
            }
            
            item.setItemMeta(meta);
        } catch (Exception e) {
            ProTrades.getInstance().getLogger().warning("Failed to set custom NBT data: " + key + " = " + value);
        }
    }
    
    /**
     * Gets custom NBT data from an ItemStack.
     */
    @Nullable
    public static Object getCustomNBTData(ItemStack item, String key, Class<?> type) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return null;
        }
        
        PersistentDataContainer container = meta.getPersistentDataContainer();
        NamespacedKey nbtKey = createItemXKey(key);
        
        try {
            if (type == String.class) {
                return container.get(nbtKey, PersistentDataType.STRING);
            } else if (type == Integer.class) {
                return container.get(nbtKey, PersistentDataType.INTEGER);
            } else if (type == Double.class) {
                return container.get(nbtKey, PersistentDataType.DOUBLE);
            } else if (type == Boolean.class) {
                Byte byteValue = container.get(nbtKey, PersistentDataType.BYTE);
                return byteValue != null ? byteValue == 1 : null;
            } else if (type == Long.class) {
                return container.get(nbtKey, PersistentDataType.LONG);
            } else if (type == Float.class) {
                return container.get(nbtKey, PersistentDataType.FLOAT);
            }
        } catch (Exception e) {
            ProTrades.getInstance().getLogger().warning("Failed to get custom NBT data: " + key);
        }
        
        return null;
    }
    
    /**
     * Gets all custom NBT data from an ItemX item.
     */
    public static Map<String, Object> getAllCustomNBTData(ItemStack item) {
        Map<String, Object> data = new HashMap<>();
        
        if (item == null || !item.hasItemMeta()) {
            return data;
        }
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return data;
        }
        
        PersistentDataContainer container = meta.getPersistentDataContainer();
        Set<NamespacedKey> keys = container.getKeys();
        
        for (NamespacedKey key : keys) {
            if (ITEMX_NAMESPACE.equals(key.getNamespace())) {
                String keyName = key.getKey();
                
                // Try to get the value with different types
                Object value = tryGetValue(container, key);
                if (value != null) {
                    data.put(keyName, value);
                }
            }
        }
        
        return data;
    }
    
    /**
     * Tries to get a value from the container with different data types.
     */
    private static Object tryGetValue(PersistentDataContainer container, NamespacedKey key) {
        try {
            if (container.has(key, PersistentDataType.STRING)) {
                return container.get(key, PersistentDataType.STRING);
            } else if (container.has(key, PersistentDataType.INTEGER)) {
                return container.get(key, PersistentDataType.INTEGER);
            } else if (container.has(key, PersistentDataType.DOUBLE)) {
                return container.get(key, PersistentDataType.DOUBLE);
            } else if (container.has(key, PersistentDataType.BYTE)) {
                Byte byteValue = container.get(key, PersistentDataType.BYTE);
                return byteValue != null ? byteValue == 1 : null;
            } else if (container.has(key, PersistentDataType.LONG)) {
                return container.get(key, PersistentDataType.LONG);
            } else if (container.has(key, PersistentDataType.FLOAT)) {
                return container.get(key, PersistentDataType.FLOAT);
            }
        } catch (Exception e) {
            ProTrades.getInstance().getLogger().warning("Failed to get value for key: " + key);
        }
        
        return null;
    }
    
    /**
     * Removes custom NBT data from an ItemStack.
     */
    public static void removeCustomNBTData(ItemStack item, String key) {
        if (item == null || !item.hasItemMeta()) {
            return;
        }
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }
        
        PersistentDataContainer container = meta.getPersistentDataContainer();
        NamespacedKey nbtKey = createItemXKey(key);
        
        container.remove(nbtKey);
        item.setItemMeta(meta);
    }
    
    /**
     * Clears all ItemX custom NBT data from an ItemStack.
     */
    public static void clearItemXNBTData(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return;
        }
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }
        
        PersistentDataContainer container = meta.getPersistentDataContainer();
        Set<NamespacedKey> keys = container.getKeys();
        
        // Remove all ItemX keys
        for (NamespacedKey key : keys) {
            if (ITEMX_NAMESPACE.equals(key.getNamespace())) {
                container.remove(key);
            }
        }
        
        item.setItemMeta(meta);
    }
    
    /**
     * Creates a copy of an ItemStack with preserved ItemX NBT data.
     */
    public static ItemStack createItemXSafeCopy(ItemStack original) {
        if (original == null) {
            return null;
        }
        
        // Use the existing NBT system for creating a safe copy
        ItemStack copy = NBTUtils.createTradingSafeCopy(original);
        
        // Ensure ItemX NBT data is preserved
        if (isItemXItem(original)) {
            String itemId = getItemXId(original);
            if (itemId != null) {
                setItemXId(copy, itemId);
            }
            
            // Copy custom NBT data
            Map<String, Object> customData = getAllCustomNBTData(original);
            for (Map.Entry<String, Object> entry : customData.entrySet()) {
                setCustomNBTData(copy, entry.getKey(), entry.getValue());
            }
        }
        
        return copy;
    }
    
    /**
     * Validates that an ItemStack is a valid ItemX item.
     */
    public static boolean validateItemXItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        
        // Check if it has ItemX ID
        String itemId = getItemXId(item);
        if (itemId == null || itemId.isEmpty()) {
            return false;
        }
        
        // Additional validation can be added here
        return true;
    }
    
    /**
     * Gets a summary of an ItemX item's NBT data.
     */
    public static String getItemXNBTSummary(ItemStack item) {
        if (!isItemXItem(item)) {
            return "Not an ItemX item";
        }
        
        StringBuilder summary = new StringBuilder();
        summary.append("ItemX Item: ").append(getItemXId(item)).append("\n");
        
        Map<String, Object> customData = getAllCustomNBTData(item);
        if (!customData.isEmpty()) {
            summary.append("Custom NBT Data:\n");
            for (Map.Entry<String, Object> entry : customData.entrySet()) {
                summary.append("  ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
            }
        }
        
        return summary.toString();
    }
}