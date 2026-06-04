package com.massivecraft.factions.integration.zmenu;

import com.massivecraft.factions.Faction;
import com.massivecraft.factions.FactionsPlugin;
import com.massivecraft.factions.util.Logger;
import fr.maxlego08.menu.api.ButtonManager;
import fr.maxlego08.menu.api.InventoryManager;
import fr.maxlego08.menu.api.event.events.ButtonLoaderRegisterEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Entry point for the zMenu integration. Loaded only when the zMenu plugin is present.
 *
 * <p>Responsibilities:</p>
 * <ul>
 *     <li>Resolve zMenu's {@link InventoryManager} service.</li>
 *     <li>Register Factions' custom button loaders (via {@link ButtonLoaderRegisterEvent}).</li>
 *     <li>Save &amp; load the bundled inventory YAML resources.</li>
 *     <li>Open Factions menus through zMenu, passing per-player context.</li>
 * </ul>
 *
 * <p>Migrating a GUI to zMenu is opt-in per menu: callers try {@link #openFactionWarps} and fall
 * back to the legacy GUI when this returns {@code false} (zMenu missing or load failed).</p>
 */
public class ZMenuHook implements Listener {

    private static ZMenuHook instance;

    private final FactionsPlugin plugin;
    private InventoryManager inventoryManager;
    private boolean ready;

    /** Per-player target faction for the next menu open (own faction vs. ally faction). */
    private final Map<UUID, Faction> targetFactions = new ConcurrentHashMap<>();

    private ZMenuHook(FactionsPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Initialises the hook if zMenu is installed. Safe to call unconditionally.
     *
     * @return the hook instance if zMenu is available and wiring succeeded, otherwise {@code null}.
     */
    public static ZMenuHook init(FactionsPlugin plugin) {
        if (Bukkit.getPluginManager().getPlugin("zMenu") == null) {
            return null;
        }
        ZMenuHook hook = new ZMenuHook(plugin);
        if (hook.setup()) {
            instance = hook;
            return hook;
        }
        return null;
    }

    public static ZMenuHook get() {
        return instance;
    }

    private boolean setup() {
        try {
            RegisteredServiceProvider<InventoryManager> provider =
                    Bukkit.getServicesManager().getRegistration(InventoryManager.class);
            if (provider == null) {
                Logger.print("zMenu found but its InventoryManager service is missing; skipping zMenu menus.", Logger.PrefixType.WARNING);
                return false;
            }
            this.inventoryManager = provider.getProvider();

            // zMenu already fired ButtonLoaderRegisterEvent during its own enable (before us),
            // so register our button loaders directly on the ButtonManager service now. We also
            // keep the event listener below so the buttons are re-registered on /zmenu reload.
            RegisteredServiceProvider<ButtonManager> buttonProvider =
                    Bukkit.getServicesManager().getRegistration(ButtonManager.class);
            if (buttonProvider != null) {
                registerButtons(buttonProvider.getProvider());
            }
            Bukkit.getPluginManager().registerEvents(this, this.plugin);

            // Save the default inventory configs from the jar (if absent) and load them.
            this.inventoryManager.loadInventoryOrSaveResource(this.plugin, "inventories/factions_warps.yml");

            this.ready = true;
            Logger.print("Hooked into zMenu for inventory menus.", Logger.PrefixType.DEFAULT);
            return true;
        } catch (Throwable throwable) {
            Logger.print("Failed to hook into zMenu: " + throwable.getMessage(), Logger.PrefixType.FAILED);
            throwable.printStackTrace();
            return false;
        }
    }

    @EventHandler
    public void onButtonRegister(ButtonLoaderRegisterEvent event) {
        // Fired on /zmenu reload (after zMenu clears its buttons). Re-register ours.
        registerButtons(event.getButtonManager());
    }

    private void registerButtons(ButtonManager buttonManager) {
        try {
            buttonManager.register(new FactionWarpsButtonLoader(this.plugin));
        } catch (Throwable throwable) {
            // Already registered (e.g. duplicate event) - safe to ignore.
        }
    }

    public boolean isReady() {
        return ready && inventoryManager != null;
    }

    static Faction getTargetFaction(UUID uuid) {
        ZMenuHook hook = instance;
        return hook == null ? null : hook.targetFactions.get(uuid);
    }

    public void setTargetFaction(UUID uuid, Faction faction) {
        this.targetFactions.put(uuid, faction);
    }

    /**
     * Opens the faction warps menu for the player, showing the given faction's warps.
     *
     * @return {@code true} if the menu was opened through zMenu, {@code false} to fall back.
     */
    public boolean openFactionWarps(Player player, Faction faction) {
        if (!isReady()) {
            return false;
        }
        try {
            setTargetFaction(player.getUniqueId(), faction);
            this.inventoryManager.openInventory(player, this.plugin, "factions_warps");
            return true;
        } catch (Throwable throwable) {
            Logger.print("Failed to open zMenu faction warps menu: " + throwable.getMessage(), Logger.PrefixType.FAILED);
            return false;
        }
    }
}