package de.tasticgames.api;

import de.tasticgames.service.Service;

import java.util.List;
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

    boolean enabled();
}
