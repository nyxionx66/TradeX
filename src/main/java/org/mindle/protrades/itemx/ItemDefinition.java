package org.mindle.protrades.itemx;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.trim.ArmorTrim;
import org.bukkit.inventory.meta.trim.TrimMaterial;
import org.bukkit.inventory.meta.trim.TrimPattern;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Represents a custom item definition with all ItemX properties.
 * This class holds all the configuration data for a custom item.
 */
public class ItemDefinition {
    
    private final String id;
    private final Material material;
    private final String name;
    private final List<String> lore;
    private final boolean unbreakable;
    private final boolean useVanillaLore;
    private final Map<String, Integer> enchantments;
    private final boolean disableUse;
    private final String nbtId;
    private final ArmorTrimData armorTrim;
    private final Map<String, Object> customNBT;
    
    public ItemDefinition(String id, Material material, String name, List<String> lore,
                         boolean unbreakable, boolean useVanillaLore, Map<String, Integer> enchantments,
                         boolean disableUse, String nbtId, ArmorTrimData armorTrim,
                         Map<String, Object> customNBT) {
        this.id = Objects.requireNonNull(id, "Item ID cannot be null");
        this.material = Objects.requireNonNull(material, "Material cannot be null");
        this.name = name;
        this.lore = lore != null ? List.copyOf(lore) : List.of();
        this.unbreakable = unbreakable;
        this.useVanillaLore = useVanillaLore;
        this.enchantments = enchantments != null ? Map.copyOf(enchantments) : Map.of();
        this.disableUse = disableUse;
        this.nbtId = nbtId != null ? nbtId : id;
        this.armorTrim = armorTrim;
        this.customNBT = customNBT != null ? Map.copyOf(customNBT) : Map.of();
    }
    
    public String getId() { return id; }
    public Material getMaterial() { return material; }
    public String getName() { return name; }
    public List<String> getLore() { return lore; }
    public boolean isUnbreakable() { return unbreakable; }
    public boolean isUseVanillaLore() { return useVanillaLore; }
    public Map<String, Integer> getEnchantments() { return enchantments; }
    public boolean isDisableUse() { return disableUse; }
    public String getNbtId() { return nbtId; }
    public ArmorTrimData getArmorTrim() { return armorTrim; }
    public Map<String, Object> getCustomNBT() { return customNBT; }
    
    /**
     * Checks if this item definition is valid.
     */
    public boolean isValid() {
        return id != null && !id.isBlank() && material != null && material != Material.AIR;
    }
    
    /**
     * Gets the display name for this item, or the formatted material name if no custom name.
     */
    public String getDisplayName() {
        if (name != null && !name.isBlank()) {
            return name;
        }
        return formatMaterialName(material);
    }
    
    /**
     * Formats a material name to be more readable.
     */
    private String formatMaterialName(Material material) {
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
     * Represents armor trim data for an item.
     */
    public static class ArmorTrimData {
        private final String pattern;
        private final String material;
        
        public ArmorTrimData(String pattern, String material) {
            this.pattern = pattern;
            this.material = material;
        }
        
        public String getPattern() { return pattern; }
        public String getMaterial() { return material; }
        
        public boolean isValid() {
            return pattern != null && !pattern.isBlank() && 
                   material != null && !material.isBlank();
        }
        
        /**
         * Parses the trim pattern from string.
         */
        public TrimPattern parseTrimPattern() {
            if (pattern == null) return null;
            
            try {
                return TrimPattern.valueOf(pattern.toUpperCase());
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
        
        /**
         * Parses the trim material from string.
         */
        public TrimMaterial parseTrimMaterial() {
            if (material == null) return null;
            
            try {
                return TrimMaterial.valueOf(material.toUpperCase());
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        ItemDefinition that = (ItemDefinition) obj;
        return Objects.equals(id, that.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "ItemDefinition{" +
               "id='" + id + '\'' +
               ", material=" + material +
               ", name='" + name + '\'' +
               ", hasLore=" + !lore.isEmpty() +
               ", unbreakable=" + unbreakable +
               ", hasEnchantments=" + !enchantments.isEmpty() +
               ", disableUse=" + disableUse +
               '}';
    }
}