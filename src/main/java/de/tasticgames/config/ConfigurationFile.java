package de.tasticgames.config;

import org.bukkit.configuration.file.FileConfiguration;

import java.nio.file.Path;

public interface ConfigurationFile {

    String name();

    Path path();

    FileConfiguration configuration();

    void reload();

    void save();
}
