package de.tasticgames.localization.internal;

import de.tasticgames.localization.LanguageRegistry;
import de.tasticgames.localization.LocalizationService;
import de.tasticgames.localization.SupportedLanguage;
import de.tasticgames.localization.TranslationBundle;
import de.tasticgames.localization.TranslationKey;
import de.tasticgames.player.TasticPlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

public final class DefaultLocalizationService
        implements LocalizationService {

    private static final Pattern PLACEHOLDER_PATTERN =
            Pattern.compile(
                    "^[a-z0-9_-]+$"
            );

    private final LanguageRegistry languageRegistry;
    private final PropertiesTranslationLoader loader;
    private final SupportedLanguage defaultLanguage;
    private final MiniMessage miniMessage;

    public DefaultLocalizationService(
            LanguageRegistry languageRegistry,
            PropertiesTranslationLoader loader,
            SupportedLanguage defaultLanguage
    ) {
        this.languageRegistry = Objects.requireNonNull(
                languageRegistry,
                "languageRegistry"
        );

        this.loader = Objects.requireNonNull(
                loader,
                "loader"
        );

        this.defaultLanguage = Objects.requireNonNull(
                defaultLanguage,
                "defaultLanguage"
        );

        this.miniMessage =
                MiniMessage.miniMessage();
    }

    @Override
    public String id() {
        return "localization-service";
    }

    @Override
    public void start() throws IOException {
        for (SupportedLanguage language
                : SupportedLanguage.values()) {
            languageRegistry.register(
                    loader.load(
                            language
                    )
            );
        }

        if (!languageRegistry.contains(
                defaultLanguage
        )) {
            throw new IllegalStateException(
                    "Default language is not registered: "
                            + defaultLanguage.code()
            );
        }
    }

    @Override
    public void stop() {
    }

    @Override
    public SupportedLanguage defaultLanguage() {
        return defaultLanguage;
    }

    @Override
    public SupportedLanguage languageOf(
            TasticPlayer player
    ) {
        Objects.requireNonNull(
                player,
                "player"
        );

        return SupportedLanguage.find(
                player.language()
        ).orElse(
                defaultLanguage
        );
    }

    @Override
    public Component translate(
            SupportedLanguage language,
            TranslationKey key
    ) {
        return translate(
                language,
                key,
                Map.of()
        );
    }

    @Override
    public Component translate(
            SupportedLanguage language,
            TranslationKey key,
            Map<String, ?> placeholders
    ) {
        Objects.requireNonNull(
                language,
                "language"
        );

        Objects.requireNonNull(
                key,
                "key"
        );

        Objects.requireNonNull(
                placeholders,
                "placeholders"
        );

        String template =
                resolveTemplate(
                        language,
                        key
                );

        if (template == null) {
            return Component.text(
                    key.value()
            );
        }

        TagResolver resolver =
                createResolver(
                        placeholders
                );

        return miniMessage.deserialize(
                template,
                resolver
        );
    }

    @Override
    public Component translate(
            TasticPlayer player,
            TranslationKey key
    ) {
        return translate(
                player,
                key,
                Map.of()
        );
    }

    @Override
    public Component translate(
            TasticPlayer player,
            TranslationKey key,
            Map<String, ?> placeholders
    ) {
        Objects.requireNonNull(
                player,
                "player"
        );

        return translate(
                languageOf(
                        player
                ),
                key,
                placeholders
        );
    }

    @Override
    public boolean contains(
            SupportedLanguage language,
            TranslationKey key
    ) {
        Objects.requireNonNull(
                language,
                "language"
        );

        Objects.requireNonNull(
                key,
                "key"
        );

        return languageRegistry
                .find(
                        language
                )
                .map(
                        bundle ->
                                bundle.contains(
                                        key
                                )
                )
                .orElse(
                        false
                );
    }

    private String resolveTemplate(
            SupportedLanguage language,
            TranslationKey key
    ) {
        TranslationBundle bundle =
                languageRegistry.require(
                        language
                );

        String translation =
                bundle.find(
                                key
                        )
                        .orElse(
                                null
                        );

        if (translation != null) {
            return translation;
        }

        if (language == defaultLanguage) {
            return null;
        }

        return languageRegistry
                .require(
                        defaultLanguage
                )
                .find(
                        key
                )
                .orElse(
                        null
                );
    }

    private TagResolver createResolver(
            Map<String, ?> placeholders
    ) {
        if (placeholders.isEmpty()) {
            return TagResolver.empty();
        }

        TagResolver.Builder builder =
                TagResolver.builder();

        placeholders.forEach(
                (rawKey, value) -> {
                    String key =
                            normalizePlaceholderKey(
                                    rawKey
                            );

                    Objects.requireNonNull(
                            value,
                            "Placeholder value for '"
                                    + key
                                    + "'"
                    );

                    if (value instanceof Component component) {
                        builder.resolver(
                                Placeholder.component(
                                        key,
                                        component
                                )
                        );

                        return;
                    }

                    builder.resolver(
                            Placeholder.unparsed(
                                    key,
                                    String.valueOf(
                                            value
                                    )
                            )
                    );
                }
        );

        return builder.build();
    }

    private String normalizePlaceholderKey(
            String key
    ) {
        Objects.requireNonNull(
                key,
                "key"
        );

        String normalized =
                key.trim()
                        .toLowerCase(
                                Locale.ROOT
                        );

        if (!PLACEHOLDER_PATTERN
                .matcher(
                        normalized
                )
                .matches()) {
            throw new IllegalArgumentException(
                    "Invalid MiniMessage placeholder key: "
                            + key
            );
        }

        return normalized;
    }
}
