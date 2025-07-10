package org.mindle.protrades.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.mindle.protrades.utils.NBTUtils;

import java.util.List;
import java.util.Map;

/**
 * Enhanced utility class for item-related operations with full NBT support.
 * Now integrates with the NBT management system for ProItems compatibility.
 */
public class ItemUtils {

    /**
     * Gets the display name of an item, or the material name if no display name is set.
     * 
     * @param item The item to get the display name from
     * @return The display name as a string
     */
    public static String getDisplayName(ItemStack item) {
        if (item == null) {
            return "Unknown";
        }
        
        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.displayName() != null) {
            return PlainTextComponentSerializer.plainText().serialize(meta.displayName());
        }
        
        return formatMaterialName(item.getType());
    }

    /**
     * Gets a detailed description of an item including enchantments and lore.
     * 
     * @param item The item to describe
     * @return A detailed description string
     */
    public static String getDetailedDescription(ItemStack item) {
        if (item == null) {
            return "Unknown Item";
        }
        
        StringBuilder description = new StringBuilder();
        description.append(getDisplayName(item));
        
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            // Add enchantments
            Map<Enchantment, Integer> enchantments = meta.getEnchants();
            if (!enchantments.isEmpty()) {
                description.append(" (");
                boolean first = true;
                for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
                    if (!first) {
                        description.append(", ");
                    }
                    description.append(formatEnchantmentName(entry.getKey()))
                              .append(" ")
                              .append(entry.getValue());
                    first = false;
                }
                description.append(")");
            }
            
            // Add lore information
            List<Component> lore = meta.lore();
            if (lore != null && !lore.isEmpty()) {
                description.append(" [").append(lore.size()).append(" lore lines]");
            }
        }
        
        return description.toString();
    }

    /**
     * Formats an enchantment name to be more readable.
     * 
     * @param enchantment The enchantment to format
     * @return The formatted name
     */
    public static String formatEnchantmentName(Enchantment enchantment) {
        String name = enchantment.getKey().getKey();
        StringBuilder formatted = new StringBuilder();
        
        String[] parts = name.split("_");
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) {
                formatted.append(" ");
            }
            formatted.append(parts[i].charAt(0))
                     .append(parts[i].substring(1).toLowerCase());
        }
        
        return formatted.toString();
    }

    /**
     * Formats a material name to be more readable.
     * 
     * @param material The material to format
     * @return The formatted name
     */
    public static String formatMaterialName(Material material) {
        String name = material.name();
        StringBuilder formatted = new StringBuilder();
        
        String[] parts = name.split("_");
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) {
                formatted.append(" ");
            }
            formatted.append(parts[i].charAt(0))
                     .append(parts[i].substring(1).toLowerCase());
        }
        
        return formatted.toString();
    }

    /**
     * Checks if a player has the required items for a trade with exact NBT matching.
     * Now uses the enhanced NBT system for ProItems compatibility.
     * 
     * @param player The player to check
     * @param requiredItems The required items
     * @return true if the player has all required items, false otherwise
     */
    public static boolean hasRequiredItems(Player player, List<ItemStack> requiredItems) {
        for (ItemStack required : requiredItems) {
            if (!hasItem(player, required)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks if a player has a specific item in their inventory with exact NBT matching.
     * Now uses enhanced NBT comparison for ProItems compatibility.
     * 
     * @param player The player to check
     * @param required The required item
     * @return true if the player has the item, false otherwise
     */
    public static boolean hasItem(Player player, ItemStack required) {
        int neededAmount = required.getAmount();
        
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && isExactMatch(item, required)) {
                neededAmount -= item.getAmount();
                if (neededAmount <= 0) {
                    return true;
                }
            }
        }
        
        return false;
    }

    /**
     * Removes required items from a player's inventory with exact NBT matching.
     * 
     * @param player The player to remove items from
     * @param requiredItems The items to remove
     * @return true if all items were removed successfully, false otherwise
     */
    public static boolean removeRequiredItems(Player player, List<ItemStack> requiredItems) {
        // First check if player has all required items
        if (!hasRequiredItems(player, requiredItems)) {
            return false;
        }
        
        // Remove each required item
        for (ItemStack required : requiredItems) {
            removeItem(player, required);
        }
        
        return true;
    }

    /**
     * Removes a specific item from a player's inventory with exact NBT matching.
     * 
     * @param player The player to remove the item from
     * @param toRemove The item to remove
     */
    private static void removeItem(Player player, ItemStack toRemove) {
        int amountToRemove = toRemove.getAmount();
        
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (item != null && isExactMatch(item, toRemove)) {
                if (item.getAmount() <= amountToRemove) {
                    amountToRemove -= item.getAmount();
                    player.getInventory().setItem(i, null);
                } else {
                    item.setAmount(item.getAmount() - amountToRemove);
                    amountToRemove = 0;
                }
                
                if (amountToRemove <= 0) {
                    break;
                }
            }
        }
    }

    /**
     * Checks if two ItemStacks are exactly the same including all NBT data.
     * This method now uses the enhanced NBT system for ProItems compatibility.
     * 
     * @param item1 The first item
     * @param item2 The second item
     * @return true if the items are exactly the same, false otherwise
     */
    public static boolean isExactMatch(ItemStack item1, ItemStack item2) {
        if (item1 == null && item2 == null) {
            return true;
        }
        
        if (item1 == null || item2 == null) {
            return false;
        }
        
        // Basic material check
        if (item1.getType() != item2.getType()) {
            return false;
        }
        
        // Use enhanced NBT comparison for exact matching
        return NBTUtils.areItemsEquivalentForTrading(item1, item2);
    }

    /**
     * Checks if two ItemStacks are similar (same material and meta) but ignores amount.
     * This is useful for display purposes and general item comparison.
     * 
     * @param item1 The first item
     * @param item2 The second item
     * @return true if the items are similar, false otherwise
     */
    public static boolean isSimilar(ItemStack item1, ItemStack item2) {
        if (item1 == null && item2 == null) {
            return true;
        }
        
        if (item1 == null || item2 == null) {
            return false;
        }
        
        // Create copies with the same amount for comparison
        ItemStack copy1 = item1.clone();
        ItemStack copy2 = item2.clone();
        copy1.setAmount(1);
        copy2.setAmount(1);
        
        return isExactMatch(copy1, copy2);
    }

    /**
     * Checks if two ItemStacks have the same material and basic properties,
     * but allows for different custom names, lore, and enchantments.
     * This is useful for basic material-based trading.
     * 
     * @param item1 The first item
     * @param item2 The second item
     * @return true if the items have the same basic properties, false otherwise
     */
    public static boolean isSameMaterial(ItemStack item1, ItemStack item2) {
        if (item1 == null && item2 == null) {
            return true;
        }
        
        if (item1 == null || item2 == null) {
            return false;
        }
        
        return item1.getType() == item2.getType();
    }

    /**
     * Gives an item to a player, dropping it if inventory is full.
     * 
     * @param player The player to give the item to
     * @param item The item to give
     */
    public static void giveItem(Player player, ItemStack item) {
        if (player.getInventory().firstEmpty() != -1) {
            player.getInventory().addItem(item);
        } else {
            player.getWorld().dropItem(player.getLocation(), item);
        }
    }

    /**
     * Creates a copy of an ItemStack with a new amount while preserving all NBT data.
     * Uses the ultra cloning system for maximum data preservation.
     * 
     * @param original The original ItemStack
     * @param newAmount The new amount
     * @return A new ItemStack with the specified amount
     */
    public static ItemStack withAmount(ItemStack original, int newAmount) {
        if (original == null) {
            return null;
        }
        
        // Use ultra cloning system for perfect preservation
        return CloningUtils.ultraCloneWithAmount(original, newAmount);
    }

    /**
     * Checks if an ItemStack has custom NBT data (enchantments, custom name, lore, etc.).
     * Now uses the enhanced NBT system for better detection.
     * 
     * @param item The item to check
     * @return true if the item has custom NBT data, false otherwise
     */
    public static boolean hasCustomNBT(ItemStack item) {
        return NBTUtils.hasCustomNBT(item);
    }

    /**
     * Creates a summary string of an item's NBT data for logging or display purposes.
     * Now uses the enhanced NBT system for more detailed information.
     * 
     * @param item The item to summarize
     * @return A summary string of the item's NBT data
     */
    public static String getNBTSummary(ItemStack item) {
        return NBTUtils.getNBTSummary(item);
    }

    /**
     * Checks if an ItemStack is a ProItem.
     * 
     * @param item The item to check
     * @return true if the item is a ProItem, false otherwise
     */
    public static boolean isProItem(ItemStack item) {
        return NBTUtils.isProItem(item);
    }

    /**
     * Gets the ProItem ID from an ItemStack.
     * 
     * @param item The item to get the ID from
     * @return The ProItem ID, or null if not a ProItem
     */
    public static String getProItemId(ItemStack item) {
        return NBTUtils.getProItemId(item);
    }

    /**
     * Creates a trading-safe copy of an ItemStack with full NBT preservation.
     * This method ensures that all NBT data, including ProItems data, is preserved.
     * 
     * @param original The original ItemStack
     * @return A trading-safe copy
     */
    public static ItemStack createTradingSafeCopy(ItemStack original) {
        return NBTUtils.createTradingSafeCopy(original);
    }

    /**
     * Restores an ItemStack from trading-safe format.
     * 
     * @param tradingSafe The trading-safe ItemStack
     * @return The restored ItemStack
     */
    public static ItemStack restoreFromTradingSafe(ItemStack tradingSafe) {
        return NBTUtils.restoreFromTradingSafe(tradingSafe);
    }

    /**
     * Gets debug information about an item's NBT data.
     * 
     * @param item The item to debug
     * @return Debug information string
     */
    public static String getDebugNBTInfo(ItemStack item) {
        return NBTUtils.getDebugNBTInfo(item);
    }
}