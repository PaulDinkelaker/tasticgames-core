package de.tasticgames.player;

import de.tasticgames.service.Service;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface PlayerManager extends Service {

    CompletableFuture<TasticPlayer> load(
            Player player
    );

    CompletableFuture<Void> unload(
            UUID minecraftUuid
    );

    Optional<TasticPlayer> find(
            UUID minecraftUuid
    );

    TasticPlayer requireLoaded(
            UUID minecraftUuid
    );

    TasticPlayer require(
            UUID minecraftUuid
    );

    TasticPlayer markReady(
            UUID minecraftUuid
    );

    CompletableFuture<Void> failLoad(
            UUID minecraftUuid
    );

    boolean isLoaded(
            UUID minecraftUuid
    );

    Collection<TasticPlayer> onlinePlayers();
}
