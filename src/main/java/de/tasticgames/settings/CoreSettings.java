package de.tasticgames.settings;

import java.util.Locale;
import java.util.Set;

public final class CoreSettings {

    private static final Set<String> SUPPORTED_LANGUAGES =
            Set.of(
                    "en",
                    "de",
                    "hi"
            );

    public static final SettingKey<Boolean> MUSIC_ENABLED =
            SettingKey.booleanKey(
                    "music.enabled",
                    true
            );

    public static final SettingKey<Integer> MUSIC_VOLUME =
            SettingKey.integerKey(
                    "music.volume",
                    100,
                    0,
                    100
            );

    public static final SettingKey<Boolean> SOUNDS_ENABLED =
            SettingKey.booleanKey(
                    "sounds.enabled",
                    true
            );

    public static final SettingKey<Integer> SOUND_VOLUME =
            SettingKey.integerKey(
                    "sounds.volume",
                    100,
                    0,
                    100
            );

    public static final SettingKey<String> LANGUAGE =
            SettingKey.stringKey(
                    "language",
                    "en",
                    CoreSettings::isSupportedLanguage
            );

    public static final SettingKey<Boolean> UI_ANIMATIONS =
            SettingKey.booleanKey(
                    "ui.animations",
                    true
            );

    public static final SettingKey<Boolean> REDUCED_EFFECTS =
            SettingKey.booleanKey(
                    "accessibility.reduced-effects",
                    false
            );

    public static final SettingKey<Boolean> EVENT_NOTIFICATIONS =
            SettingKey.booleanKey(
                    "notifications.events",
                    true
            );

    public static final SettingKey<Boolean> PRIVATE_MESSAGES =
            SettingKey.booleanKey(
                    "chat.private-messages",
                    true
            );

    public static final SettingKey<Boolean> FRIEND_REQUESTS =
            SettingKey.booleanKey(
                    "social.friend-requests",
                    true
            );

    public static final SettingKey<Boolean> PARTY_INVITES =
            SettingKey.booleanKey(
                    "social.party-invites",
                    true
            );

    public static final SettingKey<Boolean> COSMETICS_VISIBLE =
            SettingKey.booleanKey(
                    "cosmetics.visible",
                    true
            );

    private CoreSettings() {
    }

    private static boolean isSupportedLanguage(
            String language
    ) {
        if (language == null) {
            return false;
        }

        return SUPPORTED_LANGUAGES.contains(
                language
                        .trim()
                        .toLowerCase(Locale.ROOT)
        );
    }
}
