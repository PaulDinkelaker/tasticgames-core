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

    <T> void setPersisted(
            SettingKey<T> key,
            T value
    );

    <T> long restoreIfCurrent(
            SettingKey<T> key,
            T expectedCurrentValue,
            T previousValue
    );

    boolean contains(
            SettingKey<?> key
    );

    void reset(
            SettingKey<?> key
    );

    void resetAll();

    Map<String, Object> snapshot();

    PlayerSettingsState state();

    long revision();

    boolean dirty();

    void markClean();

    boolean markClean(
            long expectedRevision
    );

    void markDirty();

    void load(
            Map<String, Object> persistedValues
    );
}