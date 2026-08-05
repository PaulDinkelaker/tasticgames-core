package de.tasticgames.settings;

import de.tasticgames.service.Service;

import java.util.Collection;
import java.util.Optional;

public interface SettingRegistry extends Service {

    <T> void register(
            SettingKey<T> settingKey
    );

    Optional<SettingKey<?>> find(
            String id
    );

    SettingKey<?> require(
            String id
    );

    boolean contains(
            String id
    );

    Collection<SettingKey<?>> settings();

    int size();
}
