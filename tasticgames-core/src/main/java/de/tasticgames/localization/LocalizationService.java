package de.tasticgames.localization;

import de.tasticgames.player.TasticPlayer;
import de.tasticgames.service.Service;
import net.kyori.adventure.text.Component;

import java.util.Map;

public interface LocalizationService
        extends Service {

    SupportedLanguage defaultLanguage();

    SupportedLanguage languageOf(
            TasticPlayer player
    );

    Component translate(
            SupportedLanguage language,
            TranslationKey key
    );

    Component translate(
            SupportedLanguage language,
            TranslationKey key,
            Map<String, ?> placeholders
    );

    Component translate(
            TasticPlayer player,
            TranslationKey key
    );

    Component translate(
            TasticPlayer player,
            TranslationKey key,
            Map<String, ?> placeholders
    );

    boolean contains(
            SupportedLanguage language,
            TranslationKey key
    );
}
