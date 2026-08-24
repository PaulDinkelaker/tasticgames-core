package de.tasticgames.settings.internal;

import de.tasticgames.api.ApiClient;
import de.tasticgames.api.PlayerSetting;
import de.tasticgames.api.PlayerSettingType;
import de.tasticgames.api.PlayerSettingUpdate;
import de.tasticgames.api.PlayerSettingsSnapshot;
import de.tasticgames.player.PlayerManager;
import de.tasticgames.player.PlayerState;
import de.tasticgames.player.TasticPlayer;
import de.tasticgames.settings.CoreSettings;
import de.tasticgames.settings.PlayerSettingChange;
import de.tasticgames.settings.PlayerSettings;
import de.tasticgames.settings.PlayerSettingsService;
import de.tasticgames.settings.PlayerSettingsState;
import de.tasticgames.settings.SettingKey;
import de.tasticgames.settings.SettingRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public final class DefaultPlayerSettingsService
        implements PlayerSettingsService {

    private final PlayerManager playerManager;
    private final SettingRegistry settingRegistry;
    private final ApiClient apiClient;

    private final ConcurrentMap<UUID, CompletableFuture<Void>>
            flushOperations =
            new ConcurrentHashMap<>();

    public DefaultPlayerSettingsService(
            PlayerManager playerManager,
            SettingRegistry settingRegistry,
            ApiClient apiClient
    ) {
        this.playerManager = Objects.requireNonNull(
                playerManager,
                "playerManager"
        );

        this.settingRegistry = Objects.requireNonNull(
                settingRegistry,
                "settingRegistry"
        );

        this.apiClient = Objects.requireNonNull(
                apiClient,
                "apiClient"
        );
    }

    @Override
    public String id() {
        return "player-settings-service";
    }

    @Override
    public void start() {
        if (settingRegistry.size() == 0) {
            throw new IllegalStateException(
                    "Cannot start PlayerSettingsService because no settings are registered."
            );
        }
    }

    @Override
    public void stop() {
        try {
            flushAll()
                    .join();
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Failed to flush player settings during shutdown.",
                    unwrap(
                            exception
                    )
            );
        } finally {
            flushOperations.clear();
        }
    }

    @Override
    public <T> T get(
            TasticPlayer player,
            SettingKey<T> key
    ) {
        Objects.requireNonNull(
                player,
                "player"
        );

        Objects.requireNonNull(
                key,
                "key"
        );

        requireReady(
                player
        );

        return player.settings()
                .get(
                        key
                );
    }

    @Override
    public <T> void set(
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

        requireReady(
                player
        );

        requireNormalSetting(
                key
        );

        player.settings()
                .set(
                        key,
                        value
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

        requireReady(
                player
        );

        if (key.id().equals(
                CoreSettings.LANGUAGE.id()
        )) {
            return CompletableFuture.failedFuture(
                    new IllegalArgumentException(
                            "Language must be updated through PlayerOnboardingService."
                    )
            );
        }

        PlayerSettings settings =
                player.settings();

        T previousValue =
                settings.get(
                        key
                );

        T validatedValue =
                key.validate(
                        value
                );

        PlayerSettingChange<T> change =
                new PlayerSettingChange<>(
                        player,
                        key,
                        previousValue,
                        validatedValue
                );

        if (!change.changed()) {
            return CompletableFuture.completedFuture(
                    change
            );
        }

        settings.set(
                key,
                validatedValue
        );

        return flushPlayer(
                player
        ).handle((ignored, throwable) -> {
            if (throwable == null) {
                return change;
            }

            settings.restoreIfCurrent(
                    key,
                    validatedValue,
                    previousValue
            );

            throw new CompletionException(
                    unwrap(
                            throwable
                    )
            );
        });
    }

    @Override
    public void reset(
            TasticPlayer player,
            SettingKey<?> key
    ) {
        Objects.requireNonNull(
                player,
                "player"
        );

        Objects.requireNonNull(
                key,
                "key"
        );

        requireReady(
                player
        );

        requireNormalSetting(
                key
        );

        player.settings()
                .reset(
                        key
                );
    }

    @Override
    public void resetAll(
            TasticPlayer player
    ) {
        Objects.requireNonNull(
                player,
                "player"
        );

        requireReady(
                player
        );

        String language =
                player.settings()
                        .get(
                                CoreSettings.LANGUAGE
                        );

        player.settings()
                .resetAll();

        player.settings()
                .setPersisted(
                        CoreSettings.LANGUAGE,
                        language
                );
    }

    @Override
    public Map<String, Object> snapshot(
            TasticPlayer player
    ) {
        Objects.requireNonNull(
                player,
                "player"
        );

        requireReady(
                player
        );

        return player.settings()
                .snapshot();
    }

    @Override
    public CompletableFuture<Void> load(
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

        return apiClient
                .getSettings(
                        minecraftUuid
                )
                .thenAccept(snapshot ->
                        applySnapshot(
                                player,
                                snapshot
                        )
                );
    }

    @Override
    public CompletableFuture<Void> flush(
            UUID minecraftUuid
    ) {
        Objects.requireNonNull(
                minecraftUuid,
                "minecraftUuid"
        );

        TasticPlayer player =
                playerManager
                        .find(
                                minecraftUuid
                        )
                        .orElse(
                                null
                        );

        if (player == null) {
            return CompletableFuture.completedFuture(
                    null
            );
        }

        return flushPlayer(
                player
        );
    }

    @Override
    public CompletableFuture<Void> flushAll() {
        List<CompletableFuture<Void>> operations =
                new ArrayList<>();

        for (TasticPlayer player
                : playerManager.onlinePlayers()) {
            operations.add(
                    flushPlayer(
                            player
                    )
            );
        }

        if (operations.isEmpty()) {
            return CompletableFuture.completedFuture(
                    null
            );
        }

        return CompletableFuture.allOf(
                operations.toArray(
                        CompletableFuture[]::new
                )
        );
    }

    private CompletableFuture<Void> flushPlayer(
            TasticPlayer player
    ) {
        Objects.requireNonNull(
                player,
                "player"
        );

        UUID minecraftUuid =
                player.minecraftUuid();

        CompletableFuture<Void> operation =
                flushOperations.compute(
                        minecraftUuid,
                        (ignored, previousOperation) -> {
                            CompletableFuture<Void> predecessor;

                            if (previousOperation == null) {
                                predecessor =
                                        CompletableFuture.completedFuture(
                                                null
                                        );
                            } else {
                                predecessor =
                                        previousOperation.handle(
                                                (ignoredResult, ignoredThrowable) ->
                                                        null
                                        );
                            }

                            return predecessor.thenCompose(
                                    ignoredResult ->
                                            flushPlayerNow(
                                                    player
                                            )
                            );
                        }
                );

        operation.whenComplete(
                (ignored, throwable) ->
                        flushOperations.remove(
                                minecraftUuid,
                                operation
                        )
        );

        return operation;
    }

    private CompletableFuture<Void> flushPlayerNow(
            TasticPlayer player
    ) {
        if (player.state() == PlayerState.FAILED) {
            return CompletableFuture.completedFuture(
                    null
            );
        }

        PlayerSettings settings =
                player.settings();

        if (!settings.dirty()) {
            return CompletableFuture.completedFuture(
                    null
            );
        }

        PlayerSettingsState state =
                settings.state();

        List<PlayerSettingUpdate> updates =
                createUpdates(
                        state.values()
                );

        return apiClient
                .replaceSettings(
                        player.minecraftUuid(),
                        updates
                )
                .thenAccept(snapshot -> {
                    validateSavedSnapshot(
                            player,
                            snapshot
                    );

                    settings.markClean(
                            state.revision()
                    );
                });
    }

    private void validateSavedSnapshot(
            TasticPlayer player,
            PlayerSettingsSnapshot snapshot
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
                    "Saved settings snapshot UUID does not match player UUID. "
                            + "Expected "
                            + player.minecraftUuid()
                            + " but received "
                            + snapshot.minecraftUuid()
            );
        }

        if (player.accountId()
                != snapshot.accountId()) {
            throw new IllegalStateException(
                    "Saved settings snapshot account ID does not match player account. "
                            + "Expected "
                            + player.accountId()
                            + " but received "
                            + snapshot.accountId()
            );
        }
    }

    private List<PlayerSettingUpdate> createUpdates(
            Map<String, Object> snapshot
    ) {
        Objects.requireNonNull(
                snapshot,
                "snapshot"
        );

        List<PlayerSettingUpdate> updates =
                new ArrayList<>(
                        snapshot.size()
                );

        for (Map.Entry<String, Object> entry
                : snapshot.entrySet()) {
            String settingId =
                    entry.getKey();

            if (settingId.equals(
                    CoreSettings.LANGUAGE.id()
            )) {
                continue;
            }

            SettingKey<?> key =
                    settingRegistry.require(
                            settingId
                    );

            Object validatedValue =
                    key.validate(
                            entry.getValue()
                    );

            updates.add(
                    new PlayerSettingUpdate(
                            key.id(),
                            validatedValue,
                            determineType(
                                    key
                            )
                    )
            );
        }

        return List.copyOf(
                updates
        );
    }

    private PlayerSettingType determineType(
            SettingKey<?> key
    ) {
        Objects.requireNonNull(
                key,
                "key"
        );

        if (key.type() == Boolean.class) {
            return PlayerSettingType.BOOLEAN;
        }

        if (key.type() == Integer.class) {
            return PlayerSettingType.INTEGER;
        }

        if (key.type() == String.class) {
            return PlayerSettingType.STRING;
        }

        throw new IllegalArgumentException(
                "Unsupported setting type for '"
                        + key.id()
                        + "': "
                        + key.type().getName()
        );
    }

    private void applySnapshot(
            TasticPlayer player,
            PlayerSettingsSnapshot snapshot
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
                    "Settings snapshot UUID does not match player UUID. "
                            + "Expected "
                            + player.minecraftUuid()
                            + " but received "
                            + snapshot.minecraftUuid()
            );
        }

        if (player.accountId()
                != snapshot.accountId()) {
            throw new IllegalStateException(
                    "Settings snapshot account ID does not match player account. "
                            + "Expected "
                            + player.accountId()
                            + " but received "
                            + snapshot.accountId()
            );
        }

        Map<String, Object> persistedValues =
                snapshot.settings()
                        .stream()
                        .collect(
                                Collectors.toUnmodifiableMap(
                                        PlayerSetting::key,
                                        PlayerSetting::value,
                                        (first, second) -> {
                                            throw new IllegalStateException(
                                                    "Duplicate setting returned by API."
                                            );
                                        }
                                )
                        );

        player.settings()
                .load(
                        persistedValues
                );

        player.settings()
                .setPersisted(
                        CoreSettings.LANGUAGE,
                        player.language()
                );

        player.settings()
                .markClean();
    }

    private void requireReady(
            TasticPlayer player
    ) {
        if (!player.ready()) {
            throw new IllegalStateException(
                    "TasticPlayer is not ready: "
                            + player.minecraftUuid()
                            + " [state="
                            + player.state()
                            + "]"
            );
        }
    }

    private void requireNormalSetting(
            SettingKey<?> key
    ) {
        if (key.id().equals(
                CoreSettings.LANGUAGE.id()
        )) {
            throw new IllegalArgumentException(
                    "Language must be updated through PlayerOnboardingService."
            );
        }
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
}
