package de.tasticgames.onboarding.internal;

import de.tasticgames.api.ApiClient;
import de.tasticgames.api.OnboardingSnapshot;
import de.tasticgames.onboarding.PlayerOnboarding;
import de.tasticgames.onboarding.PlayerOnboardingService;
import de.tasticgames.player.PlayerManager;
import de.tasticgames.player.TasticPlayer;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class DefaultPlayerOnboardingService
        implements PlayerOnboardingService {

    private final ApiClient apiClient;
    private final PlayerManager playerManager;

    private final ConcurrentMap<UUID, PlayerOnboarding> onboardingStates =
            new ConcurrentHashMap<>();

    private final ConcurrentMap<UUID, CompletableFuture<PlayerOnboarding>>
            loadingOperations =
            new ConcurrentHashMap<>();

    public DefaultPlayerOnboardingService(
            ApiClient apiClient,
            PlayerManager playerManager
    ) {
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
                ignored -> startLoad(
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

        Objects.requireNonNull(
                language,
                "language"
        );

        require(
                minecraftUuid
        );

        return apiClient
                .selectOnboardingLanguage(
                        minecraftUuid,
                        language
                )
                .thenApply(
                        snapshot ->
                                applySnapshot(
                                        minecraftUuid,
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
                .thenApply(
                        snapshot ->
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
                        .thenApply(
                                snapshot ->
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
        TasticPlayer player =
                playerManager.requireLoaded(
                        minecraftUuid
                );

        return createOrUpdateState(
                player,
                snapshot
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
                .equals(snapshot.minecraftUuid())) {
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
