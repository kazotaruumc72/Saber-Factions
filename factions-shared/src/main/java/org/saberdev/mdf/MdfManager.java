package org.saberdev.mdf;

import com.massivecraft.factions.util.Logger;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.*;
import java.util.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class MdfManager {

    private static final int SNAPSHOT_MAGIC = 0x53424D44; // "SBMD"
    private static final byte SNAPSHOT_VERSION = 1;

    private final File dataFolder;
    private final File metadataFile;
    private final File snapshotsFolder;
    private final Map<String, MdfZone> zones = new HashMap<>();

    public MdfManager(File pluginDataFolder) {
        this.dataFolder = new File(pluginDataFolder, "mdf");
        this.metadataFile = new File(this.dataFolder, "zones.json");
        this.snapshotsFolder = new File(this.dataFolder, "snapshots");
        if (!this.dataFolder.exists()) this.dataFolder.mkdirs();
        if (!this.snapshotsFolder.exists()) this.snapshotsFolder.mkdirs();
        load();
    }

    public Collection<MdfZone> getAll() { return zones.values(); }

    public MdfZone getByName(String name) {
        return name == null ? null : zones.get(name.toLowerCase(Locale.ROOT));
    }

    public MdfZone getAt(Location loc) {
        if (loc == null || loc.getWorld() == null) return null;
        for (MdfZone z : zones.values()) {
            if (z.contains(loc)) return z;
        }
        return null;
    }

    public MdfZone getByOwner(String factionId) {
        if (factionId == null) return null;
        for (MdfZone z : zones.values()) {
            if (factionId.equals(z.getOwnerFactionId())) return z;
        }
        return null;
    }

    public List<MdfZone> getAllByOwner(String factionId) {
        List<MdfZone> out = new ArrayList<>();
        if (factionId == null) return out;
        for (MdfZone z : zones.values()) {
            if (factionId.equals(z.getOwnerFactionId())) out.add(z);
        }
        return out;
    }

    public boolean register(MdfZone zone) {
        String key = zone.getName().toLowerCase(Locale.ROOT);
        if (zones.containsKey(key)) return false;
        zones.put(key, zone);
        saveMetadata();
        return true;
    }

    public void remove(MdfZone zone) {
        zones.remove(zone.getName().toLowerCase(Locale.ROOT));
        File snap = snapshotFile(zone);
        if (snap.exists()) snap.delete();
        saveMetadata();
    }

    public void persist() { saveMetadata(); }

    private File snapshotFile(MdfZone zone) {
        return new File(snapshotsFolder, zone.getName().toLowerCase(Locale.ROOT) + ".snap.gz");
    }

    @SuppressWarnings("unchecked")
    private void load() {
        if (!metadataFile.exists()) return;
        try (Reader r = new InputStreamReader(new FileInputStream(metadataFile))) {
            Object parsed = new JSONParser().parse(r);
            if (!(parsed instanceof JSONArray)) return;
            for (Object o : (JSONArray) parsed) {
                JSONObject obj = (JSONObject) o;
                MdfZone zone = new MdfZone(
                        (String) obj.get("name"),
                        (String) obj.get("world"),
                        ((Number) obj.get("minX")).intValue(),
                        ((Number) obj.get("minY")).intValue(),
                        ((Number) obj.get("minZ")).intValue(),
                        ((Number) obj.get("maxX")).intValue(),
                        ((Number) obj.get("maxY")).intValue(),
                        ((Number) obj.get("maxZ")).intValue()
                );
                Object owner = obj.get("owner");
                if (owner instanceof String) zone.setOwnerFactionId((String) owner);
                Object wl = obj.get("whitelist");
                if (wl instanceof JSONArray) {
                    for (Object u : (JSONArray) wl) {
                        try {
                            zone.addToWhitelist(UUID.fromString((String) u));
                        } catch (IllegalArgumentException ignored) {}
                    }
                }
                zones.put(zone.getName().toLowerCase(Locale.ROOT), zone);
            }
        } catch (Exception e) {
            Logger.print("Failed to load MDF zones: " + e.getMessage(), Logger.PrefixType.WARNING);
        }
    }

    @SuppressWarnings("unchecked")
    private void saveMetadata() {
        JSONArray array = new JSONArray();
        for (MdfZone zone : zones.values()) {
            JSONObject obj = new JSONObject();
            obj.put("name", zone.getName());
            obj.put("world", zone.getWorldName());
            obj.put("minX", zone.getMinX());
            obj.put("minY", zone.getMinY());
            obj.put("minZ", zone.getMinZ());
            obj.put("maxX", zone.getMaxX());
            obj.put("maxY", zone.getMaxY());
            obj.put("maxZ", zone.getMaxZ());
            if (zone.getOwnerFactionId() != null) obj.put("owner", zone.getOwnerFactionId());
            JSONArray wl = new JSONArray();
            for (UUID u : zone.getWhitelist()) wl.add(u.toString());
            obj.put("whitelist", wl);
            array.add(obj);
        }
        try (Writer w = new OutputStreamWriter(new FileOutputStream(metadataFile))) {
            w.write(array.toJSONString());
        } catch (IOException e) {
            Logger.print("Failed to save MDF zones: " + e.getMessage(), Logger.PrefixType.WARNING);
        }
    }

    public boolean snapshot(MdfZone zone) {
        World world = zone.getWorld();
        if (world == null) return false;

        int minX = zone.getMinX();
        int minY = zone.getMinY();
        int minZ = zone.getMinZ();
        int maxX = zone.getMaxX();
        int maxY = zone.getMaxY();
        int maxZ = zone.getMaxZ();

        int width = zone.getWidth();
        int height = zone.getHeight();
        int depth = zone.getDepth();

        Map<String, Integer> paletteIndex = new LinkedHashMap<>();
        List<String> palette = new ArrayList<>();
        int[] indices = new int[width * height * depth];

        // Force-load chunks covering the zone
        int minCx = minX >> 4;
        int maxCx = maxX >> 4;
        int minCz = minZ >> 4;
        int maxCz = maxZ >> 4;
        for (int cx = minCx; cx <= maxCx; cx++) {
            for (int cz = minCz; cz <= maxCz; cz++) {
                Chunk c = world.getChunkAt(cx, cz);
                if (!c.isLoaded()) c.load(true);
            }
        }

        int idx = 0;
        for (int y = minY; y <= maxY; y++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int x = minX; x <= maxX; x++) {
                    String data = world.getBlockAt(x, y, z).getBlockData().getAsString();
                    Integer pidx = paletteIndex.get(data);
                    if (pidx == null) {
                        pidx = palette.size();
                        palette.add(data);
                        paletteIndex.put(data, pidx);
                    }
                    indices[idx++] = pidx;
                }
            }
        }

        try (DataOutputStream out = new DataOutputStream(new GZIPOutputStream(new FileOutputStream(snapshotFile(zone))))) {
            out.writeInt(SNAPSHOT_MAGIC);
            out.writeByte(SNAPSHOT_VERSION);
            out.writeInt(minX);
            out.writeInt(minY);
            out.writeInt(minZ);
            out.writeInt(width);
            out.writeInt(height);
            out.writeInt(depth);
            out.writeInt(palette.size());
            for (String s : palette) out.writeUTF(s);
            for (int v : indices) out.writeInt(v);
            return true;
        } catch (IOException e) {
            Logger.print("Failed to write MDF snapshot for " + zone.getName() + ": " + e.getMessage(), Logger.PrefixType.WARNING);
            return false;
        }
    }

    public boolean restore(MdfZone zone) {
        World world = zone.getWorld();
        if (world == null) return false;
        File snap = snapshotFile(zone);
        if (!snap.exists()) return false;

        try (DataInputStream in = new DataInputStream(new GZIPInputStream(new FileInputStream(snap)))) {
            int magic = in.readInt();
            if (magic != SNAPSHOT_MAGIC) {
                Logger.print("MDF snapshot for " + zone.getName() + " has invalid magic.", Logger.PrefixType.WARNING);
                return false;
            }
            in.readByte(); // version
            int minX = in.readInt();
            int minY = in.readInt();
            int minZ = in.readInt();
            int width = in.readInt();
            int height = in.readInt();
            int depth = in.readInt();
            int paletteSize = in.readInt();
            BlockData[] palette = new BlockData[paletteSize];
            for (int i = 0; i < paletteSize; i++) {
                palette[i] = Bukkit.createBlockData(in.readUTF());
            }

            int minCx = minX >> 4;
            int maxCx = (minX + width - 1) >> 4;
            int minCz = minZ >> 4;
            int maxCz = (minZ + depth - 1) >> 4;
            for (int cx = minCx; cx <= maxCx; cx++) {
                for (int cz = minCz; cz <= maxCz; cz++) {
                    Chunk c = world.getChunkAt(cx, cz);
                    if (!c.isLoaded()) c.load(true);
                }
            }

            for (int y = 0; y < height; y++) {
                for (int z = 0; z < depth; z++) {
                    for (int x = 0; x < width; x++) {
                        int pidx = in.readInt();
                        Block b = world.getBlockAt(minX + x, minY + y, minZ + z);
                        BlockData target = palette[pidx];
                        if (!b.getBlockData().equals(target)) {
                            b.setBlockData(target, false);
                        }
                    }
                }
            }
            return true;
        } catch (IOException e) {
            Logger.print("Failed to restore MDF snapshot for " + zone.getName() + ": " + e.getMessage(), Logger.PrefixType.WARNING);
            return false;
        }
    }
}