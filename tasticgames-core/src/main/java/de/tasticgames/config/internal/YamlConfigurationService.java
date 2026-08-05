package de.tasticgames.config.internal;

import de.tasticgames.TasticCorePlugin;
import de.tasticgames.config.ConfigurationException;
import de.tasticgames.config.ConfigurationFile;
import de.tasticgames.config.ConfigurationService;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public final class YamlConfigurationService implements ConfigurationService {

    private static final Set<String> DEFAULT_CONFIG_NAMES = Set.of(
            "core",
            "api",
            "database",
            "language",
            "chat",
            "gameplay"
    );

    private final TasticCorePlugin plugin;
    private final Map<String, ConfigurationFile> configurations;
    private final AtomicBoolean running;

    private final Path configDirectory;
    private final Path cacheDirectory;
    private final Path dataDirectory;
    private final Path logsDirectory;
    private final Path modulesDirectory;

    public YamlConfigurationService(TasticCorePlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.configurations = new LinkedHashMap<>();
        this.running = new AtomicBoolean(false);

        Path pluginDirectory = plugin.getDataFolder().toPath();

        this.configDirectory = pluginDirectory.resolve("config");
        this.cacheDirectory = pluginDirectory.resolve("cache");
        this.dataDirectory = pluginDirectory.resolve("data");
        this.logsDirectory = pluginDirectory.resolve("logs");
        this.modulesDirectory = pluginDirectory.resolve("modules");
    }

    @Override
    public String id() {
        return "configuration";
    }

    @Override
    public void start() {
        if (!running.compareAndSet(false, true)) {
            throw new IllegalStateException(
                    "Configuration service is already running."
            );
        }

        try {
            createDirectories();

            for (String configName : DEFAULT_CONFIG_NAMES) {
                copyDefaultConfiguration(configName);
                loadConfiguration(configName);
            }

            plugin.getLogger().info(
                    "Loaded " + configurations.size() + " configuration files."
            );
        } catch (Exception exception) {
            running.set(false);

            throw new ConfigurationException(
                    "Failed to start the configuration service.",
                    exception
            );
        }
    }

    @Override
    public void stop() {
        if (!running.compareAndSet(true, false)) {
            return;
        }

        configurations.clear();
    }

    @Override
    public FileConfiguration require(String configName) {
        String normalizedName = normalizeName(configName);
        ConfigurationFile configurationFile =
                configurations.get(normalizedName);

        if (configurationFile == null) {
            throw new ConfigurationException(
                    "Configuration is not loaded: " + normalizedName
            );
        }

        return configurationFile.configuration();
    }

    @Override
    public boolean contains(String configName) {
        return configurations.containsKey(normalizeName(configName));
    }

    @Override
    public void reload(String configName) {
        ensureRunning();

        String normalizedName = normalizeName(configName);
        ConfigurationFile configurationFile =
                configurations.get(normalizedName);

        if (configurationFile == null) {
            throw new ConfigurationException(
                    "Configuration is not loaded: " + normalizedName
            );
        }

        configurationFile.reload();
    }

    @Override
    public void reloadAll() {
        ensureRunning();

        for (ConfigurationFile configurationFile : configurations.values()) {
            configurationFile.reload();
        }
    }

    @Override
    public Set<String> configNames() {
        return Set.copyOf(configurations.keySet());
    }

    @Override
    public Path configDirectory() {
        return configDirectory;
    }

    private void createDirectories() {
        try {
            Files.createDirectories(configDirectory);
            Files.createDirectories(cacheDirectory);
            Files.createDirectories(dataDirectory);
            Files.createDirectories(logsDirectory);
            Files.createDirectories(modulesDirectory);
        } catch (Exception exception) {
            throw new ConfigurationException(
                    "Failed to create TasticCore directories.",
                    exception
            );
        }
    }

    private void copyDefaultConfiguration(String configName) {
        String normalizedName = normalizeName(configName);
        String resourcePath = "config/" + normalizedName + ".yml";
        Path targetPath = configDirectory.resolve(normalizedName + ".yml");

        if (Files.exists(targetPath)) {
            return;
        }

        try (InputStream ignored = plugin.getResource(resourcePath)) {
            if (ignored == null) {
                throw new ConfigurationException(
                        "Default configuration resource does not exist: "
                                + resourcePath
                );
            }
        } catch (ConfigurationException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ConfigurationException(
                    "Failed to inspect configuration resource: "
                            + resourcePath,
                    exception
            );
        }

        plugin.saveResource(resourcePath, false);
    }

    private void loadConfiguration(String configName) {
        String normalizedName = normalizeName(configName);
        Path path = configDirectory.resolve(normalizedName + ".yml");

        ConfigurationFile configurationFile =
                new YamlConfigurationFile(normalizedName, path);

        configurations.put(normalizedName, configurationFile);
    }

    private String normalizeName(String configName) {
        Objects.requireNonNull(configName, "configName");

        String normalizedName = configName
                .trim()
                .toLowerCase(Locale.ROOT);

        if (normalizedName.endsWith(".yml")) {
            normalizedName = normalizedName.substring(
                    0,
                    normalizedName.length() - 4
            );
        }

        if (normalizedName.isBlank()) {
            throw new IllegalArgumentException(
                    "Configuration name must not be blank."
            );
        }

        if (!normalizedName.matches("[a-z0-9_-]+")) {
            throw new IllegalArgumentException(
                    "Invalid configuration name: " + configName
            );
        }

        return normalizedName;
    }

    private void ensureRunning() {
        if (!running.get()) {
            throw new IllegalStateException(
                    "Configuration service is not running."
            );
        }
    }
}
