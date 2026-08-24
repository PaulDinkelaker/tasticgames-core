package de.tasticgames.api;

import de.tasticgames.account.AccountService;
import de.tasticgames.localization.LocalizationService;
import de.tasticgames.localization.PlayerLanguageUpdateDispatcher;
import de.tasticgames.onboarding.PlayerOnboardingService;
import de.tasticgames.pass.PlayerPassService;
import de.tasticgames.player.PlayerManager;
import de.tasticgames.settings.PlayerSettingUpdateDispatcher;
import de.tasticgames.settings.PlayerSettingsService;
import de.tasticgames.settings.SettingRegistry;
import de.tasticgames.title.PlayerTitleService;

public interface TasticCoreApi {

    PlayerManager playerManager();

    PlayerSettingsService playerSettingsService();

    PlayerSettingUpdateDispatcher playerSettingUpdateDispatcher();

    PlayerOnboardingService playerOnboardingService();

    LocalizationService localizationService();

    PlayerLanguageUpdateDispatcher playerLanguageUpdateDispatcher();

    PlayerPassService playerPassService();

    PlayerTitleService playerTitleService();

    SettingRegistry settingRegistry();

    AccountService accountService();

    ApiClient apiClient();
}
