package com.massivecraft.factions.cmd;

import com.massivecraft.factions.Faction;
import com.massivecraft.factions.FactionsPlugin;
import com.massivecraft.factions.struct.Permission;
import com.massivecraft.factions.zcore.util.TL;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public class CmdEvolution extends FCommand {

    public CmdEvolution() {
        super();
        this.getAliases().addAll(Aliases.evolution);
        this.setRequirements(new CommandRequirements.Builder(Permission.EVOLUTION).playerOnly().build());
    }

    @Override
    public void perform(CommandContext context) {
        FileConfiguration cfg = FactionsPlugin.getInstance().getConfig();
        if (!cfg.getBoolean("evolution.enabled", true)) {
            context.msg(TL.GENERIC_DISABLED, "Evolution");
            return;
        }

        String command = cfg.getString("evolution.command", "");
        if (command == null || command.trim().isEmpty()) {
            context.player.sendMessage(ChatColor.RED + "[Evolution] No command configured. Set 'evolution.command' in config.yml.");
            return;
        }

        Player player = context.player;
        Faction faction = context.fPlayer.getFaction();
        String resolved = command
                .replace("{player}", player.getName())
                .replace("{uuid}", player.getUniqueId().toString())
                .replace("{faction}", faction.getTag())
                .replace("{faction-tag}", ChatColor.stripColor(faction.getTag()));

        if (resolved.startsWith("/")) resolved = resolved.substring(1);

        boolean asConsole = cfg.getBoolean("evolution.as-console", true);
        if (asConsole) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), resolved);
        } else {
            player.performCommand(resolved);
        }

        String message = cfg.getString("evolution.message", "");
        if (message != null && !message.isEmpty()) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
        }
    }

    @Override
    public TL getUsageTranslation() {
        return TL.GENERIC_PLACEHOLDER;
    }
}
