package com.massivecraft.factions.cmd.outpost;

import com.massivecraft.factions.FactionsPlugin;
import com.massivecraft.factions.cmd.CommandContext;
import com.massivecraft.factions.cmd.CommandRequirements;
import com.massivecraft.factions.cmd.FCommand;
import com.massivecraft.factions.struct.Permission;
import com.massivecraft.factions.zcore.util.TL;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.saberdev.outpost.OutpostManager;
import org.saberdev.outpost.OutpostRegion;
import org.saberdev.outpost.OutpostSelection;

import java.util.Collections;

public class CmdAdminApSave extends FCommand {

    public CmdAdminApSave() {
        super();
        this.getAliases().addAll(Collections.singletonList("save"));
        this.getRequiredArgs().add("name");
        this.setRequirements(new CommandRequirements.Builder(Permission.OUTPOST_ADMIN).playerOnly().build());
    }

    @Override
    public void perform(CommandContext context) {
        String name = context.argAsString(0);
        if (name == null || name.isEmpty()) {
            context.player.sendMessage(ChatColor.RED + "[Outpost] Provide a region name.");
            return;
        }

        OutpostManager manager = FactionsPlugin.getInstance().getOutpostManager();
        if (manager.getByName(name) != null) {
            context.player.sendMessage(ChatColor.RED + "[Outpost] A region named " + name + " already exists.");
            return;
        }

        Location p1 = OutpostSelection.getPos1(context.player.getUniqueId());
        Location p2 = OutpostSelection.getPos2(context.player.getUniqueId());
        if (p1 == null || p2 == null) {
            context.player.sendMessage(ChatColor.RED + "[Outpost] Set both positions with the wand first.");
            return;
        }
        if (!p1.getWorld().equals(p2.getWorld())) {
            context.player.sendMessage(ChatColor.RED + "[Outpost] Both positions must be in the same world.");
            return;
        }

        int cx1 = p1.getBlockX() >> 4;
        int cz1 = p1.getBlockZ() >> 4;
        int cx2 = p2.getBlockX() >> 4;
        int cz2 = p2.getBlockZ() >> 4;
        OutpostRegion region = new OutpostRegion(name, p1.getWorld().getName(), cx1, cz1, cx2, cz2);
        if (!manager.register(region)) {
            context.player.sendMessage(ChatColor.RED + "[Outpost] Could not register region.");
            return;
        }

        OutpostSelection.clear(context.player.getUniqueId());
        context.player.sendMessage(ChatColor.GOLD + "[Outpost] " + ChatColor.GRAY + "Saved region " + ChatColor.YELLOW + name + ChatColor.GRAY
                + " (" + region.getChunkWidth() + "x" + region.getChunkDepth() + " chunks, full vertical column).");
    }

    @Override
    public TL getUsageTranslation() {
        return TL.GENERIC_PLACEHOLDER;
    }
}