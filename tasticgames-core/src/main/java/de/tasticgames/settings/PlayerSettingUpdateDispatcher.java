package de.tasticgames.settings;

import de.tasticgames.player.TasticPlayer;

import java.util.concurrent.CompletableFuture;

public interface PlayerSettingUpdateDispatcher {

    <T> CompletableFuture<PlayerSettingChange<T>> update(
            TasticPlayer player,
            SettingKey<T> key,
            T value
    );
}
