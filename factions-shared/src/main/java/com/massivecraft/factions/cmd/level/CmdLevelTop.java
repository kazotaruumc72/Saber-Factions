package com.massivecraft.factions.cmd.level;

import com.massivecraft.factions.Faction;
import com.massivecraft.factions.Factions;
import com.massivecraft.factions.cmd.Aliases;
import com.massivecraft.factions.cmd.CommandContext;
import com.massivecraft.factions.cmd.CommandRequirements;
import com.massivecraft.factions.cmd.FCommand;
import com.massivecraft.factions.struct.Permission;
import com.massivecraft.factions.util.LevelManager;
import com.massivecraft.factions.zcore.util.TL;
import org.bukkit.ChatColor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class CmdLevelTop extends FCommand {

    private static final int PAGE_SIZE = 10;

    public CmdLevelTop() {
        super();
        this.getAliases().addAll(Aliases.level_top);
        this.getOptionalArgs().put("page", "1");
        this.setRequirements(new CommandRequirements.Builder(Permission.LEVEL_TOP).playerOnly().build());
    }

    @Override
    public void perform(CommandContext context) {
        LevelManager mgr = LevelManager.getInstance();
        List<Faction> factions = new ArrayList<>(Factions.getInstance().getAllNormalFactions());
        factions.sort(Comparator
                .comparingInt((Faction f) -> mgr.getLevel(f)).reversed()
                .thenComparingLong((Faction f) -> mgr.getXp(f)).reversed());

        int page = Math.max(1, context.argAsInt(0, 1));
        int totalPages = Math.max(1, (int) Math.ceil(factions.size() / (double) PAGE_SIZE));
        page = Math.min(page, totalPages);

        int start = (page - 1) * PAGE_SIZE;
        int end = Math.min(factions.size(), start + PAGE_SIZE);

        context.player.sendMessage(ChatColor.GOLD + "==== Level Leaderboard "
                + ChatColor.GRAY + "(" + ChatColor.YELLOW + page + ChatColor.GRAY + "/" + ChatColor.YELLOW + totalPages + ChatColor.GRAY + ")" + ChatColor.GOLD + " ====");

        if (factions.isEmpty()) {
            context.player.sendMessage(ChatColor.GRAY + "No factions to rank yet.");
            return;
        }

        for (int i = start; i < end; i++) {
            Faction f = factions.get(i);
            int level = mgr.getLevel(f);
            long xp = mgr.getXp(f);
            context.player.sendMessage(ChatColor.GRAY + "#" + (i + 1) + " "
                    + ChatColor.YELLOW + f.getTag()
                    + ChatColor.GRAY + " - Level " + ChatColor.AQUA + level
                    + ChatColor.GRAY + " (" + ChatColor.GREEN + xp + ChatColor.GRAY + " XP)");
        }
    }

    @Override
    public TL getUsageTranslation() {
        return TL.GENERIC_PLACEHOLDER;
    }
}