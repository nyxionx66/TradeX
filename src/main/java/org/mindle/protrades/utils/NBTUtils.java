package org.mindle.protrades.utils;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.mindle.protrades.ProTrades;
import org.mindle.protrades.nbt.NBTManager;
import org.mindle.protrades.nbt.data.NBTCompoundData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Utility class for NBT operations and ProItems integration.
 * Provides high-level NBT operations for trading systems.
 */
public class NBTUtils {
    
    /**
     * Creates a trading-safe copy of an ItemStack with full NBT preservation.
     * This is the primary method for ensuring items maintain their data during trades.
     */
    @NotNull
    public static ItemStack createTradingSafeCopy(@NotNull ItemStack original) {
        NBTManager nbtManager = ProTrades.getInstance().getNBTManager();
        return nbtManager.createTradingSafeCopy(original);
    }
    
    /**
     * Restores an ItemStack from trading-safe format back to its original state.
     */
    @Nullable
    public static ItemStack restoreFromTradingSafe(@NotNull ItemStack tradingSafe) {
        NBTManager nbtManager = ProTrades.getInstance().getNBTManager();
        return nbtManager.restoreCompleteNBTData(tradingSafe);
    }
    
    /**
     * Checks if two ItemStacks are equivalent for trading purposes.
     * Uses exact NBT matching for ProItems and custom items.
     */
    public static boolean areItemsEquivalentForTrading(@NotNull ItemStack item1, @NotNull ItemStack item2) {
        // Basic material and amount check
        if (item1.getType() != item2.getType()) {
            return false;
        }
        
        // Use NBT manager for detailed comparison
        NBTManager nbtManager = ProTrades.getInstance().getNBTManager();
        return nbtManager.areNBTEquivalent(item1, item2);
    }
    
    /**
     * Checks if an ItemStack has custom NBT data (including ProItems data).
     */
    public static boolean hasCustomNBT(@NotNull ItemStack item) {
        if (item.getType() == Material.AIR || !item.hasItemMeta()) {
            return false;
        }
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }
        
        // Check for persistent data
        PersistentDataContainer container = meta.getPersistentDataContainer();
        if (!container.getKeys().isEmpty()) {
            return true;
        }
        
        // Check for item metadata
        return meta.hasDisplayName() || meta.hasLore() || meta.hasEnchants() || 
               meta.hasAttributeModifiers() || meta.isUnbreakable() || 
               meta.hasCustomModelData();
    }
    
    /**
     * Checks if an ItemStack is a ProItem.
     */
    public static boolean isProItem(@NotNull ItemStack item) {
        NBTManager nbtManager = ProTrades.getInstance().getNBTManager();
        return nbtManager.isProItem(item);
    }
    
    /**
     * Gets the ProItem ID from an ItemStack.
     */
    @Nullable
    public static String getProItemId(@NotNull ItemStack item) {
        NBTManager nbtManager = ProTrades.getInstance().getNBTManager();
        return nbtManager.getProItemId(item);
    }
    
    /**
     * Extracts complete NBT data from an ItemStack.
     */
    @Nullable
    public static NBTCompoundData extractNBTData(@NotNull ItemStack item) {
        NBTManager nbtManager = ProTrades.getInstance().getNBTManager();
        return nbtManager.extractCompleteNBTData(item);
    }
    
    /**
     * Applies NBT data to an ItemStack.
     */
    public static boolean applyNBTData(@NotNull ItemStack item, @NotNull NBTCompoundData nbtData) {
        NBTManager nbtManager = ProTrades.getInstance().getNBTManager();
        return nbtManager.applyCompleteNBTData(item, nbtData);
    }
    
    /**
     * Creates a summary string of an item's NBT data.
     */
    @NotNull
    public static String getNBTSummary(@NotNull ItemStack item) {
        if (item.getType() == Material.AIR) {
            return "AIR";
        }
        
        StringBuilder summary = new StringBuilder();
        summary.append(item.getType().name()).append(":").append(item.getAmount());
        
        // Check if it's a ProItem
        if (isProItem(item)) {
            String proItemId = getProItemId(item);
            summary.append(" [ProItem: ").append(proItemId).append("]");
        }
        
        // Check for custom NBT
        if (hasCustomNBT(item)) {
            NBTCompoundData nbtData = extractNBTData(item);
            if (nbtData != null) {
                summary.append(" [NBT: ").append(nbtData.size()).append(" entries]");
            }
        }
        
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (meta.hasDisplayName()) {
                summary.append(" [Named]");
            }
            if (meta.hasLore()) {
                List<String> lore = meta.getLore();
                if (lore != null) {
                    summary.append(" [Lore: ").append(lore.size()).append(" lines]");
                }
            }
            if (meta.hasEnchants()) {
                summary.append(" [Enchanted: ").append(meta.getEnchants().size()).append("]");
            }
        }
        
        return summary.toString();
    }
    
    /**
     * Checks if an item has preserved NBT data.
     */
    public static boolean hasPreservedNBT(@NotNull ItemStack item) {
        NBTManager nbtManager = ProTrades.getInstance().getNBTManager();
        return nbtManager.hasPreservedNBT(item);
    }
    
    /**
     * Creates a safe copy of an ItemStack for storage/transmission.
     * This method preserves all NBT data in a way that can be restored later.
     */
    @NotNull
    public static ItemStack createStorageSafeCopy(@NotNull ItemStack original) {
        // For now, use the same method as trading-safe copy
        // This can be expanded later if different behavior is needed
        return createTradingSafeCopy(original);
    }
    
    /**
     * Restores an ItemStack from storage-safe format.
     */
    @Nullable
    public static ItemStack restoreFromStorageSafe(@NotNull ItemStack storageSafe) {
        // For now, use the same method as trading-safe restore
        // This can be expanded later if different behavior is needed
        return restoreFromTradingSafe(storageSafe);
    }
    
    /**
     * Validates that an ItemStack is safe for trading.
     * Checks for potential issues that might cause problems.
     */
    public static boolean isValidForTrading(@NotNull ItemStack item) {
        if (item.getType() == Material.AIR) {
            return false;
        }
        
        if (item.getAmount() <= 0) {
            return false;
        }
        
        // Additional validation can be added here
        return true;
    }
    
    /**
     * Creates a debug string for an ItemStack's NBT data.
     */
    @NotNull
    public static String getDebugNBTInfo(@NotNull ItemStack item) {
        StringBuilder debug = new StringBuilder();
        debug.append("=== NBT Debug Info ===\n");
        debug.append("Material: ").append(item.getType()).append("\n");
        debug.append("Amount: ").append(item.getAmount()).append("\n");
        
        if (isProItem(item)) {
            debug.append("ProItem ID: ").append(getProItemId(item)).append("\n");
        }
        
        if (hasCustomNBT(item)) {
            NBTCompoundData nbtData = extractNBTData(item);
            if (nbtData != null) {
                debug.append("NBT Data: ").append(nbtData.getSummary()).append("\n");
            }
        }
        
        if (hasPreservedNBT(item)) {
            debug.append("Has Preserved NBT: Yes\n");
        }
        
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            debug.append("Has ItemMeta: Yes\n");
            debug.append("Display Name: ").append(meta.hasDisplayName() ? "Yes" : "No").append("\n");
            debug.append("Lore: ").append(meta.hasLore() ? "Yes (" + (meta.getLore() != null ? meta.getLore().size() : 0) + " lines)" : "No").append("\n");
            debug.append("Enchantments: ").append(meta.hasEnchants() ? "Yes (" + meta.getEnchants().size() + ")" : "No").append("\n");
            debug.append("Unbreakable: ").append(meta.isUnbreakable() ? "Yes" : "No").append("\n");
            debug.append("Custom Model Data: ").append(meta.hasCustomModelData() ? "Yes (" + meta.getCustomModelData() + ")" : "No").append("\n");
            
            PersistentDataContainer container = meta.getPersistentDataContainer();
            debug.append("Persistent Data Keys: ").append(container.getKeys().size()).append("\n");
            for (org.bukkit.NamespacedKey key : container.getKeys()) {
                debug.append("  - ").append(key.toString()).append("\n");
            }
        }
        
        debug.append("====================");
        return debug.toString();
    }
}