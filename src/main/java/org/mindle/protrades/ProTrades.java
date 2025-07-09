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
import java.util.logging.Level;

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
            
            // Create trades directory if it doesn't exist
            File tradesDir = new File(getDataFolder(), "trades");
            if (!tradesDir.exists()) {
                tradesDir.mkdirs();
            }

            // Initialize managers
            this.configManager = new ConfigManager(this);
            this.nbtManager = new NBTManager(this);
            this.tradeManager = new TradeManager(this, configManager);
            this.guiManager = new GUIManager(this, tradeManager);

            // Register commands
            var ProTradesCommand = new ProTradesCommand(this, tradeManager, guiManager);
            var tradeCommand = new TradeCommand(this, tradeManager, guiManager);
            var nbtCommand = new org.mindle.protrades.commands.NBTCommand(this, tradeManager);
            
            getCommand("protrades").setExecutor(ProTradesCommand);
            getCommand("trade").setExecutor(tradeCommand);
            getCommand("ptnbt").setExecutor(nbtCommand);
            getCommand("ptnbt").setTabCompleter(nbtCommand);

            // Register listeners
            getServer().getPluginManager().registerEvents(new InventoryClickListener(this, tradeManager, guiManager), this);
            getServer().getPluginManager().registerEvents(new org.mindle.protrades.listeners.NBTTradeListener(this), this);

            // Load all trades
            tradeManager.loadAllTrades();

            getLogger().info("ProTrades has been enabled successfully with NBT support!");
            getLogger().info("ProItems integration: " + (isProItemsInstalled() ? "ENABLED" : "DISABLED"));
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Failed to enable ProTrades", e);
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        try {
            if (tradeManager != null) {
                tradeManager.saveAllTrades();
            }
            if (nbtManager != null) {
                nbtManager.clearCache();
            }
            getLogger().info("ProTrades has been disabled successfully!");
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Error during plugin disable", e);
        }
    }

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

    /**
     * Checks if ProItems plugin is installed and enabled.
     */
    public boolean isProItemsInstalled() {
        return getServer().getPluginManager().getPlugin("ProItems") != null &&
               getServer().getPluginManager().isPluginEnabled("ProItems");
    }
}