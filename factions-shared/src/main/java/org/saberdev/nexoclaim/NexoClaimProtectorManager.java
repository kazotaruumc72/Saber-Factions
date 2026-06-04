package org.saberdev.nexoclaim;

import com.massivecraft.factions.Board;
import com.massivecraft.factions.FLocation;
import com.massivecraft.factions.Faction;
import com.massivecraft.factions.Factions;
import com.massivecraft.factions.FactionsPlugin;
import com.massivecraft.factions.util.Logger;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class NexoClaimProtectorManager {

    private final FactionsPlugin plugin;
    private final File configFile;
    private final File dataFile;
    private final Map<String, ProtectorData> protectors = new ConcurrentHashMap<>();

    private boolean enabled;
    private boolean requireOwnTerritory = true;
    private boolean unclaimOnEmpty = true;
    private long fuelTickSeconds = 1L;
    private final Map<String, ProtectorDefinition> definitions = new HashMap<>();

    private com.tcoded.folialib.wrapper.task.WrappedTask taskId = null;

    public NexoClaimProtectorManager(FactionsPlugin plugin) {
        this.plugin = plugin;
        File configDir = new File(plugin.getDataFolder(), "configuration");
        if (!configDir.exists()) configDir.mkdirs();
        this.configFile = new File(configDir, "nexo-claim-protector.yml");
        File dataDir = new File(plugin.getDataFolder(), "nexo-claim-protector");
        if (!dataDir.exists()) dataDir.mkdirs();
        this.dataFile = new File(dataDir, "protectors.json");
        exportDefaultsIfMissing();
        reloadConfig();
        loadFromDisk();
    }

    private void exportDefaultsIfMissing() {
        if (configFile.exists()) return;
        try (java.io.InputStream in = plugin.getResource("nexo-claim-protector.yml");
             FileOutputStream out = new FileOutputStream(configFile)) {
            if (in == null) return;
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
        } catch (IOException e) {
            Logger.print("Failed to export default nexo-claim-protector.yml: " + e.getMessage(),
                    Logger.PrefixType.WARNING);
        }
    }

    public void reloadConfig() {
        definitions.clear();
        if (!configFile.exists()) {
            this.enabled = false;
            return;
        }
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(configFile);
        this.enabled = cfg.getBoolean("enabled", true);
        this.requireOwnTerritory = cfg.getBoolean("require-own-territory", true);
        this.unclaimOnEmpty = cfg.getBoolean("unclaim-on-fuel-empty", true);
        this.fuelTickSeconds = Math.max(1L, cfg.getLong("fuel-tick-seconds", 1L));

        ConfigurationSection root = cfg.getConfigurationSection("base_claim_protector");
        if (root == null) return;

        for (String key : root.getKeys(false)) {
            ConfigurationSection sec = root.getConfigurationSection(key);
            if (sec == null) continue;
            int level = clamp(sec.getInt("level", 1), 1, 5);
            int radius = Math.max(0, sec.getInt("chunk_radius", sec.getInt("chunck_radius", 1)));
            List<String> rawFood = sec.getStringList("food");
            List<FoodEntry> food = new ArrayList<>(rawFood.size());
            for (String entry : rawFood) {
                FoodEntry fe = FoodEntry.parse(entry);
                if (fe != null) food.add(fe);
            }
            definitions.put(key, new ProtectorDefinition(key, level, radius, food));
        }
    }

    private static int clamp(int v, int min, int max) {
        return v < min ? min : (v > max ? max : v);
    }

    public boolean isEnabled() { return enabled; }
    public boolean isRequireOwnTerritory() { return requireOwnTerritory; }

    public ProtectorDefinition getDefinition(String nexoItemId) {
        return nexoItemId == null ? null : definitions.get(nexoItemId);
    }

    public Map<String, ProtectorDefinition> getDefinitions() {
        return Collections.unmodifiableMap(definitions);
    }

    public ProtectorData getAt(Location location) {
        if (location == null || location.getWorld() == null) return null;
        return protectors.get(ProtectorData.keyOf(
                location.getWorld().getName(),
                location.getBlockX(), location.getBlockY(), location.getBlockZ()));
    }

    public ProtectorData getByGeneratedChunk(FLocation flocation) {
        if (flocation == null) return null;
        for (ProtectorData data : protectors.values()) {
            if (!data.getWorldName().equals(flocation.getWorldName())) continue;
            for (long[] coords : data.getClaimedChunks()) {
                if (coords[0] == flocation.getIntX() && coords[1] == flocation.getIntZ()) return data;
            }
        }
        return null;
    }

    /**
     * Place a protector at the given block. Claims the surrounding chunks.
     * Returns the created data, or null if placement is forbidden.
     */
    public ProtectorData onPlace(Player player, Location blockLocation, ProtectorDefinition def, Faction faction) {
        if (!enabled || def == null || faction == null || faction.isWilderness()) return null;
        World world = blockLocation.getWorld();
        if (world == null) return null;

        FLocation center = FLocation.wrap(blockLocation);

        if (requireOwnTerritory) {
            Faction at = Board.getInstance().getFactionAt(center);
            if (at == null || !at.equals(faction)) {
                player.sendMessage(ChatColor.RED + "[BaseClaimProtector] " + ChatColor.GRAY
                        + "Vous devez poser ce bloc sur un chunk déjà claim par votre faction.");
                return null;
            }
        }

        ProtectorData data = new ProtectorData(
                world.getName(),
                blockLocation.getBlockX(), blockLocation.getBlockY(), blockLocation.getBlockZ(),
                faction.getId(), def.getNexoItemId(), def.getLevel(), def.getChunkRadius());

        protectors.put(data.key(), data);
        attemptClaimRadius(data, faction, player);
        save();

        player.sendMessage(ChatColor.GOLD + "[BaseClaimProtector] " + ChatColor.GRAY
                + "Protecteur niveau " + def.getLevel() + " posé. "
                + "Rayon de " + def.getChunkRadius() + " chunks claimés. "
                + "Nourrissez-le pour le maintenir actif.");
        return data;
    }

    private void attemptClaimRadius(ProtectorData data, Faction faction, Player notifier) {
        int radius = data.getChunkRadius();
        World world = Bukkit.getWorld(data.getWorldName());
        if (world == null) return;
        int centerCx = data.getBlockX() >> 4;
        int centerCz = data.getBlockZ() >> 4;
        int claimed = 0;
        int skipped = 0;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                int cx = centerCx + dx;
                int cz = centerCz + dz;
                FLocation fl = FLocation.wrap(world.getName(), cx, cz);
                Faction occupant = Board.getInstance().getFactionAt(fl);
                if (occupant != null && !occupant.isWilderness() && !occupant.equals(faction)) {
                    skipped++;
                    continue;
                }
                if (occupant != null && occupant.equals(faction)) {
                    // already ours; still register so we own the unclaim later only if previously unclaimed
                    continue;
                }
                Board.getInstance().setFactionAt(faction, fl);
                data.rememberClaimed(cx, cz);
                claimed++;
            }
        }
        data.setActive(true);
        if (notifier != null && (claimed > 0 || skipped > 0)) {
            notifier.sendMessage(ChatColor.GRAY + "[BaseClaimProtector] " + claimed
                    + " chunks claimés, " + skipped + " ignorés (déjà occupés).");
        }
    }

    public void onBreak(Player breaker, Location blockLocation) {
        if (!enabled) return;
        ProtectorData data = getAt(blockLocation);
        if (data == null) return;
        unclaimAllOf(data);
        protectors.remove(data.key());
        save();
        if (breaker != null) {
            breaker.sendMessage(ChatColor.RED + "[BaseClaimProtector] " + ChatColor.GRAY
                    + "Protecteur détruit. Tous les chunks générés ont été unclaimés.");
        }
    }

    private void unclaimAllOf(ProtectorData data) {
        Faction faction = Factions.getInstance().getFactionById(data.getFactionId());
        for (FLocation fl : data.toFLocations()) {
            Faction at = Board.getInstance().getFactionAt(fl);
            if (at == null || at.isWilderness()) continue;
            if (faction != null && !at.equals(faction)) continue;
            Board.getInstance().removeAt(fl);
        }
        data.clearClaimed();
        data.setActive(false);
    }

    public boolean onFeed(Player player, Location blockLocation, String nexoItemId) {
        ProtectorData data = getAt(blockLocation);
        if (data == null) return false;
        ProtectorDefinition def = definitions.get(data.getNexoItemId());
        if (def == null) return false;
        Long fuel = def.fuelFor(nexoItemId);
        if (fuel == null) return false;
        boolean wasInactive = !data.isActive();
        data.addFuelSeconds(fuel);
        if (wasInactive) {
            Faction faction = Factions.getInstance().getFactionById(data.getFactionId());
            if (faction != null && !faction.isWilderness()) {
                attemptClaimRadius(data, faction, player);
            }
        }
        save();
        if (player != null) {
            player.sendMessage(ChatColor.GREEN + "[BaseClaimProtector] " + ChatColor.GRAY
                    + "+" + formatDuration(fuel) + " de fuel. Total: " + formatDuration(data.getFuelSeconds()) + ".");
        }
        return true;
    }

    public void startTicking() {
        if (taskId != null || !enabled) return;
        long ticks = fuelTickSeconds * 20L;
        taskId = com.massivecraft.factions.util.FactionsScheduler.runTimer(this::tick, ticks, ticks);
    }

    public void stopTicking() {
        if (taskId != null) {
            com.massivecraft.factions.util.FactionsScheduler.cancel(taskId);
            taskId = null;
        }
    }

    private void tick() {
        boolean dirty = false;
        for (ProtectorData data : protectors.values()) {
            if (!data.isActive()) continue;
            long remaining = data.getFuelSeconds() - fuelTickSeconds;
            if (remaining <= 0) {
                data.setFuelSeconds(0);
                if (unclaimOnEmpty) {
                    unclaimAllOf(data);
                    notifyFaction(data, ChatColor.RED + "[BaseClaimProtector] "
                            + ChatColor.GRAY + "Plus de fuel ! Les chunks ont été unclaimés. Refeed pour reclaim.");
                } else {
                    data.setActive(false);
                    notifyFaction(data, ChatColor.RED + "[BaseClaimProtector] "
                            + ChatColor.GRAY + "Plus de fuel ! Le protecteur est désactivé.");
                }
                dirty = true;
            } else {
                data.setFuelSeconds(remaining);
            }
        }
        if (dirty) save();
    }

    private void notifyFaction(ProtectorData data, String message) {
        Faction faction = Factions.getInstance().getFactionById(data.getFactionId());
        if (faction == null) return;
        try {
            faction.getFPlayersWhereOnline(true).forEach(fp -> fp.msg(message));
        } catch (Throwable ignored) {
        }
    }

    private String formatDuration(long seconds) {
        long h = seconds / 3600;
        long m = (seconds % 3600) / 60;
        long s = seconds % 60;
        StringBuilder sb = new StringBuilder();
        if (h > 0) sb.append(h).append("h");
        if (m > 0) sb.append(m).append("m");
        if (s > 0 || sb.length() == 0) sb.append(s).append("s");
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private void save() {
        JSONArray array = new JSONArray();
        for (ProtectorData data : protectors.values()) {
            JSONObject obj = new JSONObject();
            obj.put("world", data.getWorldName());
            obj.put("x", data.getBlockX());
            obj.put("y", data.getBlockY());
            obj.put("z", data.getBlockZ());
            obj.put("faction", data.getFactionId());
            obj.put("nexoId", data.getNexoItemId());
            obj.put("level", data.getLevel());
            obj.put("radius", data.getChunkRadius());
            obj.put("fuel", data.getFuelSeconds());
            obj.put("active", data.isActive());
            JSONArray chunks = new JSONArray();
            for (long[] coords : data.getClaimedChunks()) {
                JSONArray pair = new JSONArray();
                pair.add(coords[0]);
                pair.add(coords[1]);
                chunks.add(pair);
            }
            obj.put("chunks", chunks);
            array.add(obj);
        }
        try (Writer w = new OutputStreamWriter(new FileOutputStream(dataFile))) {
            w.write(array.toJSONString());
        } catch (IOException e) {
            Logger.print("Failed to persist NexoClaimProtector data: " + e.getMessage(),
                    Logger.PrefixType.WARNING);
        }
    }

    private void loadFromDisk() {
        if (!dataFile.exists()) return;
        try (Reader r = new InputStreamReader(new FileInputStream(dataFile))) {
            Object parsed = new JSONParser().parse(r);
            if (!(parsed instanceof JSONArray)) return;
            for (Object o : (JSONArray) parsed) {
                if (!(o instanceof JSONObject)) continue;
                JSONObject obj = (JSONObject) o;
                ProtectorData data = new ProtectorData(
                        (String) obj.get("world"),
                        ((Number) obj.get("x")).intValue(),
                        ((Number) obj.get("y")).intValue(),
                        ((Number) obj.get("z")).intValue(),
                        (String) obj.get("faction"),
                        (String) obj.get("nexoId"),
                        ((Number) obj.get("level")).intValue(),
                        ((Number) obj.get("radius")).intValue());
                Object fuel = obj.get("fuel");
                if (fuel instanceof Number) data.setFuelSeconds(((Number) fuel).longValue());
                Object active = obj.get("active");
                if (active instanceof Boolean) data.setActive((Boolean) active);
                Object chunks = obj.get("chunks");
                if (chunks instanceof JSONArray) {
                    for (Object pairRaw : (JSONArray) chunks) {
                        if (!(pairRaw instanceof JSONArray)) continue;
                        JSONArray pair = (JSONArray) pairRaw;
                        if (pair.size() < 2) continue;
                        int cx = ((Number) pair.get(0)).intValue();
                        int cz = ((Number) pair.get(1)).intValue();
                        data.rememberClaimed(cx, cz);
                    }
                }
                protectors.put(data.key(), data);
            }
        } catch (Exception e) {
            Logger.print("Failed to load NexoClaimProtector data: " + e.getMessage(),
                    Logger.PrefixType.WARNING);
        }
    }

    public Set<ProtectorData> getAll() {
        return new HashSet<>(protectors.values());
    }
}