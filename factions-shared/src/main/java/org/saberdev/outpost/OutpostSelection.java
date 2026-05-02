package org.saberdev.outpost;

import org.bukkit.Location;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class OutpostSelection {

    private static final Map<UUID, Location> POS1 = new HashMap<>();
    private static final Map<UUID, Location> POS2 = new HashMap<>();

    private OutpostSelection() {}

    public static void setPos1(UUID player, Location loc) { POS1.put(player, loc); }
    public static void setPos2(UUID player, Location loc) { POS2.put(player, loc); }

    public static Location getPos1(UUID player) { return POS1.get(player); }
    public static Location getPos2(UUID player) { return POS2.get(player); }

    public static void clear(UUID player) {
        POS1.remove(player);
        POS2.remove(player);
    }
}
