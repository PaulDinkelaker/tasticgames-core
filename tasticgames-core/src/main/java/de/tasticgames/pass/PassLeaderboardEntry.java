package de.tasticgames.pass;

import java.util.Objects;
import java.util.UUID;

/**
 * Ein Platz der Season-Rangliste.
 */
public record PassLeaderboardEntry(
        int rank,
        UUID player,
        String name,
        int level,
        long totalXp,
        boolean premium
) {

    public PassLeaderboardEntry {
        Objects.requireNonNull(
                player,
                "player"
        );
    }
}
