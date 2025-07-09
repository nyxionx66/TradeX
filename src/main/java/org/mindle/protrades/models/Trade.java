package org.mindle.protrades.models;

import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Objects;

/**
 * Represents a trade with input items and an output item.
 * Uses Java 21 record for immutable data structure.
 */
public record Trade(
        String id,
        List<ItemStack> inputs,
        ItemStack output
) {
    
    public Trade {
        // Compact constructor for validation
        Objects.requireNonNull(id, "Trade ID cannot be null");
        Objects.requireNonNull(inputs, "Trade inputs cannot be null");
        Objects.requireNonNull(output, "Trade output cannot be null");
        
        if (id.isBlank()) {
            throw new IllegalArgumentException("Trade ID cannot be blank");
        }
        
        if (inputs.isEmpty()) {
            throw new IllegalArgumentException("Trade must have at least one input");
        }
        
        // Ensure inputs list is immutable
        inputs = List.copyOf(inputs);
    }
    
    /**
     * Creates a new Trade with the given parameters.
     * 
     * @param id The unique identifier for this trade
     * @param inputs List of required input items
     * @param output The output item given when trade is completed
     * @return A new Trade instance
     */
    public static Trade of(String id, List<ItemStack> inputs, ItemStack output) {
        return new Trade(id, inputs, output);
    }
    
    /**
     * Checks if this trade is valid (has all required components).
     * 
     * @return true if the trade is valid, false otherwise
     */
    public boolean isValid() {
        return id != null && !id.isBlank() && 
               inputs != null && !inputs.isEmpty() && 
               output != null;
    }
}