package org.saberdev.outpost;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

public class OutpostRegion {

    private final String name;
    private final String worldName;
    private final int minChunkX;
    private final int minChunkZ;
    private final int maxChunkX;
    private final int maxChunkZ;
    private String ownerFactionId;

    public OutpostRegion(String name, String worldName, int minChunkX, int minChunkZ, int maxChunkX, int maxChunkZ) {
        this.name = name;
        this.worldName = worldName;
        this.minChunkX = Math.min(minChunkX, maxChunkX);
        this.minChunkZ = Math.min(minChunkZ, maxChunkZ);
        this.maxChunkX = Math.max(minChunkX, maxChunkX);
        this.maxChunkZ = Math.max(minChunkZ, maxChunkZ);
    }

    public String getName() { return name; }
    public String getWorldName() { return worldName; }
    public int getMinChunkX() { return minChunkX; }
    public int getMinChunkZ() { return minChunkZ; }
    public int getMaxChunkX() { return maxChunkX; }
    public int getMaxChunkZ() { return maxChunkZ; }
    public String getOwnerFactionId() { return ownerFactionId; }
    public void setOwnerFactionId(String ownerFactionId) { this.ownerFactionId = ownerFactionId; }
    public boolean hasOwner() { return ownerFactionId != null; }

    public World getWorld() { return Bukkit.getWorld(worldName); }

    public int getMinBlockX() { return minChunkX << 4; }
    public int getMinBlockZ() { return minChunkZ << 4; }
    public int getMaxBlockX() { return (maxChunkX << 4) + 15; }
    public int getMaxBlockZ() { return (maxChunkZ << 4) + 15; }

    public int getChunkWidth() { return maxChunkX - minChunkX + 1; }
    public int getChunkDepth() { return maxChunkZ - minChunkZ + 1; }

    public boolean contains(Location loc) {
        if (loc == null || loc.getWorld() == null || !loc.getWorld().getName().equals(worldName)) return false;
        int cx = loc.getBlockX() >> 4;
        int cz = loc.getBlockZ() >> 4;
        return cx >= minChunkX && cx <= maxChunkX && cz >= minChunkZ && cz <= maxChunkZ;
    }
}