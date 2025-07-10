package org.mindle.protrades;

import org.bukkit.plugin.java.JavaPlugin;
import org.mindle.protrades.commands.ProTradesCommand;
import org.mindle.protrades.commands.TradeCommand;
import org.mindle.protrades.listeners.InventoryClickListener;
import org.mindle.protrades.managers.ConfigManager;
import org.mindle.protrades.managers.GUIManager;
import org.mindle.protrades.managers.TradeManager;
import org.mindle.protrades.nbt.NBTManager;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

/**
 * ProTrades plugin - Trade System Manager
 * Focused on trade management and GUI-based trading functionality.
 */
public final class ProTrades extends JavaPlugin {

    private static ProTrades instance;
    private TradeManager tradeManager;
    private GUIManager guiManager;
    private ConfigManager configManager;
    private NBTManager nbtManager;

    @Override
    public void onEnable() {
        try {
            instance = this;
            
            // Load and save default configuration
            saveDefaultConfig();
            
            // Create required directories
            createDirectories();

            // Initialize managers in proper order
            initializeManagers();
            
            // Register commands
            registerCommands();

            // Register event listeners
            registerListeners();

            // Load configurations and data
            loadConfigurations();

            // Validate system
            validateSystem();

            // Success message
            logSuccessfulStartup();
            
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Failed to enable ProTrades", e);
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        try {
            // Save all data before shutdown
            if (tradeManager != null) {
                tradeManager.saveAllTrades();
            }
            
            // Clear caches
            if (nbtManager != null) {
                nbtManager.clearCache();
            }
            
            getLogger().info("ProTrades has been disabled successfully!");
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Error during plugin disable", e);
        }
    }

    /**
     * Creates required directories for the trade system.
     */
    private void createDirectories() {
        // Create main directories
        File dataFolder = getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        
        // Create configs directory structure
        File configsDir = new File(dataFolder, "configs");
        if (!configsDir.exists()) {
            configsDir.mkdirs();
        }
        
        File tradesDir = new File(configsDir, "trades");
        if (!tradesDir.exists()) {
            tradesDir.mkdirs();
        }
        
        getLogger().info("Created directory structure successfully");
    }

    /**
     * Initializes all managers in the correct order.
     */
    private void initializeManagers() {
        // Core managers
        this.configManager = new ConfigManager(this);
        this.nbtManager = new NBTManager(this);
        
        // Trade system managers
        this.tradeManager = new TradeManager(this, configManager);
        this.guiManager = new GUIManager(this, tradeManager);
        
        getLogger().info("Initialized all managers successfully");
    }

    /**
     * Registers all commands with their executors and tab completers.
     */
    private void registerCommands() {
        // Create command instances
        var proTradesCommand = new ProTradesCommand(this, tradeManager, guiManager);
        var tradeCommand = new TradeCommand(this, tradeManager, guiManager);
        var nbtCommand = new org.mindle.protrades.commands.NBTCommand(this, tradeManager);
        
        // Register commands
        getCommand("protrades").setExecutor(proTradesCommand);
        getCommand("trade").setExecutor(tradeCommand);
        getCommand("ptnbt").setExecutor(nbtCommand);
        getCommand("ptnbt").setTabCompleter(nbtCommand);
        
        getLogger().info("Registered all commands successfully");
    }

    /**
     * Registers all event listeners.
     */
    private void registerListeners() {
        getServer().getPluginManager().registerEvents(
            new InventoryClickListener(this, tradeManager, guiManager), this
        );
        getServer().getPluginManager().registerEvents(
            new org.mindle.protrades.listeners.NBTTradeListener(this), this
        );
        
        getLogger().info("Registered all event listeners successfully");
    }

    /**
     * Loads all configurations and data.
     */
    private void loadConfigurations() {
        // Load configurations
        reloadConfig();
        
        // Load system data
        tradeManager.loadAllTrades();
        itemManager.loadAllItems();
        templateManager.loadAllTemplates();
        
        getLogger().info("Loaded all configurations successfully");
    }

    /**
     * Validates the system configuration and setup.
     */
    private void validateSystem() {
        // Validate configuration structure
        if (!configManager.validateConfiguration()) {
            getLogger().warning("Configuration validation failed");
        }
        
        // Validate items
        if (!itemManager.validateAllItems()) {
            getLogger().warning("Item validation failed");
        }
        
        // Validate templates
        if (!templateManager.validateAllTemplates()) {
            getLogger().warning("Template validation failed");
        }
        
        getLogger().info("System validation completed");
    }

    /**
     * Logs successful startup information.
     */
    private void logSuccessfulStartup() {
        getLogger().info("=== ProTrades Enabled Successfully ===");
        getLogger().info("Version: " + getDescription().getVersion());
        getLogger().info("ItemX System: ENABLED");
        getLogger().info("Trade Templates: ENABLED");
        getLogger().info("Dynamic Trade Creation: ENABLED");
        getLogger().info("NBT Support: ENABLED");
        getLogger().info("ProItems Integration: " + (isProItemsInstalled() ? "ENABLED" : "DISABLED"));
        getLogger().info("Resource Structure: NEW (Organized)");
        getLogger().info("Items Loaded: " + itemManager.getAllItemIds().size());
        getLogger().info("Templates Loaded: " + templateManager.getAllTemplateIds().size());
        getLogger().info("Trades Loaded: " + tradeManager.getAllTradeIds().size());
        getLogger().info("=====================================");
    }

    /**
     * Reloads the entire plugin configuration.
     */
    public void reloadPlugin() {
        try {
            getLogger().info("Reloading ProTrades configuration...");
            
            // Reload main config
            reloadConfig();
            
            // Reload all managers
            itemManager.reload();
            templateManager.reload();
            tradeManager.loadAllTrades();
            
            // Validate after reload
            validateSystem();
            
            getLogger().info("ProTrades configuration reloaded successfully!");
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Error during configuration reload", e);
        }
    }

    // Getters for all managers
    public static ProTrades getInstance() {
        return instance;
    }

    public TradeManager getTradeManager() {
        return tradeManager;
    }

    public GUIManager getGUIManager() {
        return guiManager;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public NBTManager getNBTManager() {
        return nbtManager;
    }

    public ItemManager getItemManager() {
        return itemManager;
    }

    public TradeTemplateManager getTemplateManager() {
        return templateManager;
    }

    public TradeCreationGUI getTradeCreationGUI() {
        return tradeCreationGUI;
    }

    /**
     * Checks if ProItems plugin is installed and enabled.
     */
    public boolean isProItemsInstalled() {
        return getServer().getPluginManager().getPlugin("ProItems") != null &&
               getServer().getPluginManager().isPluginEnabled("ProItems");
    }

    /**
     * Gets plugin statistics.
     */
    public Map<String, Object> getPluginStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("version", getDescription().getVersion());
        stats.put("enabled", isEnabled());
        stats.put("items", itemManager.getStatistics());
        stats.put("templates", templateManager.getStatistics());
        stats.put("trades", tradeManager.getAllTradeIds().size());
        stats.put("proitems_integration", isProItemsInstalled());
        
        return stats;
    }
}