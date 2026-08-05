package de.tasticgames.scheduler.internal;

import de.tasticgames.TasticCorePlugin;
import de.tasticgames.scheduler.ScheduledTask;
import de.tasticgames.scheduler.SchedulerService;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public final class BukkitSchedulerService implements SchedulerService {

    private final TasticCorePlugin plugin;
    private final BukkitScheduler scheduler;
    private final Map<Integer, ScheduledTask> activeTasks;
    private final AtomicBoolean running;

    public BukkitSchedulerService(TasticCorePlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.scheduler = plugin.getServer().getScheduler();
        this.activeTasks = new ConcurrentHashMap<>();
        this.running = new AtomicBoolean(false);
    }

    @Override
    public String id() {
        return "scheduler";
    }

    @Override
    public void start() {
        if (!running.compareAndSet(false, true)) {
            throw new IllegalStateException(
                    "Scheduler service is already running."
            );
        }
    }

    @Override
    public void stop() {
        if (!running.compareAndSet(true, false)) {
            return;
        }

        cancelAll();
    }

    @Override
    public ScheduledTask runSync(Runnable task) {
        ensureRunning();
        Objects.requireNonNull(task, "task");

        return track(scheduler.runTask(plugin, task));
    }

    @Override
    public ScheduledTask runAsync(Runnable task) {
        ensureRunning();
        Objects.requireNonNull(task, "task");

        return track(scheduler.runTaskAsynchronously(plugin, task));
    }

    @Override
    public ScheduledTask runSyncLater(
            Runnable task,
            long delayTicks
    ) {
        ensureRunning();
        validateDelay(delayTicks);
        Objects.requireNonNull(task, "task");

        return track(
                scheduler.runTaskLater(plugin, task, delayTicks)
        );
    }

    @Override
    public ScheduledTask runAsyncLater(
            Runnable task,
            long delayTicks
    ) {
        ensureRunning();
        validateDelay(delayTicks);
        Objects.requireNonNull(task, "task");

        return track(
                scheduler.runTaskLaterAsynchronously(
                        plugin,
                        task,
                        delayTicks
                )
        );
    }

    @Override
    public ScheduledTask runSyncRepeating(
            Runnable task,
            long initialDelayTicks,
            long periodTicks
    ) {
        ensureRunning();
        validateDelay(initialDelayTicks);
        validatePeriod(periodTicks);
        Objects.requireNonNull(task, "task");

        return track(
                scheduler.runTaskTimer(
                        plugin,
                        task,
                        initialDelayTicks,
                        periodTicks
                )
        );
    }

    @Override
    public ScheduledTask runAsyncRepeating(
            Runnable task,
            long initialDelayTicks,
            long periodTicks
    ) {
        ensureRunning();
        validateDelay(initialDelayTicks);
        validatePeriod(periodTicks);
        Objects.requireNonNull(task, "task");

        return track(
                scheduler.runTaskTimerAsynchronously(
                        plugin,
                        task,
                        initialDelayTicks,
                        periodTicks
                )
        );
    }

    @Override
    public void cancelAll() {
        scheduler.cancelTasks(plugin);
        activeTasks.clear();
    }

    @Override
    public int activeTaskCount() {
        return activeTasks.size();
    }

    private ScheduledTask track(BukkitTask bukkitTask) {
        int taskId = bukkitTask.getTaskId();

        ScheduledTask scheduledTask = new BukkitScheduledTask(
                bukkitTask,
                () -> activeTasks.remove(taskId)
        );

        activeTasks.put(taskId, scheduledTask);
        return scheduledTask;
    }

    private void ensureRunning() {
        if (!running.get()) {
            throw new IllegalStateException(
                    "Scheduler service is not running."
            );
        }
    }

    private void validateDelay(long delayTicks) {
        if (delayTicks < 0) {
            throw new IllegalArgumentException(
                    "Delay must not be negative."
            );
        }
    }

    private void validatePeriod(long periodTicks) {
        if (periodTicks <= 0) {
            throw new IllegalArgumentException(
                    "Period must be greater than zero."
            );
        }
    }
}
