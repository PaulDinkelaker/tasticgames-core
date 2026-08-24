package de.tasticgames.pass;

import java.time.Instant;
import java.util.Objects;

/**
 * Quest-Definition zusammen mit dem Fortschritt des Spielers in der
 * laufenden Periode.
 *
 * <p>{@code periodKey} benennt den UTC-Tag, die ISO-Woche oder die
 * Season, zu der der Fortschritt gehört.</p>
 */
public record PassQuestSnapshot(
        String questKey,
        PassQuestScope scope,
        PassGame game,
        String metric,
        long target,
        long progress,
        boolean completed,
        int xpReward,
        boolean premiumOnly,
        String displayKey,
        String periodKey,
        Instant resetsAt,
        int sortOrder
) {

    public PassQuestSnapshot {
        Objects.requireNonNull(
                questKey,
                "questKey"
        );

        Objects.requireNonNull(
                scope,
                "scope"
        );

        Objects.requireNonNull(
                game,
                "game"
        );

        Objects.requireNonNull(
                metric,
                "metric"
        );
    }
}
