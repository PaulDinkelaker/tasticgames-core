package de.tasticgames.api.internal;

import de.tasticgames.account.AccountService;
import de.tasticgames.api.ApiClient;
import de.tasticgames.api.TasticCoreApi;
import de.tasticgames.localization.LocalizationService;
import de.tasticgames.localization.PlayerLanguageUpdateDispatcher;
import de.tasticgames.onboarding.PlayerOnboardingService;
import de.tasticgames.pass.PlayerPassService;
import de.tasticgames.player.PlayerManager;
import de.tasticgames.settings.PlayerSettingUpdateDispatcher;
import de.tasticgames.settings.PlayerSettingsService;
import de.tasticgames.settings.SettingRegistry;
import de.tasticgames.title.PlayerTitleService;

import java.util.Objects;

public final class DefaultTasticCoreApi
        implements TasticCoreApi {

    private final PlayerManager playerManager;
    private final PlayerSettingsService playerSettingsService;
    private final PlayerSettingUpdateDispatcher playerSettingUpdateDispatcher;
    private final PlayerOnboardingService playerOnboardingService;
    private final LocalizationService localizationService;
    private final PlayerLanguageUpdateDispatcher playerLanguageUpdateDispatcher;
    private final PlayerPassService playerPassService;
    private final PlayerTitleService playerTitleService;
    private final SettingRegistry settingRegistry;
    private final AccountService accountService;
    private final ApiClient apiClient;

    public DefaultTasticCoreApi(
            PlayerManager playerManager,
            PlayerSettingsService playerSettingsService,
            PlayerSettingUpdateDispatcher playerSettingUpdateDispatcher,
            PlayerOnboardingService playerOnboardingService,
            LocalizationService localizationService,
            PlayerLanguageUpdateDispatcher playerLanguageUpdateDispatcher,
            PlayerPassService playerPassService,
            PlayerTitleService playerTitleService,
            SettingRegistry settingRegistry,
            AccountService accountService,
            ApiClient apiClient
    ) {
        this.playerManager = Objects.requireNonNull(
                playerManager,
                "playerManager"
        );

        this.playerSettingsService = Objects.requireNonNull(
                playerSettingsService,
                "playerSettingsService"
        );

        this.playerSettingUpdateDispatcher = Objects.requireNonNull(
                playerSettingUpdateDispatcher,
                "playerSettingUpdateDispatcher"
        );

        this.playerOnboardingService = Objects.requireNonNull(
                playerOnboardingService,
                "playerOnboardingService"
        );

        this.localizationService = Objects.requireNonNull(
                localizationService,
                "localizationService"
        );

        this.playerLanguageUpdateDispatcher = Objects.requireNonNull(
                playerLanguageUpdateDispatcher,
                "playerLanguageUpdateDispatcher"
        );

        this.playerPassService = Objects.requireNonNull(
                playerPassService,
                "playerPassService"
        );

        this.playerTitleService = Objects.requireNonNull(
                playerTitleService,
                "playerTitleService"
        );

        this.settingRegistry = Objects.requireNonNull(
                settingRegistry,
                "settingRegistry"
        );

        this.accountService = Objects.requireNonNull(
                accountService,
                "accountService"
        );

        this.apiClient = Objects.requireNonNull(
                apiClient,
                "apiClient"
        );
    }

    @Override
    public PlayerManager playerManager() {
        return playerManager;
    }

    @Override
    public PlayerSettingsService playerSettingsService() {
        return playerSettingsService;
    }

    @Override
    public PlayerSettingUpdateDispatcher playerSettingUpdateDispatcher() {
        return playerSettingUpdateDispatcher;
    }

    @Override
    public PlayerOnboardingService playerOnboardingService() {
        return playerOnboardingService;
    }

    @Override
    public LocalizationService localizationService() {
        return localizationService;
    }

    @Override
    public PlayerLanguageUpdateDispatcher playerLanguageUpdateDispatcher() {
        return playerLanguageUpdateDispatcher;
    }

    @Override
    public PlayerPassService playerPassService() {
        return playerPassService;
    }

    @Override
    public PlayerTitleService playerTitleService() {
        return playerTitleService;
    }

    @Override
    public SettingRegistry settingRegistry() {
        return settingRegistry;
    }

    @Override
    public AccountService accountService() {
        return accountService;
    }

    @Override
    public ApiClient apiClient() {
        return apiClient;
    }
}
