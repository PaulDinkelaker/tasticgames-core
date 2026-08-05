package de.tasticgames.player;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

public final class PlayerRuntimeContext {

    private final Instant loadedAt;

    private volatile String serverName;
    private volatile boolean unloading;

    public PlayerRuntimeContext(
            Instant loadedAt,
            String serverName
    ) {
        this.loadedAt = Objects.requireNonNull(
                loadedAt,
                "loadedAt"
        );

        this.serverName = normalizeServerName(
                serverName
        );

        this.unloading = false;
    }

    public static PlayerRuntimeContext create(
            String serverName
    ) {
        return new PlayerRuntimeContext(
                Instant.now(),
                serverName
        );
    }

    public Instant loadedAt() {
        return loadedAt;
    }

    public Duration loadedDuration() {
        return Duration.between(
                loadedAt,
                Instant.now()
        );
    }

    public String serverName() {
        return serverName;
    }

    public void serverName(
            String serverName
    ) {
        this.serverName = normalizeServerName(
                serverName
        );
    }

    public boolean unloading() {
        return unloading;
    }

    public void beginUnload() {
        unloading = true;
    }

    private static String normalizeServerName(
            String serverName
    ) {
        Objects.requireNonNull(
                serverName,
                "serverName"
        );

        String normalized = serverName
                .trim()
                .toLowerCase();

        if (!normalized.matches(
                "^[a-z0-9._-]{1,64}$"
        )) {
            throw new IllegalArgumentException(
                    "Invalid server name: "
                            + serverName
            );
        }

        return normalized;
    }
}
