package de.tasticgames.title;

import de.tasticgames.service.Service;
import org.bukkit.entity.TextDisplay;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Der netzwerkweite Title eines Spielers: geladen aus der API, gehalten für
 * die lokale Session und – sofern eingeschaltet – als eigene Zeile unter dem
 * Namensschild angezeigt.
 *
 * <p>Der Title ist bewusst optional: ist die API deaktiviert oder nicht
 * erreichbar, liefert jeder Aufruf {@link NetworkTitle#none()} und es wird
 * nichts gerendert. Kein Aufruf schlägt deshalb fehl.</p>
 */
public interface PlayerTitleService extends Service {

    /**
     * Lädt den Title eines Spielers aus der API und zeigt ihn an.
     *
     * <p>Schlägt der Abruf fehl, liefert das Ergebnis
     * {@link NetworkTitle#none()}.</p>
     */
    CompletableFuture<NetworkTitle> load(
            UUID minecraftUuid
    );

    /**
     * Lädt den Title erneut, etwa nachdem der Spieler ein anderes
     * Title-Kosmetik angelegt hat.
     */
    CompletableFuture<NetworkTitle> refresh(
            UUID minecraftUuid
    );

    /**
     * Übernimmt einen bereits bekannten Title ohne API-Abruf.
     *
     * <p>Die Lobby kennt den neuen Title unmittelbar nach dem Anlegen und
     * spart damit die Runde über die API.</p>
     */
    void apply(
            UUID minecraftUuid,
            NetworkTitle title
    );

    /** Der lokal bekannte Title; leer, solange nichts geladen wurde. */
    Optional<NetworkTitle> title(
            UUID minecraftUuid
    );

    /** Wie {@link #title}, aber nie leer. */
    NetworkTitle titleOrNone(
            UUID minecraftUuid
    );

    /** Entfernt Title und Namensschild-Zeile beim Verlassen des Servers. */
    void unload(
            UUID minecraftUuid
    );

    /** Ob Titles überhaupt eingeschaltet sind ({@code titles.enabled}). */
    boolean enabled();

    /**
     * Die Anzeige-Entitäten unter dem Namensschild – eine je Sprache, damit jeder Betrachter den
     * Title in seiner eigenen liest.
     *
     * <p>Wer Spieler versteckt (Sichtbarkeits-Modi, Vanish), muss diese Entitäten mit verstecken –
     * sie sind eigenständige Objekte und bleiben sonst sichtbar.</p>
     */
    List<TextDisplay> nameTags(
            UUID minecraftUuid
    );
}
