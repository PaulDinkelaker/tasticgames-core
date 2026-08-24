package de.tasticgames.pass.internal;

import de.tasticgames.TasticCorePlugin;
import de.tasticgames.api.ApiClient;
import de.tasticgames.pass.PassLeaderboardEntry;
import de.tasticgames.pass.PassQuestDefinition;
import de.tasticgames.pass.PassQuestSnapshot;
import de.tasticgames.pass.PassQuestUpdate;
import de.tasticgames.pass.PassRewardGrant;
import de.tasticgames.pass.PassRewardGrantResult;
import de.tasticgames.pass.PassSeasonSnapshot;
import de.tasticgames.pass.PassSnapshot;
import de.tasticgames.pass.PassTrack;
import de.tasticgames.pass.PassXpResult;
import de.tasticgames.pass.PassXpSource;
import de.tasticgames.pass.PlayerPassService;
import de.tasticgames.pass.event.TasticPassClaimEvent;
import de.tasticgames.pass.event.TasticPassLevelUpEvent;
import de.tasticgames.pass.event.TasticPassQuestCompletedEvent;
import de.tasticgames.player.PlayerManager;
import de.tasticgames.player.TasticPlayer;
import de.tasticgames.scheduler.ScheduledTask;
import de.tasticgames.scheduler.SchedulerService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Standardimplementierung des {@link PlayerPassService}.
 *
 * <p>Der Service spiegelt Aufbau, Threading und Lebenszyklus des
 * Onboarding-Service, bricht aber bewusst nie ab: ist die API
 * deaktiviert oder nicht erreichbar, protokolliert der Start eine Zeile
 * und alle Aufrufe verhalten sich als No-op.</p>
 *
 * <p>Gemeldete XP und Quest-Fortschritte werden je Spieler gebündelt.
 * Jede Übertragung erhält eine eigene {@code operationId} und wird bei
 * einem Fehlschlag mit derselben ID erneut versucht, solange der Spieler
 * geladen ist.</p>
 */
public final class DefaultPlayerPassService
        implements PlayerPassService {

    private static final long SEASON_REFRESH_TICKS = 20L * 60L * 5L;
    private static final long FLUSH_INTERVAL_TICKS = 20L * 20L;
    private static final long FLUSH_THRESHOLD = 250L;
    private static final int MAX_PENDING_OPERATIONS = 64;
    private static final int LEADERBOARD_MIN_LIMIT = 1;
    private static final int LEADERBOARD_MAX_LIMIT = 100;
    private static final long SHUTDOWN_FLUSH_SECONDS = 5L;
    /** Minimum quiet time before a new outage is logged again. */
    private static final long DEGRADED_QUIET_MILLIS = 5L * 60L * 1000L;
    /** Minimum quiet time before a recovery is announced. */
    private static final long RECOVERY_QUIET_MILLIS = 60L * 1000L;
    private static final long PLAYTIME_INTERVAL_TICKS = 20L * 60L;
    /** Netzwerkweite Spielzeit-Metrik (Quest {@code daily_playtime}); XP gibt es dafuer bewusst nicht. */
    private static final String PLAYTIME_METRIC = "network.playtime_minutes";

    private final TasticCorePlugin plugin;
    private final ApiClient apiClient;
    private final PlayerManager playerManager;
    private final SchedulerService schedulerService;
    private final String serverId;
    /** Season pass switch ({@code pass.enabled} in core.yml): off = the service never touches the API. */
    private final boolean enabled;

    private final ConcurrentMap<UUID, PassSnapshot> passStates =
            new ConcurrentHashMap<>();

    private final ConcurrentMap<UUID, CompletableFuture<PassSnapshot>>
            loadingOperations =
            new ConcurrentHashMap<>();

    private final ConcurrentMap<UUID, PlayerBatch> batches =
            new ConcurrentHashMap<>();

    private final AtomicReference<PassSeasonSnapshot> season =
            new AtomicReference<>();

    private final AtomicBoolean active =
            new AtomicBoolean(false);

    private final AtomicBoolean degraded =
            new AtomicBoolean(false);

    /** When the last failure was logged; keeps a flapping API from alternating two log lines forever. */
    private final AtomicLong lastDegradedAt =
            new AtomicLong(0L);

    private ScheduledTask seasonTask;
    private ScheduledTask flushTask;
    private ScheduledTask playtimeTask;

    public DefaultPlayerPassService(
            TasticCorePlugin plugin,
            ApiClient apiClient,
            PlayerManager playerManager,
            SchedulerService schedulerService,
            String serverId,
            boolean enabled
    ) {
        this.enabled = enabled;
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

        this.schedulerService = Objects.requireNonNull(
                schedulerService,
                "schedulerService"
        );

        this.serverId = Objects.requireNonNull(
                serverId,
                "serverId"
        );
    }

    @Override
    public String id() {
        return "player-pass-service";
    }

    @Override
    public void start() {
        if (!enabled) {
            plugin.getLogger().info(
                    "Season pass is disabled (pass.enabled = false in core.yml) - no pass data is sent."
            );

            return;
        }

        if (!apiClient.enabled()) {
            plugin.getLogger().info(
                    "Season pass is disabled because the API client is disabled."
            );

            return;
        }

        try {
            active.set(
                    true
            );

            seasonTask =
                    schedulerService.runAsyncRepeating(
                            this::refreshSeason,
                            SEASON_REFRESH_TICKS,
                            SEASON_REFRESH_TICKS
                    );

            flushTask =
                    schedulerService.runAsyncRepeating(
                            this::flushAll,
                            FLUSH_INTERVAL_TICKS,
                            FLUSH_INTERVAL_TICKS
                    );

            playtimeTask =
                    schedulerService.runAsyncRepeating(
                            this::reportPlaytime,
                            PLAYTIME_INTERVAL_TICKS,
                            PLAYTIME_INTERVAL_TICKS
                    );

            refreshSeason();
        } catch (Exception exception) {
            active.set(
                    false
            );

            plugin.getLogger().warning(
                    "Season pass stays disabled because it could not be started: "
                            + rootMessage(
                            exception
                    )
            );
        }
    }

    @Override
    public void stop() {
        active.set(
                false
        );

        seasonTask =
                cancel(
                        seasonTask
                );

        flushTask =
                cancel(
                        flushTask
                );

        playtimeTask =
                cancel(
                        playtimeTask
                );

        try {
            flushAll()
                    .get(
                            SHUTDOWN_FLUSH_SECONDS,
                            TimeUnit.SECONDS
                    );
        } catch (InterruptedException exception) {
            Thread.currentThread()
                    .interrupt();

            plugin.getLogger().warning(
                    "Interrupted while flushing pending pass updates during shutdown."
            );
        } catch (ExecutionException | TimeoutException exception) {
            plugin.getLogger().warning(
                    "Failed to flush pending pass updates during shutdown: "
                            + rootMessage(
                            exception
                    )
            );
        } finally {
            batches.clear();
            loadingOperations.clear();
            passStates.clear();

            season.set(
                    null
            );
        }
    }

    @Override
    public Optional<PassSeasonSnapshot> season() {
        return Optional.ofNullable(
                season.get()
        );
    }

    @Override
    public CompletableFuture<PassSnapshot> load(
            UUID minecraftUuid
    ) {
        Objects.requireNonNull(
                minecraftUuid,
                "minecraftUuid"
        );

        if (!active.get()) {
            return CompletableFuture.completedFuture(
                    PassSnapshot.inactive()
            );
        }

        PassSnapshot existingState =
                passStates.get(
                        minecraftUuid
                );

        if (existingState != null) {
            return CompletableFuture.completedFuture(
                    existingState
            );
        }

        return loadingOperations.computeIfAbsent(
                minecraftUuid,
                this::startLoad
        );
    }

    @Override
    public Optional<PassSnapshot> state(
            UUID minecraftUuid
    ) {
        Objects.requireNonNull(
                minecraftUuid,
                "minecraftUuid"
        );

        return Optional.ofNullable(
                passStates.get(
                        minecraftUuid
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

        passStates.remove(
                minecraftUuid
        );

        loadingOperations.remove(
                minecraftUuid
        );

        PlayerBatch batch =
                batches.remove(
                        minecraftUuid
                );

        if (batch == null) {
            return;
        }

        flushBatch(
                minecraftUuid,
                batch,
                true
        );
    }

    @Override
    public void awardXp(
            UUID minecraftUuid,
            PassXpSource source,
            long amount,
            String reason
    ) {
        Objects.requireNonNull(
                minecraftUuid,
                "minecraftUuid"
        );

        Objects.requireNonNull(
                source,
                "source"
        );

        if (!active.get()
                || amount <= 0L
                || !tracked(
                minecraftUuid
        )) {
            return;
        }

        PlayerBatch batch =
                batches.computeIfAbsent(
                        minecraftUuid,
                        ignored ->
                                new PlayerBatch()
                );

        long pending =
                batch.xp
                        .computeIfAbsent(
                                source,
                                ignored ->
                                        new PendingAmount()
                        )
                        .add(
                                amount,
                                reason
                        );

        if (pending > FLUSH_THRESHOLD) {
            flushBatch(
                    minecraftUuid,
                    batch,
                    false
            );
        }
    }

    @Override
    public void metric(
            UUID minecraftUuid,
            String metric,
            long amount
    ) {
        Objects.requireNonNull(
                minecraftUuid,
                "minecraftUuid"
        );

        Objects.requireNonNull(
                metric,
                "metric"
        );

        if (!active.get()
                || amount <= 0L
                || !tracked(
                minecraftUuid
        )) {
            return;
        }

        PassSeasonSnapshot currentSeason =
                season.get();

        if (currentSeason == null) {
            return;
        }

        PassSnapshot state =
                passStates.get(
                        minecraftUuid
                );

        boolean premium =
                state != null
                        && state.premium();

        boolean thresholdReached = false;
        PlayerBatch batch = null;

        for (PassQuestDefinition quest
                : currentSeason.quests()) {
            if (!quest.metric().equals(
                    metric
            )) {
                continue;
            }

            if (quest.premiumOnly()
                    && !premium) {
                continue;
            }

            if (completed(
                    state,
                    quest.questKey()
            )) {
                continue;
            }

            if (batch == null) {
                batch =
                        batches.computeIfAbsent(
                                minecraftUuid,
                                ignored ->
                                        new PlayerBatch()
                        );
            }

            long pending =
                    batch.quests
                            .computeIfAbsent(
                                    quest.questKey(),
                                    ignored ->
                                            new PendingAmount()
                            )
                            .add(
                                    amount,
                                    null
                            );

            if (pending > FLUSH_THRESHOLD) {
                thresholdReached = true;
            }
        }

        if (batch != null
                && thresholdReached) {
            flushBatch(
                    minecraftUuid,
                    batch,
                    false
            );
        }
    }

    @Override
    public CompletableFuture<Boolean> unlockAchievement(
            UUID minecraftUuid,
            String achievementKey
    ) {
        Objects.requireNonNull(
                minecraftUuid,
                "minecraftUuid"
        );

        Objects.requireNonNull(
                achievementKey,
                "achievementKey"
        );

        if (!active.get()) {
            return CompletableFuture.completedFuture(
                    Boolean.FALSE
            );
        }

        try {
            return apiClient
                    .unlockPassAchievement(
                            minecraftUuid,
                            UUID.randomUUID(),
                            achievementKey,
                            serverId
                    )
                    .handle((unlock, throwable) -> {
                        if (throwable != null
                                || unlock == null) {
                            logDegraded(
                                    "unlock the pass achievement '"
                                            + achievementKey
                                            + "'",
                                    throwable
                            );

                            return Boolean.FALSE;
                        }

                        logRecovered();

                        if (unlock.unlockedNow()) {
                            passStates.computeIfPresent(
                                    minecraftUuid,
                                    (ignored, currentState) ->
                                            currentState.withAchievement(
                                                    achievementKey
                                            )
                            );
                        }

                        handleXpResult(
                                minecraftUuid,
                                unlock.xp()
                        );

                        return unlock.unlockedNow();
                    });
        } catch (Exception exception) {
            logDegraded(
                    "unlock the pass achievement '"
                            + achievementKey
                            + "'",
                    exception
            );

            return CompletableFuture.completedFuture(
                    Boolean.FALSE
            );
        }
    }

    @Override
    public CompletableFuture<PassRewardGrantResult> claim(
            UUID minecraftUuid,
            int level,
            PassTrack track
    ) {
        Objects.requireNonNull(
                minecraftUuid,
                "minecraftUuid"
        );

        Objects.requireNonNull(
                track,
                "track"
        );

        if (!active.get()) {
            return CompletableFuture.completedFuture(
                    PassRewardGrantResult.unavailable()
            );
        }

        try {
            return apiClient
                    .claimPassReward(
                            minecraftUuid,
                            UUID.randomUUID(),
                            level,
                            track
                    )
                    .handle((result, throwable) ->
                            handleClaimResult(
                                    minecraftUuid,
                                    result,
                                    throwable
                            )
                    );
        } catch (Exception exception) {
            logDegraded(
                    "claim pass tier "
                            + level,
                    exception
            );

            return CompletableFuture.completedFuture(
                    PassRewardGrantResult.unavailable()
            );
        }
    }

    @Override
    public CompletableFuture<PassRewardGrantResult> claimAll(
            UUID minecraftUuid
    ) {
        Objects.requireNonNull(
                minecraftUuid,
                "minecraftUuid"
        );

        if (!active.get()) {
            return CompletableFuture.completedFuture(
                    PassRewardGrantResult.unavailable()
            );
        }

        try {
            return apiClient
                    .claimAllPassRewards(
                            minecraftUuid,
                            UUID.randomUUID()
                    )
                    .handle((result, throwable) ->
                            handleClaimResult(
                                    minecraftUuid,
                                    result,
                                    throwable
                            )
                    );
        } catch (Exception exception) {
            logDegraded(
                    "claim all pass tiers",
                    exception
            );

            return CompletableFuture.completedFuture(
                    PassRewardGrantResult.unavailable()
            );
        }
    }

    @Override
    public CompletableFuture<List<PassLeaderboardEntry>> leaderboard(
            int limit
    ) {
        if (!active.get()) {
            return CompletableFuture.completedFuture(
                    List.of()
            );
        }

        int normalizedLimit =
                Math.clamp(
                        limit,
                        LEADERBOARD_MIN_LIMIT,
                        LEADERBOARD_MAX_LIMIT
                );

        try {
            return apiClient
                    .getPassLeaderboard(
                            null,
                            normalizedLimit
                    )
                    .handle((entries, throwable) -> {
                        if (throwable != null) {
                            logDegraded(
                                    "load the pass leaderboard",
                                    throwable
                            );

                            return List.<PassLeaderboardEntry>of();
                        }

                        logRecovered();

                        return entries;
                    });
        } catch (Exception exception) {
            logDegraded(
                    "load the pass leaderboard",
                    exception
            );

            return CompletableFuture.completedFuture(
                    List.of()
            );
        }
    }

    @Override
    public boolean premium(
            UUID minecraftUuid
    ) {
        Objects.requireNonNull(
                minecraftUuid,
                "minecraftUuid"
        );

        PassSnapshot state =
                passStates.get(
                        minecraftUuid
                );

        return state != null
                && state.premium();
    }

    @Override
    public int level(
            UUID minecraftUuid
    ) {
        Objects.requireNonNull(
                minecraftUuid,
                "minecraftUuid"
        );

        PassSnapshot state =
                passStates.get(
                        minecraftUuid
                );

        return state == null
                ? 0
                : state.level();
    }

    @Override
    public boolean hasFeature(
            UUID minecraftUuid,
            String featureKey
    ) {
        Objects.requireNonNull(
                minecraftUuid,
                "minecraftUuid"
        );

        Objects.requireNonNull(
                featureKey,
                "featureKey"
        );

        PassSnapshot state =
                passStates.get(
                        minecraftUuid
                );

        return state != null
                && state.features().contains(
                featureKey
        );
    }

    @Override
    public int loadedCount() {
        return passStates.size();
    }

    private CompletableFuture<PassSnapshot> startLoad(
            UUID minecraftUuid
    ) {
        CompletableFuture<PassSnapshot> operation =
                requestPass(
                        minecraftUuid
                )
                        .handle((snapshot, throwable) -> {
                            if (throwable != null) {
                                logDegraded(
                                        "load the pass state of "
                                                + minecraftUuid,
                                        throwable
                                );

                                return PassSnapshot.inactive();
                            }

                            logRecovered();

                            passStates.put(
                                    minecraftUuid,
                                    snapshot
                            );

                            return snapshot;
                        });

        // Asynchron aufräumen: dieser Aufruf läuft innerhalb der
        // computeIfAbsent-Funktion von loadingOperations und darf die
        // Map deshalb nicht direkt verändern.
        operation.whenCompleteAsync(
                (snapshot, throwable) ->
                        loadingOperations.remove(
                                minecraftUuid,
                                operation
                        )
        );

        return operation;
    }

    private CompletableFuture<PassSnapshot> requestPass(
            UUID minecraftUuid
    ) {
        try {
            return apiClient.getPass(
                    minecraftUuid
            );
        } catch (Exception exception) {
            return CompletableFuture.failedFuture(
                    exception
            );
        }
    }

    private void refreshSeason() {
        try {
            apiClient
                    .getCurrentPassSeason()
                    .whenComplete((currentSeason, throwable) -> {
                        if (throwable != null) {
                            logDegraded(
                                    "refresh the active pass season",
                                    throwable
                            );

                            return;
                        }

                        logRecovered();

                        season.set(
                                currentSeason.orElse(
                                        null
                                )
                        );
                    });
        } catch (Exception exception) {
            logDegraded(
                    "refresh the active pass season",
                    exception
            );
        }
    }

    private CompletableFuture<Void> flushAll() {
        List<CompletableFuture<Void>> operations =
                new ArrayList<>();

        for (Map.Entry<UUID, PlayerBatch> entry
                : batches.entrySet()) {
            UUID minecraftUuid =
                    entry.getKey();

            PlayerBatch batch =
                    entry.getValue();

            operations.add(
                    flushBatch(
                            minecraftUuid,
                            batch,
                            false
                    )
            );

            if (!tracked(
                    minecraftUuid
            )
                    && batch.idle()) {
                batches.remove(
                        minecraftUuid,
                        batch
                );
            }
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

    private CompletableFuture<Void> flushBatch(
            UUID minecraftUuid,
            PlayerBatch batch,
            boolean finalFlush
    ) {
        for (Map.Entry<PassXpSource, PendingAmount> entry
                : batch.xp.entrySet()) {
            long amount =
                    entry.getValue()
                            .drain();

            if (amount <= 0L) {
                continue;
            }

            batch.xpOperations.add(
                    new XpOperation(
                            UUID.randomUUID(),
                            entry.getKey(),
                            amount,
                            entry.getValue()
                                    .reason()
                    )
            );
        }

        for (Map.Entry<String, PendingAmount> entry
                : batch.quests.entrySet()) {
            long amount =
                    entry.getValue()
                            .drain();

            if (amount <= 0L) {
                continue;
            }

            batch.questOperations.add(
                    new QuestOperation(
                            UUID.randomUUID(),
                            entry.getKey(),
                            amount
                    )
            );
        }

        List<CompletableFuture<Void>> operations =
                new ArrayList<>();

        XpOperation xpOperation;

        while ((xpOperation = batch.xpOperations.poll()) != null) {
            operations.add(
                    sendXp(
                            minecraftUuid,
                            batch,
                            xpOperation,
                            finalFlush
                    )
            );
        }

        QuestOperation questOperation;

        while ((questOperation = batch.questOperations.poll()) != null) {
            operations.add(
                    sendQuestProgress(
                            minecraftUuid,
                            batch,
                            questOperation,
                            finalFlush
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

    private CompletableFuture<Void> sendXp(
            UUID minecraftUuid,
            PlayerBatch batch,
            XpOperation operation,
            boolean finalFlush
    ) {
        try {
            return apiClient
                    .awardPassXp(
                            minecraftUuid,
                            operation.operationId(),
                            operation.source(),
                            operation.amount(),
                            operation.reason(),
                            serverId
                    )
                    .thenAccept(result -> {
                        logRecovered();

                        handleXpResult(
                                minecraftUuid,
                                result
                        );
                    })
                    .exceptionally(throwable -> {
                        retry(
                                minecraftUuid,
                                batch.xpOperations,
                                operation,
                                finalFlush,
                                throwable
                        );

                        return null;
                    });
        } catch (Exception exception) {
            retry(
                    minecraftUuid,
                    batch.xpOperations,
                    operation,
                    finalFlush,
                    exception
            );

            return CompletableFuture.completedFuture(
                    null
            );
        }
    }

    private CompletableFuture<Void> sendQuestProgress(
            UUID minecraftUuid,
            PlayerBatch batch,
            QuestOperation operation,
            boolean finalFlush
    ) {
        try {
            return apiClient
                    .reportPassQuestProgress(
                            minecraftUuid,
                            operation.questKey(),
                            operation.operationId(),
                            operation.amount(),
                            serverId
                    )
                    .thenAccept(update -> {
                        logRecovered();

                        handleQuestUpdate(
                                minecraftUuid,
                                update
                        );
                    })
                    .exceptionally(throwable -> {
                        retry(
                                minecraftUuid,
                                batch.questOperations,
                                operation,
                                finalFlush,
                                throwable
                        );

                        return null;
                    });
        } catch (Exception exception) {
            retry(
                    minecraftUuid,
                    batch.questOperations,
                    operation,
                    finalFlush,
                    exception
            );

            return CompletableFuture.completedFuture(
                    null
            );
        }
    }

    /**
     * Legt eine fehlgeschlagene Übertragung mit unveränderter
     * {@code operationId} zurück in die Warteschlange, damit der
     * nächste Durchlauf sie gefahrlos wiederholt.
     *
     * <p>Nach dem Abmelden des Spielers gibt es keinen weiteren
     * Durchlauf mehr; die Meldung wird dann verworfen und
     * protokolliert.</p>
     */
    private <T> void retry(
            UUID minecraftUuid,
            Queue<T> operations,
            T operation,
            boolean finalFlush,
            Throwable throwable
    ) {
        if (!finalFlush
                && batches.containsKey(
                minecraftUuid
        )
                && operations.size() < MAX_PENDING_OPERATIONS) {
            operations.add(
                    operation
            );

            logDegraded(
                    "send a pass update for "
                            + minecraftUuid,
                    throwable
            );

            return;
        }

        plugin.getLogger().warning(
                "Dropped a pass update for "
                        + minecraftUuid
                        + " because it could not be sent: "
                        + rootMessage(
                        throwable
                )
        );
    }

    private void handleXpResult(
            UUID minecraftUuid,
            PassXpResult result
    ) {
        if (result == null
                || !result.applied()) {
            return;
        }

        passStates.computeIfPresent(
                minecraftUuid,
                (ignored, currentState) ->
                        currentState.withProgress(
                                result.levelAfter(),
                                result.totalXp(),
                                result.xpIntoLevel(),
                                result.xpForNextLevel()
                        )
        );

        if (!result.levelledUp()) {
            return;
        }

        runOnPrimaryThread(
                () -> {
                    TasticPlayer player =
                            readyPlayer(
                                    minecraftUuid
                            );

                    if (player == null) {
                        return;
                    }

                    plugin.getServer()
                            .getPluginManager()
                            .callEvent(
                                    new TasticPassLevelUpEvent(
                                            player,
                                            result.levelBefore(),
                                            result.levelAfter(),
                                            result.unlockedTiers()
                                    )
                            );
                }
        );
    }

    private void handleQuestUpdate(
            UUID minecraftUuid,
            PassQuestUpdate update
    ) {
        if (update == null) {
            return;
        }

        PassQuestSnapshot quest =
                update.quest();

        if (quest != null) {
            passStates.computeIfPresent(
                    minecraftUuid,
                    (ignored, currentState) ->
                            currentState.withQuest(
                                    quest
                            )
            );
        }

        handleXpResult(
                minecraftUuid,
                update.xp()
        );

        if (!update.completedNow()
                || quest == null) {
            return;
        }

        int xpAwarded =
                update.xp() == null
                        ? quest.xpReward()
                        : (int) Math.min(
                        Integer.MAX_VALUE,
                        update.xp().appliedAmount()
                );

        runOnPrimaryThread(
                () -> {
                    TasticPlayer player =
                            readyPlayer(
                                    minecraftUuid
                            );

                    if (player == null) {
                        return;
                    }

                    plugin.getServer()
                            .getPluginManager()
                            .callEvent(
                                    new TasticPassQuestCompletedEvent(
                                            player,
                                            quest,
                                            xpAwarded
                                    )
                            );
                }
        );
    }

    private PassRewardGrantResult handleClaimResult(
            UUID minecraftUuid,
            PassRewardGrantResult result,
            Throwable throwable
    ) {
        if (throwable != null
                || result == null) {
            logDegraded(
                    "claim pass rewards for "
                            + minecraftUuid,
                    throwable
            );

            return PassRewardGrantResult.unavailable();
        }

        logRecovered();

        PassSnapshot updatedState =
                result.state();

        if (updatedState != null) {
            passStates.computeIfPresent(
                    minecraftUuid,
                    (ignored, currentState) ->
                            updatedState
            );
        }

        if (result.granted().isEmpty()) {
            return result;
        }

        runOnPrimaryThread(
                () -> {
                    TasticPlayer player =
                            readyPlayer(
                                    minecraftUuid
                            );

                    if (player == null) {
                        return;
                    }

                    for (PassRewardGrant grant
                            : result.granted()) {
                        plugin.getServer()
                                .getPluginManager()
                                .callEvent(
                                        new TasticPassClaimEvent(
                                                player,
                                                grant
                                        )
                                );
                    }
                }
        );

        return result;
    }

    private void runOnPrimaryThread(
            Runnable task
    ) {
        if (!plugin.isEnabled()) {
            return;
        }

        try {
            plugin.getServer()
                    .getScheduler()
                    .runTask(
                            plugin,
                            task
                    );
        } catch (Exception exception) {
            plugin.getLogger().warning(
                    "Failed to dispatch a pass event to the primary thread: "
                            + rootMessage(
                            exception
                    )
            );
        }
    }

    private TasticPlayer readyPlayer(
            UUID minecraftUuid
    ) {
        return playerManager
                .find(
                        minecraftUuid
                )
                .filter(
                        TasticPlayer::ready
                )
                .orElse(
                        null
                );
    }

    /**
     * Meldet jede Minute fuer jeden geladenen Spieler eine Minute Spielzeit als Metrik. Damit
     * funktionieren netzwerkweite Spielzeit-Quests auf jedem Spielserver, ohne dass ein Spielmodus
     * etwas dafuer tun muss. Bewusst nur eine Metrik und keine XP-Quelle: sonst waere reines
     * Herumstehen eine Fortschrittsquelle.
     */
    private void reportPlaytime() {
        if (!active.get()) {
            return;
        }

        for (UUID minecraftUuid : passStates.keySet()) {
            metric(
                    minecraftUuid,
                    PLAYTIME_METRIC,
                    1L
            );
        }
    }

    private boolean tracked(
            UUID minecraftUuid
    ) {
        return passStates.containsKey(
                minecraftUuid
        )
                || loadingOperations.containsKey(
                minecraftUuid
        );
    }

    private boolean completed(
            PassSnapshot state,
            String questKey
    ) {
        if (state == null) {
            return false;
        }

        for (PassQuestSnapshot quest
                : state.quests()) {
            if (quest.questKey().equals(
                    questKey
            )) {
                return quest.completed();
            }
        }

        return false;
    }

    private ScheduledTask cancel(
            ScheduledTask task
    ) {
        if (task == null) {
            return null;
        }

        try {
            task.cancel();
        } catch (Exception exception) {
            plugin.getLogger().warning(
                    "Failed to cancel a pass background task: "
                            + rootMessage(
                            exception
                    )
            );
        }

        return null;
    }

    /**
     * Protokolliert den ersten Fehlschlag, bis die API wieder
     * antwortet, damit ein Ausfall die Konsole nicht flutet.
     */
    private void logDegraded(
            String action,
            Throwable throwable
    ) {
        long now = System.currentTimeMillis();
        if (!degraded.compareAndSet(
                false,
                true
        )) {
            lastDegradedAt.set(now);
            return;
        }

        long previous = lastDegradedAt.getAndSet(now);
        if (now - previous < DEGRADED_QUIET_MILLIS) {
            return; // still the same outage, just a short window where one call happened to succeed
        }

        plugin.getLogger().warning(
                "Season pass is degraded, failed to "
                        + action
                        + ": "
                        + rootMessage(
                        throwable
                )
                        + ". Further pass failures are suppressed until the API answers again."
        );
    }

    private void logRecovered() {
        if (!degraded.compareAndSet(
                true,
                false
        )) {
            return;
        }

        long now = System.currentTimeMillis();
        long last = lastDegradedAt.getAndSet(0L);
        if (now - last < RECOVERY_QUIET_MILLIS) {
            // a single successful call between two failures is not a recovery: stay quiet instead of
            // alternating "degraded"/"available again" lines every few seconds
            return;
        }

        plugin.getLogger().info(
                "Season pass is available again."
        );
    }

    private String rootMessage(
            Throwable throwable
    ) {
        if (throwable == null) {
            return "no response";
        }

        Throwable current =
                throwable;

        while ((current instanceof CompletionException
                || current instanceof ExecutionException)
                && current.getCause() != null) {
            current =
                    current.getCause();
        }

        while (current.getCause() != null) {
            current =
                    current.getCause();
        }

        String message =
                current.getMessage();

        if (message == null
                || message.isBlank()) {
            return current
                    .getClass()
                    .getSimpleName();
        }

        return message;
    }

    private record XpOperation(
            UUID operationId,
            PassXpSource source,
            long amount,
            String reason
    ) {
    }

    private record QuestOperation(
            UUID operationId,
            String questKey,
            long amount
    ) {
    }

    /**
     * Gebündelte, noch nicht gesendete Meldungen eines Spielers.
     */
    private static final class PlayerBatch {

        private final ConcurrentMap<PassXpSource, PendingAmount> xp =
                new ConcurrentHashMap<>();

        private final ConcurrentMap<String, PendingAmount> quests =
                new ConcurrentHashMap<>();

        private final Queue<XpOperation> xpOperations =
                new ConcurrentLinkedQueue<>();

        private final Queue<QuestOperation> questOperations =
                new ConcurrentLinkedQueue<>();

        private boolean idle() {
            if (!xpOperations.isEmpty()
                    || !questOperations.isEmpty()) {
                return false;
            }

            for (PendingAmount pending : xp.values()) {
                if (pending.pending() > 0L) {
                    return false;
                }
            }

            for (PendingAmount pending : quests.values()) {
                if (pending.pending() > 0L) {
                    return false;
                }
            }

            return true;
        }
    }

    /**
     * Zähler einer noch offenen Meldung.
     */
    private static final class PendingAmount {

        private final AtomicLong amount =
                new AtomicLong();

        private volatile String reason;

        private long add(
                long value,
                String reason
        ) {
            if (reason != null) {
                this.reason = reason;
            }

            return amount.addAndGet(
                    value
            );
        }

        private long drain() {
            return amount.getAndSet(
                    0L
            );
        }

        private long pending() {
            return amount.get();
        }

        private String reason() {
            return reason;
        }
    }
}
