package com.massivecraft.factions.cmd;

import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.FPlayers;
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
    private static final List<String> ROOT_SUBS = Collections.singletonList("power");
    private static final List<String> POWER_ACTIONS = Arrays.asList("give", "remove");
    private static final List<String> AMOUNT_HINTS = Arrays.asList("1", "5", "10", "25", "50", "100");

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission(PERMISSION)) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }

        if (args.length == 0 || !args[0].equalsIgnoreCase("power")) {
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
    }
}