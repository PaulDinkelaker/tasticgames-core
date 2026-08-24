package de.tasticgames.settings;

import de.tasticgames.player.TasticPlayer;

import java.util.Objects;

public record PlayerSettingChange<T>(
        TasticPlayer player,
        SettingKey<T> key,
        T previousValue,
        T newValue
) {

    public PlayerSettingChange {
        Objects.requireNonNull(
                player,
                "player"
        );

        Objects.requireNonNull(
                key,
                "key"
        );

        previousValue =
                key.validate(
                        previousValue
                );

        newValue =
                key.validate(
                        newValue
                );
    }

    public boolean changed() {
        return !Objects.equals(
                previousValue,
                newValue
        );
    }
}
