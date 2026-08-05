package de.tasticgames.api;

import java.time.Instant;
import java.util.UUID;

public record PlayerPresence(
        long accountId,
        UUID minecraftUuid,
        String currentName,
        boolean online,
        String currentServer,
        Instant sessionStartedAt,
        Instant lastSeenAt
) {
}
