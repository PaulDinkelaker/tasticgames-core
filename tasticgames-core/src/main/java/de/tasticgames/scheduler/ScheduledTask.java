package de.tasticgames.scheduler;

public interface ScheduledTask {

    int id();

    boolean isCancelled();

    void cancel();
}
