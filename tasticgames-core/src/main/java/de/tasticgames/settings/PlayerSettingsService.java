package de.tasticgames.settings;

import de.tasticgames.player.TasticPlayer;
import de.tasticgames.service.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface PlayerSettingsService
        extends Service {

    <T> T get(
            TasticPlayer player,
            SettingKey<T> key
    );

    <T> void set(
            TasticPlayer player,
            SettingKey<T> key,
            T value
    );

    <T> CompletableFuture<PlayerSettingChange<T>> update(
            TasticPlayer player,
            SettingKey<T> key,
            T value
    );

    void reset(
            TasticPlayer player,
            SettingKey<?> key
    );

    void resetAll(
            TasticPlayer player
    );

    Map<String, Object> snapshot(
            TasticPlayer player
    );

    CompletableFuture<Void> load(
            UUID minecraftUuid
    );

    CompletableFuture<Void> flush(
            UUID minecraftUuid
    );

    CompletableFuture<Void> flushAll();
}
