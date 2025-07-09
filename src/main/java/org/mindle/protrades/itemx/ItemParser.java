package org.mindle.protrades.itemx;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.mindle.protrades.ProTrades;

import java.io.File;
import java.util.*;
import java.util.logging.Level;

/**
 * Parses YAML files into ItemDefinition objects.
 * Handles all ItemX item properties and validates configurations.
 */
public class ItemParser {
    
    private final ProTrades plugin;
    
    public ItemParser(ProTrades plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Parses all item files from the items directory.
     */
    public Map<String, ItemDefinition> parseAllItems(File itemsDirectory) {
        Map<String, ItemDefinition> items = new HashMap<>();
        
        if (!itemsDirectory.exists()) {
            plugin.getLogger().warning("Items directory does not exist: " + itemsDirectory.getPath());
            return items;
        }
        
        parseDirectory(itemsDirectory, items);
        
        plugin.getLogger().info("Parsed " + items.size() + " ItemX items from configuration files");
        return items;
    }
    
    /**
     * Recursively parses a directory for item files.
     */
    private void parseDirectory(File directory, Map<String, ItemDefinition> items) {
        File[] files = directory.listFiles();
        if (files == null) return;
        
        for (File file : files) {
            if (file.isDirectory()) {
                parseDirectory(file, items);
            } else if (file.getName().endsWith(".yml") || file.getName().endsWith(".yaml")) {
                parseFile(file, items);
            }
        }
    }
    
    /**
     * Parses a single YAML file for item definitions.
     */
    private void parseFile(File file, Map<String, ItemDefinition> items) {
        try {
            FileConfiguration config = YamlConfiguration.loadConfiguration(file);
            
            for (String key : config.getKeys(false)) {
                ConfigurationSection section = config.getConfigurationSection(key);
                if (section != null) {
                    ItemDefinition item = parseItemDefinition(key, section);
                    if (item != null) {
                        items.put(key, item);
                        plugin.getLogger().fine("Parsed item: " + key + " from file: " + file.getName());
                    }
                }
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error parsing item file: " + file.getName(), e);
        }
    }
    
    /**
     * Parses an individual item definition from a configuration section.
     */
    public ItemDefinition parseItemDefinition(String id, ConfigurationSection section) {
        try {
            // Parse material
            String materialName = section.getString("material");
            if (materialName == null) {
                plugin.getLogger().warning("Item " + id + " has no material specified");
                return null;
            }
            
            Material material = Material.matchMaterial(materialName);
            if (material == null) {
                plugin.getLogger().warning("Item " + id + " has invalid material: " + materialName);
                return null;
            }
            
            // Parse basic properties
            String name = section.getString("name");
            List<String> lore = section.getStringList("lore");
            boolean unbreakable = section.getBoolean("unbreakable", false);
            boolean useVanillaLore = section.getBoolean("use-vanilla-lore", false);
            boolean disableUse = section.getBoolean("disable-use", false);
            String nbtId = section.getString("nbt-id", id);
            
            // Parse enchantments
            Map<String, Integer> enchantments = parseEnchantments(section.getConfigurationSection("enchants"));
            
            // Parse armor trim
            ItemDefinition.ArmorTrimData armorTrim = parseArmorTrim(section.getConfigurationSection("armor-trim"));
            
            // Parse custom NBT
            Map<String, Object> customNBT = parseCustomNBT(section.getConfigurationSection("custom-nbt"));
            
            return new ItemDefinition(id, material, name, lore, unbreakable, useVanillaLore,
                    enchantments, disableUse, nbtId, armorTrim, customNBT);
                    
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error parsing item definition: " + id, e);
            return null;
        }
    }
    
    /**
     * Parses enchantments from a configuration section.
     */
    private Map<String, Integer> parseEnchantments(ConfigurationSection section) {
        if (section == null) return Map.of();
        
        Map<String, Integer> enchantments = new HashMap<>();
        
        for (String key : section.getKeys(false)) {
            try {
                int level = section.getInt(key);
                enchantments.put(key, level);
            } catch (Exception e) {
                plugin.getLogger().warning("Invalid enchantment level for " + key + ": " + section.get(key));
            }
        }
        
        return enchantments;
    }
    
    /**
     * Parses armor trim data from a configuration section.
     */
    private ItemDefinition.ArmorTrimData parseArmorTrim(ConfigurationSection section) {
        if (section == null) return null;
        
        String pattern = section.getString("pattern");
        String material = section.getString("material");
        
        if (pattern == null || material == null) {
            return null;
        }
        
        return new ItemDefinition.ArmorTrimData(pattern, material);
    }
    
    /**
     * Parses custom NBT data from a configuration section.
     */
    private Map<String, Object> parseCustomNBT(ConfigurationSection section) {
        if (section == null) return Map.of();
        
        Map<String, Object> customNBT = new HashMap<>();
        
        for (String key : section.getKeys(false)) {
            Object value = section.get(key);
            if (value != null) {
                customNBT.put(key, value);
            }
        }
        
        return customNBT;
    }
    
    /**
     * Validates an item definition.
     */
    public boolean validateItemDefinition(ItemDefinition item) {
        if (item == null) {
            return false;
        }
        
        if (!item.isValid()) {
            plugin.getLogger().warning("Item " + item.getId() + " is not valid");
            return false;
        }
        
        // Validate armor trim if present
        if (item.getArmorTrim() != null && !item.getArmorTrim().isValid()) {
            plugin.getLogger().warning("Item " + item.getId() + " has invalid armor trim data");
            return false;
        }
        
        // Validate that armor trim is only used on armor items
        if (item.getArmorTrim() != null && !isArmorItem(item.getMaterial())) {
            plugin.getLogger().warning("Item " + item.getId() + " has armor trim but is not an armor item");
            return false;
        }
        
        return true;
    }
    
    /**
     * Checks if a material is an armor item.
     */
    private boolean isArmorItem(Material material) {
        return material.name().endsWith("_HELMET") ||
               material.name().endsWith("_CHESTPLATE") ||
               material.name().endsWith("_LEGGINGS") ||
               material.name().endsWith("_BOOTS");
    }
    
    /**
     * Gets the category from a file path.
     */
    public String getCategoryFromPath(File file, File rootDirectory) {
        if (file == null || rootDirectory == null) {
            return "default";
        }
        
        String rootPath = rootDirectory.getAbsolutePath();
        String filePath = file.getParent();
        
        if (filePath == null || !filePath.startsWith(rootPath)) {
            return "default";
        }
        
        String relativePath = filePath.substring(rootPath.length());
        if (relativePath.startsWith(File.separator)) {
            relativePath = relativePath.substring(1);
        }
        
        return relativePath.isEmpty() ? "default" : relativePath.replace(File.separator, "/");
    }
}