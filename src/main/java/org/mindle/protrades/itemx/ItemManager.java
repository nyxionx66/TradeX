package org.mindle.protrades.itemx;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ArmorMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.trim.ArmorTrim;
import org.bukkit.inventory.meta.trim.TrimMaterial;
import org.bukkit.inventory.meta.trim.TrimPattern;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.mindle.protrades.ProTrades;
import org.mindle.protrades.utils.NBTUtils;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Manages all ItemX items - loading, creating, and handling events.
 * Integrates with the existing ProTrades NBT system.
 */
public class ItemManager implements Listener {
    
    private final ProTrades plugin;
    private final ItemParser parser;
    private final Map<String, ItemDefinition> items;
    private final Map<String, Set<String>> categories;
    private final NamespacedKey itemxKey;
    private final String namespacePrefix;
    private final File itemsDirectory;
    
    public ItemManager(ProTrades plugin) {
        this.plugin = plugin;
        this.parser = new ItemParser(plugin);
        this.items = new ConcurrentHashMap<>();
        this.categories = new ConcurrentHashMap<>();
        this.itemsDirectory = new File(plugin.getDataFolder(), "itemx/items");
        
        // Get configuration values
        this.namespacePrefix = plugin.getConfig().getString("itemx.nbt.namespace-prefix", "itemx");
        String nbtKey = plugin.getConfig().getString("itemx.nbt.key", "itemx:id");
        
        // Create the NBT key
        String[] keyParts = nbtKey.split(":", 2);
        if (keyParts.length == 2) {
            this.itemxKey = new NamespacedKey(keyParts[0], keyParts[1]);
        } else {
            this.itemxKey = new NamespacedKey(namespacePrefix, "id");
        }
        
        // Create directories if they don't exist
        createDirectories();
        
        // Register as event listener
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }
    
    /**
     * Creates necessary directories for ItemX.
     */
    private void createDirectories() {
        File itemxDir = new File(plugin.getDataFolder(), "itemx");
        if (!itemxDir.exists()) {
            itemxDir.mkdirs();
        }
        
        if (!itemsDirectory.exists()) {
            itemsDirectory.mkdirs();
            
            // Create default category directories
            new File(itemsDirectory, "weapons").mkdirs();
            new File(itemsDirectory, "tools").mkdirs();
            new File(itemsDirectory, "armor").mkdirs();
            new File(itemsDirectory, "misc").mkdirs();
        }
    }
    
    /**
     * Loads all ItemX items from configuration files.
     */
    public void loadAllItems() {
        items.clear();
        categories.clear();
        
        try {
            Map<String, ItemDefinition> loadedItems = parser.parseAllItems(itemsDirectory);
            
            for (Map.Entry<String, ItemDefinition> entry : loadedItems.entrySet()) {
                String itemId = entry.getKey();
                ItemDefinition item = entry.getValue();
                
                if (parser.validateItemDefinition(item)) {
                    items.put(itemId, item);
                    
                    // Add to categories (based on file location)
                    String category = determineCategory(itemId);
                    categories.computeIfAbsent(category, k -> new HashSet<>()).add(itemId);
                    
                    plugin.getLogger().fine("Loaded ItemX item: " + itemId + " (category: " + category + ")");
                }
            }
            
            plugin.getLogger().info("Loaded " + items.size() + " ItemX items in " + categories.size() + " categories");
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error loading ItemX items", e);
        }
    }
    
    /**
     * Creates an ItemStack from an ItemDefinition.
     */
    public ItemStack createItemStack(ItemDefinition item) {
        if (item == null || !item.isValid()) {
            return null;
        }
        
        try {
            ItemStack itemStack = new ItemStack(item.getMaterial());
            ItemMeta meta = itemStack.getItemMeta();
            
            if (meta == null) {
                plugin.getLogger().warning("Could not get ItemMeta for item: " + item.getId());
                return null;
            }
            
            // Set display name
            if (item.getName() != null && !item.getName().isEmpty()) {
                Component nameComponent = ColorUtil.colorize(item.getName());
                meta.displayName(nameComponent);
            }
            
            // Set lore
            if (!item.getLore().isEmpty()) {
                List<Component> loreComponents = ColorUtil.colorizeList(item.getLore());
                meta.lore(loreComponents);
            }
            
            // Set unbreakable
            if (item.isUnbreakable()) {
                meta.setUnbreakable(true);
            }
            
            // Add enchantments
            addEnchantments(meta, item.getEnchantments());
            
            // Add armor trim
            addArmorTrim(meta, item.getArmorTrim());
            
            // Add ItemX NBT identifier
            PersistentDataContainer container = meta.getPersistentDataContainer();
            container.set(itemxKey, PersistentDataType.STRING, item.getNbtId());
            
            // Add custom NBT data
            addCustomNBT(container, item.getCustomNBT());
            
            itemStack.setItemMeta(meta);
            
            // Create trading-safe copy using existing NBT system
            return NBTUtils.createTradingSafeCopy(itemStack);
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error creating ItemStack for item: " + item.getId(), e);
            return null;
        }
    }
    
    /**
     * Creates an ItemStack by item ID.
     */
    public ItemStack createItemStack(String itemId) {
        ItemDefinition item = items.get(itemId);
        return createItemStack(item);
    }
    
    /**
     * Adds enchantments to an ItemMeta.
     */
    private void addEnchantments(ItemMeta meta, Map<String, Integer> enchantments) {
        if (enchantments == null || enchantments.isEmpty()) {
            return;
        }
        
        for (Map.Entry<String, Integer> entry : enchantments.entrySet()) {
            String enchantName = entry.getKey();
            int level = entry.getValue();
            
            // Try to get the enchantment by name
            Enchantment enchant = Enchantment.getByKey(NamespacedKey.minecraft(enchantName.toLowerCase()));
            
            if (enchant != null) {
                // Use unsafe enchantment to allow levels beyond vanilla limits
                meta.addEnchant(enchant, level, true);
            } else {
                plugin.getLogger().warning("Unknown enchantment: " + enchantName);
            }
        }
    }
    
    /**
     * Adds armor trim to an ItemMeta.
     */
    private void addArmorTrim(ItemMeta meta, ItemDefinition.ArmorTrimData trimData) {
        if (trimData == null || !trimData.isValid() || !(meta instanceof ArmorMeta)) {
            return;
        }
        
        try {
            TrimPattern pattern = trimData.parseTrimPattern();
            TrimMaterial material = trimData.parseTrimMaterial();
            
            if (pattern != null && material != null) {
                ArmorTrim trim = new ArmorTrim(material, pattern);
                ((ArmorMeta) meta).setTrim(trim);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error adding armor trim: " + e.getMessage());
        }
    }
    
    /**
     * Adds custom NBT data to a PersistentDataContainer.
     */
    private void addCustomNBT(PersistentDataContainer container, Map<String, Object> customNBT) {
        if (customNBT == null || customNBT.isEmpty()) {
            return;
        }
        
        for (Map.Entry<String, Object> entry : customNBT.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            
            try {
                NamespacedKey nbtKey = new NamespacedKey(namespacePrefix, key);
                
                if (value instanceof String) {
                    container.set(nbtKey, PersistentDataType.STRING, (String) value);
                } else if (value instanceof Integer) {
                    container.set(nbtKey, PersistentDataType.INTEGER, (Integer) value);
                } else if (value instanceof Double) {
                    container.set(nbtKey, PersistentDataType.DOUBLE, (Double) value);
                } else if (value instanceof Boolean) {
                    container.set(nbtKey, PersistentDataType.BYTE, (Boolean) value ? (byte) 1 : (byte) 0);
                } else {
                    container.set(nbtKey, PersistentDataType.STRING, value.toString());
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Error adding custom NBT data for key: " + key);
            }
        }
    }
    
    /**
     * Checks if an ItemStack is an ItemX item.
     */
    public boolean isItemXItem(ItemStack item) {
        if (item == null || item.getType() == Material.AIR || !item.hasItemMeta()) {
            return false;
        }
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }
        
        PersistentDataContainer container = meta.getPersistentDataContainer();
        return container.has(itemxKey, PersistentDataType.STRING);
    }
    
    /**
     * Gets the ItemX ID from an ItemStack.
     */
    public String getItemXId(ItemStack item) {
        if (!isItemXItem(item)) {
            return null;
        }
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return null;
        }
        
        PersistentDataContainer container = meta.getPersistentDataContainer();
        return container.get(itemxKey, PersistentDataType.STRING);
    }
    
    /**
     * Gets an ItemDefinition by ID.
     */
    public ItemDefinition getItemDefinition(String itemId) {
        return items.get(itemId);
    }
    
    /**
     * Gets all item IDs.
     */
    public Set<String> getAllItemIds() {
        return new HashSet<>(items.keySet());
    }
    
    /**
     * Gets all items in a category.
     */
    public Set<String> getItemsInCategory(String category) {
        return categories.getOrDefault(category, new HashSet<>());
    }
    
    /**
     * Gets all categories.
     */
    public Set<String> getAllCategories() {
        return new HashSet<>(categories.keySet());
    }
    
    /**
     * Determines the category for an item based on its ID or configuration.
     */
    private String determineCategory(String itemId) {
        // Simple categorization based on item ID prefixes
        if (itemId.startsWith("weapon_") || itemId.contains("sword") || itemId.contains("axe") || itemId.contains("bow")) {
            return "weapons";
        } else if (itemId.startsWith("tool_") || itemId.contains("pickaxe") || itemId.contains("shovel") || itemId.contains("hoe")) {
            return "tools";
        } else if (itemId.startsWith("armor_") || itemId.contains("helmet") || itemId.contains("chestplate") || itemId.contains("leggings") || itemId.contains("boots")) {
            return "armor";
        } else {
            return "misc";
        }
    }
    
    /**
     * Event handler for block placement - handles disable-use functionality.
     */
    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        if (!isItemXItem(item)) {
            return;
        }
        
        String itemId = getItemXId(item);
        if (itemId != null) {
            ItemDefinition definition = getItemDefinition(itemId);
            if (definition != null && definition.isDisableUse()) {
                event.setCancelled(true);
                plugin.getLogger().fine("Blocked placement of disabled ItemX item: " + itemId);
            }
        }
    }
    
    /**
     * Event handler for player interaction - handles disable-use functionality.
     */
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        ItemStack item = event.getItem();
        if (item == null || !isItemXItem(item)) {
            return;
        }
        
        String itemId = getItemXId(item);
        if (itemId != null) {
            ItemDefinition definition = getItemDefinition(itemId);
            if (definition != null && definition.isDisableUse()) {
                event.setCancelled(true);
                plugin.getLogger().fine("Blocked interaction with disabled ItemX item: " + itemId);
            }
        }
    }
    
    /**
     * Reloads all ItemX items from configuration files.
     */
    public void reload() {
        plugin.getLogger().info("Reloading ItemX items...");
        loadAllItems();
        plugin.getLogger().info("ItemX items reloaded successfully!");
    }
    
    /**
     * Gets statistics about loaded items.
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("total_items", items.size());
        stats.put("categories", categories.size());
        stats.put("category_breakdown", new HashMap<>(categories));
        
        // Count items with specific features
        int enchantedItems = 0;
        int unbreakableItems = 0;
        int armorTrimmedItems = 0;
        int disabledItems = 0;
        
        for (ItemDefinition item : items.values()) {
            if (!item.getEnchantments().isEmpty()) enchantedItems++;
            if (item.isUnbreakable()) unbreakableItems++;
            if (item.getArmorTrim() != null) armorTrimmedItems++;
            if (item.isDisableUse()) disabledItems++;
        }
        
        stats.put("enchanted_items", enchantedItems);
        stats.put("unbreakable_items", unbreakableItems);
        stats.put("armor_trimmed_items", armorTrimmedItems);
        stats.put("disabled_items", disabledItems);
        
        return stats;
    }
}