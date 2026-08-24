package de.tasticgames.pass;

import java.time.Instant;
import java.util.Objects;

/**
 * Ein Tier, das der Spieler bereits eingelöst hat.
 */
public record PassClaimSnapshot(
        int level,
        PassTrack track,
        PassRewardType rewardType,
        String rewardValue,
        long rewardAmount,
        Instant claimedAt
) {

    public PassClaimSnapshot {
        Objects.requireNonNull(
                track,
                "track"
        );

        Objects.requireNonNull(
                rewardType,
                "rewardType"
        );
    }
}
