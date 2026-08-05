package de.tasticgames.api;

import de.tasticgames.account.AccountService;
import de.tasticgames.onboarding.PlayerOnboardingService;
import de.tasticgames.player.PlayerManager;
import de.tasticgames.settings.PlayerSettingsService;
import de.tasticgames.settings.SettingRegistry;

public interface TasticCoreApi {

    PlayerManager playerManager();

    PlayerSettingsService playerSettingsService();

    PlayerOnboardingService playerOnboardingService();

    SettingRegistry settingRegistry();

    AccountService accountService();

    ApiClient apiClient();
}
