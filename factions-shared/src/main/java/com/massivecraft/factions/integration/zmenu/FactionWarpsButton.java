package com.massivecraft.factions.integration.zmenu;

import com.cryptomorin.xseries.XMaterial;
import com.massivecraft.factions.Conf;
import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.FPlayers;
import com.massivecraft.factions.Faction;
import com.massivecraft.factions.FactionsPlugin;
import com.massivecraft.factions.integration.Econ;
import com.massivecraft.factions.util.LazyLocation;
import com.massivecraft.factions.util.FactionsScheduler;
import com.massivecraft.factions.util.Placeholder;
import com.massivecraft.factions.util.WarmUpUtil;
import com.massivecraft.factions.zcore.util.TL;
import com.massivecraft.factions.zcore.util.TextUtil;
import fr.maxlego08.menu.api.button.Button;
import fr.maxlego08.menu.api.engine.InventoryEngine;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * zMenu custom button that renders a faction's warp list inside the {@code factions_warps}
 * inventory. The behaviour mirrors the legacy {@code FactionWarpsFrame}: a dummy fill item,
 * one clickable item per warp at the {@code fwarp-gui.warp-slots} positions, and the same
 * password / cost / warm-up flow on click.
 *
 * <p>The target faction (own faction for {@code /f warp}, ally faction for {@code /f allywarp})
 * is supplied per player via {@link ZMenuHook#setTargetFaction}.</p>
 */
public class FactionWarpsButton extends Button {

    @Override
    public boolean hasCustomRender() {
        return true;
    }

    @Override
    public void onRender(Player player, InventoryEngine inventory) {
        FactionsPlugin plugin = FactionsPlugin.getInstance();
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("fwarp-gui");
        if (section == null) {
            return;
        }

        FPlayer fplayer = FPlayers.getInstance().getByPlayer(player);
        Faction faction = ZMenuHook.getTargetFaction(player.getUniqueId());
        if (faction == null) {
            faction = fplayer.getFaction();
        }
        if (faction == null) {
            return;
        }

        // Dummy background fill
        ItemStack dummy = buildDummyItem(section);
        if (dummy != null) {
            int size = inventory.getInventory().getSize();
            for (int i = 0; i < size; i++) {
                inventory.addItem(i, dummy.clone());
            }
        }

        // Warp items
        List<Integer> slots = new ArrayList<>(section.getIntegerList("warp-slots"));
        if (slots.isEmpty()) {
            return;
        }

        int count = 0;
        final Faction targetFaction = faction;
        for (final Map.Entry<String, LazyLocation> warp : targetFaction.getWarps().entrySet()) {
            if (count >= slots.size()) {
                slots.add(slots.get(slots.size() - 1) + 1);
            }
            int slot = slots.get(count++);
            if (slot < 0 || slot >= inventory.getInventory().getSize()) {
                continue;
            }
            ItemStack item = buildWarpAsset(section, warp, targetFaction);
            final String warpName = warp.getKey();
            inventory.addItem(slot, item).setClick(event -> handleWarpClick(player, fplayer, targetFaction, warpName));
        }
    }

    private void handleWarpClick(Player player, FPlayer fplayer, Faction faction, String warpName) {
        player.closeInventory();

        if (!faction.hasWarpPassword(warpName)) {
            if (transact(fplayer, faction)) {
                doWarmup(warpName, fplayer, faction);
            }
        } else {
            fplayer.setEnteringPassword(true, warpName);
            fplayer.msg(TL.COMMAND_FWARP_PASSWORD_REQUIRED);
            FactionsScheduler.runLater(() -> {
                if (fplayer.isEnteringPassword()) {
                    fplayer.msg(TL.COMMAND_FWARP_PASSWORD_TIMEOUT);
                    fplayer.setEnteringPassword(false, "");
                }
            }, FactionsPlugin.getInstance().getConfig().getInt("fwarp-gui.password-timeout", 5) * 20L);
        }
    }

    private void doWarmup(final String warp, FPlayer fme, Faction faction) {
        WarmUpUtil.process(fme, WarmUpUtil.Warmup.WARP, TL.WARMUPS_NOTIFY_TELEPORT, warp, () -> {
            Player player = Bukkit.getPlayer(fme.getPlayer().getUniqueId());
            if (player != null) {
                player.teleport(faction.getWarp(warp).getLocation());
                fme.msg(TL.COMMAND_FWARP_WARPED, warp);
            }
        }, FactionsPlugin.getInstance().getConfig().getLong("warmups.f-warp", 10));
    }

    private boolean transact(FPlayer player, Faction faction) {
        if (!FactionsPlugin.getInstance().getConfig().getBoolean("warp-cost.enabled", false) || player.isAdminBypassing()) {
            return true;
        }
        double cost = FactionsPlugin.getInstance().getConfig().getDouble("warp-cost.warp", 5);

        if (!Econ.shouldBeUsed() || cost == 0.0 || player.isAdminBypassing()) {
            return true;
        }

        if (Conf.bankEnabled && Conf.bankFactionPaysCosts && player.hasFaction()) {
            return Econ.withdrawFactionBalance(faction, cost);
        } else {
            return Econ.modifyMoney(player, -cost, TL.COMMAND_FWARP_TOWARP.toString(), TL.COMMAND_FWARP_FORWARPING.toString());
        }
    }

    private ItemStack buildWarpAsset(ConfigurationSection section, final Map.Entry<String, LazyLocation> warp, final Faction faction) {
        final ConfigurationSection config = section.getConfigurationSection("warp-item");
        final ItemStack item = XMaterial.matchXMaterial(config.getString("Type")).get().parseItem();
        final ItemMeta meta = item.getItemMeta();
        meta.setLore(TextUtil.parse(Placeholder.replacePlaceholders(config.getStringList("Lore"),
                new Placeholder("{warp-protected}", faction.hasWarpPassword(warp.getKey()) ? "Enabled" : "Disabled"),
                new Placeholder("{warp-cost}", FactionsPlugin.getInstance().getConfig().getBoolean("warp-cost.enabled", false)
                        ? Integer.toString(FactionsPlugin.getInstance().getConfig().getInt("warp-cost.warp", 5)) : "Disabled"))));
        meta.setDisplayName(TextUtil.parse(config.getString("Name").replace("{warp}", warp.getKey())));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildDummyItem(ConfigurationSection section) {
        final ConfigurationSection config = section.getConfigurationSection("dummy-item");
        if (config == null) {
            return null;
        }
        final ItemStack item = XMaterial.matchXMaterial(config.getString("Type")).get().parseItem();
        final ItemMeta meta = item.getItemMeta();
        meta.setLore(TextUtil.parse(config.getStringList("Lore")));
        meta.setDisplayName(TextUtil.parse(config.getString("Name")));
        item.setItemMeta(meta);
        return item;
    }
}