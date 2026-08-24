package de.tasticgames.onboarding.internal;

import de.tasticgames.TasticCorePlugin;
import de.tasticgames.api.ApiClient;
import de.tasticgames.api.OnboardingSnapshot;
import de.tasticgames.localization.SupportedLanguage;
import de.tasticgames.onboarding.PlayerOnboarding;
import de.tasticgames.onboarding.PlayerOnboardingService;
import de.tasticgames.player.PlayerLanguageChange;
import de.tasticgames.player.PlayerManager;
import de.tasticgames.player.TasticPlayer;
import de.tasticgames.player.event.TasticPlayerLanguageChangedEvent;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class DefaultPlayerOnboardingService
        implements PlayerOnboardingService {

    private final TasticCorePlugin plugin;
    private final ApiClient apiClient;
    private final PlayerManager playerManager;

    private final ConcurrentMap<UUID, PlayerOnboarding> onboardingStates =
            new ConcurrentHashMap<>();

    private final ConcurrentMap<UUID, CompletableFuture<PlayerOnboarding>>
            loadingOperations =
            new ConcurrentHashMap<>();

    public DefaultPlayerOnboardingService(
            TasticCorePlugin plugin,
            ApiClient apiClient,
            PlayerManager playerManager
    ) {
        this.plugin = Objects.requireNonNull(
                plugin,
                "plugin"
        );

        this.apiClient = Objects.requireNonNull(
                apiClient,
                "apiClient"
        );

        this.playerManager = Objects.requireNonNull(
                playerManager,
                "playerManager"
        );
    }

    @Override
    public String id() {
        return "player-onboarding-service";
    }

    @Override
    public void start() {
        if (!apiClient.enabled()) {
            throw new IllegalStateException(
                    "Cannot start PlayerOnboardingService because ApiClient is disabled."
            );
        }
    }

    @Override
    public void stop() {
        loadingOperations.clear();
        onboardingStates.clear();
    }

    @Override
    public CompletableFuture<PlayerOnboarding> load(
            UUID minecraftUuid
    ) {
        Objects.requireNonNull(
                minecraftUuid,
                "minecraftUuid"
        );

        TasticPlayer player =
                playerManager.requireLoaded(
                        minecraftUuid
                );

        PlayerOnboarding existingState =
                onboardingStates.get(
                        minecraftUuid
                );

        if (existingState != null) {
            return CompletableFuture.completedFuture(
                    existingState
            );
        }

        return loadingOperations.computeIfAbsent(
                minecraftUuid,
                ignored ->
                        startLoad(
                                player
                        )
        );
    }

    @Override
    public Optional<PlayerOnboarding> find(
            UUID minecraftUuid
    ) {
        Objects.requireNonNull(
                minecraftUuid,
                "minecraftUuid"
        );

        return Optional.ofNullable(
                onboardingStates.get(
                        minecraftUuid
                )
        );
    }

    @Override
    public PlayerOnboarding require(
            UUID minecraftUuid
    ) {
        Objects.requireNonNull(
                minecraftUuid,
                "minecraftUuid"
        );

        PlayerOnboarding onboarding =
                onboardingStates.get(
                        minecraftUuid
                );

        if (onboarding == null) {
            throw new IllegalStateException(
                    "Player onboarding state is not loaded: "
                            + minecraftUuid
            );
        }

        return onboarding;
    }

    @Override
    public CompletableFuture<PlayerOnboarding> selectLanguage(
            UUID minecraftUuid,
            String language
    ) {
        Objects.requireNonNull(
                minecraftUuid,
                "minecraftUuid"
        );

        SupportedLanguage supportedLanguage =
                SupportedLanguage.require(
                        language
                );

        require(
                minecraftUuid
        );

        return apiClient
                .selectOnboardingLanguage(
                        minecraftUuid,
                        supportedLanguage.code()
                )
                .thenCompose(snapshot ->
                        applyLanguageSelection(
                                minecraftUuid,
                                supportedLanguage,
                                snapshot
                        )
                );
    }

    @Override
    public CompletableFuture<PlayerOnboarding> complete(
            UUID minecraftUuid
    ) {
        Objects.requireNonNull(
                minecraftUuid,
                "minecraftUuid"
        );

        require(
                minecraftUuid
        );

        return apiClient
                .completeOnboarding(
                        minecraftUuid
                )
                .thenApply(snapshot ->
                        applySnapshot(
                                minecraftUuid,
                                snapshot
                        )
                );
    }

    @Override
    public void unload(
            UUID minecraftUuid
    ) {
        Objects.requireNonNull(
                minecraftUuid,
                "minecraftUuid"
        );

        onboardingStates.remove(
                minecraftUuid
        );

        loadingOperations.remove(
                minecraftUuid
        );
    }

    @Override
    public int loadedCount() {
        return onboardingStates.size();
    }

    private CompletableFuture<PlayerOnboarding> startLoad(
            TasticPlayer player
    ) {
        UUID minecraftUuid =
                player.minecraftUuid();

        CompletableFuture<PlayerOnboarding> operation =
                apiClient
                        .getOnboarding(
                                minecraftUuid
                        )
                        .thenApply(snapshot ->
                                createOrUpdateState(
                                        player,
                                        snapshot
                                )
                        );

        operation.whenComplete(
                (onboarding, throwable) ->
                        loadingOperations.remove(
                                minecraftUuid,
                                operation
                        )
        );

        return operation;
    }

    private CompletableFuture<PlayerOnboarding> applyLanguageSelection(
            UUID minecraftUuid,
            SupportedLanguage language,
            OnboardingSnapshot snapshot
    ) {
        Objects.requireNonNull(
                minecraftUuid,
                "minecraftUuid"
        );

        Objects.requireNonNull(
                language,
                "language"
        );

        Objects.requireNonNull(
                snapshot,
                "snapshot"
        );

        if (!plugin.isEnabled()) {
            return CompletableFuture.completedFuture(
                    createDetachedState(
                            snapshot
                    )
            );
        }

        CompletableFuture<PlayerOnboarding> result =
                new CompletableFuture<>();

        plugin.getServer()
                .getScheduler()
                .runTask(
                        plugin,
                        () -> {
                            try {
                                Optional<TasticPlayer> optionalPlayer =
                                        playerManager.find(
                                                minecraftUuid
                                        );

                                if (optionalPlayer.isEmpty()
                                        || !optionalPlayer.get().ready()) {
                                    result.complete(
                                            createDetachedState(
                                                    snapshot
                                            )
                                    );

                                    return;
                                }

                                PlayerOnboarding onboarding =
                                        applySnapshot(
                                                minecraftUuid,
                                                snapshot
                                        );

                                PlayerLanguageChange change =
                                        playerManager.updateLanguage(
                                                minecraftUuid,
                                                language
                                        );

                                if (change.changed()) {
                                    plugin.getServer()
                                            .getPluginManager()
                                            .callEvent(
                                                    new TasticPlayerLanguageChangedEvent(
                                                            change.player(),
                                                            change.previousLanguage(),
                                                            change.newLanguage()
                                                    )
                                            );
                                }

                                result.complete(
                                        onboarding
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

    private PlayerOnboarding createOrUpdateState(
            TasticPlayer player,
            OnboardingSnapshot snapshot
    ) {
        validateSnapshot(
                player,
                snapshot
        );

        return onboardingStates.compute(
                player.minecraftUuid(),
                (minecraftUuid, existingState) -> {
                    if (existingState == null) {
                        return new PlayerOnboarding(
                                snapshot.accountId(),
                                snapshot.minecraftUuid(),
                                snapshot.currentStep(),
                                snapshot.languageSelected(),
                                snapshot.completed(),
                                snapshot.languageSelectedAt(),
                                snapshot.completedAt()
                        );
                    }

                    existingState.apply(
                            snapshot.currentStep(),
                            snapshot.languageSelected(),
                            snapshot.completed(),
                            snapshot.languageSelectedAt(),
                            snapshot.completedAt()
                    );

                    return existingState;
                }
        );
    }

    private PlayerOnboarding applySnapshot(
            UUID minecraftUuid,
            OnboardingSnapshot snapshot
    ) {
        Objects.requireNonNull(
                minecraftUuid,
                "minecraftUuid"
        );

        Objects.requireNonNull(
                snapshot,
                "snapshot"
        );

        TasticPlayer player =
                playerManager.requireLoaded(
                        minecraftUuid
                );

        return createOrUpdateState(
                player,
                snapshot
        );
    }

    private PlayerOnboarding createDetachedState(
            OnboardingSnapshot snapshot
    ) {
        Objects.requireNonNull(
                snapshot,
                "snapshot"
        );

        return new PlayerOnboarding(
                snapshot.accountId(),
                snapshot.minecraftUuid(),
                snapshot.currentStep(),
                snapshot.languageSelected(),
                snapshot.completed(),
                snapshot.languageSelectedAt(),
                snapshot.completedAt()
        );
    }

    private void validateSnapshot(
            TasticPlayer player,
            OnboardingSnapshot snapshot
    ) {
        Objects.requireNonNull(
                player,
                "player"
        );

        Objects.requireNonNull(
                snapshot,
                "snapshot"
        );

        if (!player.minecraftUuid()
                .equals(
                        snapshot.minecraftUuid()
                )) {
            throw new IllegalStateException(
                    "Onboarding snapshot UUID does not match player UUID. "
                            + "Expected "
                            + player.minecraftUuid()
                            + " but received "
                            + snapshot.minecraftUuid()
            );
        }

        if (player.accountId()
                != snapshot.accountId()) {
            throw new IllegalStateException(
                    "Onboarding snapshot account ID does not match player account. "
                            + "Expected "
                            + player.accountId()
                            + " but received "
                            + snapshot.accountId()
            );
        }
    }
}
