package org.saberdev.nexoclaim;

import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.FPlayers;
import com.massivecraft.factions.Faction;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public final class NexoClaimProtectorListener implements Listener {

    private final NexoClaimProtectorManager manager;
    private final NexoCompat nexo;

    public NexoClaimProtectorListener(NexoClaimProtectorManager manager) {
        this.manager = manager;
        this.nexo = NexoCompat.get();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (!manager.isEnabled() || !nexo.isAvailable()) return;
        ItemStack item = event.getItemInHand();
        String nexoId = nexo.idOf(item);
        if (nexoId == null) return;
        ProtectorDefinition def = manager.getDefinition(nexoId);
        if (def == null) return;

        Player player = event.getPlayer();
        FPlayer fp = FPlayers.getInstance().getByPlayer(player);
        if (fp == null || !fp.hasFaction()) {
            player.sendMessage(ChatColor.RED + "[BaseClaimProtector] " + ChatColor.GRAY
                    + "Vous devez appartenir à une faction.");
            event.setCancelled(true);
            return;
        }
        Faction faction = fp.getFaction();
        ProtectorData data = manager.onPlace(player, event.getBlock().getLocation(), def, faction);
        if (data == null) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onBreak(BlockBreakEvent event) {
        if (!manager.isEnabled()) return;
        Location loc = event.getBlock().getLocation();
        if (manager.getAt(loc) == null) return;
        if (event.isCancelled()) return;
        manager.onBreak(event.getPlayer(), loc);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (!manager.isEnabled() || !nexo.isAvailable()) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getClickedBlock() == null) return;
        ItemStack hand = event.getItem();
        if (hand == null) return;

        Location blockLoc = event.getClickedBlock().getLocation();
        ProtectorData data = manager.getAt(blockLoc);
        if (data == null) return;

        Player player = event.getPlayer();
        FPlayer fp = FPlayers.getInstance().getByPlayer(player);
        if (fp == null || fp.getFaction() == null
                || !fp.getFaction().getId().equals(data.getFactionId())) {
            player.sendMessage(ChatColor.RED + "[BaseClaimProtector] " + ChatColor.GRAY
                    + "Seuls les membres de la faction peuvent nourrir ce bloc.");
            event.setCancelled(true);
            return;
        }

        String foodId = nexo.idOf(hand);
        if (foodId == null) return;
        if (!manager.onFeed(player, blockLoc, foodId)) {
            return;
        }

        event.setCancelled(true);
        if (player.getGameMode().name().equals("CREATIVE")) return;
        ItemStack remaining = hand.clone();
        remaining.setAmount(remaining.getAmount() - 1);
        if (remaining.getAmount() <= 0) {
            player.getInventory().setItemInMainHand(null);
        } else {
            player.getInventory().setItemInMainHand(remaining);
        }
    }
}