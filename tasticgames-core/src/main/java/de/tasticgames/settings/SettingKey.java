package de.tasticgames.settings;

import java.util.Objects;
import java.util.function.Predicate;

/**
 * Typisierte Definition einer Spielereinstellung.
 *
 * @param id eindeutige persistente Kennung, etwa "music.enabled"
 * @param type Java-Typ des Wertes
 * @param defaultValue Standardwert
 * @param validator Validierung für neue Werte
 * @param <T> Wertetyp
 */
public record SettingKey<T>(
        String id,
        Class<T> type,
        T defaultValue,
        Predicate<T> validator
) {

    public SettingKey {
        id = normalizeId(id);

        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(defaultValue, "defaultValue");
        Objects.requireNonNull(validator, "validator");

        if (!type.isInstance(defaultValue)) {
            throw new IllegalArgumentException(
                    "Default value for setting '"
                            + id
                            + "' is not of type "
                            + type.getName()
            );
        }

        if (!validator.test(defaultValue)) {
            throw new IllegalArgumentException(
                    "Default value for setting '"
                            + id
                            + "' is invalid."
            );
        }
    }

    public static SettingKey<Boolean> booleanKey(
            String id,
            boolean defaultValue
    ) {
        return new SettingKey<>(
                id,
                Boolean.class,
                defaultValue,
                ignored -> true
        );
    }

    public static SettingKey<Integer> integerKey(
            String id,
            int defaultValue,
            int minimum,
            int maximum
    ) {
        if (minimum > maximum) {
            throw new IllegalArgumentException(
                    "minimum must not be greater than maximum."
            );
        }

        return new SettingKey<>(
                id,
                Integer.class,
                defaultValue,
                value -> value >= minimum
                        && value <= maximum
        );
    }

    public static SettingKey<String> stringKey(
            String id,
            String defaultValue,
            Predicate<String> validator
    ) {
        return new SettingKey<>(
                id,
                String.class,
                defaultValue,
                validator
        );
    }

    public boolean accepts(
            Object value
    ) {
        if (!type.isInstance(value)) {
            return false;
        }

        return validator.test(
                type.cast(value)
        );
    }

    public T validate(
            Object value
    ) {
        if (!type.isInstance(value)) {
            throw new IllegalArgumentException(
                    "Value for setting '"
                            + id
                            + "' must be of type "
                            + type.getSimpleName()
            );
        }

        T typedValue = type.cast(value);

        if (!validator.test(typedValue)) {
            throw new IllegalArgumentException(
                    "Invalid value for setting '"
                            + id
                            + "': "
                            + value
            );
        }

        return typedValue;
    }

    private static String normalizeId(
            String id
    ) {
        Objects.requireNonNull(id, "id");

        String normalized = id
                .trim()
                .toLowerCase();

        if (!normalized.matches(
                "^[a-z0-9]+(?:[._-][a-z0-9]+)*$"
        )) {
            throw new IllegalArgumentException(
                    "Invalid setting id: " + id
            );
        }

        return normalized;
    }
}
