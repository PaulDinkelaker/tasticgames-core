package de.tasticgames.api;

import java.time.Instant;
import java.util.UUID;

public record MinecraftAccount(
        long accountId,
        UUID minecraftUuid,
        String currentName,
        String language,
        String status,
        Instant firstSeenAt,
        Instant lastSeenAt,
        Instant createdAt,
        Instant updatedAt
) {
}
