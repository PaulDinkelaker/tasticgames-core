package de.tasticgames.settings.internal;

import de.tasticgames.settings.PlayerSettings;
import de.tasticgames.settings.PlayerSettingsState;
import de.tasticgames.settings.SettingKey;
import de.tasticgames.settings.SettingRegistry;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class DefaultPlayerSettings
        implements PlayerSettings {

    private final SettingRegistry registry;

    private final ConcurrentMap<String, Object> values =
            new ConcurrentHashMap<>();

    private long revision;
    private long cleanRevision;

    public DefaultPlayerSettings(
            SettingRegistry registry
    ) {
        this.registry = Objects.requireNonNull(
                registry,
                "registry"
        );

        this.revision = 0L;
        this.cleanRevision = 0L;
    }

    @Override
    public synchronized <T> T get(
            SettingKey<T> key
    ) {
        requireRegistered(
                key
        );

        Object value =
                values.get(
                        key.id()
                );

        if (value == null) {
            return key.defaultValue();
        }

        return key.validate(
                value
        );
    }

    @Override
    public synchronized <T> void set(
            SettingKey<T> key,
            T value
    ) {
        requireRegistered(
                key
        );

        T validatedValue =
                key.validate(
                        value
                );

        T previousValue =
                getEffectiveValue(
                        key
                );

        if (Objects.equals(
                previousValue,
                validatedValue
        )) {
            return;
        }

        values.put(
                key.id(),
                validatedValue
        );

        incrementRevision();
    }

    @Override
    public synchronized <T> void setPersisted(
            SettingKey<T> key,
            T value
    ) {
        requireRegistered(
                key
        );

        T validatedValue =
                key.validate(
                        value
                );

        values.put(
                key.id(),
                validatedValue
        );
    }

    @Override
    public synchronized <T> long restoreIfCurrent(
            SettingKey<T> key,
            T expectedCurrentValue,
            T previousValue
    ) {
        requireRegistered(
                key
        );

        T validatedExpectedValue =
                key.validate(
                        expectedCurrentValue
                );

        T validatedPreviousValue =
                key.validate(
                        previousValue
                );

        T currentValue =
                getEffectiveValue(
                        key
                );

        if (!Objects.equals(
                currentValue,
                validatedExpectedValue
        )) {
            return -1L;
        }

        if (Objects.equals(
                currentValue,
                validatedPreviousValue
        )) {
            return revision;
        }

        if (Objects.equals(
                validatedPreviousValue,
                key.defaultValue()
        )) {
            values.remove(
                    key.id()
            );
        } else {
            values.put(
                    key.id(),
                    validatedPreviousValue
            );
        }

        incrementRevision();

        return revision;
    }

    @Override
    public synchronized boolean contains(
            SettingKey<?> key
    ) {
        requireRegistered(
                key
        );

        return values.containsKey(
                key.id()
        );
    }

    @Override
    public synchronized void reset(
            SettingKey<?> key
    ) {
        requireRegistered(
                key
        );

        Object previousValue =
                getEffectiveValueUntyped(
                        key
                );

        Object defaultValue =
                key.defaultValue();

        values.remove(
                key.id()
        );

        if (!Objects.equals(
                previousValue,
                defaultValue
        )) {
            incrementRevision();
        }
    }

    @Override
    public synchronized void resetAll() {
        boolean changed =
                false;

        for (SettingKey<?> key
                : registry.settings()) {
            Object currentValue =
                    getEffectiveValueUntyped(
                            key
                    );

            if (!Objects.equals(
                    currentValue,
                    key.defaultValue()
            )) {
                changed =
                        true;

                break;
            }
        }

        values.clear();

        if (changed) {
            incrementRevision();
        }
    }

    @Override
    public synchronized Map<String, Object> snapshot() {
        return createSnapshot();
    }

    @Override
    public synchronized PlayerSettingsState state() {
        return new PlayerSettingsState(
                createSnapshot(),
                revision
        );
    }

    @Override
    public synchronized long revision() {
        return revision;
    }

    @Override
    public synchronized boolean dirty() {
        return revision
                != cleanRevision;
    }

    @Override
    public synchronized void markClean() {
        cleanRevision =
                revision;
    }

    @Override
    public synchronized boolean markClean(
            long expectedRevision
    ) {
        if (expectedRevision < 0) {
            throw new IllegalArgumentException(
                    "expectedRevision must not be negative."
            );
        }

        if (revision != expectedRevision) {
            return false;
        }

        cleanRevision =
                expectedRevision;

        return true;
    }

    @Override
    public synchronized void markDirty() {
        if (dirty()) {
            return;
        }

        incrementRevision();
    }

    @Override
    public synchronized void load(
            Map<String, Object> persistedValues
    ) {
        Objects.requireNonNull(
                persistedValues,
                "persistedValues"
        );

        Map<String, Object> loadedValues =
                new LinkedHashMap<>();

        persistedValues.forEach(
                (id, value) -> {
                    SettingKey<?> key =
                            registry.find(
                                    id
                            ).orElse(
                                    null
                            );

                    if (key == null) {
                        return;
                    }

                    Object validatedValue =
                            validateUntyped(
                                    key,
                                    value
                            );

                    loadedValues.put(
                            key.id(),
                            validatedValue
                    );
                }
        );

        values.clear();

        values.putAll(
                loadedValues
        );

        incrementRevision();

        cleanRevision =
                revision;
    }

    private Map<String, Object> createSnapshot() {
        Map<String, Object> snapshot =
                new LinkedHashMap<>();

        for (SettingKey<?> key
                : registry.settings()) {
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

        return Map.copyOf(
                snapshot
        );
    }

    private <T> T getEffectiveValue(
            SettingKey<T> key
    ) {
        Object value =
                values.get(
                        key.id()
                );

        if (value == null) {
            return key.defaultValue();
        }

        return key.validate(
                value
        );
    }

    private Object getEffectiveValueUntyped(
            SettingKey<?> key
    ) {
        Object value =
                values.get(
                        key.id()
                );

        if (value == null) {
            return key.defaultValue();
        }

        return key.validate(
                value
        );
    }

    private void incrementRevision() {
        if (revision == Long.MAX_VALUE) {
            throw new IllegalStateException(
                    "Player settings revision overflow."
            );
        }

        revision++;
    }

    private void requireRegistered(
            SettingKey<?> key
    ) {
        Objects.requireNonNull(
                key,
                "key"
        );

        SettingKey<?> registered =
                registry.find(
                                key.id()
                        )
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "Setting is not registered: "
                                                        + key.id()
                                        )
                        );

        if (registered != key
                && !registered.equals(
                key
        )) {
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
        return key.validate(
                value
        );
    }
}
