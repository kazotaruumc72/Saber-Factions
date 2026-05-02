package com.massivecraft.factions.cmd.outpost;

import com.massivecraft.factions.Faction;
import com.massivecraft.factions.Factions;
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

public class CmdAdminApUnset extends FCommand {

    public CmdAdminApUnset() {
        super();
        this.getAliases().addAll(Collections.singletonList("unset"));
        this.getRequiredArgs().add("faction");
        this.setRequirements(new CommandRequirements.Builder(Permission.OUTPOST_ADMIN).playerOnly().build());
    }

    @Override
    public void perform(CommandContext context) {
        OutpostManager manager = FactionsPlugin.getInstance().getOutpostManager();
        OutpostRegion region = manager.getAt(context.player.getLocation());
        if (region == null) {
            context.player.sendMessage(ChatColor.RED + "[Outpost] You must stand inside an assigned outpost region.");
            return;
        }
        if (!region.hasOwner()) {
            context.player.sendMessage(ChatColor.RED + "[Outpost] This region has no owner.");
            return;
        }

        Faction target = context.argAsFaction(0);
        if (target == null) return;
        Faction owner = Factions.getInstance().getFactionById(region.getOwnerFactionId());
        if (owner == null || !owner.equals(target)) {
            context.player.sendMessage(ChatColor.RED + "[Outpost] This region is not assigned to " + target.getTag() + ".");
            return;
        }

        context.player.sendMessage(ChatColor.GRAY + "[Outpost] Restoring terrain for " + region.getName() + " (this may lag for large regions)...");
        if (!manager.restore(region)) {
            context.player.sendMessage(ChatColor.RED + "[Outpost] No snapshot found or restore failed; ownership cleared anyway.");
        }

        region.setOwnerFactionId(null);
        manager.persistOwnership();
        context.player.sendMessage(ChatColor.GOLD + "[Outpost] " + ChatColor.GRAY + "Region " + ChatColor.YELLOW + region.getName()
                + ChatColor.GRAY + " released from " + target.getTag() + ChatColor.GRAY + " and terrain restored.");
    }

    @Override
    public TL getUsageTranslation() {
        return TL.GENERIC_PLACEHOLDER;
    }
}