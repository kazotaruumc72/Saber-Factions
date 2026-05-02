package org.saberdev.outpost;

import com.massivecraft.factions.FactionsPlugin;
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

public class OutpostManager {

    private static final int SNAPSHOT_MAGIC = 0x53424F50; // "SBOP"
    private static final byte SNAPSHOT_VERSION = 1;

    private final File dataFolder;
    private final File metadataFile;
    private final File snapshotsFolder;
    private final Map<String, OutpostRegion> regions = new HashMap<>();

    public OutpostManager(File pluginDataFolder) {
        this.dataFolder = new File(pluginDataFolder, "outposts");
        this.metadataFile = new File(this.dataFolder, "regions.json");
        this.snapshotsFolder = new File(this.dataFolder, "snapshots");
        if (!this.dataFolder.exists()) this.dataFolder.mkdirs();
        if (!this.snapshotsFolder.exists()) this.snapshotsFolder.mkdirs();
        load();
    }

    public Collection<OutpostRegion> getAll() { return regions.values(); }

    public OutpostRegion getByName(String name) {
        return name == null ? null : regions.get(name.toLowerCase(Locale.ROOT));
    }

    public OutpostRegion getAt(Location loc) {
        if (loc == null || loc.getWorld() == null) return null;
        for (OutpostRegion r : regions.values()) {
            if (r.contains(loc)) return r;
        }
        return null;
    }

    public boolean register(OutpostRegion region) {
        String key = region.getName().toLowerCase(Locale.ROOT);
        if (regions.containsKey(key)) return false;
        regions.put(key, region);
        saveMetadata();
        return true;
    }

    public void remove(OutpostRegion region) {
        regions.remove(region.getName().toLowerCase(Locale.ROOT));
        File snap = snapshotFile(region);
        if (snap.exists()) snap.delete();
        saveMetadata();
    }

    public void persistOwnership() { saveMetadata(); }

    private File snapshotFile(OutpostRegion region) {
        return new File(snapshotsFolder, region.getName().toLowerCase(Locale.ROOT) + ".snap.gz");
    }

    @SuppressWarnings("unchecked")
    private void load() {
        if (!metadataFile.exists()) return;
        try (Reader r = new InputStreamReader(new FileInputStream(metadataFile))) {
            Object parsed = new JSONParser().parse(r);
            if (!(parsed instanceof JSONArray)) return;
            for (Object o : (JSONArray) parsed) {
                JSONObject obj = (JSONObject) o;
                OutpostRegion region = new OutpostRegion(
                        (String) obj.get("name"),
                        (String) obj.get("world"),
                        ((Number) obj.get("minChunkX")).intValue(),
                        ((Number) obj.get("minChunkZ")).intValue(),
                        ((Number) obj.get("maxChunkX")).intValue(),
                        ((Number) obj.get("maxChunkZ")).intValue()
                );
                Object owner = obj.get("owner");
                if (owner instanceof String) region.setOwnerFactionId((String) owner);
                regions.put(region.getName().toLowerCase(Locale.ROOT), region);
            }
        } catch (Exception e) {
            Logger.print("Failed to load outpost regions: " + e.getMessage(), Logger.PrefixType.WARNING);
        }
    }

    @SuppressWarnings("unchecked")
    private void saveMetadata() {
        JSONArray array = new JSONArray();
        for (OutpostRegion region : regions.values()) {
            JSONObject obj = new JSONObject();
            obj.put("name", region.getName());
            obj.put("world", region.getWorldName());
            obj.put("minChunkX", region.getMinChunkX());
            obj.put("minChunkZ", region.getMinChunkZ());
            obj.put("maxChunkX", region.getMaxChunkX());
            obj.put("maxChunkZ", region.getMaxChunkZ());
            if (region.getOwnerFactionId() != null) obj.put("owner", region.getOwnerFactionId());
            array.add(obj);
        }
        try (Writer w = new OutputStreamWriter(new FileOutputStream(metadataFile))) {
            w.write(array.toJSONString());
        } catch (IOException e) {
            Logger.print("Failed to save outpost regions: " + e.getMessage(), Logger.PrefixType.WARNING);
        }
    }

    public boolean snapshot(OutpostRegion region) {
        World world = region.getWorld();
        if (world == null) return false;

        int minY = world.getMinHeight();
        int maxY = world.getMaxHeight() - 1;
        int minX = region.getMinBlockX();
        int minZ = region.getMinBlockZ();
        int maxX = region.getMaxBlockX();
        int maxZ = region.getMaxBlockZ();

        Map<String, Integer> paletteIndex = new LinkedHashMap<>();
        List<String> palette = new ArrayList<>();
        int width = maxX - minX + 1;
        int depth = maxZ - minZ + 1;
        int height = maxY - minY + 1;
        int[] indices = new int[width * depth * height];

        // Force-load chunks
        for (int cx = region.getMinChunkX(); cx <= region.getMaxChunkX(); cx++) {
            for (int cz = region.getMinChunkZ(); cz <= region.getMaxChunkZ(); cz++) {
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

        try (DataOutputStream out = new DataOutputStream(new GZIPOutputStream(new FileOutputStream(snapshotFile(region))))) {
            out.writeInt(SNAPSHOT_MAGIC);
            out.writeByte(SNAPSHOT_VERSION);
            out.writeInt(minY);
            out.writeInt(maxY);
            out.writeInt(width);
            out.writeInt(depth);
            out.writeInt(palette.size());
            for (String s : palette) out.writeUTF(s);
            for (int v : indices) out.writeInt(v);
            return true;
        } catch (IOException e) {
            Logger.print("Failed to write outpost snapshot for " + region.getName() + ": " + e.getMessage(), Logger.PrefixType.WARNING);
            return false;
        }
    }

    public boolean restore(OutpostRegion region) {
        World world = region.getWorld();
        if (world == null) return false;
        File snap = snapshotFile(region);
        if (!snap.exists()) return false;

        try (DataInputStream in = new DataInputStream(new GZIPInputStream(new FileInputStream(snap)))) {
            int magic = in.readInt();
            if (magic != SNAPSHOT_MAGIC) {
                Logger.print("Outpost snapshot for " + region.getName() + " has invalid magic.", Logger.PrefixType.WARNING);
                return false;
            }
            in.readByte(); // version
            int minY = in.readInt();
            int maxY = in.readInt();
            int width = in.readInt();
            int depth = in.readInt();
            int paletteSize = in.readInt();
            BlockData[] palette = new BlockData[paletteSize];
            for (int i = 0; i < paletteSize; i++) {
                palette[i] = Bukkit.createBlockData(in.readUTF());
            }
            int minX = region.getMinBlockX();
            int minZ = region.getMinBlockZ();

            for (int cx = region.getMinChunkX(); cx <= region.getMaxChunkX(); cx++) {
                for (int cz = region.getMinChunkZ(); cz <= region.getMaxChunkZ(); cz++) {
                    Chunk c = world.getChunkAt(cx, cz);
                    if (!c.isLoaded()) c.load(true);
                }
            }

            for (int y = minY; y <= maxY; y++) {
                for (int z = 0; z < depth; z++) {
                    for (int x = 0; x < width; x++) {
                        int pidx = in.readInt();
                        Block b = world.getBlockAt(minX + x, y, minZ + z);
                        BlockData target = palette[pidx];
                        if (!b.getBlockData().equals(target)) {
                            b.setBlockData(target, false);
                        }
                    }
                }
            }
            return true;
        } catch (IOException e) {
            Logger.print("Failed to restore outpost snapshot for " + region.getName() + ": " + e.getMessage(), Logger.PrefixType.WARNING);
            return false;
        }
    }
}