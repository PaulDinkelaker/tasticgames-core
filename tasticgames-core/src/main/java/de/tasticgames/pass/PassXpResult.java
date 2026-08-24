package de.tasticgames.pass;

import java.util.List;

/**
 * Ergebnis einer idempotenten XP-Vergabe.
 *
 * <p>{@code outcome} trägt die serverseitige Entscheidung (etwa
 * {@code CAPPED} oder {@code NO_ACTIVE_SEASON}), {@code unlockedTiers}
 * die durch die Vergabe freigeschalteten Tiers.</p>
 */
public record PassXpResult(
        boolean applied,
        String outcome,
        boolean duplicate,
        long requestedAmount,
        long appliedAmount,
        long dailyRemaining,
        int levelBefore,
        int levelAfter,
        long totalXp,
        long xpIntoLevel,
        long xpForNextLevel,
        List<PassTierSnapshot> unlockedTiers
) {

    public PassXpResult {
        unlockedTiers = unlockedTiers == null
                ? List.of()
                : List.copyOf(unlockedTiers);
    }

    /**
     * Meldet, ob die Vergabe einen Levelaufstieg ausgelöst hat.
     */
    public boolean levelledUp() {
        return levelAfter > levelBefore;
    }
}
