package org.mindle.protrades.itemx;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for handling all color formats supported by ItemX.
 * Supports MiniMessage, legacy color codes, and hex color codes.
 */
public class ColorUtil {
    
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.legacyAmpersand();
    
    // Regex patterns for different color formats
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private static final Pattern MINI_MESSAGE_PATTERN = Pattern.compile("<[^>]+>");
    
    /**
     * Processes a string with all supported color formats and returns a colored Component.
     * Supports MiniMessage, legacy codes, and hex codes.
     */
    public static Component colorize(String text) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }
        
        // First, convert hex codes to MiniMessage format
        String processedText = convertHexToMiniMessage(text);
        
        // Check if it contains MiniMessage tags
        if (MINI_MESSAGE_PATTERN.matcher(processedText).find()) {
            try {
                // Parse with MiniMessage and remove italic by default
                return MINI_MESSAGE.deserialize(processedText)
                        .decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
            } catch (Exception e) {
                // Fall back to legacy parsing if MiniMessage fails
                return LEGACY_SERIALIZER.deserialize(processedText)
                        .decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
            }
        } else {
            // Parse as legacy color codes
            return LEGACY_SERIALIZER.deserialize(processedText)
                    .decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
        }
    }
    
    /**
     * Processes a list of strings with color formatting.
     */
    public static List<Component> colorizeList(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return List.of();
        }
        
        return texts.stream()
                .map(ColorUtil::colorize)
                .toList();
    }
    
    /**
     * Converts hex color codes (&#RRGGBB) to MiniMessage format.
     */
    private static String convertHexToMiniMessage(String text) {
        if (text == null) return "";
        
        Matcher matcher = HEX_PATTERN.matcher(text);
        StringBuffer result = new StringBuffer();
        
        while (matcher.find()) {
            String hexCode = matcher.group(1);
            matcher.appendReplacement(result, "<color:#" + hexCode + ">");
        }
        matcher.appendTail(result);
        
        return result.toString();
    }
    
    /**
     * Removes all color formatting from a string.
     */
    public static String stripColor(String text) {
        if (text == null) return "";
        
        // Remove MiniMessage tags
        String result = text.replaceAll("<[^>]+>", "");
        
        // Remove legacy color codes
        result = result.replaceAll("&[0-9a-fk-or]", "");
        
        // Remove hex codes
        result = result.replaceAll("&#[A-Fa-f0-9]{6}", "");
        
        return result;
    }
    
    /**
     * Checks if a string contains any color formatting.
     */
    public static boolean hasColor(String text) {
        if (text == null) return false;
        
        return text.contains("&") || text.contains("<") || HEX_PATTERN.matcher(text).find();
    }
    
    /**
     * Converts a Component back to a legacy string for storage.
     */
    public static String componentToLegacy(Component component) {
        if (component == null) return "";
        return LEGACY_SERIALIZER.serialize(component);
    }
    
    /**
     * Converts a Component to MiniMessage format for storage.
     */
    public static String componentToMiniMessage(Component component) {
        if (component == null) return "";
        return MINI_MESSAGE.serialize(component);
    }
    
    /**
     * Creates a gradient text component.
     */
    public static Component createGradient(String text, String startColor, String endColor) {
        if (text == null || text.isEmpty()) return Component.empty();
        
        String miniMessageText = String.format("<gradient:%s:%s>%s</gradient>", 
                startColor, endColor, text);
        
        try {
            return MINI_MESSAGE.deserialize(miniMessageText)
                    .decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
        } catch (Exception e) {
            return Component.text(text)
                    .decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
        }
    }
    
    /**
     * Creates a rainbow text component.
     */
    public static Component createRainbow(String text) {
        if (text == null || text.isEmpty()) return Component.empty();
        
        String miniMessageText = String.format("<rainbow>%s</rainbow>", text);
        
        try {
            return MINI_MESSAGE.deserialize(miniMessageText)
                    .decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
        } catch (Exception e) {
            return Component.text(text)
                    .decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
        }
    }
    
    /**
     * Processes placeholders in text.
     */
    public static String processPlaceholders(String text, String... replacements) {
        if (text == null) return "";
        
        String result = text;
        for (int i = 0; i < replacements.length - 1; i += 2) {
            result = result.replace(replacements[i], replacements[i + 1]);
        }
        
        return result;
    }
}