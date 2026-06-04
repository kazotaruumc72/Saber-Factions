package org.saberdev.mdf;

import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.FPlayers;
import com.massivecraft.factions.Faction;
import com.massivecraft.factions.Factions;
import com.massivecraft.factions.struct.Permission;
import com.massivecraft.factions.struct.Role;
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

public class MdfListener implements Listener {

    private final MdfManager manager;

    public MdfListener(MdfManager manager) {
        this.manager = manager;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onWandInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!MdfWand.isWand(event.getItem())) return;
        if (!Permission.MDF_ADMIN.has(player, false)) return;

        Block block = event.getClickedBlock();
        Action action = event.getAction();
        if (block == null) return;

        if (action == Action.LEFT_CLICK_BLOCK) {
            event.setCancelled(true);
            MdfSelection.setPos1(player.getUniqueId(), block.getLocation());
            player.sendMessage(ChatColor.AQUA + "[MDF] " + ChatColor.GRAY + "Pos 1 : " + format(block.getLocation()));
        } else if (action == Action.RIGHT_CLICK_BLOCK) {
            event.setCancelled(true);
            MdfSelection.setPos2(player.getUniqueId(), block.getLocation());
            player.sendMessage(ChatColor.AQUA + "[MDF] " + ChatColor.GRAY + "Pos 2 : " + format(block.getLocation()));
        }
    }

    // Override WorldGuard / faction protection: re-allow building inside MDF zones for whitelisted players.
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

    // Deny outsiders even if other plugins would allow them.
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockPlaceDeny(BlockPlaceEvent event) {
        if (denyOutsider(event.getPlayer(), event.getBlock().getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockBreakDeny(BlockBreakEvent event) {
        if (denyOutsider(event.getPlayer(), event.getBlock().getLocation())) {
            event.setCancelled(true);
        }
    }

    private boolean denyOutsider(Player player, Location loc) {
        MdfZone zone = manager.getAt(loc);
        if (zone == null || !zone.hasOwner()) return false;
        if (Permission.MDF_ADMIN.has(player, false)) return false;
        return !canBuild(player, zone);
    }

    private boolean overrideAllowed(Player player, Location loc) {
        MdfZone zone = manager.getAt(loc);
        if (zone == null || !zone.hasOwner()) return false;
        if (Permission.MDF_ADMIN.has(player, false)) return true;
        return canBuild(player, zone);
    }

    private boolean canBuild(Player player, MdfZone zone) {
        FPlayer fp = FPlayers.getInstance().getByPlayer(player);
        if (fp == null || !fp.hasFaction()) return false;
        Faction owner = Factions.getInstance().getFactionById(zone.getOwnerFactionId());
        if (owner == null) return false;
        if (!fp.getFaction().equals(owner)) return false;
        if (fp.getRole() == Role.LEADER) return true;
        return zone.isWhitelisted(player.getUniqueId());
    }

    private String format(Location l) {
        return l.getWorld().getName() + " " + l.getBlockX() + "," + l.getBlockY() + "," + l.getBlockZ();
    }
}