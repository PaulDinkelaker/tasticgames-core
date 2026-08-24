package de.tasticgames.pass;

import de.tasticgames.service.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Verwaltet den Season-Pass-Zustand geladener Spieler.
 *
 * <p>Der Service kapselt die Kommunikation mit der TasticGames API,
 * hält den Zustand während der lokalen Paper-Session im Speicher und
 * bündelt gemeldete XP sowie Quest-Fortschritte, bevor sie gesendet
 * werden.</p>
 *
 * <p>Der Pass ist bewusst optional: ist die API deaktiviert oder nicht
 * erreichbar, arbeitet jede Methode als No-op und liefert leere
 * Ergebnisse. Kein Aufruf schlägt deshalb fehl, und kein Aufruf bricht
 * den Gameserver ab.</p>
 */
public interface PlayerPassService extends Service {

    /**
     * Die zwischengespeicherte aktive Season.
     *
     * <p>Leer, solange keine Season geladen werden konnte oder keine
     * Season aktiv ist.</p>
     */
    Optional<PassSeasonSnapshot> season();

    /**
     * Lädt den Pass-Zustand eines bereits geladenen Spielers.
     *
     * <p>Schlägt der Abruf fehl, liefert das Ergebnis
     * {@link PassSnapshot#inactive()} und der Zustand bleibt
     * ungeladen.</p>
     *
     * @param minecraftUuid UUID des Spielers
     * @return geladener Runtime-Zustand
     */
    CompletableFuture<PassSnapshot> load(
            UUID minecraftUuid
    );

    /**
     * Sucht den lokal geladenen Pass-Zustand.
     */
    Optional<PassSnapshot> state(
            UUID minecraftUuid
    );

    /**
     * Entfernt den lokalen Pass-Zustand beim Verlassen des
     * Backend-Servers und sendet die noch offenen Meldungen ein
     * letztes Mal.
     */
    void unload(
            UUID minecraftUuid
    );

    /**
     * Meldet XP für den Spieler.
     *
     * <p>Die Meldungen werden je Spieler und Quelle gebündelt und
     * zyklisch, beim Überschreiten der Bündelgrenze sowie beim
     * Entladen gesendet.</p>
     */
    void awardXp(
            UUID minecraftUuid,
            PassXpSource source,
            long amount,
            String reason
    );

    /**
     * Meldet einen spielspezifischen Zähler.
     *
     * <p>Der Zähler wird auf alle aktiven Quests der zwischengespeicherten
     * Season mit dieser Metrik verteilt; Premium-Quests werden für
     * Spieler ohne Premium übersprungen.</p>
     */
    void metric(
            UUID minecraftUuid,
            String metric,
            long amount
    );

    /**
     * Schaltet ein Achievement frei.
     *
     * @return {@code true}, wenn die Freischaltung neu war
     */
    CompletableFuture<Boolean> unlockAchievement(
            UUID minecraftUuid,
            String achievementKey
    );

    /**
     * Löst ein einzelnes Tier ein.
     */
    CompletableFuture<PassRewardGrantResult> claim(
            UUID minecraftUuid,
            int level,
            PassTrack track
    );

    /**
     * Löst alle freigeschalteten und noch offenen Tiers ein.
     */
    CompletableFuture<PassRewardGrantResult> claimAll(
            UUID minecraftUuid
    );

    /**
     * Rangliste der aktiven Season.
     *
     * @param limit gewünschte Anzahl Plätze
     */
    CompletableFuture<List<PassLeaderboardEntry>> leaderboard(
            int limit
    );

    /**
     * Meldet, ob der Spieler Premium der laufenden Season besitzt.
     */
    boolean premium(
            UUID minecraftUuid
    );

    /**
     * Aktuelles Pass-Level des Spielers, {@code 0} ohne geladenen
     * Zustand.
     */
    int level(
            UUID minecraftUuid
    );

    /**
     * Meldet, ob der Spieler ein über den Pass vergebenes Feature
     * freigeschaltet hat.
     */
    boolean hasFeature(
            UUID minecraftUuid,
            String featureKey
    );

    /**
     * Anzahl aktuell geladener Pass-Zustände.
     */
    int loadedCount();
}
