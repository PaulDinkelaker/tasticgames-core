package de.tasticgames.player.internal;

import de.tasticgames.account.AccountProfile;
import de.tasticgames.account.AccountService;
import de.tasticgames.api.MinecraftAccount;
import de.tasticgames.player.PlayerManager;
import de.tasticgames.player.PlayerRuntimeContext;
import de.tasticgames.player.PlayerState;
import de.tasticgames.player.TasticPlayer;
import de.tasticgames.settings.SettingRegistry;
import de.tasticgames.settings.internal.DefaultPlayerSettings;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;

public final class DefaultPlayerManager
        implements PlayerManager {

    private final AccountService accountService;
    private final SettingRegistry settingRegistry;
    private final String serverName;

    private final ConcurrentMap<UUID, PlayerSession> sessions =
            new ConcurrentHashMap<>();

    private final ConcurrentMap<UUID, CompletableFuture<TasticPlayer>>
            loadingOperations =
            new ConcurrentHashMap<>();

    public DefaultPlayerManager(
            AccountService accountService,
            SettingRegistry settingRegistry,
            String serverName
    ) {
        this.accountService = Objects.requireNonNull(
                accountService,
                "accountService"
        );

        this.settingRegistry = Objects.requireNonNull(
                settingRegistry,
                "settingRegistry"
        );

        this.serverName = normalizeServerName(
                serverName
        );
    }

    @Override
    public CompletableFuture<TasticPlayer> load(
            Player player
    ) {
        Objects.requireNonNull(
                player,
                "player"
        );

        UUID minecraftUuid =
                player.getUniqueId();

        String username =
                player.getName();

        PlayerSession existingSession =
                sessions.get(
                        minecraftUuid
                );

        if (existingSession != null) {
            if (existingSession.state() == PlayerState.FAILED
                    || existingSession.state() == PlayerState.UNLOADING) {
                return CompletableFuture.failedFuture(
                        new IllegalStateException(
                                "Cannot load TasticPlayer runtime from state "
                                        + existingSession.state()
                                        + ": "
                                        + minecraftUuid
                        )
                );
            }

            return CompletableFuture.completedFuture(
                    existingSession
            );
        }

        return loadingOperations.computeIfAbsent(
                minecraftUuid,
                ignored ->
                        startLoad(
                                minecraftUuid,
                                username
                        )
        );
    }

    @Override
    public CompletableFuture<Void> unload(
            UUID minecraftUuid
    ) {
        Objects.requireNonNull(
                minecraftUuid,
                "minecraftUuid"
        );

        CompletableFuture<TasticPlayer> loadingOperation =
                loadingOperations.get(
                        minecraftUuid
                );

        if (loadingOperation != null) {
            return loadingOperation
                    .handle(
                            (player, throwable) ->
                                    null
                    )
                    .thenRun(
                            () ->
                                    unloadImmediately(
                                            minecraftUuid
                                    )
                    );
        }

        unloadImmediately(
                minecraftUuid
        );

        return CompletableFuture.completedFuture(
                null
        );
    }

    @Override
    public Optional<TasticPlayer> find(
            UUID minecraftUuid
    ) {
        Objects.requireNonNull(
                minecraftUuid,
                "minecraftUuid"
        );

        return Optional.ofNullable(
                sessions.get(
                        minecraftUuid
                )
        );
    }

    @Override
    public TasticPlayer requireLoaded(
            UUID minecraftUuid
    ) {
        Objects.requireNonNull(
                minecraftUuid,
                "minecraftUuid"
        );

        PlayerSession session =
                sessions.get(
                        minecraftUuid
                );

        if (session == null) {
            throw new IllegalStateException(
                    "TasticPlayer runtime is not loaded: "
                            + minecraftUuid
            );
        }

        PlayerState state =
                session.state();

        if (state == PlayerState.FAILED) {
            throw new IllegalStateException(
                    "TasticPlayer runtime initialization failed: "
                            + minecraftUuid
            );
        }

        if (state == PlayerState.UNLOADING) {
            throw new IllegalStateException(
                    "TasticPlayer runtime is unloading: "
                            + minecraftUuid
            );
        }

        return session;
    }

    @Override
    public TasticPlayer require(
            UUID minecraftUuid
    ) {
        Objects.requireNonNull(
                minecraftUuid,
                "minecraftUuid"
        );

        PlayerSession session =
                sessions.get(
                        minecraftUuid
                );

        if (session == null) {
            throw new IllegalStateException(
                    "TasticPlayer is not loaded: "
                            + minecraftUuid
            );
        }

        if (!session.ready()) {
            throw new IllegalStateException(
                    "TasticPlayer is not ready: "
                            + minecraftUuid
                            + " [state="
                            + session.state()
                            + "]"
            );
        }

        return session;
    }

    @Override
    public TasticPlayer markReady(
            UUID minecraftUuid
    ) {
        Objects.requireNonNull(
                minecraftUuid,
                "minecraftUuid"
        );

        PlayerSession session =
                sessions.get(
                        minecraftUuid
                );

        if (session == null) {
            throw new IllegalStateException(
                    "Cannot mark missing TasticPlayer runtime as ready: "
                            + minecraftUuid
            );
        }

        synchronized (session) {
            PlayerState state =
                    session.state();

            if (state == PlayerState.READY) {
                return session;
            }

            if (state != PlayerState.LOADING) {
                throw new IllegalStateException(
                        "Cannot mark TasticPlayer runtime as ready from state "
                                + state
                                + ": "
                                + minecraftUuid
                );
            }

            session.state(
                    PlayerState.READY
            );
        }

        return session;
    }

    @Override
    public CompletableFuture<Void> failLoad(
            UUID minecraftUuid
    ) {
        Objects.requireNonNull(
                minecraftUuid,
                "minecraftUuid"
        );

        PlayerSession session =
                sessions.get(
                        minecraftUuid
                );

        if (session != null) {
            synchronized (session) {
                if (session.state() != PlayerState.UNLOADING) {
                    session.state(
                            PlayerState.FAILED
                    );
                }
            }

            sessions.remove(
                    minecraftUuid,
                    session
            );
        }

        loadingOperations.remove(
                minecraftUuid
        );

        return CompletableFuture.completedFuture(
                null
        );
    }

    @Override
    public boolean isLoaded(
            UUID minecraftUuid
    ) {
        Objects.requireNonNull(
                minecraftUuid,
                "minecraftUuid"
        );

        PlayerSession session =
                sessions.get(
                        minecraftUuid
                );

        return session != null
                && session.ready();
    }

    @Override
    public Collection<TasticPlayer> onlinePlayers() {
        return List.copyOf(
                sessions.values()
        );
    }

    @Override
    public String id() {
        return "player-manager";
    }

    @Override
    public void start() {
    }

    @Override
    public void stop() {
        clear();
    }

    public void clear() {
        sessions.values()
                .forEach(session -> {
                    synchronized (session) {
                        if (session.state()
                                != PlayerState.UNLOADING) {
                            session.state(
                                    PlayerState.UNLOADING
                            );
                        }
                    }
                });

        sessions.clear();
        loadingOperations.clear();
    }

    public int loadedPlayerCount() {
        return sessions.size();
    }

    public int loadingPlayerCount() {
        return loadingOperations.size();
    }

    private CompletableFuture<TasticPlayer> startLoad(
            UUID minecraftUuid,
            String username
    ) {
        CompletableFuture<TasticPlayer> operation =
                accountService
                        .registerLogin(
                                minecraftUuid,
                                username
                        )
                        .thenApply(
                                this::createSession
                        )
                        .thenApply(session -> {
                            PlayerSession previous =
                                    sessions.putIfAbsent(
                                            minecraftUuid,
                                            session
                                    );

                            if (previous != null) {
                                if (previous.state()
                                        == PlayerState.FAILED
                                        || previous.state()
                                        == PlayerState.UNLOADING) {
                                    throw new IllegalStateException(
                                            "Existing TasticPlayer runtime is not usable: "
                                                    + minecraftUuid
                                                    + " [state="
                                                    + previous.state()
                                                    + "]"
                                    );
                                }

                                return previous;
                            }

                            return session;
                        });

        operation.whenComplete(
                (player, throwable) ->
                        loadingOperations.remove(
                                minecraftUuid,
                                operation
                        )
        );

        return operation.exceptionallyCompose(
                throwable ->
                        CompletableFuture.failedFuture(
                                unwrap(
                                        throwable
                                )
                        )
        );
    }

    private PlayerSession createSession(
            MinecraftAccount account
    ) {
        Objects.requireNonNull(
                account,
                "account"
        );

        AccountProfile accountProfile =
                new AccountProfile(
                        account.accountId(),
                        account.minecraftUuid(),
                        account.currentName(),
                        account.language(),
                        account.status(),
                        account.firstSeenAt(),
                        account.lastSeenAt(),
                        account.createdAt(),
                        account.updatedAt()
                );

        DefaultPlayerSettings settings =
                new DefaultPlayerSettings(
                        settingRegistry
                );

        PlayerRuntimeContext runtimeContext =
                PlayerRuntimeContext.create(
                        serverName
                );

        return new PlayerSession(
                accountProfile,
                settings,
                runtimeContext
        );
    }

    private void unloadImmediately(
            UUID minecraftUuid
    ) {
        PlayerSession session =
                sessions.remove(
                        minecraftUuid
                );

        if (session == null) {
            loadingOperations.remove(
                    minecraftUuid
            );

            return;
        }

        synchronized (session) {
            if (session.state()
                    != PlayerState.UNLOADING) {
                session.state(
                        PlayerState.UNLOADING
                );
            }
        }

        loadingOperations.remove(
                minecraftUuid
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

    private String normalizeServerName(
            String serverName
    ) {
        Objects.requireNonNull(
                serverName,
                "serverName"
        );

        String normalized =
                serverName
                        .trim()
                        .toLowerCase(
                                Locale.ROOT
                        );

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
