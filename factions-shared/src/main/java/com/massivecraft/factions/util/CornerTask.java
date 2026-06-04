package com.massivecraft.factions.util;

import com.massivecraft.factions.FLocation;
import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.zcore.util.TL;
import com.tcoded.folialib.wrapper.task.WrappedTask;

import java.util.List;

public class CornerTask implements Runnable {
    private final FPlayer fPlayer;
    private final List<FLocation> surrounding;
    private int amount;
    private WrappedTask wrappedTask;

    public CornerTask(FPlayer fPlayer, List<FLocation> surrounding) {
        this.amount = 0;
        this.fPlayer = fPlayer;
        this.surrounding = surrounding;
    }

    public void start(long delay, long period) {
        this.wrappedTask = FactionsScheduler.runTimer(this, delay, period);
    }

    private void cancel() {
        FactionsScheduler.cancel(this.wrappedTask);
    }

    public void run() {
        if (this.fPlayer.isOffline()) {
            cancel();
            return;
        }

        while (!this.surrounding.isEmpty()) {
            FLocation fLocation = this.surrounding.remove(0);
            if (this.fPlayer.attemptClaim(this.fPlayer.getFaction(), fLocation, true)) {
                ++amount;
            } else {
                this.fPlayer.sendMessage(TL.COMMAND_CORNER_FAIL_WITH_FEEDBACK.toString() + amount);
                cancel();
                return;
            }
        }

        this.fPlayer.sendMessage(TL.COMMAND_CORNER_CLAIMED.format(this.amount));
        cancel();
    }
}