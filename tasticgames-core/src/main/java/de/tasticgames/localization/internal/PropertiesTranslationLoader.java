package de.tasticgames.localization.internal;

import de.tasticgames.localization.SupportedLanguage;
import de.tasticgames.localization.TranslationBundle;
import de.tasticgames.localization.TranslationKey;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

public final class PropertiesTranslationLoader {

    private static final String DIRECTORY =
            "languages/";

    public TranslationBundle load(
            SupportedLanguage language
    ) throws IOException {
        Objects.requireNonNull(
                language,
                "language"
        );

        String resource =
                DIRECTORY
                        + language.code()
                        + ".properties";

        try (InputStream inputStream =
                     PropertiesTranslationLoader.class
                             .getClassLoader()
                             .getResourceAsStream(
                                     resource
                             )) {

            if (inputStream == null) {
                throw new IOException(
                        "Translation resource not found: "
                                + resource
                );
            }

            Properties properties =
                    new Properties();

            properties.load(
                    new InputStreamReader(
                            inputStream,
                            StandardCharsets.UTF_8
                    )
            );

            Map<TranslationKey, String> translations =
                    new LinkedHashMap<>();

            for (String key
                    : properties.stringPropertyNames()) {

                TranslationKey translationKey =
                        TranslationKey.of(
                                key
                        );

                String translation =
                        Objects.requireNonNull(
                                properties.getProperty(
                                        key
                                ),
                                "translation"
                        ).trim();

                if (translation.isEmpty()) {
                    throw new IOException(
                            "Translation is blank: "
                                    + translationKey.value()
                                    + " [language="
                                    + language.code()
                                    + "]"
                    );
                }

                translations.put(
                        translationKey,
                        translation
                );
            }

            return new TranslationBundle(
                    language,
                    translations
            );
        }
    }
}
