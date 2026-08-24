package de.tasticgames.pass.event;

import de.tasticgames.pass.PassTierSnapshot;
import de.tasticgames.player.TasticPlayer;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.List;
import java.util.Objects;

/**
 * Wird ausgelöst, wenn ein Spieler mindestens ein Pass-Level
 * aufgestiegen ist.
 *
 * <p>Das Event läuft immer auf dem Hauptthread.</p>
 */
public final class TasticPassLevelUpEvent
        extends Event {

    private static final HandlerList HANDLERS =
            new HandlerList();

    private final TasticPlayer tasticPlayer;
    private final int fromLevel;
    private final int toLevel;
    private final List<PassTierSnapshot> unlocked;

    public TasticPassLevelUpEvent(
            TasticPlayer tasticPlayer,
            int fromLevel,
            int toLevel,
            List<PassTierSnapshot> unlocked
    ) {
        super(false);

        this.tasticPlayer = Objects.requireNonNull(
                tasticPlayer,
                "tasticPlayer"
        );

        if (toLevel <= fromLevel) {
            throw new IllegalArgumentException(
                    "New pass level must be greater than the previous level."
            );
        }

        this.fromLevel = fromLevel;
        this.toLevel = toLevel;

        this.unlocked = List.copyOf(
                Objects.requireNonNull(
                        unlocked,
                        "unlocked"
                )
        );
    }

    public TasticPlayer tasticPlayer() {
        return tasticPlayer;
    }

    public int fromLevel() {
        return fromLevel;
    }

    public int toLevel() {
        return toLevel;
    }

    /**
     * Die durch den Aufstieg freigeschalteten Tiers beider Spuren.
     */
    public List<PassTierSnapshot> unlocked() {
        return unlocked;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
