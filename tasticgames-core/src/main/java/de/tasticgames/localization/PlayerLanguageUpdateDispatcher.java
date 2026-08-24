package de.tasticgames.localization;

import de.tasticgames.onboarding.PlayerOnboarding;
import de.tasticgames.player.TasticPlayer;

import java.util.concurrent.CompletableFuture;

public interface PlayerLanguageUpdateDispatcher {

    CompletableFuture<PlayerOnboarding> update(
            TasticPlayer player,
            SupportedLanguage language
    );

    CompletableFuture<PlayerOnboarding> update(
            TasticPlayer player,
            String language
    );
}
