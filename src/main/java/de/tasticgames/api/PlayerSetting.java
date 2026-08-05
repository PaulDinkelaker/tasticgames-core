package de.tasticgames.api;

import java.time.Instant;
import java.util.Objects;

public record PlayerSetting(
        String key,
        Object value,
        PlayerSettingType type,
        Instant updatedAt
) {

    public PlayerSetting {
        key = requireKey(key);

        Objects.requireNonNull(
                value,
                "value"
        );

        Objects.requireNonNull(
                type,
                "type"
        );

        Objects.requireNonNull(
                updatedAt,
                "updatedAt"
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
