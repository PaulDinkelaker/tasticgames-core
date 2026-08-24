package de.tasticgames.pass;

import java.util.Objects;

/**
 * Quest-Definition einer Season ohne Spielerfortschritt.
 *
 * <p>{@code metric} ist der spielspezifisch benannte Zähler, den die
 * Gameserver melden (zum Beispiel {@code cookie.clicks}).</p>
 */
public record PassQuestDefinition(
        String questKey,
        PassQuestScope scope,
        PassGame game,
        String metric,
        long target,
        int xpReward,
        boolean premiumOnly,
        String displayKey,
        int sortOrder
) {

    public PassQuestDefinition {
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
