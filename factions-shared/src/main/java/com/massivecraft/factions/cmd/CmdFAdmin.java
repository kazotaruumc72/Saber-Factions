package com.massivecraft.factions.cmd;

import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.FPlayers;
import com.massivecraft.factions.Faction;
import com.massivecraft.factions.Factions;
import com.massivecraft.factions.event.FactionDisbandEvent.PlayerDisbandReason;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class CmdFAdmin implements CommandExecutor, TabCompleter {

    private static final String PERMISSION = "factions.fadmin";
    private static final List<String> ROOT_SUBS = Arrays.asList("power", "disband");
    private static final List<String> POWER_ACTIONS = Arrays.asList("give", "remove");
    private static final List<String> AMOUNT_HINTS = Arrays.asList("1", "5", "10", "25", "50", "100");

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission(PERMISSION)) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }

        if (args.length == 0) {
            sendUsage(sender, label);
            return true;
        }

        String root = args[0].toLowerCase(Locale.ROOT);
        if (root.equals("disband")) {
            return handleDisband(sender, label, args);
        }
        if (!root.equals("power")) {
            sendUsage(sender, label);
            return true;
        }

        if (args.length < 3) {
            sendUsage(sender, label);
            return true;
        }

        String action = args[1].toLowerCase(Locale.ROOT);
        if (!action.equals("give") && !action.equals("remove")) {
            sendUsage(sender, label);
            return true;
        }

        String targetName = args[2];
        OfflinePlayer offlineTarget = Bukkit.getOfflinePlayer(targetName);
        if (offlineTarget == null || (!offlineTarget.hasPlayedBefore() && !offlineTarget.isOnline())) {
            sender.sendMessage(ChatColor.RED + "Unknown player: " + targetName);
            return true;
        }

        FPlayer fTarget = FPlayers.getInstance().getByOfflinePlayer(offlineTarget);
        if (fTarget == null) {
            sender.sendMessage(ChatColor.RED + "Could not load faction data for " + targetName);
            return true;
        }

        double amount = 1.0D;
        if (args.length >= 4) {
            try {
                amount = Double.parseDouble(args[3]);
            } catch (NumberFormatException ex) {
                sender.sendMessage(ChatColor.RED + "Invalid amount: " + args[3]);
                return true;
            }
            if (amount <= 0) {
                sender.sendMessage(ChatColor.RED + "Amount must be greater than 0.");
                return true;
            }
        }

        double delta = action.equals("give") ? amount : -amount;
        fTarget.alterPower(delta);

        String verb = action.equals("give") ? "Given" : "Removed";
        String prep = action.equals("give") ? "to" : "from";
        sender.sendMessage(ChatColor.GREEN + verb + " "
                + ChatColor.YELLOW + amount
                + ChatColor.GREEN + " power " + prep + " "
                + ChatColor.YELLOW + fTarget.getName()
                + ChatColor.GREEN + ". New power: "
                + ChatColor.YELLOW + fTarget.getPowerRounded()
                + ChatColor.GREEN + "/"
                + ChatColor.YELLOW + fTarget.getPowerMaxRounded()
                + ChatColor.GREEN + ".");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission(PERMISSION)) {
            return Collections.emptyList();
        }

        switch (args.length) {
            case 1:
                return filter(ROOT_SUBS, args[0]);
            case 2:
                if (args[0].equalsIgnoreCase("power")) {
                    return filter(POWER_ACTIONS, args[1]);
                }
                if (args[0].equalsIgnoreCase("disband")) {
                    List<String> tags = new ArrayList<>();
                    tags.add("all");
                    for (Faction faction : Factions.getInstance().getAllNormalFactions()) {
                        tags.add(faction.getTag());
                    }
                    return filter(tags, args[1]);
                }
                return Collections.emptyList();
            case 3:
                if (args[0].equalsIgnoreCase("power") && isPowerAction(args[1])) {
                    List<String> names = new ArrayList<>();
                    for (Player online : Bukkit.getOnlinePlayers()) {
                        names.add(online.getName());
                    }
                    return filter(names, args[2]);
                }
                return Collections.emptyList();
            case 4:
                if (args[0].equalsIgnoreCase("power") && isPowerAction(args[1])) {
                    return filter(AMOUNT_HINTS, args[3]);
                }
                return Collections.emptyList();
            default:
                return Collections.emptyList();
        }
    }

    private boolean handleDisband(CommandSender sender, String label, String[] args) {
        if (args.length < 2) {
            sendUsage(sender, label);
            return true;
        }

        String target = args[1];
        if (target.equalsIgnoreCase("all")) {
            List<Faction> factions = new ArrayList<>(Factions.getInstance().getAllNormalFactions());
            int disbanded = 0;
            for (Faction faction : factions) {
                if (faction == null || !faction.isNormal()) {
                    continue;
                }
                String tag = faction.getTag();
                faction.disband(null, PlayerDisbandReason.PLUGIN);
                disbanded++;
                sender.sendMessage(ChatColor.GRAY + " - Disbanded " + ChatColor.YELLOW + tag);
            }
            sender.sendMessage(ChatColor.GREEN + "Disbanded "
                    + ChatColor.YELLOW + disbanded
                    + ChatColor.GREEN + " faction(s).");
            return true;
        }

        Faction faction = Factions.getInstance().getByTag(target);
        if (faction == null) {
            faction = Factions.getInstance().getBestTagMatch(target);
        }
        if (faction == null || !faction.isNormal()) {
            sender.sendMessage(ChatColor.RED + "Unknown faction: " + target);
            return true;
        }

        String tag = faction.getTag();
        faction.disband(null, PlayerDisbandReason.PLUGIN);
        sender.sendMessage(ChatColor.GREEN + "Disbanded faction "
                + ChatColor.YELLOW + tag
                + ChatColor.GREEN + ".");
        return true;
    }

    private boolean isPowerAction(String value) {
        return value.equalsIgnoreCase("give") || value.equalsIgnoreCase("remove");
    }

    private List<String> filter(List<String> source, String prefix) {
        String lower = prefix.toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (String entry : source) {
            if (entry.toLowerCase(Locale.ROOT).startsWith(lower)) {
                out.add(entry);
            }
        }
        return out;
    }

    private void sendUsage(CommandSender sender, String label) {
        sender.sendMessage(ChatColor.YELLOW + "Usage:");
        sender.sendMessage(ChatColor.GRAY + "/" + label + " power give <player> [amount]");
        sender.sendMessage(ChatColor.GRAY + "/" + label + " power remove <player> [amount]");
        sender.sendMessage(ChatColor.GRAY + "/" + label + " disband <faction>");
        sender.sendMessage(ChatColor.GRAY + "/" + label + " disband all");
    }
}