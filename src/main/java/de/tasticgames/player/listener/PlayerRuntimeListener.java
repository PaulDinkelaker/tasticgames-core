package de.tasticgames.player.listener;

import de.tasticgames.TasticCorePlugin;
import de.tasticgames.onboarding.PlayerOnboardingService;
import de.tasticgames.player.PlayerManager;
import de.tasticgames.player.TasticPlayer;
import de.tasticgames.player.event.TasticPlayerLoadFailedEvent;
import de.tasticgames.player.event.TasticPlayerReadyEvent;
import de.tasticgames.player.event.TasticPlayerUnloadEvent;
import de.tasticgames.settings.PlayerSettingsService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

public final class PlayerRuntimeListener
        implements Listener {

    private final TasticCorePlugin plugin;
    private final PlayerManager playerManager;
    private final PlayerSettingsService playerSettingsService;
    private final PlayerOnboardingService playerOnboardingService;

    public PlayerRuntimeListener(
            TasticCorePlugin plugin,
            PlayerManager playerManager,
            PlayerSettingsService playerSettingsService,
            PlayerOnboardingService playerOnboardingService
    ) {
        this.plugin = Objects.requireNonNull(
                plugin,
                "plugin"
        );

        this.playerManager = Objects.requireNonNull(
                playerManager,
                "playerManager"
        );

        this.playerSettingsService = Objects.requireNonNull(
                playerSettingsService,
                "playerSettingsService"
        );

        this.playerOnboardingService = Objects.requireNonNull(
                playerOnboardingService,
                "playerOnboardingService"
        );
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(
            PlayerJoinEvent event
    ) {
        Player player =
                event.getPlayer();

        UUID minecraftUuid =
                player.getUniqueId();

        String username =
                player.getName();

        playerManager
                .load(player)
                .thenCompose(tasticPlayer ->
                        playerSettingsService
                                .load(minecraftUuid)
                                .thenCompose(ignored ->
                                        playerOnboardingService
                                                .load(minecraftUuid)
                                )
                                .thenApply(ignored ->
                                        tasticPlayer
                                )
                )
                .whenComplete((tasticPlayer, throwable) -> {
                    if (throwable != null) {
                        handleAsyncLoadFailure(
                                minecraftUuid,
                                username,
                                unwrap(throwable)
                        );

                        return;
                    }

                    plugin.getServer()
                            .getScheduler()
                            .runTask(
                                    plugin,
                                    () -> finalizePlayerLoad(
                                            minecraftUuid,
                                            username
                                    )
                            );
                });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(
            PlayerQuitEvent event
    ) {
        UUID minecraftUuid =
                event.getPlayer()
                        .getUniqueId();

        String username =
                event.getPlayer()
                        .getName();

        TasticPlayer tasticPlayer =
                playerManager
                        .find(minecraftUuid)
                        .orElse(null);

        playerSettingsService
                .flush(minecraftUuid)
                .handle((ignored, flushThrowable) -> {
                    if (flushThrowable != null) {
                        plugin.getLogger().warning(
                                "Failed to flush settings for "
                                        + username
                                        + " ["
                                        + minecraftUuid
                                        + "]: "
                                        + safeMessage(
                                        unwrap(flushThrowable)
                                )
                        );
                    }

                    return null;
                })
                .thenCompose(ignored -> {
                    playerOnboardingService.unload(
                            minecraftUuid
                    );

                    return playerManager.unload(
                            minecraftUuid
                    );
                })
                .whenComplete((ignored, unloadThrowable) ->
                        plugin.getServer()
                                .getScheduler()
                                .runTask(
                                        plugin,
                                        () -> {
                                            if (unloadThrowable != null) {
                                                plugin.getLogger().warning(
                                                        "Failed to unload TasticPlayer "
                                                                + username
                                                                + " ["
                                                                + minecraftUuid
                                                                + "]: "
                                                                + safeMessage(
                                                                unwrap(
                                                                        unloadThrowable
                                                                )
                                                        )
                                                );

                                                return;
                                            }

                                            if (tasticPlayer != null) {
                                                plugin.getServer()
                                                        .getPluginManager()
                                                        .callEvent(
                                                                new TasticPlayerUnloadEvent(
                                                                        tasticPlayer
                                                                )
                                                        );
                                            }

                                            plugin.getLogger().info(
                                                    "Unloaded TasticPlayer "
                                                            + username
                                                            + " ["
                                                            + minecraftUuid
                                                            + "]"
                                            );
                                        }
                                )
                );
    }

    private void finalizePlayerLoad(
            UUID minecraftUuid,
            String username
    ) {
        Player onlinePlayer =
                Bukkit.getPlayer(
                        minecraftUuid
                );

        if (onlinePlayer == null
                || !onlinePlayer.isOnline()) {
            discardDisconnectedPlayer(
                    minecraftUuid,
                    username
            );

            return;
        }

        try {
            TasticPlayer readyPlayer =
                    playerManager.markReady(
                            minecraftUuid
                    );

            logLoadedPlayer(
                    readyPlayer
            );

            plugin.getServer()
                    .getPluginManager()
                    .callEvent(
                            new TasticPlayerReadyEvent(
                                    readyPlayer
                            )
                    );
        } catch (Exception exception) {
            handleAsyncLoadFailure(
                    minecraftUuid,
                    username,
                    exception
            );
        }
    }

    private void handleAsyncLoadFailure(
            UUID minecraftUuid,
            String username,
            Throwable cause
    ) {
        cleanupFailedLoad(
                minecraftUuid
        ).whenComplete((ignored, cleanupThrowable) ->
                plugin.getServer()
                        .getScheduler()
                        .runTask(
                                plugin,
                                () -> {
                                    if (cleanupThrowable != null) {
                                        plugin.getLogger().warning(
                                                "Failed to clean up player runtime after load failure for "
                                                        + username
                                                        + " ["
                                                        + minecraftUuid
                                                        + "]: "
                                                        + safeMessage(
                                                        unwrap(
                                                                cleanupThrowable
                                                        )
                                                )
                                        );
                                    }

                                    handlePlayerLoadFailure(
                                            minecraftUuid,
                                            username,
                                            cause
                                    );
                                }
                        )
        );
    }

    private CompletableFuture<Void> cleanupFailedLoad(
            UUID minecraftUuid
    ) {
        playerOnboardingService.unload(
                minecraftUuid
        );

        return playerManager.failLoad(
                minecraftUuid
        );
    }

    private void discardDisconnectedPlayer(
            UUID minecraftUuid,
            String username
    ) {
        playerOnboardingService.unload(
                minecraftUuid
        );

        playerManager
                .unload(minecraftUuid)
                .whenComplete((ignored, throwable) -> {
                    if (throwable != null) {
                        plugin.getLogger().warning(
                                "Failed to discard disconnected TasticPlayer "
                                        + username
                                        + " ["
                                        + minecraftUuid
                                        + "]: "
                                        + safeMessage(
                                        unwrap(throwable)
                                )
                        );

                        return;
                    }

                    plugin.getLogger().info(
                            "Discarded loaded TasticPlayer "
                                    + username
                                    + " ["
                                    + minecraftUuid
                                    + "] because the player is no longer online."
                    );
                });
    }

    private void logLoadedPlayer(
            TasticPlayer player
    ) {
        plugin.getLogger().info(
                "Loaded TasticPlayer "
                        + player.username()
                        + " ["
                        + player.minecraftUuid()
                        + "], account "
                        + player.accountId()
                        + ", language "
                        + player.language()
        );
    }

    private void handlePlayerLoadFailure(
            UUID minecraftUuid,
            String username,
            Throwable cause
    ) {
        Player onlinePlayer =
                Bukkit.getPlayer(
                        minecraftUuid
                );

        plugin.getLogger().severe(
                "Failed to load TasticPlayer "
                        + username
                        + " ["
                        + minecraftUuid
                        + "]: "
                        + safeMessage(cause)
        );

        if (onlinePlayer == null
                || !onlinePlayer.isOnline()) {
            return;
        }

        plugin.getServer()
                .getPluginManager()
                .callEvent(
                        new TasticPlayerLoadFailedEvent(
                                onlinePlayer,
                                cause
                        )
                );
    }

    private Throwable unwrap(
            Throwable throwable
    ) {
        Throwable current =
                throwable;

        while ((current instanceof CompletionException
                || current instanceof ExecutionException)
                && current.getCause() != null) {
            current =
                    current.getCause();
        }

        return current;
    }

    private String safeMessage(
            Throwable throwable
    ) {
        String message =
                throwable.getMessage();

        if (message == null
                || message.isBlank()) {
            return throwable
                    .getClass()
                    .getSimpleName();
        }

        return message;
    }
}
