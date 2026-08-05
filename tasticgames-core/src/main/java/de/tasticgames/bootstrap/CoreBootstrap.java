package de.tasticgames.bootstrap;

import de.tasticgames.TasticCorePlugin;
import de.tasticgames.api.ApiClient;
import de.tasticgames.api.internal.HttpApiClient;
import de.tasticgames.config.ConfigurationService;
import de.tasticgames.config.internal.YamlConfigurationService;
import de.tasticgames.module.ModuleManager;
import de.tasticgames.player.PlayerManager;
import de.tasticgames.account.AccountService;
import de.tasticgames.settings.CoreSettings;
import de.tasticgames.settings.SettingRegistry;
import de.tasticgames.api.TasticCoreApi;
import de.tasticgames.api.internal.DefaultTasticCoreApi;
import de.tasticgames.settings.PlayerSettingsService;
import de.tasticgames.settings.internal.DefaultPlayerSettingsService;
import de.tasticgames.settings.internal.DefaultSettingRegistry;
import de.tasticgames.account.internal.DefaultAccountService;
import de.tasticgames.command.TasticCoreCommand;
import de.tasticgames.player.listener.PlayerRuntimeListener;
import de.tasticgames.player.internal.DefaultPlayerManager;
import de.tasticgames.player.listener.PlayerConnectionListener;
import de.tasticgames.scheduler.SchedulerService;
import de.tasticgames.scheduler.internal.BukkitSchedulerService;
import de.tasticgames.service.ServiceRegistry;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.HandlerList;
import de.tasticgames.onboarding.PlayerOnboardingService;
import de.tasticgames.onboarding.internal.DefaultPlayerOnboardingService;

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
    private PlayerConnectionListener playerConnectionListener;
    private PlayerManager playerManager;
    private PlayerRuntimeListener playerRuntimeListener;
    private TasticCoreCommand tasticCoreCommand;
    private AccountService accountService;
    private SettingRegistry settingRegistry;
    private PlayerSettingsService playerSettingsService;
    private TasticCoreApi coreApi;
    private PlayerOnboardingService playerOnboardingService;

    public CoreBootstrap(TasticCorePlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.running = new AtomicBoolean(false);
        this.serviceRegistry = new ServiceRegistry();
        this.moduleManager = new ModuleManager();
    }

    public void start() throws Exception {
        if (!running.compareAndSet(false, true)) {
            throw new IllegalStateException(
                    "TasticCore bootstrap is already running."
            );
        }

        plugin.getLogger().info("Starting TasticCore bootstrap...");

        try {
            startConfigurationService();
            startSchedulerService();
            startApiClient();
            startAccountService();
            startSettingRegistry();
            startPlayerManager();
            startPlayerSettingsService();
            startPlayerOnboardingService();
            startCoreApi();
            startPlayerRuntimeListener();
            startCommands();
            startPlayerConnectionListener();

            plugin.getLogger().info(
                    "TasticCore bootstrap started successfully."
            );
        } catch (Exception exception) {
            running.set(false);

            try {
                stopInternal();
            } catch (Exception stopException) {
                exception.addSuppressed(stopException);
            }

            throw exception;
        }
    }

    public void stop() throws Exception {
        if (!running.compareAndSet(true, false)) {
            return;
        }

        plugin.getLogger().info("Stopping TasticCore bootstrap...");

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

    private void startConfigurationService() throws Exception {
        configurationService = new YamlConfigurationService(plugin);
        configurationService.start();

        serviceRegistry.register(
                ConfigurationService.class,
                configurationService
        );

        plugin.getLogger().info(
                "Registered service: " + configurationService.id()
        );
    }

    private void startSchedulerService() throws Exception {
        schedulerService = new BukkitSchedulerService(plugin);
        schedulerService.start();

        serviceRegistry.register(
                SchedulerService.class,
                schedulerService
        );

        plugin.getLogger().info(
                "Registered service: " + schedulerService.id()
        );
    }

    private void startApiClient() throws Exception {
        apiClient = new HttpApiClient(configurationService);
        apiClient.start();

        serviceRegistry.register(
                ApiClient.class,
                apiClient
        );

        plugin.getLogger().info(
                "Registered service: " + apiClient.id()
        );

        if (!apiClient.enabled()) {
            plugin.getLogger().warning(
                    "API client is disabled. Player connection tracking will not work."
            );
            return;
        }

        apiClient.health().whenComplete((health, throwable) -> {
            if (throwable != null) {
                plugin.getLogger().warning(
                        "API health check failed: "
                                + rootMessage(throwable)
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
        });
    }

    private void startAccountService() throws Exception {
        if (apiClient == null) {
            throw new IllegalStateException(
                    "Cannot start AccountService because ApiClient is not initialized."
            );
        }

        accountService = new DefaultAccountService(
                apiClient
        );

        accountService.start();

        serviceRegistry.register(
                AccountService.class,
                accountService
        );

        plugin.getLogger().info(
                "Registered service: " + accountService.id()
        );
    }

    private void startSettingRegistry() throws Exception {
        settingRegistry = new DefaultSettingRegistry();

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

    private void startPlayerManager() throws Exception {
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
                        .require("core")
                        .getString(
                                "server.name"
                        );

        if (serverName == null
                || serverName.isBlank()) {
            throw new IllegalStateException(
                    "Missing configuration value: server.name"
            );
        }

        playerManager = new DefaultPlayerManager(
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
                "Registered service: " + playerManager.id()
        );
    }

    private void startPlayerSettingsService() throws Exception {
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

    private void startPlayerOnboardingService() throws Exception {
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

        if (playerOnboardingService == null) {
            throw new IllegalStateException(
                    "Cannot create TasticCoreApi because PlayerOnboardingService is not initialized."
            );
        }

        coreApi = new DefaultTasticCoreApi(
                playerManager,
                playerSettingsService,
                playerOnboardingService,
                settingRegistry,
                accountService,
                apiClient
        );

        plugin.getLogger().info(
                "Initialized TasticCore API facade."
        );
    }

    public TasticCoreApi coreApi() {
        if (coreApi == null) {
            throw new IllegalStateException(
                    "TasticCore API is not initialized."
            );
        }

        return coreApi;
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

        playerRuntimeListener =
                new PlayerRuntimeListener(
                        plugin,
                        playerManager,
                        playerSettingsService,
                        playerOnboardingService
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

        plugin.getCommand("tasticcore")
                .setExecutor(
                        tasticCoreCommand
                );

        plugin.getLogger().info(
                "Registered commands."
        );
    }

    private void startPlayerConnectionListener() {
        FileConfiguration coreConfig =
                configurationService.require("core");

        boolean managedByProxy = coreConfig.getBoolean(
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

        if (apiClient == null || !apiClient.enabled()) {
            plugin.getLogger().warning(
                    "Player connection listener was not registered "
                            + "because the API client is disabled."
            );

            return;
        }

        String configuredServerName =
                coreConfig.getString("server.name");

        if (configuredServerName == null
                || configuredServerName.isBlank()) {
            throw new IllegalStateException(
                    "Missing configuration value: server.name"
            );
        }

        String serverName = configuredServerName
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

    private void stopInternal() throws Exception {
        Exception failure = null;

        moduleManager.clear();

        if (playerConnectionListener != null) {
            try {
                HandlerList.unregisterAll(
                        playerConnectionListener
                );

                playerConnectionListener.stop();
            } catch (Exception exception) {
                failure = appendFailure(
                        failure,
                        exception
                );
            } finally {
                playerConnectionListener = null;
            }
        }

        if (playerRuntimeListener != null) {
            try {
                HandlerList.unregisterAll(
                        playerRuntimeListener
                );
            } catch (Exception exception) {
                failure = appendFailure(
                        failure,
                        exception
                );
            } finally {
                playerRuntimeListener = null;
            }
        }

        tasticCoreCommand = null;

        coreApi = null;

        if (playerSettingsService != null) {
            try {
                playerSettingsService.stop();
            } catch (Exception exception) {
                failure = appendFailure(
                        failure,
                        exception
                );
            } finally {
                serviceRegistry.unregister(
                        PlayerSettingsService.class
                );

                playerSettingsService = null;
            }
        }

        if (playerOnboardingService != null) {
            try {
                playerOnboardingService.stop();
            } catch (Exception exception) {
                failure = appendFailure(
                        failure,
                        exception
                );
            } finally {
                serviceRegistry.unregister(
                        PlayerOnboardingService.class
                );

                playerOnboardingService = null;
            }
        }

        if (playerManager != null) {
            try {
                playerManager.stop();
            } catch (Exception exception) {
                failure = appendFailure(
                        failure,
                        exception
                );
            } finally {
                serviceRegistry.unregister(
                        PlayerManager.class
                );

                playerManager = null;
            }
        }

        if (settingRegistry != null) {
            try {
                settingRegistry.stop();
            } catch (Exception exception) {
                failure = appendFailure(
                        failure,
                        exception
                );
            } finally {
                serviceRegistry.unregister(
                        SettingRegistry.class
                );

                settingRegistry = null;
            }
        }

        if (accountService != null) {
            try {
                accountService.stop();
            } catch (Exception exception) {
                failure = appendFailure(
                        failure,
                        exception
                );
            } finally {
                serviceRegistry.unregister(
                        AccountService.class
                );

                accountService = null;
            }
        }

        if (apiClient != null) {
            try {
                apiClient.stop();
            } catch (Exception exception) {
                failure = appendFailure(
                        failure,
                        exception
                );
            } finally {
                serviceRegistry.unregister(
                        ApiClient.class
                );

                apiClient = null;
            }
        }

        if (schedulerService != null) {
            try {
                schedulerService.stop();
            } catch (Exception exception) {
                failure = appendFailure(
                        failure,
                        exception
                );
            } finally {
                serviceRegistry.unregister(
                        SchedulerService.class
                );

                schedulerService = null;
            }
        }

        if (configurationService != null) {
            try {
                configurationService.stop();
            } catch (Exception exception) {
                failure = appendFailure(
                        failure,
                        exception
                );
            } finally {
                serviceRegistry.unregister(
                        ConfigurationService.class
                );

                configurationService = null;
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

        currentFailure.addSuppressed(newFailure);
        return currentFailure;
    }

    private String rootMessage(Throwable throwable) {
        Throwable current = throwable;

        while (current.getCause() != null) {
            current = current.getCause();
        }

        String message = current.getMessage();

        if (message == null || message.isBlank()) {
            return current.getClass().getSimpleName();
        }

        return message;
    }
}
