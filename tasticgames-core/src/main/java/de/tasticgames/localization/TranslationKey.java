package de.tasticgames.localization;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

public record TranslationKey(
        String value
) {

    private static final Pattern KEY_PATTERN =
            Pattern.compile(
                    "^[a-z0-9]+(?:[._-][a-z0-9]+)*$"
            );

    public TranslationKey {
        Objects.requireNonNull(
                value,
                "value"
        );

        value = normalize(
                value
        );

        if (!KEY_PATTERN.matcher(
                value
        ).matches()) {
            throw new IllegalArgumentException(
                    "Invalid translation key: "
                            + value
            );
        }
    }

    public static TranslationKey of(
            String value
    ) {
        return new TranslationKey(
                value
        );
    }

    private static String normalize(
            String value
    ) {
        return value
                .trim()
                .toLowerCase(
                        Locale.ROOT
                );
    }
}
