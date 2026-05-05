package org.saberdev.nexoclaim;

import com.massivecraft.factions.FLocation;

import java.util.HashSet;
import java.util.Set;

public final class ProtectorData {

    private final String worldName;
    private final int blockX;
    private final int blockY;
    private final int blockZ;
    private final String factionId;
    private final String nexoItemId;
    private final int level;
    private final int chunkRadius;
    private final Set<long[]> claimedChunks = new HashSet<>();
    private long fuelSeconds;
    private boolean active;

    public ProtectorData(String worldName, int blockX, int blockY, int blockZ,
                         String factionId, String nexoItemId, int level, int chunkRadius) {
        this.worldName = worldName;
        this.blockX = blockX;
        this.blockY = blockY;
        this.blockZ = blockZ;
        this.factionId = factionId;
        this.nexoItemId = nexoItemId;
        this.level = level;
        this.chunkRadius = chunkRadius;
        this.fuelSeconds = 0;
        this.active = false;
    }

    public String getWorldName() { return worldName; }
    public int getBlockX() { return blockX; }
    public int getBlockY() { return blockY; }
    public int getBlockZ() { return blockZ; }
    public String getFactionId() { return factionId; }
    public String getNexoItemId() { return nexoItemId; }
    public int getLevel() { return level; }
    public int getChunkRadius() { return chunkRadius; }

    public long getFuelSeconds() { return fuelSeconds; }
    public void setFuelSeconds(long fuelSeconds) { this.fuelSeconds = Math.max(0L, fuelSeconds); }
    public void addFuelSeconds(long delta) { setFuelSeconds(fuelSeconds + delta); }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public Set<long[]> getClaimedChunks() { return claimedChunks; }

    public void rememberClaimed(int cx, int cz) {
        claimedChunks.add(new long[]{cx, cz});
    }

    public void clearClaimed() {
        claimedChunks.clear();
    }

    public Set<FLocation> toFLocations() {
        Set<FLocation> set = new HashSet<>(claimedChunks.size());
        for (long[] coords : claimedChunks) {
            set.add(FLocation.wrap(worldName, (int) coords[0], (int) coords[1]));
        }
        return set;
    }

    public String key() {
        return worldName + ":" + blockX + ":" + blockY + ":" + blockZ;
    }

    public static String keyOf(String worldName, int x, int y, int z) {
        return worldName + ":" + x + ":" + y + ":" + z;
    }
}