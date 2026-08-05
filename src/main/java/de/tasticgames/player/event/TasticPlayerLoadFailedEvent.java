package de.tasticgames.player.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.Objects;
import java.util.UUID;

public final class TasticPlayerLoadFailedEvent extends Event {

    private static final HandlerList HANDLER_LIST =
            new HandlerList();

    private final Player player;
    private final UUID minecraftUuid;
    private final Throwable cause;

    public TasticPlayerLoadFailedEvent(
            Player player,
            Throwable cause
    ) {
        super(false);

        this.player = Objects.requireNonNull(
                player,
                "player"
        );

        this.minecraftUuid = player.getUniqueId();

        this.cause = Objects.requireNonNull(
                cause,
                "cause"
        );
    }

    public Player player() {
        return player;
    }

    public UUID minecraftUuid() {
        return minecraftUuid;
    }

    public Throwable cause() {
        return cause;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLER_LIST;
    }

    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }
}
