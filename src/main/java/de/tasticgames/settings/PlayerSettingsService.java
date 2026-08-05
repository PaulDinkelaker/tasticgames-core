package de.tasticgames.settings;

import de.tasticgames.player.TasticPlayer;
import de.tasticgames.service.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Zentraler Service für Spielereinstellungen.
 *
 * <p>Feature-Module greifen ausschließlich über diesen Service auf
 * Settings zu. Die konkrete Speicherung und Synchronisierung bleibt
 * dadurch intern austauschbar.</p>
 */
public interface PlayerSettingsService extends Service {

    /**
     * Gibt einen typisierten Einstellungswert zurück.
     *
     * @param player Spieler-Runtime
     * @param key Setting-Key
     * @param <T> Wertetyp
     * @return gespeicherter Wert oder Standardwert
     */
    <T> T get(
            TasticPlayer player,
            SettingKey<T> key
    );

    /**
     * Setzt einen typisierten Einstellungswert.
     *
     * @param player Spieler-Runtime
     * @param key Setting-Key
     * @param value neuer Wert
     * @param <T> Wertetyp
     */
    <T> void set(
            TasticPlayer player,
            SettingKey<T> key,
            T value
    );

    /**
     * Setzt eine Einstellung auf ihren Standardwert zurück.
     */
    void reset(
            TasticPlayer player,
            SettingKey<?> key
    );

    /**
     * Setzt sämtliche Einstellungen des Spielers zurück.
     */
    void resetAll(
            TasticPlayer player
    );

    /**
     * Liefert einen unveränderlichen Snapshot aller effektiven Werte.
     */
    Map<String, Object> snapshot(
            TasticPlayer player
    );

    /**
     * Lädt persistierte Einstellungen in die lokale Runtime.
     *
     * <p>Die konkrete API-Anbindung ergänzen wir im nächsten Schritt.</p>
     */
    CompletableFuture<Void> load(
            UUID minecraftUuid
    );

    /**
     * Speichert geänderte Einstellungen.
     */
    CompletableFuture<Void> flush(
            UUID minecraftUuid
    );

    /**
     * Speichert alle aktuell geänderten Player-Runtimes.
     */
    CompletableFuture<Void> flushAll();
}
