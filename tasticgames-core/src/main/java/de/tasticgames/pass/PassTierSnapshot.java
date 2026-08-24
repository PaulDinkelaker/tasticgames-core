package de.tasticgames.pass;

import java.util.Objects;

/**
 * Ein Belohnungsplatz der Season-Strecke.
 *
 * <p>{@code rewardValue} trägt je nach {@link PassRewardType} die
 * Cosmetic-ID, den Feature-Key oder die Boost-Dauer,
 * {@code rewardAmount} den numerischen Anteil.</p>
 */
public record PassTierSnapshot(
        int level,
        PassTrack track,
        PassRewardType rewardType,
        String rewardValue,
        long rewardAmount,
        String displayKey,
        String icon
) {

    public PassTierSnapshot {
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
