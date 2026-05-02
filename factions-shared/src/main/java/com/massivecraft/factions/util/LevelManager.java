package com.massivecraft.factions.util;

import com.massivecraft.factions.Faction;
import com.massivecraft.factions.FactionsPlugin;
import com.massivecraft.factions.zcore.file.CustomFile;
import com.massivecraft.factions.zcore.frame.fupgrades.UpgradeManager;
import org.bukkit.configuration.ConfigurationSection;

import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public final class LevelManager {

    private static LevelManager instance;

    private final TreeMap<Integer, Long> thresholds = new TreeMap<>();
    private String xpSource = "points";

    private LevelManager() { reload(); }

    public static LevelManager getInstance() {
        if (instance == null) instance = new LevelManager();
        return instance;
    }

    public void reload() {
        thresholds.clear();
        CustomFile file = FactionsPlugin.getInstance().getFileManager().getLevels();
        if (file == null || file.getConfig() == null) return;
        xpSource = file.getConfig().getString("xp-source", "points").toLowerCase(Locale.ROOT);
        ConfigurationSection section = file.getConfig().getConfigurationSection("levels");
        if (section == null) return;
        for (String key : section.getKeys(false)) {
            try {
                int level = Integer.parseInt(key);
                long threshold = section.getLong(key);
                thresholds.put(level, threshold);
            } catch (NumberFormatException ignored) {}
        }
    }

    public long getXp(Faction faction) {
        if (faction == null || !faction.isNormal()) return 0;
        switch (xpSource) {
            case "kills": return faction.getKills();
            case "power": return faction.getPowerRounded();
            case "claims": return faction.getAllClaims().size();
            case "upgrades-sum": {
                int sum = 0;
                for (String id : UpgradeManager.getInstance().getUpgrades().keySet()) sum += faction.getUpgrade(id);
                return sum;
            }
            case "points":
            default: return faction.getPoints();
        }
    }

    public int getLevel(Faction faction) {
        long xp = getXp(faction);
        int level = 0;
        for (Map.Entry<Integer, Long> entry : thresholds.entrySet()) {
            if (xp >= entry.getValue()) level = entry.getKey();
            else break;
        }
        return level;
    }

    public long getCurrentThreshold(Faction faction) {
        int level = getLevel(faction);
        return level == 0 ? 0L : thresholds.getOrDefault(level, 0L);
    }

    public long getNextThreshold(Faction faction) {
        int level = getLevel(faction);
        Integer next = thresholds.higherKey(level);
        return next == null ? -1L : thresholds.get(next);
    }

    public int getMaxLevel() {
        return thresholds.isEmpty() ? 0 : thresholds.lastKey();
    }
}