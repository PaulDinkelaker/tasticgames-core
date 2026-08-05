package de.tasticgames.scheduler.internal;

import de.tasticgames.scheduler.ScheduledTask;
import org.bukkit.scheduler.BukkitTask;

import java.util.Objects;

final class BukkitScheduledTask implements ScheduledTask {

    private final BukkitTask task;
    private final Runnable cancellationCallback;

    BukkitScheduledTask(
            BukkitTask task,
            Runnable cancellationCallback
    ) {
        this.task = Objects.requireNonNull(task, "task");
        this.cancellationCallback = Objects.requireNonNull(
                cancellationCallback,
                "cancellationCallback"
        );
    }

    @Override
    public int id() {
        return task.getTaskId();
    }

    @Override
    public boolean isCancelled() {
        return task.isCancelled();
    }

    @Override
    public void cancel() {
        if (task.isCancelled()) {
            return;
        }

        task.cancel();
        cancellationCallback.run();
    }
}
