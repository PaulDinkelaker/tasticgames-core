package de.tasticgames.settings.internal;

import de.tasticgames.settings.PlayerSettings;
import de.tasticgames.settings.SettingKey;
import de.tasticgames.settings.SettingRegistry;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

public final class DefaultPlayerSettings
        implements PlayerSettings {

    private final SettingRegistry registry;

    private final ConcurrentMap<String, Object> values =
            new ConcurrentHashMap<>();

    private final AtomicBoolean dirty =
            new AtomicBoolean(false);

    public DefaultPlayerSettings(
            SettingRegistry registry
    ) {
        this.registry = Objects.requireNonNull(
                registry,
                "registry"
        );
    }

    @Override
    public <T> T get(
            SettingKey<T> key
    ) {
        requireRegistered(key);

        Object value = values.get(
                key.id()
        );

        if (value == null) {
            return key.defaultValue();
        }

        return key.validate(value);
    }

    @Override
    public <T> void set(
            SettingKey<T> key,
            T value
    ) {
        requireRegistered(key);

        T validatedValue =
                key.validate(value);

        Object previousValue =
                values.put(
                        key.id(),
                        validatedValue
                );

        if (!Objects.equals(
                previousValue,
                validatedValue
        )) {
            dirty.set(true);
        }
    }

    @Override
    public boolean contains(
            SettingKey<?> key
    ) {
        requireRegistered(key);

        return values.containsKey(
                key.id()
        );
    }

    @Override
    public void reset(
            SettingKey<?> key
    ) {
        requireRegistered(key);

        Object removed =
                values.remove(
                        key.id()
                );

        if (removed != null) {
            dirty.set(true);
        }
    }

    @Override
    public void resetAll() {
        if (values.isEmpty()) {
            return;
        }

        values.clear();
        dirty.set(true);
    }

    @Override
    public Map<String, Object> snapshot() {
        Map<String, Object> snapshot =
                new LinkedHashMap<>();

        for (SettingKey<?> key : registry.settings()) {
            Object value =
                    values.getOrDefault(
                            key.id(),
                            key.defaultValue()
                    );

            snapshot.put(
                    key.id(),
                    value
            );
        }

        return Map.copyOf(snapshot);
    }

    @Override
    public boolean dirty() {
        return dirty.get();
    }

    @Override
    public void markClean() {
        dirty.set(false);
    }

    @Override
    public void load(
            Map<String, Object> persistedValues
    ) {
        Objects.requireNonNull(
                persistedValues,
                "persistedValues"
        );

        values.clear();

        persistedValues.forEach(
                (id, value) -> {
                    SettingKey<?> key =
                            registry.find(id)
                                    .orElse(null);

                    if (key == null) {
                        return;
                    }

                    Object validated =
                            validateUntyped(
                                    key,
                                    value
                            );

                    values.put(
                            key.id(),
                            validated
                    );
                }
        );

        dirty.set(false);
    }

    private void requireRegistered(
            SettingKey<?> key
    ) {
        Objects.requireNonNull(
                key,
                "key"
        );

        SettingKey<?> registered =
                registry.find(key.id())
                        .orElseThrow(
                                () -> new IllegalArgumentException(
                                        "Setting is not registered: "
                                                + key.id()
                                )
                        );

        if (registered != key
                && !registered.equals(key)) {
            throw new IllegalArgumentException(
                    "A different SettingKey instance is registered for id: "
                            + key.id()
            );
        }
    }

    private Object validateUntyped(
            SettingKey<?> key,
            Object value
    ) {
        return key.validate(value);
    }
}
