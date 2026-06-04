package org.saberdev.mdf;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class MdfZone {

    private final String name;
    private final String worldName;
    private final int minX;
    private final int minY;
    private final int minZ;
    private final int maxX;
    private final int maxY;
    private final int maxZ;
    private String ownerFactionId;
    private final Set<UUID> whitelist = new HashSet<>();

    public MdfZone(String name, String worldName, int x1, int y1, int z1, int x2, int y2, int z2) {
        this.name = name;
        this.worldName = worldName;
        this.minX = Math.min(x1, x2);
        this.minY = Math.min(y1, y2);
        this.minZ = Math.min(z1, z2);
        this.maxX = Math.max(x1, x2);
        this.maxY = Math.max(y1, y2);
        this.maxZ = Math.max(z1, z2);
    }

    public String getName() { return name; }
    public String getWorldName() { return worldName; }
    public int getMinX() { return minX; }
    public int getMinY() { return minY; }
    public int getMinZ() { return minZ; }
    public int getMaxX() { return maxX; }
    public int getMaxY() { return maxY; }
    public int getMaxZ() { return maxZ; }

    public String getOwnerFactionId() { return ownerFactionId; }
    public void setOwnerFactionId(String ownerFactionId) { this.ownerFactionId = ownerFactionId; }
    public boolean hasOwner() { return ownerFactionId != null; }

    public Set<UUID> getWhitelist() { return whitelist; }
    public boolean isWhitelisted(UUID uuid) { return whitelist.contains(uuid); }
    public boolean addToWhitelist(UUID uuid) { return whitelist.add(uuid); }
    public boolean removeFromWhitelist(UUID uuid) { return whitelist.remove(uuid); }
    public void clearWhitelist() { whitelist.clear(); }

    public World getWorld() { return Bukkit.getWorld(worldName); }

    public int getWidth() { return maxX - minX + 1; }
    public int getHeight() { return maxY - minY + 1; }
    public int getDepth() { return maxZ - minZ + 1; }

    public boolean contains(Location loc) {
        if (loc == null || loc.getWorld() == null || !loc.getWorld().getName().equals(worldName)) return false;
        int x = loc.getBlockX();
        int y = loc.getBlockY();
        int z = loc.getBlockZ();
        return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
    }
}