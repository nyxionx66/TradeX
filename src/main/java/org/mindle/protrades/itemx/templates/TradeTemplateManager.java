package org.mindle.protrades.itemx.templates;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.mindle.protrades.ProTrades;
import org.mindle.protrades.itemx.ItemManager;
import org.mindle.protrades.models.Trade;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Manages trade templates for ItemX integration.
 * Handles loading, creating, and converting templates to actual trades.
 */
public class TradeTemplateManager {
    
    private final ProTrades plugin;
    private final ItemManager itemManager;
    private final Map<String, TradeTemplate> templates;
    private final Map<String, Set<String>> categories;
    private final File templatesDirectory;
    
    public TradeTemplateManager(ProTrades plugin, ItemManager itemManager) {
        this.plugin = plugin;
        this.itemManager = itemManager;
        this.templates = new ConcurrentHashMap<>();
        this.categories = new ConcurrentHashMap<>();
        this.templatesDirectory = new File(plugin.getDataFolder(), "itemx/templates");
        
        createDirectories();
    }
    
    /**
     * Creates necessary directories for templates.
     */
    private void createDirectories() {
        if (!templatesDirectory.exists()) {
            templatesDirectory.mkdirs();
            
            // Create default template categories
            new File(templatesDirectory, "starter").mkdirs();
            new File(templatesDirectory, "advanced").mkdirs();
            new File(templatesDirectory, "endgame").mkdirs();
        }
    }
    
    /**
     * Loads all trade templates from configuration files.
     */
    public void loadAllTemplates() {
        templates.clear();
        categories.clear();
        
        try {
            loadDirectory(templatesDirectory);
            
            plugin.getLogger().info("Loaded " + templates.size() + " trade templates in " + categories.size() + " categories");
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error loading trade templates", e);
        }
    }
    
    /**
     * Recursively loads templates from a directory.
     */
    private void loadDirectory(File directory) {
        File[] files = directory.listFiles();
        if (files == null) return;
        
        for (File file : files) {
            if (file.isDirectory()) {
                loadDirectory(file);
            } else if (file.getName().endsWith(".yml") || file.getName().endsWith(".yaml")) {
                loadTemplateFile(file);
            }
        }
    }
    
    /**
     * Loads templates from a single YAML file.
     */
    private void loadTemplateFile(File file) {
        try {
            FileConfiguration config = YamlConfiguration.loadConfiguration(file);
            
            for (String key : config.getKeys(false)) {
                ConfigurationSection section = config.getConfigurationSection(key);
                if (section != null) {
                    TradeTemplate template = parseTemplate(key, section);
                    if (template != null && template.isValid()) {
                        templates.put(key, template);
                        
                        String category = template.getCategory();
                        categories.computeIfAbsent(category, k -> new HashSet<>()).add(key);
                        
                        plugin.getLogger().fine("Loaded trade template: " + key + " (category: " + category + ")");
                    }
                }
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error loading template file: " + file.getName(), e);
        }
    }
    
    /**
     * Parses a trade template from configuration.
     */
    private TradeTemplate parseTemplate(String id, ConfigurationSection section) {
        try {
            String name = section.getString("name", id);
            String description = section.getString("description", "");
            String category = section.getString("category", "default");
            boolean enabled = section.getBoolean("enabled", true);
            
            // Parse input items
            List<TradeTemplate.TradeTemplateItem> inputItems = new ArrayList<>();
            ConfigurationSection inputsSection = section.getConfigurationSection("inputs");
            if (inputsSection != null) {
                for (String inputKey : inputsSection.getKeys(false)) {
                    ConfigurationSection inputSection = inputsSection.getConfigurationSection(inputKey);
                    if (inputSection != null) {
                        TradeTemplate.TradeTemplateItem item = parseTemplateItem(inputSection);
                        if (item != null) {
                            inputItems.add(item);
                        }
                    }
                }
            }
            
            // Parse output item
            ConfigurationSection outputSection = section.getConfigurationSection("output");
            if (outputSection == null) {
                plugin.getLogger().warning("Template " + id + " has no output item");
                return null;
            }
            
            TradeTemplate.TradeTemplateItem outputItem = parseTemplateItem(outputSection);
            if (outputItem == null) {
                plugin.getLogger().warning("Template " + id + " has invalid output item");
                return null;
            }
            
            // Parse metadata
            Map<String, Object> metadata = new HashMap<>();
            ConfigurationSection metaSection = section.getConfigurationSection("metadata");
            if (metaSection != null) {
                for (String metaKey : metaSection.getKeys(false)) {
                    metadata.put(metaKey, metaSection.get(metaKey));
                }
            }
            
            return new TradeTemplate(id, name, description, category, inputItems, outputItem, metadata, enabled);
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error parsing template: " + id, e);
            return null;
        }
    }
    
    /**
     * Parses a template item from configuration.
     */
    private TradeTemplate.TradeTemplateItem parseTemplateItem(ConfigurationSection section) {
        try {
            String type = section.getString("type", "regular");
            String itemId = section.getString("item");
            int amount = section.getInt("amount", 1);
            
            if (itemId == null) {
                return null;
            }
            
            Map<String, Object> properties = new HashMap<>();
            ConfigurationSection propsSection = section.getConfigurationSection("properties");
            if (propsSection != null) {
                for (String propKey : propsSection.getKeys(false)) {
                    properties.put(propKey, propsSection.get(propKey));
                }
            }
            
            return new TradeTemplate.TradeTemplateItem(type, itemId, amount, properties);
            
        } catch (Exception e) {
            plugin.getLogger().warning("Error parsing template item: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Creates a Trade from a template.
     */
    public Trade createTradeFromTemplate(String templateId, String tradeId) {
        TradeTemplate template = templates.get(templateId);
        if (template == null || !template.isEnabled()) {
            return null;
        }
        
        try {
            // Create input items
            List<ItemStack> inputs = new ArrayList<>();
            for (TradeTemplate.TradeTemplateItem templateItem : template.getInputItems()) {
                ItemStack item = createItemFromTemplate(templateItem);
                if (item == null) {
                    plugin.getLogger().warning("Failed to create input item for template: " + templateId);
                    return null;
                }
                inputs.add(item);
            }
            
            // Create output item
            ItemStack output = createItemFromTemplate(template.getOutputItem());
            if (output == null) {
                plugin.getLogger().warning("Failed to create output item for template: " + templateId);
                return null;
            }
            
            return Trade.of(tradeId, inputs, output);
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error creating trade from template: " + templateId, e);
            return null;
        }
    }
    
    /**
     * Creates an ItemStack from a template item.
     */
    private ItemStack createItemFromTemplate(TradeTemplate.TradeTemplateItem templateItem) {
        if (templateItem == null || !templateItem.isValid()) {
            return null;
        }
        
        ItemStack item;
        
        if (templateItem.isItemX()) {
            // Create ItemX item
            item = itemManager.createItemStack(templateItem.getItemId());
            if (item == null) {
                plugin.getLogger().warning("Failed to create ItemX item: " + templateItem.getItemId());
                return null;
            }
        } else {
            // Create regular item
            Material material = Material.matchMaterial(templateItem.getItemId());
            if (material == null) {
                plugin.getLogger().warning("Invalid material: " + templateItem.getItemId());
                return null;
            }
            item = new ItemStack(material);
        }
        
        // Set amount
        item.setAmount(templateItem.getAmount());
        
        return item;
    }
    
    /**
     * Gets a trade template by ID.
     */
    public TradeTemplate getTemplate(String templateId) {
        return templates.get(templateId);
    }
    
    /**
     * Gets all template IDs.
     */
    public Set<String> getAllTemplateIds() {
        return new HashSet<>(templates.keySet());
    }
    
    /**
     * Gets templates in a category.
     */
    public Set<String> getTemplatesInCategory(String category) {
        return categories.getOrDefault(category, new HashSet<>());
    }
    
    /**
     * Gets all categories.
     */
    public Set<String> getAllCategories() {
        return new HashSet<>(categories.keySet());
    }
    
    /**
     * Applies a template to a trade GUI.
     */
    public boolean applyTemplateToGUI(String templateId, String guiId) {
        TradeTemplate template = templates.get(templateId);
        if (template == null || !template.isEnabled()) {
            return false;
        }
        
        try {
            // Generate unique trade ID
            String tradeId = template.getId() + "_" + System.currentTimeMillis();
            
            // Create trade from template
            Trade trade = createTradeFromTemplate(templateId, tradeId);
            if (trade == null) {
                return false;
            }
            
            // Add trade to GUI
            plugin.getTradeManager().addTrade(guiId, trade);
            
            plugin.getLogger().info("Applied template " + templateId + " to GUI " + guiId + " as trade " + tradeId);
            return true;
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error applying template to GUI", e);
            return false;
        }
    }
    
    /**
     * Gets statistics about templates.
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("total_templates", templates.size());
        stats.put("template_categories", categories.size());
        stats.put("category_breakdown", new HashMap<>(categories));
        
        // Count templates by type
        int itemxTemplates = 0;
        int mixedTemplates = 0;
        int regularTemplates = 0;
        int enabledTemplates = 0;
        
        for (TradeTemplate template : templates.values()) {
            if (template.isEnabled()) enabledTemplates++;
            
            boolean hasItemX = false;
            boolean hasRegular = false;
            
            for (TradeTemplate.TradeTemplateItem item : template.getInputItems()) {
                if (item.isItemX()) hasItemX = true;
                if (item.isRegular()) hasRegular = true;
            }
            
            if (template.getOutputItem().isItemX()) hasItemX = true;
            if (template.getOutputItem().isRegular()) hasRegular = true;
            
            if (hasItemX && hasRegular) {
                mixedTemplates++;
            } else if (hasItemX) {
                itemxTemplates++;
            } else {
                regularTemplates++;
            }
        }
        
        stats.put("enabled_templates", enabledTemplates);
        stats.put("itemx_templates", itemxTemplates);
        stats.put("mixed_templates", mixedTemplates);
        stats.put("regular_templates", regularTemplates);
        
        return stats;
    }
    
    /**
     * Reloads all templates.
     */
    public void reload() {
        plugin.getLogger().info("Reloading trade templates...");
        loadAllTemplates();
        plugin.getLogger().info("Trade templates reloaded successfully!");
    }
}