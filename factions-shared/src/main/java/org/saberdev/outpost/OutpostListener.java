package org.saberdev.outpost;

import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.FPlayers;
import com.massivecraft.factions.Faction;
import com.massivecraft.factions.Factions;
import com.massivecraft.factions.FactionsPlugin;
import com.massivecraft.factions.struct.Permission;
import com.massivecraft.factions.struct.Relation;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class OutpostListener implements Listener {

    private final OutpostManager manager;

    public OutpostListener(OutpostManager manager) {
        this.manager = manager;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onWandInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!OutpostWand.isWand(event.getItem())) return;
        if (!Permission.OUTPOST_ADMIN.has(player, false)) return;

        Block block = event.getClickedBlock();
        Action action = event.getAction();
        if (block == null) return;

        if (action == Action.LEFT_CLICK_BLOCK) {
            event.setCancelled(true);
            OutpostSelection.setPos1(player.getUniqueId(), block.getLocation());
            player.sendMessage(ChatColor.GOLD + "[Outpost] " + ChatColor.GRAY + "Pos 1 set to " + format(block.getLocation()));
        } else if (action == Action.RIGHT_CLICK_BLOCK) {
            event.setCancelled(true);
            OutpostSelection.setPos2(player.getUniqueId(), block.getLocation());
            player.sendMessage(ChatColor.GOLD + "[Outpost] " + ChatColor.GRAY + "Pos 2 set to " + format(block.getLocation()));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!event.isCancelled()) return;
        if (overrideAllowed(event.getPlayer(), event.getBlock().getLocation())) {
            event.setCancelled(false);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!event.isCancelled()) return;
        if (overrideAllowed(event.getPlayer(), event.getBlock().getLocation())) {
            event.setCancelled(false);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockPlaceDeny(BlockPlaceEvent event) {
        denyOutsiders(event.getPlayer(), event.getBlock().getLocation(), event::setCancelled);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockBreakDeny(BlockBreakEvent event) {
        denyOutsiders(event.getPlayer(), event.getBlock().getLocation(), event::setCancelled);
    }

    private void denyOutsiders(Player player, Location loc, java.util.function.Consumer<Boolean> cancel) {
        OutpostRegion region = manager.getAt(loc);
        if (region == null || !region.hasOwner()) return;
        if (Permission.OUTPOST_ADMIN.has(player, false)) return;
        if (canBuild(player, region)) return;
        cancel.accept(true);
    }

    private boolean overrideAllowed(Player player, Location loc) {
        OutpostRegion region = manager.getAt(loc);
        if (region == null || !region.hasOwner()) return false;
        return canBuild(player, region);
    }

    private boolean canBuild(Player player, OutpostRegion region) {
        FPlayer fp = FPlayers.getInstance().getByPlayer(player);
        if (fp == null || !fp.hasFaction()) return false;
        Faction owner = Factions.getInstance().getFactionById(region.getOwnerFactionId());
        if (owner == null) return false;
        if (fp.getFaction().equals(owner)) return true;
        Relation rel = fp.getFaction().getRelationTo(owner);
        return rel == Relation.ALLY || rel == Relation.MEMBER;
    }

    private String format(Location l) {
        return l.getWorld().getName() + " " + l.getBlockX() + "," + l.getBlockY() + "," + l.getBlockZ();
    }
}
