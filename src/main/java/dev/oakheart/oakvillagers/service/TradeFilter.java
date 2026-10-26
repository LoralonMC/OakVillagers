package dev.oakheart.oakvillagers.service;

import dev.oakheart.oakvillagers.OakVillagers;
import dev.oakheart.oakvillagers.config.ConfigManager;
import dev.oakheart.util.DebugLogger;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.AbstractVillager;
import org.bukkit.entity.Villager;
import org.bukkit.entity.WanderingTrader;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Decides which trades a villager may have, and refills a villager from its own vanilla
 * trade pool when blocked trades were taken away.
 */
public class TradeFilter {

    // Shopkeepers and Citizens tag their entities with these; their trades are not ours.
    private static final String SHOPKEEPER_METADATA = "shopkeeper";
    private static final String NPC_METADATA = "NPC";

    private final OakVillagers plugin;
    private final ConfigManager configManager;
    private final DebugLogger debug;
    // Highest level a villager was topped up for, so a short pool isn't retried on every click.
    private final NamespacedKey toppedUpKey;
    private final Set<UUID> pending = new HashSet<>();
    private final Set<UUID> toppingUp = new HashSet<>();

    public TradeFilter(OakVillagers plugin, ConfigManager configManager, DebugLogger debug) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.debug = debug;
        this.toppedUpKey = new NamespacedKey(plugin, "topped_up_level");
    }

    /** True for real villagers and wandering traders this plugin should police. */
    public boolean isManaged(AbstractVillager merchant) {
        if (!configManager.isEnabled() || configManager.isWorldDisabled(merchant.getWorld().getName())) {
            return false;
        }
        if (merchant.hasMetadata(SHOPKEEPER_METADATA) || merchant.hasMetadata(NPC_METADATA)) {
            return false;
        }
        return !(merchant instanceof WanderingTrader) || configManager.isWanderingTraders();
    }

    public boolean isBlocked(MerchantRecipe recipe) {
        ItemStack result = recipe.getResult();
        return configManager.isBlocked(result.getType());
    }

    public boolean isToppingUp(AbstractVillager merchant) {
        return toppingUp.contains(merchant.getUniqueId());
    }

    /** True when the merchant already offers this exact trade. Rerolls can land on one. */
    public boolean isDuplicate(AbstractVillager merchant, MerchantRecipe recipe) {
        for (MerchantRecipe existing : merchant.getRecipes()) {
            if (existing.getResult().equals(recipe.getResult())
                    && existing.getIngredients().equals(recipe.getIngredients())) {
                return true;
            }
        }
        return false;
    }

    /** Tops the villager up next tick, once the trade roll that is running now has finished. */
    public void scheduleTopUp(Villager villager) {
        if (!configManager.isRerollEnabled() || toppingUp.contains(villager.getUniqueId())
                || !pending.add(villager.getUniqueId())) {
            return;
        }
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            pending.remove(villager.getUniqueId());
            if (villager.isValid()) {
                topUp(villager);
            }
        });
    }

    /**
     * Rolls more trades from the villager's own pool until it has its usual count for its
     * level, or the attempts run out. Blocked and duplicate rolls are rejected by the trade
     * listener as they happen. Returns the number of trades added.
     */
    public int topUp(Villager villager) {
        if (!configManager.isRerollEnabled() || !canTrade(villager)) {
            return 0;
        }
        int level = villager.getVillagerLevel();
        int target = level * configManager.getTradesPerLevel();
        int before = villager.getRecipeCount();
        toppingUp.add(villager.getUniqueId());
        try {
            for (int i = 0; i < configManager.getMaxAttempts() && villager.getRecipeCount() < target; i++) {
                villager.addTrades(1);
            }
        } finally {
            toppingUp.remove(villager.getUniqueId());
        }
        villager.getPersistentDataContainer().set(toppedUpKey, PersistentDataType.INTEGER, level);
        int added = villager.getRecipeCount() - before;
        if (added > 0 || villager.getRecipeCount() < target) {
            debug.log(() -> villager.getProfession().key().value() + " level " + level + " at "
                    + describe(villager) + ": +" + added + " trade(s), now " + villager.getRecipeCount() + "/" + target);
        }
        return added;
    }

    /**
     * Removes blocked trades the merchant already has and, for villagers not yet topped up
     * at their level, refills them. Returns true when anything changed.
     */
    public boolean heal(AbstractVillager merchant) {
        List<MerchantRecipe> kept = new ArrayList<>();
        for (MerchantRecipe recipe : merchant.getRecipes()) {
            if (!isBlocked(recipe)) {
                kept.add(recipe);
            }
        }
        int removed = merchant.getRecipeCount() - kept.size();
        if (removed > 0) {
            merchant.setRecipes(kept);
            debug.log(() -> "Removed " + removed + " blocked trade(s) from " + merchant.getType() + " at " + describe(merchant));
        }
        if (!(merchant instanceof Villager villager) || !canTrade(villager)) {
            return removed > 0;
        }
        Integer toppedUp = villager.getPersistentDataContainer().get(toppedUpKey, PersistentDataType.INTEGER);
        boolean due = removed > 0 || toppedUp == null || toppedUp < villager.getVillagerLevel();
        return (due && topUp(villager) > 0) || removed > 0;
    }

    private static boolean canTrade(Villager villager) {
        Villager.Profession profession = villager.getProfession();
        return profession != Villager.Profession.NONE && profession != Villager.Profession.NITWIT;
    }

    private static String describe(AbstractVillager merchant) {
        var loc = merchant.getLocation();
        return loc.getWorld().getName() + " " + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
    }
}
