package org.saberdev.nexoclaim;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ProtectorDefinition {

    private final String nexoItemId;
    private final int level;
    private final int chunkRadius;
    private final List<FoodEntry> food;
    private final Map<String, Long> foodIndex;

    public ProtectorDefinition(String nexoItemId, int level, int chunkRadius, List<FoodEntry> food) {
        this.nexoItemId = nexoItemId;
        this.level = level;
        this.chunkRadius = chunkRadius;
        this.food = Collections.unmodifiableList(food);
        Map<String, Long> map = new HashMap<>(food.size());
        for (FoodEntry entry : food) {
            map.put(entry.getNexoItemId(), entry.getFuelSeconds());
        }
        this.foodIndex = Collections.unmodifiableMap(map);
    }

    public String getNexoItemId() { return nexoItemId; }
    public int getLevel() { return level; }
    public int getChunkRadius() { return chunkRadius; }
    public List<FoodEntry> getFood() { return food; }

    public Long fuelFor(String nexoItemId) {
        return nexoItemId == null ? null : foodIndex.get(nexoItemId);
    }
}