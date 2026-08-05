package de.tasticgames.player.event;

import de.tasticgames.player.TasticPlayer;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.Objects;

public final class TasticPlayerReadyEvent extends Event {

    private static final HandlerList HANDLERS =
            new HandlerList();

    private final TasticPlayer tasticPlayer;

    public TasticPlayerReadyEvent(
            TasticPlayer tasticPlayer
    ) {
        /*
         * false bedeutet:
         * Das Event muss synchron auf dem Paper-Main-Thread
         * ausgelöst werden.
         */
        super(false);

        this.tasticPlayer = Objects.requireNonNull(
                tasticPlayer,
                "tasticPlayer"
        );

        if (!tasticPlayer.ready()) {
            throw new IllegalArgumentException(
                    "TasticPlayer must be ready before "
                            + "TasticPlayerReadyEvent is created."
            );
        }
    }

    public TasticPlayer tasticPlayer() {
        return tasticPlayer;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
