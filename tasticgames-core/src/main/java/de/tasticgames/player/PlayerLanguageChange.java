package de.tasticgames.player;

import de.tasticgames.localization.SupportedLanguage;

import java.util.Objects;

public record PlayerLanguageChange(
        TasticPlayer player,
        SupportedLanguage previousLanguage,
        SupportedLanguage newLanguage
) {

    public PlayerLanguageChange {
        Objects.requireNonNull(
                player,
                "player"
        );

        Objects.requireNonNull(
                previousLanguage,
                "previousLanguage"
        );

        Objects.requireNonNull(
                newLanguage,
                "newLanguage"
        );
    }

    public boolean changed() {
        return previousLanguage != newLanguage;
    }
}
