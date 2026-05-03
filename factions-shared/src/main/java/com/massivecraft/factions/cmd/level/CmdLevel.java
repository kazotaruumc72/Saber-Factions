package com.massivecraft.factions.cmd.level;

import com.massivecraft.factions.Faction;
import com.massivecraft.factions.cmd.Aliases;
import com.massivecraft.factions.cmd.CommandContext;
import com.massivecraft.factions.cmd.CommandRequirements;
import com.massivecraft.factions.cmd.FCommand;
import com.massivecraft.factions.struct.Permission;
import com.massivecraft.factions.util.LevelManager;
import com.massivecraft.factions.zcore.util.TL;
import org.bukkit.ChatColor;

public class CmdLevel extends FCommand {

    public final CmdLevelTop cmdLevelTop = new CmdLevelTop();

    public CmdLevel() {
        super();
        this.getAliases().addAll(Aliases.level);
        this.addSubCommand(this.cmdLevelTop);
        this.setRequirements(new CommandRequirements.Builder(Permission.LEVEL).playerOnly().memberOnly().build());
    }

    @Override
    public void perform(CommandContext context) {
        Faction faction = context.fPlayer.getFaction();
        LevelManager mgr = LevelManager.getInstance();
        int level = mgr.getLevel(faction);
        long xp = mgr.getXp(faction);
        long current = mgr.getCurrentThreshold(faction);
        long next = mgr.getNextThreshold(faction);
        int max = mgr.getMaxLevel();

        context.player.sendMessage(ChatColor.GOLD + "==== " + ChatColor.YELLOW + faction.getTag() + ChatColor.GOLD + " - Level ====");
        context.player.sendMessage(ChatColor.GRAY + "Level: " + ChatColor.AQUA + level + ChatColor.GRAY + " / " + ChatColor.AQUA + max);
        context.player.sendMessage(ChatColor.GRAY + "XP: " + ChatColor.GREEN + xp);

        if (next < 0) {
            context.player.sendMessage(ChatColor.GRAY + "Status: " + ChatColor.GOLD + "Max level reached.");
        } else {
            long span = next - current;
            long progress = xp - current;
            int pct = span <= 0 ? 100 : (int) Math.max(0, Math.min(100, (progress * 100) / span));
            context.player.sendMessage(ChatColor.GRAY + "Next: " + ChatColor.YELLOW + next + ChatColor.GRAY
                    + " (" + ChatColor.YELLOW + (next - xp) + ChatColor.GRAY + " XP to go, " + ChatColor.AQUA + pct + "%" + ChatColor.GRAY + ")");
            context.player.sendMessage(ChatColor.GRAY + bar(pct));
        }
        context.player.sendMessage(ChatColor.DARK_GRAY + "Use " + ChatColor.YELLOW + "/f level top" + ChatColor.DARK_GRAY + " for the leaderboard.");
    }

    private String bar(int pct) {
        int filled = pct / 5; // 20 segments
        StringBuilder sb = new StringBuilder(ChatColor.DARK_GRAY + "[");
        for (int i = 0; i < 20; i++) {
            sb.append(i < filled ? ChatColor.GREEN + "|" : ChatColor.GRAY + "|");
        }
        sb.append(ChatColor.DARK_GRAY).append("]");
        return sb.toString();
    }

    @Override
    public TL getUsageTranslation() {
        return TL.GENERIC_PLACEHOLDER;
    }
}