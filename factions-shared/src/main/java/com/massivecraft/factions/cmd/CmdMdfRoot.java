package com.massivecraft.factions.cmd;

import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.FPlayers;
import com.massivecraft.factions.Faction;
import com.massivecraft.factions.Factions;
import com.massivecraft.factions.FactionsPlugin;
import com.massivecraft.factions.struct.Role;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.saberdev.mdf.MdfManager;
import org.saberdev.mdf.MdfSelection;
import org.saberdev.mdf.MdfWand;
import org.saberdev.mdf.MdfZone;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class CmdMdfRoot implements CommandExecutor, TabCompleter {

    private static final String ADMIN_PERM = "factions.admin.mdf";
    private static final String MEMBER_PERM = "factions.mdf";

    private static final List<String> ADMIN_SUBS = Arrays.asList("wand", "save", "delete", "select", "reset", "hardreset");
    private static final List<String> MEMBER_SUBS = Arrays.asList("add", "remove");

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendUsage(sender, label);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "wand":      return handleWand(sender);
            case "save":      return handleSave(sender, label, args);
            case "delete":    return handleDelete(sender, label, args);
            case "select":
            case "set":       return handleSelect(sender, label, args);
            case "add":       return handleAdd(sender, label, args);
            case "remove":    return handleRemove(sender, label, args);
            case "reset":     return handleReset(sender, label, args);
            case "hardreset": return handleHardReset(sender, label, args);
            default:
                sendUsage(sender, label);
                return true;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        boolean isAdmin = sender.hasPermission(ADMIN_PERM);
        boolean isMember = sender.hasPermission(MEMBER_PERM);
        if (!isAdmin && !isMember) return Collections.emptyList();

        if (args.length == 1) {
            List<String> subs = new ArrayList<>();
            if (isAdmin) subs.addAll(ADMIN_SUBS);
            if (isMember) subs.addAll(MEMBER_SUBS);
            return filter(subs, args[0]);
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        MdfManager manager = FactionsPlugin.getInstance().getMdfManager();
        if (manager == null) return Collections.emptyList();

        if (args.length == 2) {
            switch (sub) {
                case "delete":
                case "select":
                case "set":
                    return filter(zoneNames(manager), args[1]);
                case "reset":
                    return filter(factionTagsWithZone(manager), args[1]);
                case "hardreset":
                    return filter(Collections.singletonList("all"), args[1]);
                case "add":
                case "remove": {
                    if (!(sender instanceof Player)) return Collections.emptyList();
                    Faction f = playerFaction((Player) sender);
                    if (f == null) return Collections.emptyList();
                    return filter(memberNames(f), args[1]);
                }
                default:
                    return Collections.emptyList();
            }
        }

        if (args.length == 3 && (sub.equals("select") || sub.equals("set"))) {
            return filter(allFactionTags(), args[2]);
        }

        return Collections.emptyList();
    }

    // ---------------------------------------------------------------- Handlers

    private boolean handleWand(CommandSender sender) {
        if (!requireAdmin(sender)) return true;
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Joueur uniquement.");
            return true;
        }
        Player p = (Player) sender;
        p.getInventory().addItem(MdfWand.create());
        p.sendMessage(ChatColor.AQUA + "[MDF] " + ChatColor.GRAY + "Wand donné. Clic gauche / clic droit pour les positions, puis "
                + ChatColor.YELLOW + "/fmdf save <nom>" + ChatColor.GRAY + ".");
        return true;
    }

    private boolean handleSave(CommandSender sender, String label, String[] args) {
        if (!requireAdmin(sender)) return true;
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Joueur uniquement.");
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /" + label + " save <nom>");
            return true;
        }
        Player p = (Player) sender;
        String name = args[1];
        MdfManager manager = FactionsPlugin.getInstance().getMdfManager();
        if (manager.getByName(name) != null) {
            p.sendMessage(ChatColor.RED + "[MDF] Une zone nommée " + name + " existe déjà.");
            return true;
        }
        Location p1 = MdfSelection.getPos1(p.getUniqueId());
        Location p2 = MdfSelection.getPos2(p.getUniqueId());
        if (p1 == null || p2 == null) {
            p.sendMessage(ChatColor.RED + "[MDF] Définis d'abord les 2 positions avec le wand.");
            return true;
        }
        if (!p1.getWorld().equals(p2.getWorld())) {
            p.sendMessage(ChatColor.RED + "[MDF] Les 2 positions doivent être dans le même monde.");
            return true;
        }
        MdfZone zone = new MdfZone(name, p1.getWorld().getName(),
                p1.getBlockX(), p1.getBlockY(), p1.getBlockZ(),
                p2.getBlockX(), p2.getBlockY(), p2.getBlockZ());
        if (!manager.register(zone)) {
            p.sendMessage(ChatColor.RED + "[MDF] Échec de l'enregistrement.");
            return true;
        }
        p.sendMessage(ChatColor.AQUA + "[MDF] " + ChatColor.GRAY + "Capture du snapshot...");
        if (!manager.snapshot(zone)) {
            p.sendMessage(ChatColor.RED + "[MDF] Snapshot échoué — /fmdf reset ne fonctionnera pas pour cette zone.");
        }
        MdfSelection.clear(p.getUniqueId());
        p.sendMessage(ChatColor.AQUA + "[MDF] " + ChatColor.GRAY + "Zone " + ChatColor.YELLOW + name + ChatColor.GRAY
                + " sauvegardée (" + zone.getWidth() + "x" + zone.getHeight() + "x" + zone.getDepth() + " blocs).");
        return true;
    }

    private boolean handleDelete(CommandSender sender, String label, String[] args) {
        if (!requireAdmin(sender)) return true;
        if (args.length < 2) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /" + label + " delete <nom>");
            return true;
        }
        MdfManager manager = FactionsPlugin.getInstance().getMdfManager();
        MdfZone zone = manager.getByName(args[1]);
        if (zone == null) {
            sender.sendMessage(ChatColor.RED + "[MDF] Aucune zone nommée " + args[1] + ".");
            return true;
        }
        if (zone.hasOwner()) {
            sender.sendMessage(ChatColor.GRAY + "[MDF] Restauration du snapshot avant suppression...");
            manager.restore(zone);
        }
        manager.remove(zone);
        sender.sendMessage(ChatColor.AQUA + "[MDF] " + ChatColor.GRAY + "Zone " + ChatColor.YELLOW + zone.getName()
                + ChatColor.GRAY + " supprimée.");
        return true;
    }

    private boolean handleSelect(CommandSender sender, String label, String[] args) {
        if (!requireAdmin(sender)) return true;
        if (args.length < 3) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /" + label + " select <zone> <faction>");
            return true;
        }
        MdfManager manager = FactionsPlugin.getInstance().getMdfManager();
        MdfZone zone = manager.getByName(args[1]);
        if (zone == null) {
            sender.sendMessage(ChatColor.RED + "[MDF] Aucune zone nommée " + args[1] + ".");
            return true;
        }
        Faction target = resolveFaction(args[2]);
        if (target == null || !target.isNormal()) {
            sender.sendMessage(ChatColor.RED + "[MDF] Faction inconnue : " + args[2]);
            return true;
        }
        if (zone.hasOwner() && zone.getOwnerFactionId().equals(target.getId())) {
            sender.sendMessage(ChatColor.RED + "[MDF] La zone " + zone.getName() + " est déjà attribuée à " + target.getTag() + ".");
            return true;
        }
        if (zone.hasOwner()) {
            sender.sendMessage(ChatColor.GRAY + "[MDF] Restauration du snapshot avant réattribution...");
            manager.restore(zone);
            zone.clearWhitelist();
        }
        zone.setOwnerFactionId(target.getId());
        manager.persist();
        sender.sendMessage(ChatColor.AQUA + "[MDF] " + ChatColor.GRAY + "Zone " + ChatColor.YELLOW + zone.getName()
                + ChatColor.GRAY + " attribuée à " + ChatColor.YELLOW + target.getTag() + ChatColor.GRAY
                + ". Le chef peut maintenant ajouter des membres avec /fmdf add.");
        return true;
    }

    private boolean handleAdd(CommandSender sender, String label, String[] args) {
        if (!sender.hasPermission(MEMBER_PERM)) {
            sender.sendMessage(ChatColor.RED + "Permission manquante.");
            return true;
        }
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Joueur uniquement.");
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /" + label + " add <membre>");
            return true;
        }
        Player p = (Player) sender;
        FPlayer fp = FPlayers.getInstance().getByPlayer(p);
        if (fp == null || !fp.hasFaction()) {
            sender.sendMessage(ChatColor.RED + "[MDF] Tu dois être dans une faction.");
            return true;
        }
        if (fp.getRole().value < Role.COLEADER.value) {
            sender.sendMessage(ChatColor.RED + "[MDF] Réservé au chef et aux co-chefs.");
            return true;
        }
        MdfManager manager = FactionsPlugin.getInstance().getMdfManager();
        MdfZone zone = manager.getByOwner(fp.getFaction().getId());
        if (zone == null) {
            sender.sendMessage(ChatColor.RED + "[MDF] Ta faction n'a pas de zone MDF attribuée.");
            return true;
        }
        FPlayer target = resolveFactionMember(fp.getFaction(), args[1]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "[MDF] Aucun membre nommé " + args[1] + " dans ta faction.");
            return true;
        }
        UUID uuid;
        try {
            uuid = UUID.fromString(target.getId());
        } catch (IllegalArgumentException ex) {
            sender.sendMessage(ChatColor.RED + "[MDF] Identifiant joueur invalide.");
            return true;
        }
        if (!zone.addToWhitelist(uuid)) {
            sender.sendMessage(ChatColor.RED + "[MDF] " + target.getName() + " est déjà dans la whitelist.");
            return true;
        }
        manager.persist();
        sender.sendMessage(ChatColor.AQUA + "[MDF] " + ChatColor.GRAY + ChatColor.YELLOW + target.getName()
                + ChatColor.GRAY + " peut maintenant build dans la zone.");
        return true;
    }

    private boolean handleRemove(CommandSender sender, String label, String[] args) {
        if (!sender.hasPermission(MEMBER_PERM)) {
            sender.sendMessage(ChatColor.RED + "Permission manquante.");
            return true;
        }
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Joueur uniquement.");
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /" + label + " remove <membre>");
            return true;
        }
        Player p = (Player) sender;
        FPlayer fp = FPlayers.getInstance().getByPlayer(p);
        if (fp == null || !fp.hasFaction()) {
            sender.sendMessage(ChatColor.RED + "[MDF] Tu dois être dans une faction.");
            return true;
        }
        if (fp.getRole().value < Role.COLEADER.value) {
            sender.sendMessage(ChatColor.RED + "[MDF] Réservé au chef et aux co-chefs.");
            return true;
        }
        MdfManager manager = FactionsPlugin.getInstance().getMdfManager();
        MdfZone zone = manager.getByOwner(fp.getFaction().getId());
        if (zone == null) {
            sender.sendMessage(ChatColor.RED + "[MDF] Ta faction n'a pas de zone MDF attribuée.");
            return true;
        }
        FPlayer target = resolveFactionMember(fp.getFaction(), args[1]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "[MDF] Aucun membre nommé " + args[1] + " dans ta faction.");
            return true;
        }
        UUID uuid;
        try {
            uuid = UUID.fromString(target.getId());
        } catch (IllegalArgumentException ex) {
            sender.sendMessage(ChatColor.RED + "[MDF] Identifiant joueur invalide.");
            return true;
        }
        if (!zone.removeFromWhitelist(uuid)) {
            sender.sendMessage(ChatColor.RED + "[MDF] " + target.getName() + " n'était pas dans la whitelist.");
            return true;
        }
        manager.persist();
        sender.sendMessage(ChatColor.AQUA + "[MDF] " + ChatColor.GRAY + ChatColor.YELLOW + target.getName()
                + ChatColor.GRAY + " ne peut plus build dans la zone.");
        return true;
    }

    private boolean handleReset(CommandSender sender, String label, String[] args) {
        if (!requireAdmin(sender)) return true;
        if (args.length < 2) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /" + label + " reset <faction>");
            return true;
        }
        MdfManager manager = FactionsPlugin.getInstance().getMdfManager();
        Faction faction = resolveFaction(args[1]);
        if (faction == null) {
            sender.sendMessage(ChatColor.RED + "[MDF] Faction inconnue : " + args[1]);
            return true;
        }
        List<MdfZone> zones = manager.getAllByOwner(faction.getId());
        if (zones.isEmpty()) {
            sender.sendMessage(ChatColor.RED + "[MDF] " + faction.getTag() + " n'a aucune zone MDF.");
            return true;
        }
        int restored = 0;
        for (MdfZone zone : zones) {
            sender.sendMessage(ChatColor.GRAY + "[MDF] Restauration de " + zone.getName() + "...");
            if (manager.restore(zone)) restored++;
        }
        sender.sendMessage(ChatColor.AQUA + "[MDF] " + ChatColor.GRAY + restored + "/" + zones.size()
                + " zone(s) restaurée(s) pour " + faction.getTag() + ".");
        return true;
    }

    private boolean handleHardReset(CommandSender sender, String label, String[] args) {
        if (!requireAdmin(sender)) return true;
        if (args.length < 2 || !args[1].equalsIgnoreCase("all")) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /" + label + " hardreset all");
            return true;
        }
        MdfManager manager = FactionsPlugin.getInstance().getMdfManager();
        int restored = 0;
        int total = 0;
        for (MdfZone zone : new ArrayList<>(manager.getAll())) {
            total++;
            if (zone.hasOwner()) {
                if (manager.restore(zone)) restored++;
                zone.setOwnerFactionId(null);
                zone.clearWhitelist();
            }
        }
        manager.persist();
        sender.sendMessage(ChatColor.AQUA + "[MDF] " + ChatColor.GRAY + restored + "/" + total
                + " zone(s) restaurée(s) et libérée(s).");
        return true;
    }

    // ---------------------------------------------------------------- Helpers

    private boolean requireAdmin(CommandSender sender) {
        if (!sender.hasPermission(ADMIN_PERM)) {
            sender.sendMessage(ChatColor.RED + "Permission manquante.");
            return false;
        }
        return true;
    }

    private Faction resolveFaction(String input) {
        Faction f = Factions.getInstance().getByTag(input);
        if (f == null) f = Factions.getInstance().getBestTagMatch(input);
        return f;
    }

    private FPlayer resolveFactionMember(Faction faction, String name) {
        for (FPlayer member : faction.getFPlayers()) {
            if (member.getName().equalsIgnoreCase(name)) return member;
        }
        OfflinePlayer offline = Bukkit.getOfflinePlayer(name);
        if (offline != null) {
            FPlayer fp = FPlayers.getInstance().getByOfflinePlayer(offline);
            if (fp != null && fp.getFaction() != null && fp.getFaction().equals(faction)) return fp;
        }
        return null;
    }

    private Faction playerFaction(Player p) {
        FPlayer fp = FPlayers.getInstance().getByPlayer(p);
        return (fp != null && fp.hasFaction()) ? fp.getFaction() : null;
    }

    private List<String> zoneNames(MdfManager manager) {
        List<String> out = new ArrayList<>();
        for (MdfZone z : manager.getAll()) out.add(z.getName());
        return out;
    }

    private List<String> factionTagsWithZone(MdfManager manager) {
        List<String> out = new ArrayList<>();
        for (MdfZone z : manager.getAll()) {
            if (!z.hasOwner()) continue;
            Faction f = Factions.getInstance().getFactionById(z.getOwnerFactionId());
            if (f != null && !out.contains(f.getTag())) out.add(f.getTag());
        }
        return out;
    }

    private List<String> allFactionTags() {
        List<String> out = new ArrayList<>();
        for (Faction f : Factions.getInstance().getAllNormalFactions()) out.add(f.getTag());
        return out;
    }

    private List<String> memberNames(Faction faction) {
        List<String> out = new ArrayList<>();
        for (FPlayer fp : faction.getFPlayers()) out.add(fp.getName());
        return out;
    }

    private List<String> filter(List<String> source, String prefix) {
        String lower = prefix == null ? "" : prefix.toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (String entry : source) {
            if (entry.toLowerCase(Locale.ROOT).startsWith(lower)) out.add(entry);
        }
        return out;
    }

    private void sendUsage(CommandSender sender, String label) {
        boolean isAdmin = sender.hasPermission(ADMIN_PERM);
        sender.sendMessage(ChatColor.YELLOW + "Commandes MDF :");
        if (isAdmin) {
            sender.sendMessage(ChatColor.GRAY + "/" + label + " wand");
            sender.sendMessage(ChatColor.GRAY + "/" + label + " save <nom>");
            sender.sendMessage(ChatColor.GRAY + "/" + label + " delete <zone>");
            sender.sendMessage(ChatColor.GRAY + "/" + label + " select <zone> <faction>");
            sender.sendMessage(ChatColor.GRAY + "/" + label + " reset <faction>");
            sender.sendMessage(ChatColor.GRAY + "/" + label + " hardreset all");
        }
        sender.sendMessage(ChatColor.GRAY + "/" + label + " add <membre>");
        sender.sendMessage(ChatColor.GRAY + "/" + label + " remove <membre>");
    }
}