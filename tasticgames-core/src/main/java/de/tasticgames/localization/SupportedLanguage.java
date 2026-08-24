package de.tasticgames.localization;

import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

public enum SupportedLanguage {

    ENGLISH(
            "en",
            "English"
    ),

    GERMAN(
            "de",
            "Deutsch"
    ),

    HINDI(
            "hi",
            "हिन्दी"
    );

    private final String code;
    private final String displayName;

    SupportedLanguage(
            String code,
            String displayName
    ) {
        this.code = Objects.requireNonNull(
                code,
                "code"
        );

        this.displayName = Objects.requireNonNull(
                displayName,
                "displayName"
        );
    }

    public String code() {
        return code;
    }

    public String displayName() {
        return displayName;
    }

    public static Optional<SupportedLanguage> find(
            String value
    ) {
        if (value == null) {
            return Optional.empty();
        }

        String normalized =
                normalize(
                        value
                );

        return Arrays.stream(
                        values()
                )
                .filter(language ->
                        language.code.equals(
                                normalized
                        )
                )
                .findFirst();
    }

    public static SupportedLanguage require(
            String value
    ) {
        return find(
                value
        ).orElseThrow(
                () ->
                        new IllegalArgumentException(
                                "Unsupported language: "
                                        + value
                        )
        );
    }

    public static boolean isSupported(
            String value
    ) {
        return find(
                value
        ).isPresent();
    }

    private static String normalize(
            String value
    ) {
        String normalized =
                value
                        .trim()
                        .toLowerCase(
                                Locale.ROOT
                        )
                        .replace(
                                '_',
                                '-'
                        );

        int separatorIndex =
                normalized.indexOf(
                        '-'
                );

        if (separatorIndex >= 0) {
            normalized =
                    normalized.substring(
                            0,
                            separatorIndex
                    );
        }

        return normalized;
    }
}
