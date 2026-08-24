package de.tasticgames.bootstrap;

import de.tasticgames.TasticCorePlugin;
import de.tasticgames.account.AccountService;
import de.tasticgames.account.internal.DefaultAccountService;
import de.tasticgames.api.ApiClient;
import de.tasticgames.api.TasticCoreApi;
import de.tasticgames.api.internal.DefaultTasticCoreApi;
import de.tasticgames.api.internal.HttpApiClient;
import de.tasticgames.command.TasticCoreCommand;
import de.tasticgames.config.ConfigurationService;
import de.tasticgames.config.internal.YamlConfigurationService;
import de.tasticgames.localization.LanguageRegistry;
import de.tasticgames.localization.LocalizationService;
import de.tasticgames.localization.PlayerLanguageUpdateDispatcher;
import de.tasticgames.localization.SupportedLanguage;
import de.tasticgames.localization.internal.DefaultLocalizationService;
import de.tasticgames.localization.internal.DefaultPlayerLanguageUpdateDispatcher;
import de.tasticgames.localization.internal.PropertiesTranslationLoader;
import de.tasticgames.module.ModuleManager;
import de.tasticgames.onboarding.PlayerOnboardingService;
import de.tasticgames.onboarding.internal.DefaultPlayerOnboardingService;
import de.tasticgames.pass.PlayerPassService;
import de.tasticgames.pass.internal.DefaultPlayerPassService;
import de.tasticgames.player.PlayerManager;
import de.tasticgames.player.internal.DefaultPlayerManager;
import de.tasticgames.player.listener.PlayerConnectionListener;
import de.tasticgames.chat.ProxyChatListener;
import de.tasticgames.tablist.TabListService;
import de.tasticgames.title.PlayerTitleService;
import de.tasticgames.title.internal.DefaultPlayerTitleService;
import de.tasticgames.title.internal.TitleNameTagRenderer;
import de.tasticgames.player.listener.PlayerRuntimeListener;
import de.tasticgames.scheduler.SchedulerService;
import de.tasticgames.scheduler.internal.BukkitSchedulerService;
import de.tasticgames.service.ServiceRegistry;
import de.tasticgames.settings.CoreSettings;
import de.tasticgames.settings.PlayerSettingUpdateDispatcher;
import de.tasticgames.settings.PlayerSettingsService;
import de.tasticgames.settings.SettingRegistry;
import de.tasticgames.settings.internal.BukkitPlayerSettingUpdateDispatcher;
import de.tasticgames.settings.internal.DefaultPlayerSettingsService;
import de.tasticgames.settings.internal.DefaultSettingRegistry;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.HandlerList;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

public final class CoreBootstrap {

    private final TasticCorePlugin plugin;
    private final AtomicBoolean running;
    private final ServiceRegistry serviceRegistry;
    private final ModuleManager moduleManager;

    private ConfigurationService configurationService;
    private SchedulerService schedulerService;
    private ApiClient apiClient;
    private AccountService accountService;
    private SettingRegistry settingRegistry;
    private PlayerManager playerManager;
    private PlayerSettingsService playerSettingsService;
    private PlayerSettingUpdateDispatcher playerSettingUpdateDispatcher;
    private PlayerOnboardingService playerOnboardingService;
    private LanguageRegistry languageRegistry;
    private LocalizationService localizationService;
    private PlayerLanguageUpdateDispatcher playerLanguageUpdateDispatcher;
    private PlayerPassService playerPassService;
    private PlayerTitleService playerTitleService;
    private TasticCoreApi coreApi;
    private PlayerRuntimeListener playerRuntimeListener;
    private PlayerConnectionListener playerConnectionListener;
    private TabListService tabListService;
    private ProxyChatListener proxyChatListener;
    private TasticCoreCommand tasticCoreCommand;

    public CoreBootstrap(
            TasticCorePlugin plugin
    ) {
        this.plugin = Objects.requireNonNull(
                plugin,
                "plugin"
        );

        this.running =
                new AtomicBoolean(
                        false
                );

        this.serviceRegistry =
                new ServiceRegistry();

        this.moduleManager =
                new ModuleManager();
    }

    public void start() throws Exception {
        if (!running.compareAndSet(
                false,
                true
        )) {
            throw new IllegalStateException(
                    "TasticCore bootstrap is already running."
            );
        }

        plugin.getLogger().info(
                "Starting TasticCore bootstrap..."
        );

        try {
            startConfigurationService();
            startSchedulerService();
            startApiClient();
            startAccountService();
            startSettingRegistry();
            startPlayerManager();
            startPlayerSettingsService();
            startPlayerSettingUpdateDispatcher();
            startPlayerOnboardingService();
            startLocalizationService();
            startPlayerLanguageUpdateDispatcher();
            startPlayerPassService();
            startPlayerTitleService();
            startCoreApi();
            startPlayerRuntimeListener();
            startCommands();
            startPlayerConnectionListener();
            startTabListService();
            startProxyChatListener();

            plugin.getLogger().info(
                    "TasticCore bootstrap started successfully."
            );
        } catch (Exception exception) {
            running.set(
                    false
            );

            try {
                stopInternal();
            } catch (Exception stopException) {
                exception.addSuppressed(
                        stopException
                );
            }

            throw exception;
        }
    }

    public void stop() throws Exception {
        if (!running.compareAndSet(
                true,
                false
        )) {
            return;
        }

        plugin.getLogger().info(
                "Stopping TasticCore bootstrap..."
        );

        stopInternal();

        plugin.getLogger().info(
                "TasticCore bootstrap stopped successfully."
        );
    }

    public boolean isRunning() {
        return running.get();
    }

    public ServiceRegistry serviceRegistry() {
        return serviceRegistry;
    }

    public ModuleManager moduleManager() {
        return moduleManager;
    }

    public TasticCoreApi coreApi() {
        TasticCoreApi current =
                coreApi;

        if (current == null) {
            throw new IllegalStateException(
                    "TasticCore API is not initialized."
            );
        }

        return current;
    }

    private void startConfigurationService()
            throws Exception {
        configurationService =
                new YamlConfigurationService(
                        plugin
                );

        configurationService.start();

        serviceRegistry.register(
                ConfigurationService.class,
                configurationService
        );

        plugin.getLogger().info(
                "Registered service: "
                        + configurationService.id()
        );
    }

    private void startSchedulerService()
            throws Exception {
        schedulerService =
                new BukkitSchedulerService(
                        plugin
                );

        schedulerService.start();

        serviceRegistry.register(
                SchedulerService.class,
                schedulerService
        );

        plugin.getLogger().info(
                "Registered service: "
                        + schedulerService.id()
        );
    }

    private void startApiClient()
            throws Exception {
        apiClient =
                new HttpApiClient(
                        configurationService
                );

        apiClient.start();

        serviceRegistry.register(
                ApiClient.class,
                apiClient
        );

        plugin.getLogger().info(
                "Registered service: "
                        + apiClient.id()
        );

        if (!apiClient.enabled()) {
            plugin.getLogger().warning(
                    "API client is disabled. Player connection tracking will not work."
            );

            return;
        }

        apiClient.health()
                .whenComplete(
                        (health, throwable) -> {
                            if (throwable != null) {
                                plugin.getLogger().warning(
                                        "API health check failed: "
                                                + rootMessage(
                                                throwable
                                        )
                                );

                                return;
                            }

                            plugin.getLogger().info(
                                    "API connection established: "
                                            + health.service()
                                            + " "
                                            + health.version()
                                            + " ["
                                            + health.status()
                                            + "]"
                            );
                        }
                );
    }

    private void startAccountService()
            throws Exception {
        if (apiClient == null) {
            throw new IllegalStateException(
                    "Cannot start AccountService because ApiClient is not initialized."
            );
        }

        accountService =
                new DefaultAccountService(
                        apiClient
                );

        accountService.start();

        serviceRegistry.register(
                AccountService.class,
                accountService
        );

        plugin.getLogger().info(
                "Registered service: "
                        + accountService.id()
        );
    }

    private void startSettingRegistry()
            throws Exception {
        settingRegistry =
                new DefaultSettingRegistry();

        settingRegistry.start();

        registerCoreSettings(
                settingRegistry
        );

        serviceRegistry.register(
                SettingRegistry.class,
                settingRegistry
        );

        plugin.getLogger().info(
                "Registered service: "
                        + settingRegistry.id()
                        + " ["
                        + settingRegistry.size()
                        + " settings]"
        );
    }

    private void registerCoreSettings(
            SettingRegistry registry
    ) {
        registry.register(
                CoreSettings.MUSIC_ENABLED
        );

        registry.register(
                CoreSettings.MUSIC_VOLUME
        );

        registry.register(
                CoreSettings.SOUNDS_ENABLED
        );

        registry.register(
                CoreSettings.SOUND_VOLUME
        );

        registry.register(
                CoreSettings.LANGUAGE
        );

        registry.register(
                CoreSettings.UI_ANIMATIONS
        );

        registry.register(
                CoreSettings.REDUCED_EFFECTS
        );

        registry.register(
                CoreSettings.EVENT_NOTIFICATIONS
        );

        registry.register(
                CoreSettings.PRIVATE_MESSAGES
        );

        registry.register(
                CoreSettings.FRIEND_REQUESTS
        );

        registry.register(
                CoreSettings.PARTY_INVITES
        );

        registry.register(
                CoreSettings.COSMETICS_VISIBLE
        );
    }

    private void startPlayerManager()
            throws Exception {
        if (settingRegistry == null) {
            throw new IllegalStateException(
                    "Cannot start PlayerManager because SettingRegistry is not initialized."
            );
        }

        if (accountService == null) {
            throw new IllegalStateException(
                    "Cannot start PlayerManager because AccountService is not initialized."
            );
        }

        String serverName =
                configurationService
                        .require(
                                "core"
                        )
                        .getString(
                                "server.name"
                        );

        if (serverName == null
                || serverName.isBlank()) {
            throw new IllegalStateException(
                    "Missing configuration value: server.name"
            );
        }

        playerManager =
                new DefaultPlayerManager(
                        accountService,
                        settingRegistry,
                        serverName
                );

        playerManager.start();

        serviceRegistry.register(
                PlayerManager.class,
                playerManager
        );

        plugin.getLogger().info(
                "Registered service: "
                        + playerManager.id()
        );
    }

    private void startPlayerSettingsService()
            throws Exception {
        if (playerManager == null) {
            throw new IllegalStateException(
                    "Cannot start PlayerSettingsService because PlayerManager is not initialized."
            );
        }

        if (settingRegistry == null) {
            throw new IllegalStateException(
                    "Cannot start PlayerSettingsService because SettingRegistry is not initialized."
            );
        }

        if (apiClient == null) {
            throw new IllegalStateException(
                    "Cannot start PlayerSettingsService because ApiClient is not initialized."
            );
        }

        playerSettingsService =
                new DefaultPlayerSettingsService(
                        playerManager,
                        settingRegistry,
                        apiClient
                );

        playerSettingsService.start();

        serviceRegistry.register(
                PlayerSettingsService.class,
                playerSettingsService
        );

        plugin.getLogger().info(
                "Registered service: "
                        + playerSettingsService.id()
        );
    }

    private void startPlayerSettingUpdateDispatcher() {
        if (playerSettingsService == null) {
            throw new IllegalStateException(
                    "Cannot create PlayerSettingUpdateDispatcher because "
                            + "PlayerSettingsService is not initialized."
            );
        }

        playerSettingUpdateDispatcher =
                new BukkitPlayerSettingUpdateDispatcher(
                        plugin,
                        playerSettingsService
                );

        plugin.getLogger().info(
                "Initialized player setting update dispatcher."
        );
    }

    private void startPlayerOnboardingService()
            throws Exception {
        if (apiClient == null) {
            throw new IllegalStateException(
                    "Cannot start PlayerOnboardingService because ApiClient is not initialized."
            );
        }

        if (playerManager == null) {
            throw new IllegalStateException(
                    "Cannot start PlayerOnboardingService because PlayerManager is not initialized."
            );
        }

        playerOnboardingService =
                new DefaultPlayerOnboardingService(
                        plugin,
                        apiClient,
                        playerManager
                );

        playerOnboardingService.start();

        serviceRegistry.register(
                PlayerOnboardingService.class,
                playerOnboardingService
        );

        plugin.getLogger().info(
                "Registered service: "
                        + playerOnboardingService.id()
        );
    }

    private void startLocalizationService()
            throws Exception {
        languageRegistry =
                new LanguageRegistry();

        localizationService =
                new DefaultLocalizationService(
                        languageRegistry,
                        new PropertiesTranslationLoader(),
                        SupportedLanguage.ENGLISH
                );

        localizationService.start();

        serviceRegistry.register(
                LocalizationService.class,
                localizationService
        );

        plugin.getLogger().info(
                "Registered service: "
                        + localizationService.id()
                        + " ["
                        + languageRegistry.size()
                        + " languages]"
        );
    }

    private void startPlayerLanguageUpdateDispatcher() {
        if (playerOnboardingService == null) {
            throw new IllegalStateException(
                    "Cannot create PlayerLanguageUpdateDispatcher because "
                            + "PlayerOnboardingService is not initialized."
            );
        }

        playerLanguageUpdateDispatcher =
                new DefaultPlayerLanguageUpdateDispatcher(
                        playerOnboardingService
                );

        plugin.getLogger().info(
                "Initialized player language update dispatcher."
        );
    }

    private void startPlayerPassService()
            throws Exception {
        if (apiClient == null) {
            throw new IllegalStateException(
                    "Cannot start PlayerPassService because ApiClient is not initialized."
            );
        }

        if (playerManager == null) {
            throw new IllegalStateException(
                    "Cannot start PlayerPassService because PlayerManager is not initialized."
            );
        }

        if (schedulerService == null) {
            throw new IllegalStateException(
                    "Cannot start PlayerPassService because SchedulerService is not initialized."
            );
        }

        String configuredServerName =
                configurationService
                        .require(
                                "core"
                        )
                        .getString(
                                "server.name"
                        );

        if (configuredServerName == null
                || configuredServerName.isBlank()) {
            throw new IllegalStateException(
                    "Missing configuration value: server.name"
            );
        }

        boolean passEnabled =
                configurationService
                        .require(
                                "core"
                        )
                        .getBoolean(
                                "pass.enabled",
                                false
                        );

        playerPassService =
                new DefaultPlayerPassService(
                        plugin,
                        apiClient,
                        playerManager,
                        schedulerService,
                        configuredServerName
                                .trim()
                                .toLowerCase(),
                        passEnabled
                );

        playerPassService.start();

        serviceRegistry.register(
                PlayerPassService.class,
                playerPassService
        );

        plugin.getLogger().info(
                "Registered service: "
                        + playerPassService.id()
        );
    }

    private void startPlayerTitleService()
            throws Exception {
        if (apiClient == null) {
            throw new IllegalStateException(
                    "Cannot start PlayerTitleService because ApiClient is not initialized."
            );
        }

        if (playerManager == null) {
            throw new IllegalStateException(
                    "Cannot start PlayerTitleService because PlayerManager is not initialized."
            );
        }

        if (localizationService == null) {
            throw new IllegalStateException(
                    "Cannot start PlayerTitleService because LocalizationService is not initialized."
            );
        }

        boolean titlesEnabled =
                configurationService
                        .require(
                                "core"
                        )
                        .getBoolean(
                                "titles.enabled",
                                true
                        );

        boolean nameTagEnabled =
                configurationService
                        .require(
                                "core"
                        )
                        .getBoolean(
                                "titles.nametag.enabled",
                                true
                        );

        double nameTagOffset =
                configurationService
                        .require(
                                "core"
                        )
                        .getDouble(
                                "titles.nametag.offset",
                                0.55D
                        );

        double nameTagScale =
                configurationService
                        .require(
                                "core"
                        )
                        .getDouble(
                                "titles.nametag.scale",
                                0.8D
                        );

        double nameTagViewRange =
                configurationService
                        .require(
                                "core"
                        )
                        .getDouble(
                                "titles.nametag.view-range",
                                0.6D
                        );

        boolean hideWhileSneaking =
                configurationService
                        .require(
                                "core"
                        )
                        .getBoolean(
                                "titles.nametag.hide-while-sneaking",
                                true
                        );

        // der Renderer fragt die Sprache jedes Betrachters beim fertigen Service ab
        java.util.concurrent.atomic.AtomicReference<DefaultPlayerTitleService> serviceRef =
                new java.util.concurrent.atomic.AtomicReference<>();

        TitleNameTagRenderer renderer =
                new TitleNameTagRenderer(
                        plugin,
                        titlesEnabled
                                && nameTagEnabled,
                        nameTagOffset,
                        nameTagScale,
                        nameTagViewRange,
                        hideWhileSneaking,
                        viewer -> {
                            DefaultPlayerTitleService service =
                                    serviceRef.get();

                            return service == null
                                    ? localizationService.defaultLanguage()
                                    : service.languageOf(
                                            viewer
                                    );
                        }
                );

        DefaultPlayerTitleService titleService =
                new DefaultPlayerTitleService(
                        plugin,
                        apiClient,
                        playerManager,
                        localizationService,
                        renderer,
                        titlesEnabled
                );

        serviceRef.set(
                titleService
        );

        playerTitleService = titleService;
        playerTitleService.start();

        serviceRegistry.register(
                PlayerTitleService.class,
                playerTitleService
        );

        plugin.getLogger().info(
                "Registered service: "
                        + playerTitleService.id()
        );
    }

    private void startCoreApi() {
        if (playerManager == null) {
            throw new IllegalStateException(
                    "Cannot create TasticCoreApi because PlayerManager is not initialized."
            );
        }

        if (playerSettingsService == null) {
            throw new IllegalStateException(
                    "Cannot create TasticCoreApi because PlayerSettingsService is not initialized."
            );
        }

        if (playerSettingUpdateDispatcher == null) {
            throw new IllegalStateException(
                    "Cannot create TasticCoreApi because "
                            + "PlayerSettingUpdateDispatcher is not initialized."
            );
        }

        if (playerOnboardingService == null) {
            throw new IllegalStateException(
                    "Cannot create TasticCoreApi because PlayerOnboardingService is not initialized."
            );
        }

        if (localizationService == null) {
            throw new IllegalStateException(
                    "Cannot create TasticCoreApi because LocalizationService is not initialized."
            );
        }

        if (playerLanguageUpdateDispatcher == null) {
            throw new IllegalStateException(
                    "Cannot create TasticCoreApi because "
                            + "PlayerLanguageUpdateDispatcher is not initialized."
            );
        }

        if (playerPassService == null) {
            throw new IllegalStateException(
                    "Cannot create TasticCoreApi because PlayerPassService is not initialized."
            );
        }

        if (settingRegistry == null) {
            throw new IllegalStateException(
                    "Cannot create TasticCoreApi because SettingRegistry is not initialized."
            );
        }

        if (accountService == null) {
            throw new IllegalStateException(
                    "Cannot create TasticCoreApi because AccountService is not initialized."
            );
        }

        if (apiClient == null) {
            throw new IllegalStateException(
                    "Cannot create TasticCoreApi because ApiClient is not initialized."
            );
        }

        if (playerTitleService == null) {
            throw new IllegalStateException(
                    "Cannot create TasticCoreApi because PlayerTitleService is not initialized."
            );
        }

        coreApi =
                new DefaultTasticCoreApi(
                        playerManager,
                        playerSettingsService,
                        playerSettingUpdateDispatcher,
                        playerOnboardingService,
                        localizationService,
                        playerLanguageUpdateDispatcher,
                        playerPassService,
                        playerTitleService,
                        settingRegistry,
                        accountService,
                        apiClient
                );

        plugin.getLogger().info(
                "Initialized TasticCore API facade."
        );
    }

    private void startPlayerRuntimeListener() {
        if (playerManager == null) {
            throw new IllegalStateException(
                    "Cannot register PlayerRuntimeListener because PlayerManager is not initialized."
            );
        }

        if (playerSettingsService == null) {
            throw new IllegalStateException(
                    "Cannot register PlayerRuntimeListener because PlayerSettingsService is not initialized."
            );
        }

        if (playerOnboardingService == null) {
            throw new IllegalStateException(
                    "Cannot register PlayerRuntimeListener because "
                            + "PlayerOnboardingService is not initialized."
            );
        }

        if (playerPassService == null) {
            throw new IllegalStateException(
                    "Cannot register PlayerRuntimeListener because "
                            + "PlayerPassService is not initialized."
            );
        }

        if (playerTitleService == null) {
            throw new IllegalStateException(
                    "Cannot register PlayerRuntimeListener because "
                            + "PlayerTitleService is not initialized."
            );
        }

        playerRuntimeListener =
                new PlayerRuntimeListener(
                        plugin,
                        playerManager,
                        playerSettingsService,
                        playerOnboardingService,
                        playerPassService,
                        playerTitleService
                );

        plugin.getServer()
                .getPluginManager()
                .registerEvents(
                        playerRuntimeListener,
                        plugin
                );

        plugin.getLogger().info(
                "Registered player runtime listener."
        );
    }

    private void startCommands() {
        tasticCoreCommand =
                new TasticCoreCommand(
                        playerManager,
                        playerSettingsService,
                        settingRegistry,
                        serviceRegistry,
                        apiClient
                );

        Objects.requireNonNull(
                plugin.getCommand(
                        "tasticcore"
                ),
                "Command tasticcore is not defined in plugin.yml."
        ).setExecutor(
                tasticCoreCommand
        );

        plugin.getLogger().info(
                "Registered commands."
        );
    }

    private void startPlayerConnectionListener() {
        FileConfiguration coreConfig =
                configurationService.require(
                        "core"
                );

        boolean managedByProxy =
                coreConfig.getBoolean(
                        "presence.managed-by-proxy",
                        false
                );

        if (managedByProxy) {
            plugin.getLogger().info(
                    "Player presence is managed by Velocity. "
                            + "Paper connection listener remains disabled."
            );

            return;
        }

        if (apiClient == null
                || !apiClient.enabled()) {
            plugin.getLogger().warning(
                    "Player connection listener was not registered "
                            + "because the API client is disabled."
            );

            return;
        }

        String configuredServerName =
                coreConfig.getString(
                        "server.name"
                );

        if (configuredServerName == null
                || configuredServerName.isBlank()) {
            throw new IllegalStateException(
                    "Missing configuration value: server.name"
            );
        }

        String serverName =
                configuredServerName
                        .trim()
                        .toLowerCase();

        playerConnectionListener =
                new PlayerConnectionListener(
                        plugin,
                        apiClient,
                        serverName
                );

        plugin.getServer()
                .getPluginManager()
                .registerEvents(
                        playerConnectionListener,
                        plugin
                );

        plugin.getLogger().info(
                "Registered player connection listener for server: "
                        + serverName
        );
    }

    private void startTabListService() {
        boolean hidden =
                configurationService
                        .require(
                                "core"
                        )
                        .getBoolean(
                                "tablist.hidden",
                                true
                        );

        tabListService =
                new TabListService(
                        plugin,
                        hidden
                );

        tabListService.start();
    }

    private void startProxyChatListener() {
        boolean handledByProxy =
                configurationService
                        .require(
                                "core"
                        )
                        .getBoolean(
                                "chat.handled-by-proxy",
                                true
                        );

        if (!handledByProxy) {
            plugin.getLogger().info(
                    "Chat stays local (chat.handled-by-proxy=false)."
            );

            return;
        }

        proxyChatListener =
                new ProxyChatListener();

        plugin.getServer()
                .getPluginManager()
                .registerEvents(
                        proxyChatListener,
                        plugin
                );

        plugin.getLogger().info(
                "Chat is rendered by TasticProxy; local chat output is switched off."
        );
    }

    private void stopInternal()
            throws Exception {
        Exception failure =
                null;

        moduleManager.clear();

        if (proxyChatListener != null) {
            HandlerList.unregisterAll(
                    proxyChatListener
            );

            proxyChatListener =
                    null;
        }

        if (tabListService != null) {
            try {
                tabListService.stop();
            } catch (Exception exception) {
                failure =
                        appendFailure(
                                failure,
                                exception
                        );
            } finally {
                tabListService =
                        null;
            }
        }

        if (playerConnectionListener != null) {
            try {
                HandlerList.unregisterAll(
                        playerConnectionListener
                );

                playerConnectionListener.stop();
            } catch (Exception exception) {
                failure =
                        appendFailure(
                                failure,
                                exception
                        );
            } finally {
                playerConnectionListener =
                        null;
            }
        }

        if (playerRuntimeListener != null) {
            try {
                HandlerList.unregisterAll(
                        playerRuntimeListener
                );
            } catch (Exception exception) {
                failure =
                        appendFailure(
                                failure,
                                exception
                        );
            } finally {
                playerRuntimeListener =
                        null;
            }
        }

        tasticCoreCommand =
                null;

        coreApi =
                null;

        if (playerTitleService != null) {
            try {
                playerTitleService.stop();
            } catch (Exception exception) {
                failure =
                        appendFailure(
                                failure,
                                exception
                        );
            } finally {
                serviceRegistry.unregister(
                        PlayerTitleService.class
                );

                playerTitleService =
                        null;
            }
        }

        if (playerPassService != null) {
            try {
                playerPassService.stop();
            } catch (Exception exception) {
                failure =
                        appendFailure(
                                failure,
                                exception
                        );
            } finally {
                serviceRegistry.unregister(
                        PlayerPassService.class
                );

                playerPassService =
                        null;
            }
        }

        playerLanguageUpdateDispatcher =
                null;

        playerSettingUpdateDispatcher =
                null;

        if (localizationService != null) {
            try {
                localizationService.stop();
            } catch (Exception exception) {
                failure =
                        appendFailure(
                                failure,
                                exception
                        );
            } finally {
                serviceRegistry.unregister(
                        LocalizationService.class
                );

                localizationService =
                        null;

                languageRegistry =
                        null;
            }
        }

        if (playerOnboardingService != null) {
            try {
                playerOnboardingService.stop();
            } catch (Exception exception) {
                failure =
                        appendFailure(
                                failure,
                                exception
                        );
            } finally {
                serviceRegistry.unregister(
                        PlayerOnboardingService.class
                );

                playerOnboardingService =
                        null;
            }
        }

        if (playerSettingsService != null) {
            try {
                playerSettingsService.stop();
            } catch (Exception exception) {
                failure =
                        appendFailure(
                                failure,
                                exception
                        );
            } finally {
                serviceRegistry.unregister(
                        PlayerSettingsService.class
                );

                playerSettingsService =
                        null;
            }
        }

        if (playerManager != null) {
            try {
                playerManager.stop();
            } catch (Exception exception) {
                failure =
                        appendFailure(
                                failure,
                                exception
                        );
            } finally {
                serviceRegistry.unregister(
                        PlayerManager.class
                );

                playerManager =
                        null;
            }
        }

        if (settingRegistry != null) {
            try {
                settingRegistry.stop();
            } catch (Exception exception) {
                failure =
                        appendFailure(
                                failure,
                                exception
                        );
            } finally {
                serviceRegistry.unregister(
                        SettingRegistry.class
                );

                settingRegistry =
                        null;
            }
        }

        if (accountService != null) {
            try {
                accountService.stop();
            } catch (Exception exception) {
                failure =
                        appendFailure(
                                failure,
                                exception
                        );
            } finally {
                serviceRegistry.unregister(
                        AccountService.class
                );

                accountService =
                        null;
            }
        }

        if (apiClient != null) {
            try {
                apiClient.stop();
            } catch (Exception exception) {
                failure =
                        appendFailure(
                                failure,
                                exception
                        );
            } finally {
                serviceRegistry.unregister(
                        ApiClient.class
                );

                apiClient =
                        null;
            }
        }

        if (schedulerService != null) {
            try {
                schedulerService.stop();
            } catch (Exception exception) {
                failure =
                        appendFailure(
                                failure,
                                exception
                        );
            } finally {
                serviceRegistry.unregister(
                        SchedulerService.class
                );

                schedulerService =
                        null;
            }
        }

        if (configurationService != null) {
            try {
                configurationService.stop();
            } catch (Exception exception) {
                failure =
                        appendFailure(
                                failure,
                                exception
                        );
            } finally {
                serviceRegistry.unregister(
                        ConfigurationService.class
                );

                configurationService =
                        null;
            }
        }

        serviceRegistry.clear();

        if (failure != null) {
            throw failure;
        }
    }

    private Exception appendFailure(
            Exception currentFailure,
            Exception newFailure
    ) {
        if (currentFailure == null) {
            return newFailure;
        }

        currentFailure.addSuppressed(
                newFailure
        );

        return currentFailure;
    }

    private String rootMessage(
            Throwable throwable
    ) {
        Throwable current =
                throwable;

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
}
