package de.tasticgames.pass;

import java.util.List;

/**
 * Ergebnis einer Einlösung.
 *
 * <p>{@code outcome} trägt die serverseitige Entscheidung (etwa
 * {@code TIER_LOCKED}, {@code PREMIUM_REQUIRED} oder
 * {@code ALREADY_CLAIMED}), {@code state} den aufgefrischten
 * Pass-Zustand.</p>
 */
public record PassRewardGrantResult(
        boolean applied,
        String outcome,
        boolean duplicate,
        List<PassRewardGrant> granted,
        PassSnapshot state
) {

    /**
     * Ergebnis, wenn der Pass gerade nicht erreichbar ist.
     */
    public static final String OUTCOME_UNAVAILABLE = "UNAVAILABLE";

    public PassRewardGrantResult {
        granted = granted == null
                ? List.of()
                : List.copyOf(granted);
    }

    /**
     * Neutrales Ergebnis, solange der Pass nicht verfügbar ist.
     */
    public static PassRewardGrantResult unavailable() {
        return new PassRewardGrantResult(
                false,
                OUTCOME_UNAVAILABLE,
                false,
                List.of(),
                null
        );
    }
}
