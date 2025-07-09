package org.mindle.protrades.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantInventory;
import org.bukkit.inventory.MerchantRecipe;
import org.mindle.protrades.ProTrades;
import org.mindle.protrades.utils.ItemUtils;
import org.mindle.protrades.utils.NBTUtils;

/**
 * Listener specifically for handling NBT preservation during villager trading.
 * Ensures that ProItems and other custom NBT items maintain their data when traded.
 */
public class NBTTradeListener implements Listener {
    
    private final ProTrades plugin;
    
    public NBTTradeListener(ProTrades plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Handles villager trading to ensure NBT data is properly preserved.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onMerchantTrade(InventoryClickEvent event) {
        if (!(event.getInventory() instanceof MerchantInventory)) {
            return;
        }
        
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getWhoClicked();
        MerchantInventory merchantInventory = (MerchantInventory) event.getInventory();
        
        // Only handle result slot clicks (where players get their items)
        if (event.getSlotType() != InventoryType.SlotType.RESULT) {
            return;
        }
        
        // Only handle take actions
        if (event.getAction() != InventoryAction.PICKUP_ALL && 
            event.getAction() != InventoryAction.PICKUP_HALF &&
            event.getAction() != InventoryAction.PICKUP_ONE &&
            event.getAction() != InventoryAction.PICKUP_SOME &&
            event.getAction() != InventoryAction.MOVE_TO_OTHER_INVENTORY) {
            return;
        }
        
        MerchantRecipe selectedRecipe = merchantInventory.getSelectedRecipe();
        if (selectedRecipe == null) {
            return;
        }
        
        ItemStack result = selectedRecipe.getResult();
        if (result == null) {
            return;
        }
        
        // Check if the result item has preserved NBT data
        if (NBTUtils.hasPreservedNBT(result)) {
            plugin.getLogger().fine("Detected NBT-preserved item in trade result for player: " + player.getName());
            
            // Schedule NBT restoration after the trade completes
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                restoreNBTInPlayerInventory(player, result);
            }, 1L);
        }
        
        // Check and validate input items for ProItems compatibility
        validateTradeInputs(player, merchantInventory, selectedRecipe);
    }
    
    /**
     * Restores NBT data in the player's inventory for recently traded items.
     */
    private void restoreNBTInPlayerInventory(Player player, ItemStack originalResult) {
        // Find items in player's inventory that match the traded result
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            
            if (item != null && item.getType() == originalResult.getType()) {
                if (NBTUtils.hasPreservedNBT(item)) {
                    // Restore the NBT data
                    ItemStack restored = NBTUtils.restoreFromTradingSafe(item);
                    if (restored != null && !restored.equals(item)) {
                        player.getInventory().setItem(i, restored);
                        plugin.getLogger().fine("Restored NBT data for item in slot " + i + " for player: " + player.getName());
                        
                        // Log ProItems information if applicable
                        if (ItemUtils.isProItem(restored)) {
                            String proItemId = ItemUtils.getProItemId(restored);
                            plugin.getLogger().fine("Restored ProItem: " + proItemId + " for player: " + player.getName());
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Validates that the player has the correct input items for the trade.
     */
    private void validateTradeInputs(Player player, MerchantInventory merchantInventory, MerchantRecipe recipe) {
        // Get the input items from the merchant inventory
        ItemStack input1 = merchantInventory.getItem(0);
        ItemStack input2 = merchantInventory.getItem(1);
        
        // Log NBT information for debugging
        if (input1 != null && ItemUtils.hasCustomNBT(input1)) {
            plugin.getLogger().fine("Trade input 1 has NBT: " + ItemUtils.getNBTSummary(input1));
            
            if (ItemUtils.isProItem(input1)) {
                String proItemId = ItemUtils.getProItemId(input1);
                plugin.getLogger().fine("Trade input 1 is ProItem: " + proItemId);
            }
        }
        
        if (input2 != null && ItemUtils.hasCustomNBT(input2)) {
            plugin.getLogger().fine("Trade input 2 has NBT: " + ItemUtils.getNBTSummary(input2));
            
            if (ItemUtils.isProItem(input2)) {
                String proItemId = ItemUtils.getProItemId(input2);
                plugin.getLogger().fine("Trade input 2 is ProItem: " + proItemId);
            }
        }
        
        // Validate against recipe ingredients
        if (!recipe.getIngredients().isEmpty()) {
            ItemStack requiredInput1 = recipe.getIngredients().get(0);
            
            if (input1 != null && requiredInput1 != null) {
                // Use enhanced NBT comparison for exact matching
                if (!ItemUtils.isExactMatch(input1, requiredInput1)) {
                    plugin.getLogger().fine("Input 1 NBT mismatch detected for player: " + player.getName());
                    plugin.getLogger().fine("Expected: " + ItemUtils.getNBTSummary(requiredInput1));
                    plugin.getLogger().fine("Actual: " + ItemUtils.getNBTSummary(input1));
                }
            }
            
            if (recipe.getIngredients().size() > 1) {
                ItemStack requiredInput2 = recipe.getIngredients().get(1);
                
                if (input2 != null && requiredInput2 != null) {
                    // Use enhanced NBT comparison for exact matching
                    if (!ItemUtils.isExactMatch(input2, requiredInput2)) {
                        plugin.getLogger().fine("Input 2 NBT mismatch detected for player: " + player.getName());
                        plugin.getLogger().fine("Expected: " + ItemUtils.getNBTSummary(requiredInput2));
                        plugin.getLogger().fine("Actual: " + ItemUtils.getNBTSummary(input2));
                    }
                }
            }
        }
    }
    
    /**
     * Handles post-trade cleanup and NBT restoration.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPostTrade(InventoryClickEvent event) {
        if (!(event.getInventory() instanceof MerchantInventory)) {
            return;
        }
        
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getWhoClicked();
        
        // Schedule a task to check for any remaining NBT-preserved items
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            performPostTradeNBTCleanup(player);
        }, 2L);
    }
    
    /**
     * Performs cleanup and restoration of any remaining NBT-preserved items.
     */
    private void performPostTradeNBTCleanup(Player player) {
        int restoredCount = 0;
        
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            
            if (item != null && NBTUtils.hasPreservedNBT(item)) {
                ItemStack restored = NBTUtils.restoreFromTradingSafe(item);
                if (restored != null && !restored.equals(item)) {
                    player.getInventory().setItem(i, restored);
                    restoredCount++;
                }
            }
        }
        
        if (restoredCount > 0) {
            plugin.getLogger().fine("Post-trade NBT cleanup: restored " + restoredCount + " items for player: " + player.getName());
        }
    }
}