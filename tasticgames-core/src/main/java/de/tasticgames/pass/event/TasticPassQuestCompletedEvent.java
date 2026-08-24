package de.tasticgames.pass.event;

import de.tasticgames.pass.PassQuestSnapshot;
import de.tasticgames.player.TasticPlayer;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.Objects;

/**
 * Wird ausgelöst, wenn eine Quest in der laufenden Periode
 * abgeschlossen wurde.
 *
 * <p>Das Event läuft immer auf dem Hauptthread.</p>
 */
public final class TasticPassQuestCompletedEvent
        extends Event {

    private static final HandlerList HANDLERS =
            new HandlerList();

    private final TasticPlayer tasticPlayer;
    private final PassQuestSnapshot quest;
    private final int xpAwarded;

    public TasticPassQuestCompletedEvent(
            TasticPlayer tasticPlayer,
            PassQuestSnapshot quest,
            int xpAwarded
    ) {
        super(false);

        this.tasticPlayer = Objects.requireNonNull(
                tasticPlayer,
                "tasticPlayer"
        );

        this.quest = Objects.requireNonNull(
                quest,
                "quest"
        );

        this.xpAwarded = xpAwarded;
    }

    public TasticPlayer tasticPlayer() {
        return tasticPlayer;
    }

    public PassQuestSnapshot quest() {
        return quest;
    }

    /**
     * Die tatsächlich gutgeschriebenen XP des Abschlusses.
     */
    public int xpAwarded() {
        return xpAwarded;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
