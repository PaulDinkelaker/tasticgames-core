package de.tasticgames.localization;

import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class LanguageRegistry {

    private final Map<SupportedLanguage, TranslationBundle> bundles =
            new EnumMap<>(
                    SupportedLanguage.class
            );

    public void register(
            TranslationBundle bundle
    ) {
        Objects.requireNonNull(
                bundle,
                "bundle"
        );

        TranslationBundle previous =
                bundles.put(
                        bundle.language(),
                        bundle
                );

        if (previous != null) {
            throw new IllegalStateException(
                    "Translations already registered for language "
                            + bundle.language().code()
            );
        }
    }

    public Optional<TranslationBundle> find(
            SupportedLanguage language
    ) {
        Objects.requireNonNull(
                language,
                "language"
        );

        return Optional.ofNullable(
                bundles.get(
                        language
                )
        );
    }

    public TranslationBundle require(
            SupportedLanguage language
    ) {
        return find(
                language
        ).orElseThrow(
                () ->
                        new IllegalStateException(
                                "No translations registered for language "
                                        + language.code()
                        )
        );
    }

    public boolean contains(
            SupportedLanguage language
    ) {
        Objects.requireNonNull(
                language,
                "language"
        );

        return bundles.containsKey(
                language
        );
    }

    public int size() {
        return bundles.size();
    }

    public Collection<TranslationBundle> bundles() {
        return bundles.values();
    }
}
