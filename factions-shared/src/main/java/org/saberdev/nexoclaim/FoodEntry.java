package org.saberdev.nexoclaim;

public final class FoodEntry {

    private final String nexoItemId;
    private final long fuelSeconds;

    public FoodEntry(String nexoItemId, long fuelSeconds) {
        this.nexoItemId = nexoItemId;
        this.fuelSeconds = fuelSeconds;
    }

    public String getNexoItemId() {
        return nexoItemId;
    }

    public long getFuelSeconds() {
        return fuelSeconds;
    }

    public static FoodEntry parse(String raw) {
        if (raw == null) return null;
        String nexoId = null;
        long seconds = -1;
        for (String part : raw.split(",")) {
            String[] kv = part.trim().split(":", 2);
            if (kv.length != 2) continue;
            String key = kv[0].trim().toLowerCase();
            String value = kv[1].trim();
            if (key.equals("item")) {
                if (value.toLowerCase().startsWith("nexo:")) {
                    nexoId = value.substring("nexo:".length()).trim();
                } else {
                    nexoId = value;
                }
            } else if (key.equals("time")) {
                seconds = parseDuration(value);
            }
        }
        if (nexoId == null || nexoId.isEmpty() || seconds <= 0) return null;
        return new FoodEntry(nexoId, seconds);
    }

    private static long parseDuration(String value) {
        if (value == null || value.isEmpty()) return -1;
        char unit = value.charAt(value.length() - 1);
        String num = value;
        long multiplier = 1;
        if (!Character.isDigit(unit)) {
            num = value.substring(0, value.length() - 1);
            switch (Character.toLowerCase(unit)) {
                case 's': multiplier = 1L; break;
                case 'm': multiplier = 60L; break;
                case 'h': multiplier = 3600L; break;
                case 'd': multiplier = 86400L; break;
                default: return -1;
            }
        }
        try {
            return Long.parseLong(num.trim()) * multiplier;
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}