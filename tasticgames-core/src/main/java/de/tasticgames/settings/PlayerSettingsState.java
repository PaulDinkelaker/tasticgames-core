package de.tasticgames.settings;

import java.util.Map;
import java.util.Objects;

public record PlayerSettingsState(
        Map<String, Object> values,
        long revision
) {

    public PlayerSettingsState {
        Objects.requireNonNull(
                values,
                "values"
        );

        values =
                Map.copyOf(
                        values
                );

        if (revision < 0) {
            throw new IllegalArgumentException(
                    "revision must not be negative."
            );
        }
    }
}
