package dev.oakheart.oakvillagers;

import dev.oakheart.message.MessageManager;
import dev.oakheart.oakvillagers.commands.OakVillagersCommand;
import dev.oakheart.oakvillagers.config.ConfigManager;
import dev.oakheart.oakvillagers.listeners.TradeListener;
import dev.oakheart.oakvillagers.service.TradeFilter;
import dev.oakheart.util.DebugLogger;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

public class OakVillagers extends JavaPlugin {

    private ConfigManager configManager;
    private MessageManager messageManager;
    private DebugLogger debugLogger;
    private TradeFilter tradeFilter;

    @Override
    public void onEnable() {
        try {
            checkDependencies();
            initializeComponents();
            registerListeners();
            registerCommands();
            getLogger().info("OakVillagers has been enabled!");
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Failed to enable OakVillagers", e);
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        getLogger().info("OakVillagers has been disabled!");
    }

    // Two plugins blocking the same trades would fight over rerolls.
    private void checkDependencies() {
        if (getServer().getPluginManager().getPlugin("TradeManager") != null) {
            getLogger().warning("TradeManager is installed too. Remove it: OakVillagers replaces its emerald-trade blocking.");
        }
    }

    private void initializeComponents() {
        configManager = new ConfigManager(this);
        configManager.load();

        debugLogger = new DebugLogger(getLogger(), configManager::isDebug);

        messageManager = new MessageManager(this, getLogger());
        messageManager.load();

        tradeFilter = new TradeFilter(this, configManager, debugLogger);
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new TradeListener(configManager, tradeFilter, debugLogger), this);
    }

    private void registerCommands() {
        new OakVillagersCommand(this, messageManager).register();
    }

    public boolean reloadPlugin() {
        if (!configManager.reload()) {
            return false;
        }
        messageManager.reload();
        return true;
    }
}
