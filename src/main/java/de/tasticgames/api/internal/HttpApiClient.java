package de.tasticgames.api.internal;

import de.tasticgames.api.ApiClient;
import de.tasticgames.api.ApiException;
import de.tasticgames.api.ApiHealth;
import de.tasticgames.api.MinecraftAccount;
import de.tasticgames.api.PlayerPresence;
import de.tasticgames.client.TasticApiClient;
import de.tasticgames.client.config.ApiClientConfiguration;
import de.tasticgames.client.dto.ApiHealthResponse;
import de.tasticgames.client.dto.MinecraftAccountResponse;
import de.tasticgames.client.dto.PlayerPresenceResponse;
import de.tasticgames.config.ConfigurationService;
import org.bukkit.configuration.file.FileConfiguration;
import de.tasticgames.api.PlayerSetting;
import de.tasticgames.api.PlayerSettingsSnapshot;
import de.tasticgames.api.PlayerSettingType;
import de.tasticgames.api.PlayerSettingUpdate;
import de.tasticgames.client.dto.PlayerSettingResponse;
import de.tasticgames.client.dto.PlayerSettingTypeResponse;
import de.tasticgames.client.dto.PlayerSettingsResponse;
import de.tasticgames.client.dto.PlayerSettingsUpdateRequest;
import de.tasticgames.api.OnboardingSnapshot;
import de.tasticgames.client.dto.OnboardingResponse;
import de.tasticgames.client.dto.OnboardingStepResponse;
import de.tasticgames.onboarding.OnboardingStep;

import java.util.List;

import java.net.URI;
import java.time.Duration;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicBoolean;

import static de.tasticgames.onboarding.OnboardingStep.COMPLETED;
import static de.tasticgames.onboarding.OnboardingStep.LANGUAGE_SELECTION;

/**
 * Paper-spezifischer Adapter zwischen TasticCore und dem gemeinsamen
 * tasticgames-api-client.
 *
 * <p>Die eigentliche HTTP-, JSON- und Fehlerlogik liegt vollständig im
 * {@link TasticApiClient}. Diese Klasse kümmert sich nur noch um:</p>
 *
 * <ul>
 *     <li>das Laden der Bukkit-YAML-Konfiguration,</li>
 *     <li>den Service-Lebenszyklus von TasticCore,</li>
 *     <li>das Mapping der gemeinsamen DTOs auf die Core-DTOs.</li>
 * </ul>
 */
public final class HttpApiClient implements ApiClient {

    private final ConfigurationService configurationService;
    private final AtomicBoolean running;

    private volatile boolean enabled;
    private TasticApiClient delegate;

    public HttpApiClient(
            ConfigurationService configurationService
    ) {
        this.configurationService = Objects.requireNonNull(
                configurationService,
                "configurationService"
        );

        this.running = new AtomicBoolean(false);
    }

    @Override
    public String id() {
        return "api-client";
    }

    @Override
    public void start() {
        if (!running.compareAndSet(false, true)) {
            throw new IllegalStateException(
                    "API client is already running."
            );
        }

        try {
            FileConfiguration configuration =
                    configurationService.require("api");

            enabled = configuration.getBoolean(
                    "enabled",
                    true
            );

            if (!enabled) {
                return;
            }

            String baseUrl = requireString(
                    configuration,
                    "base-url"
            );

            int connectTimeoutSeconds = requirePositiveInt(
                    configuration,
                    "timeouts.connect-seconds"
            );

            int requestTimeoutSeconds = requirePositiveInt(
                    configuration,
                    "timeouts.request-seconds"
            );

            String serviceName = requireString(
                    configuration,
                    "authentication.service-name"
            );

            String apiKey = requireString(
                    configuration,
                    "authentication.api-key"
            );

            ApiClientConfiguration clientConfiguration =
                    ApiClientConfiguration.builder()
                            .baseUri(normalizeBaseUri(baseUrl))
                            .connectTimeout(
                                    Duration.ofSeconds(
                                            connectTimeoutSeconds
                                    )
                            )
                            .requestTimeout(
                                    Duration.ofSeconds(
                                            requestTimeoutSeconds
                                    )
                            )
                            .serviceName(serviceName)
                            .apiKey(apiKey)
                            .build();

            delegate = new TasticApiClient(
                    clientConfiguration
            );

        } catch (Exception exception) {
            running.set(false);
            enabled = false;

            closeDelegate();

            throw new ApiException(
                    "Failed to start API client.",
                    exception
            );
        }
    }

    @Override
    public void stop() {
        if (!running.compareAndSet(true, false)) {
            return;
        }

        enabled = false;
        closeDelegate();
    }

    @Override
    public CompletableFuture<ApiHealth> health() {
        return execute(
                delegate().health()
                        .thenApply(this::mapHealth)
        );
    }

    @Override
    public CompletableFuture<OnboardingSnapshot> getOnboarding(
            UUID minecraftUuid
    ) {
        Objects.requireNonNull(
                minecraftUuid,
                "minecraftUuid"
        );

        return execute(
                delegate()
                        .getOnboarding(minecraftUuid)
                        .thenApply(this::mapOnboardingSnapshot)
        );
    }

    @Override
    public CompletableFuture<OnboardingSnapshot> selectOnboardingLanguage(
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

        return execute(
                delegate()
                        .selectOnboardingLanguage(
                                minecraftUuid,
                                language
                        )
                        .thenApply(this::mapOnboardingSnapshot)
        );
    }

    @Override
    public CompletableFuture<OnboardingSnapshot> completeOnboarding(
            UUID minecraftUuid
    ) {
        Objects.requireNonNull(
                minecraftUuid,
                "minecraftUuid"
        );

        return execute(
                delegate()
                        .completeOnboarding(
                                minecraftUuid
                        )
                        .thenApply(this::mapOnboardingSnapshot)
        );
    }

    @Override
    public CompletableFuture<PlayerSettingsSnapshot> getSettings(
            UUID minecraftUuid
    ) {
        Objects.requireNonNull(
                minecraftUuid,
                "minecraftUuid"
        );

        return execute(
                delegate()
                        .getSettings(minecraftUuid)
                        .thenApply(this::mapSettingsSnapshot)
        );
    }

    @Override
    public CompletableFuture<PlayerSettingsSnapshot> replaceSettings(
            UUID minecraftUuid,
            List<PlayerSettingUpdate> settings
    ) {
        Objects.requireNonNull(
                minecraftUuid,
                "minecraftUuid"
        );

        Objects.requireNonNull(
                settings,
                "settings"
        );

        List<de.tasticgames.client.dto.PlayerSettingUpdateRequest>
                requestSettings =
                settings.stream()
                        .map(this::mapSettingUpdateRequest)
                        .toList();

        PlayerSettingsUpdateRequest request =
                new PlayerSettingsUpdateRequest(
                        requestSettings
                );

        return execute(
                delegate()
                        .replaceSettings(
                                minecraftUuid,
                                request
                        )
                        .thenApply(this::mapSettingsSnapshot)
        );
    }

    @Override
    public CompletableFuture<MinecraftAccount> registerLogin(
            UUID minecraftUuid,
            String username
    ) {
        Objects.requireNonNull(
                minecraftUuid,
                "minecraftUuid"
        );

        Objects.requireNonNull(
                username,
                "username"
        );

        return execute(
                delegate()
                        .registerLogin(
                                minecraftUuid,
                                username
                        )
                        .thenApply(this::mapAccount)
        );
    }

    @Override
    public CompletableFuture<MinecraftAccount> getPlayer(
            UUID minecraftUuid
    ) {
        Objects.requireNonNull(
                minecraftUuid,
                "minecraftUuid"
        );

        return execute(
                delegate()
                        .getPlayer(minecraftUuid)
                        .thenApply(this::mapAccount)
        );
    }

    @Override
    public CompletableFuture<PlayerPresence> connect(
            UUID minecraftUuid,
            String serverName
    ) {
        Objects.requireNonNull(
                minecraftUuid,
                "minecraftUuid"
        );

        Objects.requireNonNull(
                serverName,
                "serverName"
        );

        return execute(
                delegate()
                        .connect(
                                minecraftUuid,
                                serverName
                        )
                        .thenApply(this::mapPresence)
        );
    }

    @Override
    public CompletableFuture<PlayerPresence> switchServer(
            UUID minecraftUuid,
            String serverName
    ) {
        Objects.requireNonNull(
                minecraftUuid,
                "minecraftUuid"
        );

        Objects.requireNonNull(
                serverName,
                "serverName"
        );

        return execute(
                delegate()
                        .switchServer(
                                minecraftUuid,
                                serverName
                        )
                        .thenApply(this::mapPresence)
        );
    }

    @Override
    public CompletableFuture<PlayerPresence> disconnect(
            UUID minecraftUuid
    ) {
        Objects.requireNonNull(
                minecraftUuid,
                "minecraftUuid"
        );

        return execute(
                delegate()
                        .disconnect(minecraftUuid)
                        .thenApply(this::mapPresence)
        );
    }

    @Override
    public CompletableFuture<PlayerPresence> getPresence(
            UUID minecraftUuid
    ) {
        Objects.requireNonNull(
                minecraftUuid,
                "minecraftUuid"
        );

        return execute(
                delegate()
                        .getPresence(minecraftUuid)
                        .thenApply(this::mapPresence)
        );
    }

    @Override
    public boolean enabled() {
        return enabled;
    }

    private ApiHealth mapHealth(
            ApiHealthResponse response
    ) {
        return new ApiHealth(
                response.status(),
                response.service(),
                response.version()
        );
    }

    private MinecraftAccount mapAccount(
            MinecraftAccountResponse response
    ) {
        return new MinecraftAccount(
                response.accountId(),
                response.minecraftUuid(),
                response.currentName(),
                response.language(),
                response.status(),
                response.firstSeenAt(),
                response.lastSeenAt(),
                response.createdAt(),
                response.updatedAt()
        );
    }

    private PlayerSettingsSnapshot mapSettingsSnapshot(
            PlayerSettingsResponse response
    ) {
        Objects.requireNonNull(
                response,
                "response"
        );

        List<PlayerSetting> settings =
                response.settings()
                        .stream()
                        .map(this::mapSetting)
                        .toList();

        return new PlayerSettingsSnapshot(
                response.accountId(),
                response.minecraftUuid(),
                settings
        );
    }

    private PlayerSetting mapSetting(
            PlayerSettingResponse response
    ) {
        Objects.requireNonNull(
                response,
                "response"
        );

        return new PlayerSetting(
                response.key(),
                response.value(),
                mapSettingType(response.type()),
                response.updatedAt()
        );
    }

    private de.tasticgames.client.dto.PlayerSettingUpdateRequest
    mapSettingUpdateRequest(
            PlayerSettingUpdate update
    ) {
        Objects.requireNonNull(
                update,
                "update"
        );

        return new de.tasticgames.client.dto.PlayerSettingUpdateRequest(
                update.key(),
                update.value(),
                mapSettingType(update.type())
        );
    }

    private PlayerSettingType mapSettingType(
            PlayerSettingTypeResponse type
    ) {
        Objects.requireNonNull(
                type,
                "type"
        );

        return switch (type) {
            case BOOLEAN -> PlayerSettingType.BOOLEAN;
            case INTEGER -> PlayerSettingType.INTEGER;
            case STRING -> PlayerSettingType.STRING;
        };
    }

    private PlayerSettingTypeResponse mapSettingType(
            PlayerSettingType type
    ) {
        Objects.requireNonNull(
                type,
                "type"
        );

        return switch (type) {
            case BOOLEAN -> PlayerSettingTypeResponse.BOOLEAN;
            case INTEGER -> PlayerSettingTypeResponse.INTEGER;
            case STRING -> PlayerSettingTypeResponse.STRING;
        };
    }

    private PlayerPresence mapPresence(
            PlayerPresenceResponse response
    ) {
        return new PlayerPresence(
                response.accountId(),
                response.minecraftUuid(),
                response.currentName(),
                response.online(),
                response.currentServer(),
                response.sessionStartedAt(),
                response.lastSeenAt()
        );
    }

    private <T> CompletableFuture<T> execute(
            CompletableFuture<T> future
    ) {
        return future.exceptionallyCompose(
                throwable -> CompletableFuture.failedFuture(
                        mapException(throwable)
                )
        );
    }

    private RuntimeException mapException(
            Throwable throwable
    ) {
        Throwable cause = unwrap(throwable);

        if (cause instanceof ApiException apiException) {
            return apiException;
        }

        String message = cause.getMessage();

        if (message == null || message.isBlank()) {
            message = cause.getClass().getSimpleName();
        }

        return new ApiException(
                "API request failed: " + message,
                cause
        );
    }

    private Throwable unwrap(
            Throwable throwable
    ) {
        Throwable current = throwable;

        while ((current instanceof CompletionException
                || current instanceof java.util.concurrent.ExecutionException)
                && current.getCause() != null) {
            current = current.getCause();
        }

        return current;
    }

    private OnboardingSnapshot mapOnboardingSnapshot(
            OnboardingResponse response
    ) {
        Objects.requireNonNull(
                response,
                "response"
        );

        return new OnboardingSnapshot(
                response.accountId(),
                response.minecraftUuid(),
                mapOnboardingStep(
                        response.currentStep()
                ),
                response.languageSelected(),
                response.completed(),
                response.languageSelectedAt(),
                response.completedAt()
        );
    }

    private OnboardingStep mapOnboardingStep(
            OnboardingStepResponse step
    ) {
        Objects.requireNonNull(
                step,
                "step"
        );

        return switch (step) {
            case LANGUAGE_SELECTION ->
                    LANGUAGE_SELECTION;

            case COMPLETED ->
                    COMPLETED;
        };
    }

    private TasticApiClient delegate() {
        if (!running.get()) {
            throw new IllegalStateException(
                    "API client is not running."
            );
        }

        if (!enabled) {
            throw new ApiException(
                    "API client is disabled."
            );
        }

        if (delegate == null) {
            throw new IllegalStateException(
                    "Shared API client is not initialized."
            );
        }

        if (delegate.isClosed()) {
            throw new IllegalStateException(
                    "Shared API client is already closed."
            );
        }

        return delegate;
    }

    private void closeDelegate() {
        if (delegate == null) {
            return;
        }

        try {
            delegate.close();
        } finally {
            delegate = null;
        }
    }

    private String requireString(
            FileConfiguration configuration,
            String path
    ) {
        String value = configuration.getString(path);

        if (value == null || value.isBlank()) {
            throw new ApiException(
                    "Missing or blank API configuration value: "
                            + path
            );
        }

        return value.trim();
    }

    private int requirePositiveInt(
            FileConfiguration configuration,
            String path
    ) {
        int value = configuration.getInt(path, -1);

        if (value <= 0) {
            throw new ApiException(
                    "API configuration value must be positive: "
                            + path
            );
        }

        return value;
    }

    private URI normalizeBaseUri(
            String baseUrl
    ) {
        String normalized = baseUrl.endsWith("/")
                ? baseUrl
                : baseUrl + "/";

        URI uri;

        try {
            uri = URI.create(normalized);
        } catch (IllegalArgumentException exception) {
            throw new ApiException(
                    "Invalid API base URL: " + baseUrl,
                    exception
            );
        }

        String scheme = uri.getScheme();

        if (scheme == null
                || (!scheme.equalsIgnoreCase("http")
                && !scheme.equalsIgnoreCase("https"))) {
            throw new ApiException(
                    "API base URL must use HTTP or HTTPS: "
                            + baseUrl
            );
        }

        if (uri.getHost() == null
                || uri.getHost().isBlank()) {
            throw new ApiException(
                    "API base URL has no valid host: "
                            + baseUrl
            );
        }

        return uri;
    }
}
