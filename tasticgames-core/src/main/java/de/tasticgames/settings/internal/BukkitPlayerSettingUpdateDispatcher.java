package de.tasticgames.settings.internal;

import de.tasticgames.player.TasticPlayer;
import de.tasticgames.player.event.TasticPlayerSettingChangedEvent;
import de.tasticgames.settings.PlayerSettingChange;
import de.tasticgames.settings.PlayerSettingUpdateDispatcher;
import de.tasticgames.settings.PlayerSettingsService;
import de.tasticgames.settings.SettingKey;
import org.bukkit.plugin.Plugin;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public final class BukkitPlayerSettingUpdateDispatcher
        implements PlayerSettingUpdateDispatcher {

    private final Plugin plugin;
    private final PlayerSettingsService playerSettingsService;

    public BukkitPlayerSettingUpdateDispatcher(
            Plugin plugin,
            PlayerSettingsService playerSettingsService
    ) {
        this.plugin = Objects.requireNonNull(
                plugin,
                "plugin"
        );

        this.playerSettingsService = Objects.requireNonNull(
                playerSettingsService,
                "playerSettingsService"
        );
    }

    @Override
    public <T> CompletableFuture<PlayerSettingChange<T>> update(
            TasticPlayer player,
            SettingKey<T> key,
            T value
    ) {
        Objects.requireNonNull(
                player,
                "player"
        );

        Objects.requireNonNull(
                key,
                "key"
        );

        return playerSettingsService
                .update(
                        player,
                        key,
                        value
                )
                .thenCompose(
                        this::publishChange
                );
    }

    private <T> CompletableFuture<PlayerSettingChange<T>> publishChange(
            PlayerSettingChange<T> change
    ) {
        Objects.requireNonNull(
                change,
                "change"
        );

        if (!change.changed()) {
            return CompletableFuture.completedFuture(
                    change
            );
        }

        if (!plugin.isEnabled()) {
            return CompletableFuture.failedFuture(
                    new IllegalStateException(
                            "Cannot publish setting change because plugin is disabled."
                    )
            );
        }

        if (plugin.getServer().isPrimaryThread()) {
            callEvent(
                    change
            );

            return CompletableFuture.completedFuture(
                    change
            );
        }

        CompletableFuture<PlayerSettingChange<T>> result =
                new CompletableFuture<>();

        plugin.getServer()
                .getScheduler()
                .runTask(
                        plugin,
                        () -> {
                            try {
                                callEvent(
                                        change
                                );

                                result.complete(
                                        change
                                );
                            } catch (Throwable throwable) {
                                result.completeExceptionally(
                                        throwable
                                );
                            }
                        }
                );

        return result;
    }

    private void callEvent(
            PlayerSettingChange<?> change
    ) {
        plugin.getServer()
                .getPluginManager()
                .callEvent(
                        new TasticPlayerSettingChangedEvent(
                                change
                        )
                );
    }
}
