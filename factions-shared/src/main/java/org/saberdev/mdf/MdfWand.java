package org.saberdev.mdf;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.List;

public final class MdfWand {

    public static final String WAND_TAG = ChatColor.DARK_GRAY + "[mdf-wand]";
    private static final String DISPLAY_NAME = ChatColor.AQUA + "MDF Wand";

    private MdfWand() {}

    public static ItemStack create() {
        ItemStack stack = new ItemStack(Material.STICK);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(DISPLAY_NAME);
            meta.setLore(Arrays.asList(
                    ChatColor.GRAY + "Clic gauche : position 1",
                    ChatColor.GRAY + "Clic droit  : position 2",
                    WAND_TAG
            ));
            stack.setItemMeta(meta);
        }
        return stack;
    }

    public static boolean isWand(ItemStack stack) {
        if (stack == null || stack.getType() != Material.STICK) return false;
        ItemMeta meta = stack.getItemMeta();
        if (meta == null || !meta.hasLore()) return false;
        List<String> lore = meta.getLore();
        return lore != null && lore.contains(WAND_TAG);
    }
}