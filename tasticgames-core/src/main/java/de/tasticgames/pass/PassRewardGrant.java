package de.tasticgames.pass;

import java.util.Objects;

/**
 * Eine Belohnung, die eine Einlösung tatsächlich vergeben hat.
 */
public record PassRewardGrant(
        int level,
        PassTrack track,
        PassRewardType rewardType,
        String rewardValue,
        long rewardAmount,
        PassRewardStatus status
) {

    public PassRewardGrant {
        Objects.requireNonNull(
                track,
                "track"
        );

        Objects.requireNonNull(
                rewardType,
                "rewardType"
        );

        Objects.requireNonNull(
                status,
                "status"
        );
    }
}
