package de.tasticgames.config;

import de.tasticgames.service.Service;
import org.bukkit.configuration.file.FileConfiguration;

import java.nio.file.Path;
import java.util.Set;

public interface ConfigurationService extends Service {

    FileConfiguration require(String configName);

    boolean contains(String configName);

    void reload(String configName);

    void reloadAll();

    Set<String> configNames();

    Path configDirectory();
}
