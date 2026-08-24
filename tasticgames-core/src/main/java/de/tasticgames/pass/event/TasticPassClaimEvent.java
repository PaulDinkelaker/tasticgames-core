package de.tasticgames.pass.event;

import de.tasticgames.pass.PassRewardGrant;
import de.tasticgames.player.TasticPlayer;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.Objects;

/**
 * Wird für jede Belohnung ausgelöst, die eine Einlösung vergeben hat.
 *
 * <p>Das Event läuft immer auf dem Hauptthread.</p>
 */
public final class TasticPassClaimEvent
        extends Event {

    private static final HandlerList HANDLERS =
            new HandlerList();

    private final TasticPlayer tasticPlayer;
    private final PassRewardGrant grant;

    public TasticPassClaimEvent(
            TasticPlayer tasticPlayer,
            PassRewardGrant grant
    ) {
        super(false);

        this.tasticPlayer = Objects.requireNonNull(
                tasticPlayer,
                "tasticPlayer"
        );

        this.grant = Objects.requireNonNull(
                grant,
                "grant"
        );
    }

    public TasticPlayer tasticPlayer() {
        return tasticPlayer;
    }

    public PassRewardGrant grant() {
        return grant;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
