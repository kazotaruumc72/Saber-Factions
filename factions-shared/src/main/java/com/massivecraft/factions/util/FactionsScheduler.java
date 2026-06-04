package com.massivecraft.factions.util;

import com.massivecraft.factions.FactionsPlugin;
import com.tcoded.folialib.impl.PlatformScheduler;
import com.tcoded.folialib.wrapper.task.WrappedTask;
import org.bukkit.Location;
import org.bukkit.entity.Entity;

/**
 * Folia-aware scheduling facade.
 *
 * <p>Folia removed the legacy Bukkit scheduler ({@code Bukkit.getScheduler().runTask*} throws
 * {@link UnsupportedOperationException}). This helper routes every scheduling call through
 * <a href="https://github.com/TechnicallyCoded/FoliaLib">FoliaLib</a>, which transparently picks
 * the right backend (Folia global/region/async schedulers on Folia, the legacy Bukkit scheduler on
 * Paper/Spigot). The method names mirror the Bukkit scheduler so call sites migrate mechanically.</p>
 *
 * <p>All returned tasks are {@link WrappedTask}s; cancel them with {@link #cancel(WrappedTask)}
 * instead of the old integer task ids.</p>
 */
public final class FactionsScheduler {

    private FactionsScheduler() {
    }

    private static PlatformScheduler scheduler() {
        return FactionsPlugin.getInstance().getFoliaScheduler();
    }

    // ---- Global / "main thread" tasks ----------------------------------------------------------

    /** Run on the next global tick. Replaces {@code runTask}. */
    public static void run(Runnable runnable) {
        scheduler().runNextTick(task -> runnable.run());
    }

    /** Run after {@code delayTicks}. Replaces {@code runTaskLater}. */
    public static WrappedTask runLater(Runnable runnable, long delayTicks) {
        return scheduler().runLater(runnable, Math.max(1L, delayTicks));
    }

    /** Repeating global task. Replaces {@code runTaskTimer} / {@code scheduleSyncRepeatingTask}. */
    public static WrappedTask runTimer(Runnable runnable, long delayTicks, long periodTicks) {
        return scheduler().runTimer(runnable, Math.max(1L, delayTicks), Math.max(1L, periodTicks));
    }

    // ---- Async tasks ---------------------------------------------------------------------------

    /** Run off the main thread as soon as possible. Replaces {@code runTaskAsynchronously}. */
    public static void runAsync(Runnable runnable) {
        scheduler().runAsync(task -> runnable.run());
    }

    /** Run off the main thread after {@code delayTicks}. Replaces {@code runTaskLaterAsynchronously}. */
    public static WrappedTask runLaterAsync(Runnable runnable, long delayTicks) {
        return scheduler().runLaterAsync(runnable, Math.max(1L, delayTicks));
    }

    /** Repeating async task. Replaces {@code runTaskTimerAsynchronously}. */
    public static WrappedTask runTimerAsync(Runnable runnable, long delayTicks, long periodTicks) {
        return scheduler().runTimerAsync(runnable, Math.max(1L, delayTicks), Math.max(1L, periodTicks));
    }

    // ---- Region-aware tasks (Folia correctness for world/entity state) -------------------------

    /** Run owning the region of {@code location}. Replaces a main-thread task touching that location. */
    public static void runAtLocation(Location location, Runnable runnable) {
        scheduler().runAtLocation(location, task -> runnable.run());
    }

    public static WrappedTask runAtLocationLater(Location location, Runnable runnable, long delayTicks) {
        return scheduler().runAtLocationLater(location, runnable, Math.max(1L, delayTicks));
    }

    /** Run owning the region of {@code entity}. Replaces a main-thread task touching that entity. */
    public static void runAtEntity(Entity entity, Runnable runnable) {
        scheduler().runAtEntity(entity, task -> runnable.run());
    }

    public static WrappedTask runAtEntityLater(Entity entity, Runnable runnable, long delayTicks) {
        return scheduler().runAtEntityLater(entity, runnable, Math.max(1L, delayTicks));
    }

    // ---- Cancellation --------------------------------------------------------------------------

    public static void cancel(WrappedTask task) {
        if (task != null) {
            scheduler().cancelTask(task);
        }
    }

    public static void cancelAll() {
        scheduler().cancelAllTasks();
    }
}