package dev.oakheart.oakvillagers.listeners;

import dev.oakheart.oakvillagers.service.TradeFilter;
import dev.oakheart.oakvillagers.config.ConfigManager;
import dev.oakheart.util.DebugLogger;
import io.papermc.paper.event.player.PlayerTradeEvent;
import org.bukkit.entity.AbstractVillager;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.VillagerAcquireTradeEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;

/**
 * Blocks emerald-paying trades as villagers roll them, rerolls the gap, heals villagers
 * when a player opens them, and refuses any blocked trade that still slips through.
 */
public class TradeListener implements Listener {

    private final ConfigManager configManager;
    private final TradeFilter filter;
    private final DebugLogger debug;

    public TradeListener(ConfigManager configManager, TradeFilter filter, DebugLogger debug) {
        this.configManager = configManager;
        this.filter = filter;
        this.debug = debug;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onAcquireTrade(VillagerAcquireTradeEvent event) {
        AbstractVillager merchant = event.getEntity();
        if (!filter.isManaged(merchant)) {
            return;
        }
        if (filter.isBlocked(event.getRecipe())) {
            event.setCancelled(true);
            if (merchant instanceof Villager villager) {
                filter.scheduleTopUp(villager);
            }
            return;
        }
        if (filter.isToppingUp(merchant) && filter.isDuplicate(merchant, event.getRecipe())) {
            event.setCancelled(true);
        }
    }

    // LOW so the trades are fixed before the villager decides whether to open its screen.
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onInteract(PlayerInteractEntityEvent event) {
        if (!configManager.isHealOnInteract() || event.getHand() != EquipmentSlot.HAND
                || !(event.getRightClicked() instanceof AbstractVillager merchant) || !filter.isManaged(merchant)) {
            return;
        }
        filter.heal(merchant);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onTrade(PlayerTradeEvent event) {
        if (filter.isManaged(event.getVillager()) && filter.isBlocked(event.getTrade())) {
            event.setCancelled(true);
            debug.log(() -> "Refused a blocked trade for " + event.getPlayer().getName());
        }
    }
}
