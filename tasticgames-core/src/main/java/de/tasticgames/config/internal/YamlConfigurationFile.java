package de.tasticgames.config.internal;

import de.tasticgames.config.ConfigurationException;
import de.tasticgames.config.ConfigurationFile;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

final class YamlConfigurationFile implements ConfigurationFile {

    private final String name;
    private final Path path;

    private FileConfiguration configuration;

    YamlConfigurationFile(String name, Path path) {
        this.name = Objects.requireNonNull(name, "name");
        this.path = Objects.requireNonNull(path, "path");
        reload();
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public Path path() {
        return path;
    }

    @Override
    public FileConfiguration configuration() {
        return configuration;
    }

    @Override
    public void reload() {
        configuration = YamlConfiguration.loadConfiguration(path.toFile());
    }

    @Override
    public void save() {
        try {
            configuration.save(path.toFile());
        } catch (IOException exception) {
            throw new ConfigurationException(
                    "Failed to save configuration: " + name,
                    exception
            );
        }
    }
}
