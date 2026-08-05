package de.tasticgames.settings.internal;

import de.tasticgames.settings.SettingKey;
import de.tasticgames.settings.SettingRegistry;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class DefaultSettingRegistry
        implements SettingRegistry {

    private final ConcurrentMap<String, SettingKey<?>> settings =
            new ConcurrentHashMap<>();

    @Override
    public String id() {
        return "setting-registry";
    }

    @Override
    public void start() {
        // Keine externen Ressourcen erforderlich.
    }

    @Override
    public void stop() {
        settings.clear();
    }

    @Override
    public <T> void register(
            SettingKey<T> settingKey
    ) {
        Objects.requireNonNull(
                settingKey,
                "settingKey"
        );

        SettingKey<?> previous =
                settings.putIfAbsent(
                        settingKey.id(),
                        settingKey
                );

        if (previous != null) {
            throw new IllegalStateException(
                    "Setting is already registered: "
                            + settingKey.id()
            );
        }
    }

    @Override
    public Optional<SettingKey<?>> find(
            String id
    ) {
        return Optional.ofNullable(
                settings.get(
                        normalizeId(id)
                )
        );
    }

    @Override
    public SettingKey<?> require(
            String id
    ) {
        String normalizedId =
                normalizeId(id);

        SettingKey<?> settingKey =
                settings.get(normalizedId);

        if (settingKey == null) {
            throw new IllegalArgumentException(
                    "Unknown setting: "
                            + normalizedId
            );
        }

        return settingKey;
    }

    @Override
    public boolean contains(
            String id
    ) {
        return settings.containsKey(
                normalizeId(id)
        );
    }

    @Override
    public Collection<SettingKey<?>> settings() {
        return List.copyOf(
                settings.values()
        );
    }

    @Override
    public int size() {
        return settings.size();
    }

    private String normalizeId(
            String id
    ) {
        Objects.requireNonNull(
                id,
                "id"
        );

        String normalized =
                id.trim()
                        .toLowerCase(Locale.ROOT);

        if (normalized.isBlank()) {
            throw new IllegalArgumentException(
                    "Setting id must not be blank."
            );
        }

        return normalized;
    }
}
