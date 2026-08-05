package de.tasticgames.api;

import java.util.Objects;

public record PlayerSettingUpdate(
        String key,
        Object value,
        PlayerSettingType type
) {

    public PlayerSettingUpdate {
        key = requireKey(key);

        Objects.requireNonNull(
                value,
                "value"
        );

        Objects.requireNonNull(
                type,
                "type"
        );
    }

    private static String requireKey(
            String key
    ) {
        Objects.requireNonNull(
                key,
                "key"
        );

        String normalized =
                key.trim().toLowerCase();

        if (!normalized.matches(
                "^[a-z0-9]+(?:[._-][a-z0-9]+)*$"
        )) {
            throw new IllegalArgumentException(
                    "Invalid setting key: " + key
            );
        }

        return normalized;
    }
}
