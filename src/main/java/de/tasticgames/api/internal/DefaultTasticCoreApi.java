package de.tasticgames.api.internal;

import de.tasticgames.account.AccountService;
import de.tasticgames.api.ApiClient;
import de.tasticgames.api.TasticCoreApi;
import de.tasticgames.player.PlayerManager;
import de.tasticgames.settings.PlayerSettingsService;
import de.tasticgames.settings.SettingRegistry;
import de.tasticgames.onboarding.PlayerOnboardingService;

import java.util.Objects;

public final class DefaultTasticCoreApi
        implements TasticCoreApi {

    private final PlayerManager playerManager;
    private final PlayerSettingsService playerSettingsService;
    private final SettingRegistry settingRegistry;
    private final AccountService accountService;
    private final ApiClient apiClient;
    private final PlayerOnboardingService playerOnboardingService;

    public DefaultTasticCoreApi(
            PlayerManager playerManager,
            PlayerSettingsService playerSettingsService,
            PlayerOnboardingService playerOnboardingService,
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

        this.playerOnboardingService = Objects.requireNonNull(
                playerOnboardingService,
                "playerOnboardingService"
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
    public PlayerOnboardingService playerOnboardingService() {
        return playerOnboardingService;
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
