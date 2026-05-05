package org.saberdev.nexoclaim;

import com.massivecraft.factions.util.Logger;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;

public final class NexoCompat {

    private static volatile NexoCompat instance;

    private final boolean available;
    private Method idFromItemMethod;
    private Method itemFromIdMethod;
    private Method itemBuilderBuildMethod;

    private NexoCompat() {
        Plugin plugin = Bukkit.getPluginManager().getPlugin("Nexo");
        boolean ok = false;
        if (plugin != null && plugin.isEnabled()) {
            try {
                Class<?> items = Class.forName("com.nexomc.nexo.api.NexoItems");
                this.idFromItemMethod = items.getMethod("idFromItem", ItemStack.class);
                this.itemFromIdMethod = items.getMethod("itemFromId", String.class);
                Class<?> builder = Class.forName("com.nexomc.nexo.items.ItemBuilder");
                this.itemBuilderBuildMethod = builder.getMethod("build");
                ok = true;
            } catch (Throwable t) {
                Logger.print("Nexo detected but its API could not be bound: " + t.getMessage(),
                        Logger.PrefixType.WARNING);
            }
        }
        this.available = ok;
    }

    public static NexoCompat get() {
        NexoCompat ref = instance;
        if (ref == null) {
            synchronized (NexoCompat.class) {
                ref = instance;
                if (ref == null) {
                    ref = new NexoCompat();
                    instance = ref;
                }
            }
        }
        return ref;
    }

    public boolean isAvailable() {
        return available;
    }

    public String idOf(ItemStack stack) {
        if (!available || stack == null) return null;
        try {
            Object result = idFromItemMethod.invoke(null, stack);
            return result instanceof String ? (String) result : null;
        } catch (Throwable t) {
            return null;
        }
    }

    public ItemStack itemFromId(String id) {
        if (!available || id == null) return null;
        try {
            Object builder = itemFromIdMethod.invoke(null, id);
            if (builder == null) return null;
            Object built = itemBuilderBuildMethod.invoke(builder);
            return built instanceof ItemStack ? (ItemStack) built : null;
        } catch (Throwable t) {
            return null;
        }
    }
}