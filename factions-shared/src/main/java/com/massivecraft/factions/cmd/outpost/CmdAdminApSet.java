package com.massivecraft.factions.cmd.outpost;

import com.massivecraft.factions.Faction;
import com.massivecraft.factions.FactionsPlugin;
import com.massivecraft.factions.cmd.CommandContext;
import com.massivecraft.factions.cmd.CommandRequirements;
import com.massivecraft.factions.cmd.FCommand;
import com.massivecraft.factions.struct.Permission;
import com.massivecraft.factions.zcore.util.TL;
import org.bukkit.ChatColor;
import org.saberdev.outpost.OutpostManager;
import org.saberdev.outpost.OutpostRegion;

import java.util.Collections;

public class CmdAdminApSet extends FCommand {

    public CmdAdminApSet() {
        super();
        this.getAliases().addAll(Collections.singletonList("set"));
        this.getRequiredArgs().add("faction");
        this.setRequirements(new CommandRequirements.Builder(Permission.OUTPOST_ADMIN).playerOnly().build());
    }

    @Override
    public void perform(CommandContext context) {
        OutpostManager manager = FactionsPlugin.getInstance().getOutpostManager();
        OutpostRegion region = manager.getAt(context.player.getLocation());
        if (region == null) {
            context.player.sendMessage(ChatColor.RED + "[Outpost] You must stand inside a saved outpost region.");
            return;
        }

        Faction target = context.argAsFaction(0);
        if (target == null) return;
        if (!target.isNormal()) {
            context.player.sendMessage(ChatColor.RED + "[Outpost] Target must be a normal faction.");
            return;
        }

        if (region.hasOwner() && region.getOwnerFactionId().equals(target.getId())) {
            context.player.sendMessage(ChatColor.RED + "[Outpost] " + region.getName() + " is already assigned to " + target.getTag() + ".");
            return;
        }

        if (region.hasOwner()) {
            context.player.sendMessage(ChatColor.GRAY + "[Outpost] Restoring previous snapshot before reassigning...");
            manager.restore(region);
        }

        context.player.sendMessage(ChatColor.GRAY + "[Outpost] Capturing snapshot of region " + region.getName() + " (this may lag for large regions)...");
        if (!manager.snapshot(region)) {
            context.player.sendMessage(ChatColor.RED + "[Outpost] Snapshot failed; assignment aborted.");
            return;
        }

        region.setOwnerFactionId(target.getId());
        manager.persistOwnership();
        context.player.sendMessage(ChatColor.GOLD + "[Outpost] " + ChatColor.GRAY + "Region " + ChatColor.YELLOW + region.getName()
                + ChatColor.GRAY + " assigned to " + target.getTag() + ChatColor.GRAY + ". Members and allies can now build here.");
    }

    @Override
    public TL getUsageTranslation() {
        return TL.GENERIC_PLACEHOLDER;
    }
}