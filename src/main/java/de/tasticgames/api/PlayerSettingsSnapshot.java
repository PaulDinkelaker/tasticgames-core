package de.tasticgames.api;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record PlayerSettingsSnapshot(
        long accountId,
        UUID minecraftUuid,
        List<PlayerSetting> settings
) {

    public PlayerSettingsSnapshot {
        if (accountId <= 0) {
            throw new IllegalArgumentException(
                    "accountId must be positive."
            );
        }

        Objects.requireNonNull(
                minecraftUuid,
                "minecraftUuid"
        );

        Objects.requireNonNull(
                settings,
                "settings"
        );

        settings = List.copyOf(settings);
    }
}
