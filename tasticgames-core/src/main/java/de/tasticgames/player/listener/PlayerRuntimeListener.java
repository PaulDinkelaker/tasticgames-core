package de.tasticgames.player.listener;

import de.tasticgames.TasticCorePlugin;
import de.tasticgames.onboarding.PlayerOnboardingService;
import de.tasticgames.pass.PlayerPassService;
import de.tasticgames.player.PlayerManager;
import de.tasticgames.player.TasticPlayer;
import de.tasticgames.player.event.TasticPlayerLoadFailedEvent;
import de.tasticgames.player.event.TasticPlayerReadyEvent;
import de.tasticgames.player.event.TasticPlayerUnloadEvent;
import de.tasticgames.settings.PlayerSettingsService;
import de.tasticgames.title.PlayerTitleService;
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
    private final PlayerPassService playerPassService;
    private final PlayerTitleService playerTitleService;

    public PlayerRuntimeListener(
            TasticCorePlugin plugin,
            PlayerManager playerManager,
            PlayerSettingsService playerSettingsService,
            PlayerOnboardingService playerOnboardingService,
            PlayerPassService playerPassService,
            PlayerTitleService playerTitleService
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

        this.playerPassService = Objects.requireNonNull(
                playerPassService,
                "playerPassService"
        );

        this.playerTitleService = Objects.requireNonNull(
                playerTitleService,
                "playerTitleService"
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
                .load(
                        player
                )
                .thenCompose(tasticPlayer -> {
                    if (!plugin.isEnabled()) {
                        return CompletableFuture.failedFuture(
                                new PluginShutdownDuringLoadException()
                        );
                    }

                    if (!isPlayerOnline(
                            minecraftUuid
                    )) {
                        return CompletableFuture.failedFuture(
                                new PlayerDisconnectedDuringLoadException(
                                        minecraftUuid
                                )
                        );
                    }

                    return playerSettingsService
                            .load(
                                    minecraftUuid
                            )
                            .thenApply(
                                    ignored ->
                                            tasticPlayer
                            );
                })
                .thenCompose(tasticPlayer -> {
                    if (!plugin.isEnabled()) {
                        return CompletableFuture.failedFuture(
                                new PluginShutdownDuringLoadException()
                        );
                    }

                    if (!isPlayerOnline(
                            minecraftUuid
                    )) {
                        return CompletableFuture.failedFuture(
                                new PlayerDisconnectedDuringLoadException(
                                        minecraftUuid
                                )
                        );
                    }

                    return playerOnboardingService
                            .load(
                                    minecraftUuid
                            )
                            .thenApply(
                                    ignored ->
                                            tasticPlayer
                            );
                })
                .whenComplete(
                        (tasticPlayer, throwable) -> {
                            if (throwable != null) {
                                Throwable cause =
                                        unwrap(
                                                throwable
                                        );

                                if (cause instanceof PluginShutdownDuringLoadException) {
                                    return;
                                }

                                if (cause instanceof PlayerDisconnectedDuringLoadException) {
                                    discardDisconnectedPlayer(
                                            minecraftUuid,
                                            username
                                    );

                                    return;
                                }

                                handleAsyncLoadFailure(
                                        minecraftUuid,
                                        username,
                                        cause
                                );

                                return;
                            }

                            if (!plugin.isEnabled()) {
                                return;
                            }

                            plugin.getServer()
                                    .getScheduler()
                                    .runTask(
                                            plugin,
                                            () ->
                                                    finalizePlayerLoad(
                                                            minecraftUuid,
                                                            username
                                                    )
                                    );
                        }
                );
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
                        .find(
                                minecraftUuid
                        )
                        .orElse(
                                null
                        );

        playerSettingsService
                .flush(
                        minecraftUuid
                )
                .handle(
                        (ignored, flushThrowable) -> {
                            if (flushThrowable != null) {
                                plugin.getLogger()
                                        .warning(
                                                "Failed to flush settings for "
                                                        + username
                                                        + " ["
                                                        + minecraftUuid
                                                        + "]: "
                                                        + safeMessage(
                                                        unwrap(
                                                                flushThrowable
                                                        )
                                                )
                                        );
                            }

                            return null;
                        }
                )
                .thenCompose(
                        ignored -> {
                            playerPassService
                                    .unload(
                                            minecraftUuid
                                    );

                            playerTitleService
                                    .unload(
                                            minecraftUuid
                                    );

                            playerOnboardingService
                                    .unload(
                                            minecraftUuid
                                    );

                            return playerManager
                                    .unload(
                                            minecraftUuid
                                    );
                        }
                )
                .whenComplete(
                        (ignored, unloadThrowable) -> {
                            if (!plugin.isEnabled()) {
                                return;
                            }

                            plugin.getServer()
                                    .getScheduler()
                                    .runTask(
                                            plugin,
                                            () -> {
                                                if (unloadThrowable != null) {
                                                    plugin.getLogger()
                                                            .warning(
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

                                                plugin.getLogger()
                                                        .info(
                                                                "Unloaded TasticPlayer "
                                                                        + username
                                                                        + " ["
                                                                        + minecraftUuid
                                                                        + "]"
                                                        );
                                            }
                                    );
                        }
                );
    }

    private void finalizePlayerLoad(
            UUID minecraftUuid,
            String username
    ) {
        if (!plugin.isEnabled()) {
            return;
        }

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
                    playerManager
                            .markReady(
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

            // Der Title ist Schmuck: er wird nachgeladen und hält den Join nie auf.
            playerTitleService
                    .load(
                            minecraftUuid
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
        ).whenComplete(
                (ignored, cleanupThrowable) -> {
                    if (!plugin.isEnabled()) {
                        return;
                    }

                    plugin.getServer()
                            .getScheduler()
                            .runTask(
                                    plugin,
                                    () -> {
                                        if (cleanupThrowable != null) {
                                            plugin.getLogger()
                                                    .warning(
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
                            );
                }
        );
    }

    private CompletableFuture<Void> cleanupFailedLoad(
            UUID minecraftUuid
    ) {
        playerPassService
                .unload(
                        minecraftUuid
                );

        playerTitleService
                .unload(
                        minecraftUuid
                );

        playerOnboardingService
                .unload(
                        minecraftUuid
                );

        return playerManager
                .failLoad(
                        minecraftUuid
                );
    }

    private void discardDisconnectedPlayer(
            UUID minecraftUuid,
            String username
    ) {
        playerPassService
                .unload(
                        minecraftUuid
                );

        playerTitleService
                .unload(
                        minecraftUuid
                );

        playerOnboardingService
                .unload(
                        minecraftUuid
                );

        playerManager
                .unload(
                        minecraftUuid
                )
                .whenComplete(
                        (ignored, throwable) -> {
                            if (throwable != null) {
                                plugin.getLogger()
                                        .warning(
                                                "Failed to discard disconnected TasticPlayer "
                                                        + username
                                                        + " ["
                                                        + minecraftUuid
                                                        + "]: "
                                                        + safeMessage(
                                                        unwrap(
                                                                throwable
                                                        )
                                                )
                                        );

                                return;
                            }

                            plugin.getLogger()
                                    .info(
                                            "Discarded loaded TasticPlayer "
                                                    + username
                                                    + " ["
                                                    + minecraftUuid
                                                    + "] because the player is no longer online."
                                    );
                        }
                );
    }

    private boolean isPlayerOnline(
            UUID minecraftUuid
    ) {
        Player player =
                Bukkit.getPlayer(
                        minecraftUuid
                );

        return player != null
                && player.isOnline();
    }

    private void logLoadedPlayer(
            TasticPlayer player
    ) {
        plugin.getLogger()
                .info(
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
        if (!plugin.isEnabled()) {
            return;
        }

        Player onlinePlayer =
                Bukkit.getPlayer(
                        minecraftUuid
                );

        plugin.getLogger()
                .severe(
                        "Failed to load TasticPlayer "
                                + username
                                + " ["
                                + minecraftUuid
                                + "]: "
                                + safeMessage(
                                cause
                        )
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

    private static final class PlayerDisconnectedDuringLoadException
            extends RuntimeException {

        private PlayerDisconnectedDuringLoadException(
                UUID minecraftUuid
        ) {
            super(
                    "Player disconnected during runtime initialization: "
                            + minecraftUuid
            );
        }
    }

    private static final class PluginShutdownDuringLoadException
            extends RuntimeException {

        private PluginShutdownDuringLoadException() {
            super(
                    "TasticCore shutdown started during player runtime initialization."
            );
        }
    }
}
