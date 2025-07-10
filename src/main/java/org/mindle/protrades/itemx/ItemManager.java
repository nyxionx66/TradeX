package org.mindle.protrades.itemx;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.mindle.protrades.ProTrades;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Enhanced ItemManager for the new resource structure.
 * Manages custom ItemX items with improved organization and caching.
 */
public class ItemManager {
    
    private final ProTrades plugin;
    private final Map<String, ItemDefinition> items;
    private final Map<String, Set<String>> categories;
    private final File itemsDirectory;
    private final ItemParser itemParser;
    private final ColorUtil colorUtil;
    
    public ItemManager(ProTrades plugin) {
        this.plugin = plugin;
        this.items = new ConcurrentHashMap<>();
        this.categories = new ConcurrentHashMap<>();
        this.itemsDirectory = new File(plugin.getDataFolder(), "items");
        this.itemParser = new ItemParser(plugin);
        this.colorUtil = new ColorUtil();
        
        createDirectories();
    }
    
    /**
     * Creates necessary directories for items.
     */
    private void createDirectories() {
        if (!itemsDirectory.exists()) {
            itemsDirectory.mkdirs();
            plugin.getLogger().info("Created items directory: " + itemsDirectory.getPath());
        }
    }
    
    /**
     * Loads all items from the items directory.
     */
    public void loadAllItems() {
        items.clear();
        categories.clear();
        
        try {
            loadDirectory(itemsDirectory);
            
            plugin.getLogger().info("Loaded " + items.size() + " custom items in " + categories.size() + " categories");
            
            // Log category breakdown
            for (Map.Entry<String, Set<String>> entry : categories.entrySet()) {
                plugin.getLogger().info("Category '" + entry.getKey() + "': " + entry.getValue().size() + " items");
            }
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error loading items", e);
        }
    }
    
    /**
     * Recursively loads items from a directory.
     */
    private void loadDirectory(File directory) {
        File[] files = directory.listFiles();
        if (files == null) return;
        
        for (File file : files) {
            if (file.isDirectory()) {
                loadDirectory(file);
            } else if (file.getName().endsWith(".yml") || file.getName().endsWith(".yaml")) {
                loadItemFile(file);
            }
        }
    }
    
    /**
     * Loads items from a single YAML file.
     */
    private void loadItemFile(File file) {
        try {
            FileConfiguration config = YamlConfiguration.loadConfiguration(file);
            String category = getCategoryFromPath(file);
            
            for (String key : config.getKeys(false)) {
                ConfigurationSection section = config.getConfigurationSection(key);
                if (section != null) {
                    ItemDefinition item = parseItem(key, section, category);
                    if (item != null && item.isValid()) {
                        items.put(key, item);
                        categories.computeIfAbsent(category, k -> new HashSet<>()).add(key);
                        
                        plugin.getLogger().fine("Loaded item: " + key + " (category: " + category + ")");
                    } else {
                        plugin.getLogger().warning("Invalid item: " + key + " in file: " + file.getName());
                    }
                }
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error loading item file: " + file.getName(), e);
        }
    }
    
    /**
     * Gets the category from the file path.
     */
    private String getCategoryFromPath(File file) {
        String path = file.getPath();
        String itemsPath = itemsDirectory.getPath();
        
        if (path.startsWith(itemsPath)) {
            String relativePath = path.substring(itemsPath.length() + 1);
            String[] parts = relativePath.split(File.separator);
            if (parts.length > 1) {
                return parts[0]; // First directory is the category
            }
        }
        
        return "misc"; // Default category
    }
    
    /**
     * Parses an item definition from configuration.
     */
    private ItemDefinition parseItem(String id, ConfigurationSection section, String category) {
        try {
            String materialName = section.getString("material");
            if (materialName == null) {
                plugin.getLogger().warning("Item " + id + " has no material defined");
                return null;
            }
            
            Material material = Material.matchMaterial(materialName);
            if (material == null) {
                plugin.getLogger().warning("Item " + id + " has invalid material: " + materialName);
                return null;
            }
            
            String name = section.getString("name", id);
            List<String> lore = section.getStringList("lore");
            boolean unbreakable = section.getBoolean("unbreakable", false);
            boolean useVanillaLore = section.getBoolean("use-vanilla-lore", false);
            boolean disableUse = section.getBoolean("disable-use", false);
            String nbtId = section.getString("nbt-id", id);
            
            // Parse enchantments
            Map<String, Integer> enchantments = new HashMap<>();
            ConfigurationSection enchantsSection = section.getConfigurationSection("enchants");
            if (enchantsSection != null) {
                for (String enchant : enchantsSection.getKeys(false)) {
                    int level = enchantsSection.getInt(enchant);
                    enchantments.put(enchant, level);
                }
            }
            
            // Parse custom NBT
            Map<String, Object> customNbt = new HashMap<>();
            ConfigurationSection nbtSection = section.getConfigurationSection("custom-nbt");
            if (nbtSection != null) {
                for (String nbtKey : nbtSection.getKeys(false)) {
                    customNbt.put(nbtKey, nbtSection.get(nbtKey));
                }
            }
            
            // Parse armor trim
            Map<String, String> armorTrim = new HashMap<>();
            ConfigurationSection trimSection = section.getConfigurationSection("armor-trim");
            if (trimSection != null) {
                armorTrim.put("pattern", trimSection.getString("pattern"));
                armorTrim.put("material", trimSection.getString("material"));
            }
            
            return new ItemDefinition(
                id, material, name, lore, unbreakable, useVanillaLore, 
                enchantments, disableUse, nbtId, customNbt, armorTrim, category
            );
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error parsing item: " + id, e);
            return null;
        }
    }
    
    /**
     * Creates an ItemStack from an item definition.
     */
    public ItemStack createItemStack(String itemId) {
        ItemDefinition definition = items.get(itemId);
        if (definition == null) {
            plugin.getLogger().warning("Item not found: " + itemId);
            return null;
        }
        
        try {
            return itemParser.createItemStack(definition);
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error creating ItemStack for: " + itemId, e);
            return null;
        }
    }
    
    /**
     * Creates an ItemStack with a specific amount.
     */
    public ItemStack createItemStack(String itemId, int amount) {
        ItemStack item = createItemStack(itemId);
        if (item != null) {
            item.setAmount(Math.max(1, amount));
        }
        return item;
    }
    
    /**
     * Gets an item definition by ID.
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
     * Gets items in a category.
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
     * Checks if an item exists.
     */
    public boolean hasItem(String itemId) {
        return items.containsKey(itemId);
    }
    
    /**
     * Checks if an ItemStack is a custom item.
     */
    public boolean isCustomItem(ItemStack item) {
        if (item == null) return false;
        
        return itemParser.getCustomItemId(item) != null;
    }
    
    /**
     * Gets the custom item ID from an ItemStack.
     */
    public String getCustomItemId(ItemStack item) {
        return itemParser.getCustomItemId(item);
    }
    
    /**
     * Creates ItemStacks for all items in a category.
     */
    public List<ItemStack> createItemsFromCategory(String category) {
        List<ItemStack> result = new ArrayList<>();
        Set<String> itemIds = getItemsInCategory(category);
        
        for (String itemId : itemIds) {
            ItemStack item = createItemStack(itemId);
            if (item != null) {
                result.add(item);
            }
        }
        
        return result;
    }
    
    /**
     * Gets statistics about items.
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("total_items", items.size());
        stats.put("item_categories", categories.size());
        stats.put("category_breakdown", new HashMap<>(categories));
        
        // Count items by type
        int weapons = 0, armor = 0, tools = 0, misc = 0;
        for (ItemDefinition item : items.values()) {
            Material material = item.getMaterial();
            if (material.name().contains("SWORD") || material.name().contains("AXE") || material.name().contains("BOW")) {
                weapons++;
            } else if (material.name().contains("HELMET") || material.name().contains("CHESTPLATE") || 
                      material.name().contains("LEGGINGS") || material.name().contains("BOOTS")) {
                armor++;
            } else if (material.name().contains("PICKAXE") || material.name().contains("SHOVEL") || 
                      material.name().contains("HOE")) {
                tools++;
            } else {
                misc++;
            }
        }
        
        stats.put("weapons", weapons);
        stats.put("armor", armor);
        stats.put("tools", tools);
        stats.put("misc", misc);
        
        return stats;
    }
    
    /**
     * Validates all items.
     */
    public boolean validateAllItems() {
        boolean allValid = true;
        
        for (Map.Entry<String, ItemDefinition> entry : items.entrySet()) {
            ItemDefinition item = entry.getValue();
            if (!item.isValid()) {
                plugin.getLogger().warning("Invalid item: " + entry.getKey());
                allValid = false;
                continue;
            }
            
            // Try to create the item
            ItemStack testItem = createItemStack(entry.getKey());
            if (testItem == null) {
                plugin.getLogger().warning("Cannot create ItemStack for: " + entry.getKey());
                allValid = false;
            }
        }
        
        if (allValid) {
            plugin.getLogger().info("All items validated successfully");
        } else {
            plugin.getLogger().warning("Some items have validation errors");
        }
        
        return allValid;
    }
    
    /**
     * Gets the items directory.
     */
    public File getItemsDirectory() {
        return itemsDirectory;
    }
    
    /**
     * Reloads all items.
     */
    public void reload() {
        plugin.getLogger().info("Reloading custom items...");
        loadAllItems();
        validateAllItems();
        plugin.getLogger().info("Custom items reloaded successfully!");
    }
}