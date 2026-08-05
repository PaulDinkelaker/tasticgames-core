package de.tasticgames.player.listener;

import de.tasticgames.TasticCorePlugin;
import de.tasticgames.api.ApiClient;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CompletionException;

public final class PlayerConnectionListener implements Listener {

    private final TasticCorePlugin plugin;
    private final ApiClient apiClient;
    private final String serverName;

    /*
     * API-Operationen werden pro UUID sequenziell ausgeführt.
     * Ein Disconnect kann dadurch keinen noch laufenden Connect überholen.
     */
    private final ConcurrentMap<UUID, CompletableFuture<Void>> operations =
            new ConcurrentHashMap<>();

    public PlayerConnectionListener(
            TasticCorePlugin plugin,
            ApiClient apiClient,
            String serverName
    ) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.apiClient = Objects.requireNonNull(apiClient, "apiClient");
        this.serverName = validateServerName(serverName);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        UUID minecraftUuid = event.getPlayer().getUniqueId();
        String username = event.getPlayer().getName();

        enqueue(
                minecraftUuid,
                () -> apiClient
                        .registerLogin(minecraftUuid, username)
                        .thenCompose(ignored ->
                                apiClient.connect(
                                        minecraftUuid,
                                        serverName
                                )
                        )
                        .thenAccept(presence ->
                                plugin.getLogger().info(
                                        "Registered player connection: "
                                                + username
                                                + " ["
                                                + minecraftUuid
                                                + "] on "
                                                + presence.currentServer()
                                )
                        ),
                "register connection for " + username
        );
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID minecraftUuid = event.getPlayer().getUniqueId();
        String username = event.getPlayer().getName();

        enqueue(
                minecraftUuid,
                () -> apiClient
                        .disconnect(minecraftUuid)
                        .thenAccept(ignored ->
                                plugin.getLogger().info(
                                        "Registered player disconnect: "
                                                + username
                                                + " ["
                                                + minecraftUuid
                                                + "]"
                                )
                        ),
                "register disconnect for " + username
        );
    }

    public void stop() {
        operations.clear();
    }

    private void enqueue(
            UUID minecraftUuid,
            Operation operation,
            String description
    ) {
        operations.compute(
                minecraftUuid,
                (uuid, previousOperation) -> {
                    CompletableFuture<Void> predecessor =
                            previousOperation == null
                                    ? CompletableFuture.completedFuture(null)
                                    : previousOperation.handle(
                                    (ignored, throwable) -> null
                            );

                    CompletableFuture<Void> nextOperation = predecessor
                            .thenCompose(ignored -> {
                                try {
                                    return operation.execute();
                                } catch (Exception exception) {
                                    return CompletableFuture.failedFuture(
                                            exception
                                    );
                                }
                            })
                            .whenComplete((ignored, throwable) -> {
                                if (throwable != null) {
                                    Throwable cause = unwrap(throwable);

                                    plugin.getLogger().warning(
                                            "Failed to "
                                                    + description
                                                    + ": "
                                                    + cause.getMessage()
                                    );
                                }
                            });

                    nextOperation.whenComplete(
                            (ignored, throwable) ->
                                    operations.remove(
                                            uuid,
                                            nextOperation
                                    )
                    );

                    return nextOperation;
                }
        );
    }

    private Throwable unwrap(Throwable throwable) {
        Throwable current = throwable;

        while ((current instanceof CompletionException
                || current instanceof java.util.concurrent.ExecutionException)
                && current.getCause() != null) {
            current = current.getCause();
        }

        return current;
    }

    private String validateServerName(String serverName) {
        Objects.requireNonNull(serverName, "serverName");

        String normalized = serverName.trim().toLowerCase();

        if (normalized.isBlank()) {
            throw new IllegalArgumentException(
                    "Server name must not be blank."
            );
        }

        if (normalized.length() > 64) {
            throw new IllegalArgumentException(
                    "Server name must not exceed 64 characters."
            );
        }

        if (!normalized.matches("[a-z0-9._-]+")) {
            throw new IllegalArgumentException(
                    "Invalid server name: " + serverName
            );
        }

        return normalized;
    }

    @FunctionalInterface
    private interface Operation {

        CompletableFuture<Void> execute();
    }
}
