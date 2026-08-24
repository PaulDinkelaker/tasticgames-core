package de.tasticgames.title;

import de.tasticgames.localization.SupportedLanguage;

import java.util.Map;
import java.util.Objects;

/**
 * Der Netzwerk-Title eines Spielers: die ID des getragenen Title-Kosmetiks
 * und dessen Text je Sprachcode.
 *
 * <p>Die Texte kommen aus dem Katalog der API, den die Lobby pflegt. Ein
 * Title kann getragen sein, ohne dass es dazu einen Text gibt (etwa direkt
 * nach dem Entfernen aus dem Katalog); dann bleibt {@link #text} leer und es
 * wird nichts angezeigt.</p>
 */
public record NetworkTitle(
        String cosmeticId,
        Map<String, String> texts
) {

    private static final NetworkTitle NONE =
            new NetworkTitle(
                    null,
                    Map.of()
            );

    public NetworkTitle {
        texts = texts == null
                ? Map.of()
                : Map.copyOf(
                        texts
                );
    }

    /** Kein Title getragen. */
    public static NetworkTitle none() {
        return NONE;
    }

    /** Ob der Spieler ein Title-Kosmetik trägt. */
    public boolean equipped() {
        return cosmeticId != null;
    }

    /** Ob es einen anzeigbaren Text gibt. */
    public boolean renderable() {
        return !texts.isEmpty();
    }

    /**
     * Der Text in der gewünschten Sprache, sonst auf Englisch, sonst leer.
     */
    public String text(
            SupportedLanguage language
    ) {
        Objects.requireNonNull(
                language,
                "language"
        );

        String exact =
                texts.get(
                        language.code()
                );

        if (exact != null) {
            return exact;
        }

        String english =
                texts.get(
                        SupportedLanguage.ENGLISH.code()
                );

        return english == null
                ? ""
                : english;
    }
}
