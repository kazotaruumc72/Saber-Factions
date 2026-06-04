package com.massivecraft.factions.integration.zmenu;

import fr.maxlego08.menu.api.button.Button;
import fr.maxlego08.menu.api.button.DefaultButtonValue;
import fr.maxlego08.menu.api.loader.ButtonLoader;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

/**
 * Registers the {@code factions_warps} button type with zMenu so it can be referenced
 * from the {@code factions_warps.yml} inventory configuration.
 */
public class FactionWarpsButtonLoader extends ButtonLoader {

    public FactionWarpsButtonLoader(Plugin plugin) {
        super(plugin, "factions_warps");
    }

    @Override
    public Button load(YamlConfiguration configuration, String path, DefaultButtonValue defaultButtonValue) {
        return new FactionWarpsButton();
    }
}