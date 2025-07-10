package org.mindle.protrades.itemx.templates;

import org.bukkit.inventory.ItemStack;
import org.mindle.protrades.models.Trade;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Represents a trade template that can be used to create pre-made trades.
 * Templates can include ItemX items, regular items, and metadata.
 */
public class TradeTemplate {
    
    private final String id;
    private final String name;
    private final String description;
    private final String category;
    private final List<TradeTemplateItem> inputItems;
    private final TradeTemplateItem outputItem;
    private final Map<String, Object> metadata;
    private final boolean enabled;
    
    public TradeTemplate(String id, String name, String description, String category,
                        List<TradeTemplateItem> inputItems, TradeTemplateItem outputItem,
                        Map<String, Object> metadata, boolean enabled) {
        this.id = Objects.requireNonNull(id, "Template ID cannot be null");
        this.name = name != null ? name : id;
        this.description = description != null ? description : "";
        this.category = category != null ? category : "default";
        this.inputItems = inputItems != null ? List.copyOf(inputItems) : List.of();
        this.outputItem = Objects.requireNonNull(outputItem, "Output item cannot be null");
        this.metadata = metadata != null ? Map.copyOf(metadata) : Map.of();
        this.enabled = enabled;
    }
    
    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getCategory() { return category; }
    public List<TradeTemplateItem> getInputItems() { return inputItems; }
    public TradeTemplateItem getOutputItem() { return outputItem; }
    public Map<String, Object> getMetadata() { return metadata; }
    public boolean isEnabled() { return enabled; }
    
    /**
     * Checks if this template is valid.
     */
    public boolean isValid() {
        return id != null && !id.isBlank() && 
               !inputItems.isEmpty() && 
               outputItem != null && outputItem.isValid();
    }
    
    /**
     * Gets the display name for this template.
     */
    public String getDisplayName() {
        return name != null && !name.isBlank() ? name : id;
    }
    
    /**
     * Gets a metadata value.
     */
    public Object getMetadata(String key) {
        return metadata.get(key);
    }
    
    /**
     * Gets a metadata value with default.
     */
    @SuppressWarnings("unchecked")
    public <T> T getMetadata(String key, T defaultValue) {
        Object value = metadata.get(key);
        if (value != null && defaultValue.getClass().isInstance(value)) {
            return (T) value;
        }
        return defaultValue;
    }
    
    /**
     * Represents an item in a trade template.
     */
    public static class TradeTemplateItem {
        private final String type; // "itemx" or "regular"
        private final String itemId; // ItemX ID or material name
        private final int amount;
        private final Map<String, Object> properties;
        
        public TradeTemplateItem(String type, String itemId, int amount, Map<String, Object> properties) {
            this.type = Objects.requireNonNull(type, "Item type cannot be null");
            this.itemId = Objects.requireNonNull(itemId, "Item ID cannot be null");
            this.amount = Math.max(1, amount);
            this.properties = properties != null ? Map.copyOf(properties) : Map.of();
        }
        
        public String getType() { return type; }
        public String getItemId() { return itemId; }
        public int getAmount() { return amount; }
        public Map<String, Object> getProperties() { return properties; }
        
        public boolean isItemX() { return "itemx".equalsIgnoreCase(type); }
        public boolean isRegular() { return "regular".equalsIgnoreCase(type); }
        
        public boolean isValid() {
            return type != null && !type.isBlank() && 
                   itemId != null && !itemId.isBlank() && 
                   amount > 0;
        }
        
        public Object getProperty(String key) {
            return properties.get(key);
        }
        
        @SuppressWarnings("unchecked")
        public <T> T getProperty(String key, T defaultValue) {
            Object value = properties.get(key);
            if (value != null && defaultValue.getClass().isInstance(value)) {
                return (T) value;
            }
            return defaultValue;
        }
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        TradeTemplate that = (TradeTemplate) obj;
        return Objects.equals(id, that.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "TradeTemplate{" +
               "id='" + id + '\'' +
               ", name='" + name + '\'' +
               ", category='" + category + '\'' +
               ", inputs=" + inputItems.size() +
               ", enabled=" + enabled +
               '}';
    }
}