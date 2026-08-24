package de.tasticgames.localization;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class TranslationBundle {

    private final SupportedLanguage language;
    private final Map<TranslationKey, String> translations;

    public TranslationBundle(
            SupportedLanguage language,
            Map<TranslationKey, String> translations
    ) {
        this.language = Objects.requireNonNull(
                language,
                "language"
        );

        Objects.requireNonNull(
                translations,
                "translations"
        );

        this.translations =
                Collections.unmodifiableMap(
                        new LinkedHashMap<>(
                                translations
                        )
                );
    }

    public SupportedLanguage language() {
        return language;
    }

    public Optional<String> find(
            TranslationKey key
    ) {
        Objects.requireNonNull(
                key,
                "key"
        );

        return Optional.ofNullable(
                translations.get(
                        key
                )
        );
    }

    public String require(
            TranslationKey key
    ) {
        return find(
                key
        ).orElseThrow(
                () ->
                        new IllegalArgumentException(
                                "Missing translation "
                                        + key.value()
                                        + " for language "
                                        + language.code()
                        )
        );
    }

    public boolean contains(
            TranslationKey key
    ) {
        Objects.requireNonNull(
                key,
                "key"
        );

        return translations.containsKey(
                key
        );
    }

    public int size() {
        return translations.size();
    }

    public Map<TranslationKey, String> translations() {
        return translations;
    }
}
