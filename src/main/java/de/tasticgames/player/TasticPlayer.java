package de.tasticgames.player;

import java.time.Instant;
import java.util.UUID;
import de.tasticgames.settings.PlayerSettings;

public interface TasticPlayer {

    long accountId();

    UUID minecraftUuid();

    String username();

    String language();

    String accountStatus();

    Instant firstSeenAt();

    Instant lastSeenAt();

    Instant createdAt();

    Instant updatedAt();

    PlayerSettings settings();

    PlayerRuntimeContext runtime();

    PlayerState state();

    boolean ready();
}
