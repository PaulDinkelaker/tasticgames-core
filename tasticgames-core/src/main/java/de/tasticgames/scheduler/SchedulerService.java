package de.tasticgames.scheduler;

import de.tasticgames.service.Service;

public interface SchedulerService extends Service {

    ScheduledTask runSync(Runnable task);

    ScheduledTask runAsync(Runnable task);

    ScheduledTask runSyncLater(Runnable task, long delayTicks);

    ScheduledTask runAsyncLater(Runnable task, long delayTicks);

    ScheduledTask runSyncRepeating(
            Runnable task,
            long initialDelayTicks,
            long periodTicks
    );

    ScheduledTask runAsyncRepeating(
            Runnable task,
            long initialDelayTicks,
            long periodTicks
    );

    void cancelAll();

    int activeTaskCount();
}
