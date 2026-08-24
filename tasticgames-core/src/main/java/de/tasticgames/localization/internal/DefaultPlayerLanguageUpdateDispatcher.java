package de.tasticgames.localization.internal;

import de.tasticgames.localization.PlayerLanguageUpdateDispatcher;
import de.tasticgames.localization.SupportedLanguage;
import de.tasticgames.onboarding.PlayerOnboarding;
import de.tasticgames.onboarding.PlayerOnboardingService;
import de.tasticgames.player.TasticPlayer;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public final class DefaultPlayerLanguageUpdateDispatcher
        implements PlayerLanguageUpdateDispatcher {

    private final PlayerOnboardingService playerOnboardingService;

    public DefaultPlayerLanguageUpdateDispatcher(
            PlayerOnboardingService playerOnboardingService
    ) {
        this.playerOnboardingService = Objects.requireNonNull(
                playerOnboardingService,
                "playerOnboardingService"
        );
    }

    @Override
    public CompletableFuture<PlayerOnboarding> update(
            TasticPlayer player,
            SupportedLanguage language
    ) {
        Objects.requireNonNull(
                player,
                "player"
        );

        Objects.requireNonNull(
                language,
                "language"
        );

        if (!player.ready()) {
            return CompletableFuture.failedFuture(
                    new IllegalStateException(
                            "Cannot update language of a player that is not ready: "
                                    + player.minecraftUuid()
                                    + " [state="
                                    + player.state()
                                    + "]"
                    )
            );
        }

        return playerOnboardingService
                .selectLanguage(
                        player.minecraftUuid(),
                        language.code()
                );
    }

    @Override
    public CompletableFuture<PlayerOnboarding> update(
            TasticPlayer player,
            String language
    ) {
        Objects.requireNonNull(
                language,
                "language"
        );

        return update(
                player,
                SupportedLanguage.require(
                        language
                )
        );
    }
}
