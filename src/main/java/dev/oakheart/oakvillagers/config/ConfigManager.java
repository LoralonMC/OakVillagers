package dev.oakheart.oakvillagers.config;

import dev.oakheart.oakvillagers.OakVillagers;
import org.bukkit.Material;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

public class ConfigManager {

    private static final int DEFAULT_TRADES_PER_LEVEL = 2;
    private static final int DEFAULT_MAX_ATTEMPTS = 12;

    private final OakVillagers plugin;
    private final Logger logger;
    private final File configFile;
    private dev.oakheart.config.ConfigManager config;

    // Cached config values
    private boolean enabled;
    private boolean debug;
    private Set<Material> blockedResults = Set.of();
    private Set<String> disabledWorlds = Set.of();
    private boolean wanderingTraders;
    private boolean rerollEnabled;
    private int tradesPerLevel;
    private int maxAttempts;
    private boolean healOnInteract;

    public ConfigManager(OakVillagers plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.configFile = new File(plugin.getDataFolder(), "config.yml");
    }

    public void load() {
        if (!configFile.exists()) {
            plugin.saveResource("config.yml", false);
        }

        try {
            config = dev.oakheart.config.ConfigManager.load(configFile.toPath());
        } catch (IOException e) {
            throw new RuntimeException("Failed to load config.yml", e);
        }

        try (InputStream in = plugin.getResource("config.yml")) {
            if (in != null) {
                var defaults = dev.oakheart.config.ConfigManager.fromStream(in);
                boolean merged = config.mergeDefaults(defaults);
                boolean synced = config.syncComments(defaults,
                        configFile.getParentFile().toPath().resolve(".oakheart/config-baseline.yml"));
                if (merged || synced) {
                    config.save();
                }
            }
        } catch (IOException e) {
            logger.warning("Could not evolve config.yml against JAR defaults: " + e.getMessage());
        }

        if (!validate()) {
            throw new RuntimeException("Configuration validation failed");
        }

        cacheValues();
    }

    public boolean reload() {
        try {
            config.reload();
        } catch (IOException e) {
            logger.warning("Failed to reload config.yml: " + e.getMessage());
            return false;
        }

        if (!validate()) {
            logger.warning("Configuration reload failed validation. Keeping previous configuration.");
            return false;
        }

        cacheValues();
        logger.info("Configuration reloaded successfully.");
        return true;
    }

    private boolean validate() {
        List<String> warnings = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        List<String> blocked = config.getStringList("blocked-results");
        if (blocked.isEmpty()) {
            warnings.add("'blocked-results' is empty, villagers can pay out anything");
        }
        for (String name : blocked) {
            Material material = Material.matchMaterial(name);
            if (material == null || !material.isItem()) {
                errors.add("Unknown item in 'blocked-results': '" + name + "'");
            }
        }
        if (config.getInt("reroll.trades-per-level", DEFAULT_TRADES_PER_LEVEL) < 1) {
            warnings.add("'reroll.trades-per-level' must be at least 1, using 1");
        }
        if (config.getInt("reroll.max-attempts", DEFAULT_MAX_ATTEMPTS) < 0) {
            warnings.add("'reroll.max-attempts' cannot be negative, using 0");
        }

        if (!warnings.isEmpty() || !errors.isEmpty()) {
            logger.warning("=== Configuration Warnings ===");
            warnings.forEach(w -> logger.warning("  - " + w));
            errors.forEach(e -> logger.warning("  - ERROR: " + e));
        }
        return errors.isEmpty();
    }

    private void cacheValues() {
        enabled = config.getBoolean("enabled", true);
        debug = config.getBoolean("debug", false);
        Set<Material> blocked = EnumSet.noneOf(Material.class);
        for (String name : config.getStringList("blocked-results")) {
            Material material = Material.matchMaterial(name);
            if (material != null) {
                blocked.add(material);
            }
        }
        blockedResults = Set.copyOf(blocked);
        disabledWorlds = Set.copyOf(config.getStringList("disabled-worlds"));
        wanderingTraders = config.getBoolean("wandering-traders", true);
        rerollEnabled = config.getBoolean("reroll.enabled", true);
        tradesPerLevel = Math.max(1, config.getInt("reroll.trades-per-level", DEFAULT_TRADES_PER_LEVEL));
        maxAttempts = Math.max(0, config.getInt("reroll.max-attempts", DEFAULT_MAX_ATTEMPTS));
        healOnInteract = config.getBoolean("heal-on-interact", true);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isDebug() {
        return debug;
    }

    public boolean isBlocked(Material material) {
        return blockedResults.contains(material);
    }

    public boolean isWorldDisabled(String world) {
        return disabledWorlds.contains(world);
    }

    public boolean isWanderingTraders() {
        return wanderingTraders;
    }

    public boolean isRerollEnabled() {
        return rerollEnabled;
    }

    public int getTradesPerLevel() {
        return tradesPerLevel;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public boolean isHealOnInteract() {
        return healOnInteract;
    }
}
