package de.tasticgames.settings;

import java.util.Map;

public interface PlayerSettings {

    <T> T get(
            SettingKey<T> key
    );

    <T> void set(
            SettingKey<T> key,
            T value
    );

    void load(
            Map<String, Object> persistedValues
    );

    boolean contains(
            SettingKey<?> key
    );

    void reset(
            SettingKey<?> key
    );

    void resetAll();

    Map<String, Object> snapshot();

    boolean dirty();

    void markClean();
}
