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
import de.tasticgames.pass.PassAchievementUnlock;
import de.tasticgames.pass.PassClaimSnapshot;
import de.tasticgames.pass.PassGame;
import de.tasticgames.pass.PassLeaderboardEntry;
import de.tasticgames.pass.PassQuestDefinition;
import de.tasticgames.pass.PassQuestScope;
import de.tasticgames.pass.PassQuestSnapshot;
import de.tasticgames.pass.PassQuestUpdate;
import de.tasticgames.pass.PassRewardGrant;
import de.tasticgames.pass.PassRewardGrantResult;
import de.tasticgames.pass.PassRewardStatus;
import de.tasticgames.pass.PassRewardType;
import de.tasticgames.pass.PassSeasonSnapshot;
import de.tasticgames.pass.PassSnapshot;
import de.tasticgames.pass.PassTierSnapshot;
import de.tasticgames.pass.PassTrack;
import de.tasticgames.pass.PassXpResult;
import de.tasticgames.pass.PassXpSource;
import de.tasticgames.client.dto.pass.PassAchievementUnlockRequest;
import de.tasticgames.client.dto.pass.PassAchievementUnlockResponse;
import de.tasticgames.client.dto.pass.PassClaimResponse;
import de.tasticgames.client.dto.pass.PassGameResponse;
import de.tasticgames.client.dto.pass.PassLeaderboardEntryResponse;
import de.tasticgames.client.dto.pass.PassLeaderboardResponse;
import de.tasticgames.client.dto.pass.PassPlayerResponse;
import de.tasticgames.client.dto.pass.PassQuestProgressRequest;
import de.tasticgames.client.dto.pass.PassQuestProgressResponse;
import de.tasticgames.client.dto.pass.PassQuestResponse;
import de.tasticgames.client.dto.pass.PassQuestScopeResponse;
import de.tasticgames.client.dto.pass.PassQuestUpdateResponse;
import de.tasticgames.client.dto.pass.PassRewardClaimAllRequest;
import de.tasticgames.client.dto.pass.PassRewardClaimRequest;
import de.tasticgames.client.dto.pass.PassRewardClaimResponse;
import de.tasticgames.client.dto.pass.PassRewardGrantResponse;
import de.tasticgames.client.dto.pass.PassRewardStatusResponse;
import de.tasticgames.client.dto.pass.PassRewardTypeResponse;
import de.tasticgames.client.dto.pass.PassSeasonResponse;
import de.tasticgames.client.dto.pass.PassTierResponse;
import de.tasticgames.client.dto.pass.PassTrackResponse;
import de.tasticgames.client.dto.pass.PassXpRequest;
import de.tasticgames.client.dto.pass.PassXpResponse;
import de.tasticgames.client.dto.pass.PassXpSourceResponse;
import de.tasticgames.client.dto.network.NetworkTitleResponse;
import de.tasticgames.title.NetworkTitle;

import java.util.List;

import java.net.URI;
import java.time.Duration;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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
    public CompletableFuture<Optional<PassSeasonSnapshot>> getCurrentPassSeason() {
        return execute(
                delegate()
                        .pass()
                        .currentSeason()
                        .thenApply(response ->
                                response.map(
                                        this::mapPassSeason
                                )
                        )
        );
    }

    @Override
    public CompletableFuture<PassSnapshot> getPass(
            UUID minecraftUuid
    ) {
        Objects.requireNonNull(
                minecraftUuid,
                "minecraftUuid"
        );

        return execute(
                delegate()
                        .pass()
                        .player(minecraftUuid)
                        .thenApply(this::mapPassSnapshot)
        );
    }

    @Override
    public CompletableFuture<PassXpResult> awardPassXp(
            UUID minecraftUuid,
            UUID operationId,
            PassXpSource source,
            long amount,
            String reason,
            String serverId
    ) {
        Objects.requireNonNull(
                minecraftUuid,
                "minecraftUuid"
        );

        Objects.requireNonNull(
                operationId,
                "operationId"
        );

        Objects.requireNonNull(
                source,
                "source"
        );

        PassXpRequest request =
                new PassXpRequest(
                        operationId,
                        mapPassXpSource(source),
                        amount,
                        reason,
                        serverId
                );

        return execute(
                delegate()
                        .pass()
                        .awardXp(
                                minecraftUuid,
                                request
                        )
                        .thenApply(this::mapPassXpResult)
        );
    }

    @Override
    public CompletableFuture<PassQuestUpdate> reportPassQuestProgress(
            UUID minecraftUuid,
            String questKey,
            UUID operationId,
            long amount,
            String serverId
    ) {
        Objects.requireNonNull(
                minecraftUuid,
                "minecraftUuid"
        );

        Objects.requireNonNull(
                questKey,
                "questKey"
        );

        Objects.requireNonNull(
                operationId,
                "operationId"
        );

        PassQuestProgressRequest request =
                new PassQuestProgressRequest(
                        operationId,
                        amount,
                        serverId
                );

        return execute(
                delegate()
                        .pass()
                        .questProgress(
                                minecraftUuid,
                                questKey,
                                request
                        )
                        .thenApply(this::mapPassQuestUpdate)
        );
    }

    @Override
    public CompletableFuture<PassAchievementUnlock> unlockPassAchievement(
            UUID minecraftUuid,
            UUID operationId,
            String achievementKey,
            String serverId
    ) {
        Objects.requireNonNull(
                minecraftUuid,
                "minecraftUuid"
        );

        Objects.requireNonNull(
                operationId,
                "operationId"
        );

        Objects.requireNonNull(
                achievementKey,
                "achievementKey"
        );

        PassAchievementUnlockRequest request =
                new PassAchievementUnlockRequest(
                        operationId,
                        achievementKey,
                        serverId
                );

        return execute(
                delegate()
                        .pass()
                        .unlockAchievement(
                                minecraftUuid,
                                request
                        )
                        .thenApply(this::mapPassAchievementUnlock)
        );
    }

    @Override
    public CompletableFuture<PassRewardGrantResult> claimPassReward(
            UUID minecraftUuid,
            UUID operationId,
            int level,
            PassTrack track
    ) {
        Objects.requireNonNull(
                minecraftUuid,
                "minecraftUuid"
        );

        Objects.requireNonNull(
                operationId,
                "operationId"
        );

        Objects.requireNonNull(
                track,
                "track"
        );

        PassRewardClaimRequest request =
                new PassRewardClaimRequest(
                        operationId,
                        level,
                        mapPassTrack(track)
                );

        return execute(
                delegate()
                        .pass()
                        .claimReward(
                                minecraftUuid,
                                request
                        )
                        .thenApply(this::mapPassRewardGrantResult)
        );
    }

    @Override
    public CompletableFuture<PassRewardGrantResult> claimAllPassRewards(
            UUID minecraftUuid,
            UUID operationId
    ) {
        Objects.requireNonNull(
                minecraftUuid,
                "minecraftUuid"
        );

        Objects.requireNonNull(
                operationId,
                "operationId"
        );

        PassRewardClaimAllRequest request =
                new PassRewardClaimAllRequest(
                        operationId
                );

        return execute(
                delegate()
                        .pass()
                        .claimAllRewards(
                                minecraftUuid,
                                request
                        )
                        .thenApply(this::mapPassRewardGrantResult)
        );
    }

    @Override
    public CompletableFuture<List<PassLeaderboardEntry>> getPassLeaderboard(
            String seasonKey,
            int limit
    ) {
        return execute(
                delegate()
                        .pass()
                        .leaderboard(
                                seasonKey,
                                limit
                        )
                        .thenApply(this::mapPassLeaderboard)
        );
    }

    @Override
    public CompletableFuture<NetworkTitle> getNetworkTitle(
            UUID minecraftUuid
    ) {
        Objects.requireNonNull(
                minecraftUuid,
                "minecraftUuid"
        );

        return execute(
                delegate()
                        .network()
                        .playerTitle(
                                minecraftUuid
                        )
                        .thenApply(
                                response ->
                                        response.map(
                                                this::mapNetworkTitle
                                        ).orElseGet(
                                                NetworkTitle::none
                                        )
                        )
        );
    }

    @Override
    public boolean enabled() {
        return enabled;
    }

    private NetworkTitle mapNetworkTitle(
            NetworkTitleResponse response
    ) {
        Objects.requireNonNull(
                response,
                "response"
        );

        return new NetworkTitle(
                response.cosmeticId(),
                response.texts()
        );
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

    private PassSeasonSnapshot mapPassSeason(
            PassSeasonResponse response
    ) {
        Objects.requireNonNull(
                response,
                "response"
        );

        return new PassSeasonSnapshot(
                response.key(),
                response.displayName(),
                response.maxLevel(),
                response.xpBase(),
                response.xpGrowth(),
                response.premiumPriceCents(),
                response.currency(),
                response.startsAt(),
                response.endsAt(),
                response.tiers()
                        .stream()
                        .map(this::mapPassTier)
                        .toList(),
                response.quests()
                        .stream()
                        .map(this::mapPassQuestDefinition)
                        .toList(),
                mapPassDailyXpCaps(
                        response.dailyXpCaps()
                )
        );
    }

    private PassTierSnapshot mapPassTier(
            PassTierResponse response
    ) {
        Objects.requireNonNull(
                response,
                "response"
        );

        return new PassTierSnapshot(
                response.level(),
                mapPassTrack(
                        response.track()
                ),
                mapPassRewardType(
                        response.rewardType()
                ),
                response.rewardValue(),
                response.rewardAmount(),
                response.displayKey(),
                response.icon()
        );
    }

    private PassQuestDefinition mapPassQuestDefinition(
            PassQuestResponse response
    ) {
        Objects.requireNonNull(
                response,
                "response"
        );

        return new PassQuestDefinition(
                response.key(),
                mapPassQuestScope(
                        response.scope()
                ),
                mapPassGame(
                        response.game()
                ),
                response.metric(),
                response.target(),
                response.xpReward(),
                response.premiumOnly(),
                response.displayKey(),
                response.sortOrder()
        );
    }

    private Map<PassXpSource, Integer> mapPassDailyXpCaps(
            Map<String, Integer> caps
    ) {
        if (caps == null
                || caps.isEmpty()) {
            return Map.of();
        }

        Map<PassXpSource, Integer> mappedCaps =
                new EnumMap<>(
                        PassXpSource.class
                );

        for (Map.Entry<String, Integer> entry
                : caps.entrySet()) {
            PassXpSource source =
                    parsePassXpSource(
                            entry.getKey()
                    );

            if (source == null
                    || entry.getValue() == null) {
                continue;
            }

            mappedCaps.put(
                    source,
                    entry.getValue()
            );
        }

        return mappedCaps;
    }

    private PassXpSource parsePassXpSource(
            String name
    ) {
        if (name == null
                || name.isBlank()) {
            return null;
        }

        String normalized =
                name.trim();

        for (PassXpSource source
                : PassXpSource.values()) {
            if (source.name().equalsIgnoreCase(
                    normalized
            )) {
                return source;
            }
        }

        return null;
    }

    private PassSnapshot mapPassSnapshot(
            PassPlayerResponse response
    ) {
        Objects.requireNonNull(
                response,
                "response"
        );

        return new PassSnapshot(
                response.seasonActive(),
                response.seasonKey(),
                response.seasonDisplayName(),
                response.level(),
                response.maxLevel(),
                response.totalXp(),
                response.xpIntoLevel(),
                response.xpForNextLevel(),
                response.premium(),
                response.premiumPriceCents(),
                response.currency(),
                response.seasonEndsAt(),
                response.claims()
                        .stream()
                        .map(this::mapPassClaim)
                        .toList(),
                response.quests()
                        .stream()
                        .map(this::mapPassQuest)
                        .toList(),
                response.features(),
                response.achievements(),
                response.xpMultiplier(),
                response.xpMultiplierUntil(),
                response.version()
        );
    }

    private PassClaimSnapshot mapPassClaim(
            PassClaimResponse response
    ) {
        Objects.requireNonNull(
                response,
                "response"
        );

        return new PassClaimSnapshot(
                response.level(),
                mapPassTrack(
                        response.track()
                ),
                mapPassRewardType(
                        response.rewardType()
                ),
                response.rewardValue(),
                response.rewardAmount(),
                response.claimedAt()
        );
    }

    private PassQuestSnapshot mapPassQuest(
            PassQuestProgressResponse response
    ) {
        Objects.requireNonNull(
                response,
                "response"
        );

        return new PassQuestSnapshot(
                response.questKey(),
                mapPassQuestScope(
                        response.scope()
                ),
                mapPassGame(
                        response.game()
                ),
                response.metric(),
                response.target(),
                response.progress(),
                response.completed(),
                response.xpReward(),
                response.premiumOnly(),
                response.displayKey(),
                response.periodKey(),
                response.resetsAt(),
                response.sortOrder()
        );
    }

    private PassXpResult mapPassXpResult(
            PassXpResponse response
    ) {
        Objects.requireNonNull(
                response,
                "response"
        );

        return new PassXpResult(
                response.applied(),
                response.outcome(),
                response.duplicate(),
                response.requestedAmount(),
                response.appliedAmount(),
                response.dailyRemaining(),
                response.levelBefore(),
                response.levelAfter(),
                response.totalXp(),
                response.xpIntoLevel(),
                response.xpForNextLevel(),
                response.unlockedTiers()
                        .stream()
                        .map(this::mapPassTier)
                        .toList()
        );
    }

    private PassQuestUpdate mapPassQuestUpdate(
            PassQuestUpdateResponse response
    ) {
        Objects.requireNonNull(
                response,
                "response"
        );

        PassQuestProgressResponse quest =
                response.quest();

        PassXpResponse xp =
                response.xp();

        return new PassQuestUpdate(
                quest == null
                        ? null
                        : mapPassQuest(quest),
                response.completedNow(),
                xp == null
                        ? null
                        : mapPassXpResult(xp)
        );
    }

    private PassAchievementUnlock mapPassAchievementUnlock(
            PassAchievementUnlockResponse response
    ) {
        Objects.requireNonNull(
                response,
                "response"
        );

        PassXpResponse xp =
                response.xp();

        return new PassAchievementUnlock(
                response.unlockedNow(),
                response.achievementKey(),
                xp == null
                        ? null
                        : mapPassXpResult(xp)
        );
    }

    private PassRewardGrantResult mapPassRewardGrantResult(
            PassRewardClaimResponse response
    ) {
        Objects.requireNonNull(
                response,
                "response"
        );

        PassPlayerResponse state =
                response.state();

        return new PassRewardGrantResult(
                response.applied(),
                response.outcome(),
                response.duplicate(),
                response.granted()
                        .stream()
                        .map(this::mapPassRewardGrant)
                        .toList(),
                state == null
                        ? null
                        : mapPassSnapshot(state)
        );
    }

    private PassRewardGrant mapPassRewardGrant(
            PassRewardGrantResponse response
    ) {
        Objects.requireNonNull(
                response,
                "response"
        );

        return new PassRewardGrant(
                response.level(),
                mapPassTrack(
                        response.track()
                ),
                mapPassRewardType(
                        response.rewardType()
                ),
                response.rewardValue(),
                response.rewardAmount(),
                mapPassRewardStatus(
                        response.status()
                )
        );
    }

    private List<PassLeaderboardEntry> mapPassLeaderboard(
            PassLeaderboardResponse response
    ) {
        Objects.requireNonNull(
                response,
                "response"
        );

        return response.entries()
                .stream()
                .map(this::mapPassLeaderboardEntry)
                .toList();
    }

    private PassLeaderboardEntry mapPassLeaderboardEntry(
            PassLeaderboardEntryResponse response
    ) {
        Objects.requireNonNull(
                response,
                "response"
        );

        return new PassLeaderboardEntry(
                response.rank(),
                response.player(),
                response.name(),
                response.level(),
                response.totalXp(),
                response.premium()
        );
    }

    private PassTrack mapPassTrack(
            PassTrackResponse track
    ) {
        Objects.requireNonNull(
                track,
                "track"
        );

        return switch (track) {
            case FREE -> PassTrack.FREE;
            case PREMIUM -> PassTrack.PREMIUM;
        };
    }

    private PassTrackResponse mapPassTrack(
            PassTrack track
    ) {
        Objects.requireNonNull(
                track,
                "track"
        );

        return switch (track) {
            case FREE -> PassTrackResponse.FREE;
            case PREMIUM -> PassTrackResponse.PREMIUM;
        };
    }

    private PassRewardType mapPassRewardType(
            PassRewardTypeResponse rewardType
    ) {
        Objects.requireNonNull(
                rewardType,
                "rewardType"
        );

        return switch (rewardType) {
            case COSMETIC -> PassRewardType.COSMETIC;
            case CRUMBS -> PassRewardType.CRUMBS;
            case COOKIES -> PassRewardType.COOKIES;
            case XP_BOOST -> PassRewardType.XP_BOOST;
            case FEATURE -> PassRewardType.FEATURE;
        };
    }

    private PassQuestScope mapPassQuestScope(
            PassQuestScopeResponse scope
    ) {
        Objects.requireNonNull(
                scope,
                "scope"
        );

        return switch (scope) {
            case DAILY -> PassQuestScope.DAILY;
            case WEEKLY -> PassQuestScope.WEEKLY;
            case SEASON -> PassQuestScope.SEASON;
        };
    }

    private PassGame mapPassGame(
            PassGameResponse game
    ) {
        Objects.requireNonNull(
                game,
                "game"
        );

        return switch (game) {
            case COOKIE -> PassGame.COOKIE;
            case SURVIVAL -> PassGame.SURVIVAL;
            case DUELS -> PassGame.DUELS;
            case CREATIVE -> PassGame.CREATIVE;
            case NETWORK -> PassGame.NETWORK;
        };
    }

    private PassRewardStatus mapPassRewardStatus(
            PassRewardStatusResponse status
    ) {
        Objects.requireNonNull(
                status,
                "status"
        );

        return switch (status) {
            case GRANTED -> PassRewardStatus.GRANTED;
            case ALREADY_OWNED -> PassRewardStatus.ALREADY_OWNED;
            case DEFERRED -> PassRewardStatus.DEFERRED;
        };
    }

    private PassXpSourceResponse mapPassXpSource(
            PassXpSource source
    ) {
        Objects.requireNonNull(
                source,
                "source"
        );

        return switch (source) {
            case COOKIE_CLICKS -> PassXpSourceResponse.COOKIE_CLICKS;
            case COOKIE_PURCHASE -> PassXpSourceResponse.COOKIE_PURCHASE;
            case COOKIE_PRESTIGE -> PassXpSourceResponse.COOKIE_PRESTIGE;
            case COOKIE_GOLDEN -> PassXpSourceResponse.COOKIE_GOLDEN;
            case COOKIE_ZONE -> PassXpSourceResponse.COOKIE_ZONE;
            case QUEST -> PassXpSourceResponse.QUEST;
            case ACHIEVEMENT -> PassXpSourceResponse.ACHIEVEMENT;
            case PLAYTIME -> PassXpSourceResponse.PLAYTIME;
            case EVENT -> PassXpSourceResponse.EVENT;
            case ADMIN -> PassXpSourceResponse.ADMIN;
            case SURVIVAL -> PassXpSourceResponse.SURVIVAL;
            case DUELS -> PassXpSourceResponse.DUELS;
            case CREATIVE -> PassXpSourceResponse.CREATIVE;
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
