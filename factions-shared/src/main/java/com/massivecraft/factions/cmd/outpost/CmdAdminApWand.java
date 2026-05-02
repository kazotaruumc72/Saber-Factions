package com.massivecraft.factions.cmd.outpost;

import com.massivecraft.factions.cmd.CommandContext;
import com.massivecraft.factions.cmd.CommandRequirements;
import com.massivecraft.factions.cmd.FCommand;
import com.massivecraft.factions.struct.Permission;
import com.massivecraft.factions.zcore.util.TL;
import org.bukkit.ChatColor;
import org.saberdev.outpost.OutpostWand;

import java.util.Collections;

public class CmdAdminApWand extends FCommand {

    public CmdAdminApWand() {
        super();
        this.getAliases().addAll(Collections.singletonList("wand"));
        this.setRequirements(new CommandRequirements.Builder(Permission.OUTPOST_ADMIN).playerOnly().build());
    }

    @Override
    public void perform(CommandContext context) {
        context.player.getInventory().addItem(OutpostWand.create());
        context.player.sendMessage(ChatColor.GOLD + "[Outpost] " + ChatColor.GRAY + "Wand granted. Left-click & right-click blocks to set positions, then run " + ChatColor.YELLOW + "/f admin ap save <name>" + ChatColor.GRAY + ".");
    }

    @Override
    public TL getUsageTranslation() {
        return TL.GENERIC_PLACEHOLDER;
    }
}