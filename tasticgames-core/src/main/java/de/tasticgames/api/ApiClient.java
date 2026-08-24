package de.tasticgames.api;

import de.tasticgames.pass.PassAchievementUnlock;
import de.tasticgames.pass.PassLeaderboardEntry;
import de.tasticgames.pass.PassQuestUpdate;
import de.tasticgames.pass.PassRewardGrantResult;
import de.tasticgames.pass.PassSeasonSnapshot;
import de.tasticgames.pass.PassSnapshot;
import de.tasticgames.pass.PassTrack;
import de.tasticgames.pass.PassXpResult;
import de.tasticgames.pass.PassXpSource;
import de.tasticgames.service.Service;
import de.tasticgames.title.NetworkTitle;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface ApiClient extends Service {

    CompletableFuture<ApiHealth> health();

    CompletableFuture<MinecraftAccount> registerLogin(
            UUID minecraftUuid,
            String username
    );

    CompletableFuture<PlayerSettingsSnapshot> getSettings(
            UUID minecraftUuid
    );

    CompletableFuture<OnboardingSnapshot> getOnboarding(
            UUID minecraftUuid
    );

    CompletableFuture<OnboardingSnapshot> selectOnboardingLanguage(
            UUID minecraftUuid,
            String language
    );

    CompletableFuture<OnboardingSnapshot> completeOnboarding(
            UUID minecraftUuid
    );

    CompletableFuture<PlayerSettingsSnapshot> replaceSettings(
            UUID minecraftUuid,
            List<PlayerSettingUpdate> settings
    );

    CompletableFuture<MinecraftAccount> getPlayer(
            UUID minecraftUuid
    );

    CompletableFuture<PlayerPresence> connect(
            UUID minecraftUuid,
            String serverName
    );

    CompletableFuture<PlayerPresence> switchServer(
            UUID minecraftUuid,
            String serverName
    );

    CompletableFuture<PlayerPresence> disconnect(
            UUID minecraftUuid
    );

    CompletableFuture<PlayerPresence> getPresence(
            UUID minecraftUuid
    );

    CompletableFuture<Optional<PassSeasonSnapshot>> getCurrentPassSeason();

    CompletableFuture<PassSnapshot> getPass(
            UUID minecraftUuid
    );

    CompletableFuture<PassXpResult> awardPassXp(
            UUID minecraftUuid,
            UUID operationId,
            PassXpSource source,
            long amount,
            String reason,
            String serverId
    );

    CompletableFuture<PassQuestUpdate> reportPassQuestProgress(
            UUID minecraftUuid,
            String questKey,
            UUID operationId,
            long amount,
            String serverId
    );

    CompletableFuture<PassAchievementUnlock> unlockPassAchievement(
            UUID minecraftUuid,
            UUID operationId,
            String achievementKey,
            String serverId
    );

    CompletableFuture<PassRewardGrantResult> claimPassReward(
            UUID minecraftUuid,
            UUID operationId,
            int level,
            PassTrack track
    );

    CompletableFuture<PassRewardGrantResult> claimAllPassRewards(
            UUID minecraftUuid,
            UUID operationId
    );

    CompletableFuture<List<PassLeaderboardEntry>> getPassLeaderboard(
            String seasonKey,
            int limit
    );

    /**
     * Der netzwerkweite Title eines Spielers; {@link NetworkTitle#none()},
     * wenn die API den Spieler nicht kennt.
     */
    CompletableFuture<NetworkTitle> getNetworkTitle(
            UUID minecraftUuid
    );

    boolean enabled();
}
