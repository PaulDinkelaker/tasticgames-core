package de.tasticgames.onboarding;

import de.tasticgames.service.Service;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Verwaltet den Onboarding-Zustand geladener Spieler.
 *
 * <p>Der Service kapselt die Kommunikation mit der TasticGames API
 * und hält den aktuellen Zustand während der lokalen Paper-Session
 * im Speicher.</p>
 */
public interface PlayerOnboardingService extends Service {

    /**
     * Lädt den Onboarding-Zustand eines bereits geladenen Spielers.
     *
     * @param minecraftUuid UUID des Spielers
     * @return geladener Runtime-Zustand
     */
    CompletableFuture<PlayerOnboarding> load(
            UUID minecraftUuid
    );

    /**
     * Sucht den lokal geladenen Onboarding-Zustand.
     */
    Optional<PlayerOnboarding> find(
            UUID minecraftUuid
    );

    /**
     * Gibt den geladenen Zustand zurück.
     *
     * @throws IllegalStateException wenn kein Zustand geladen ist
     */
    PlayerOnboarding require(
            UUID minecraftUuid
    );

    /**
     * Speichert die bewusste Sprachauswahl über die API und
     * aktualisiert anschließend den lokalen Runtime-Zustand.
     */
    CompletableFuture<PlayerOnboarding> selectLanguage(
            UUID minecraftUuid,
            String language
    );

    /**
     * schließt das gesamte Onboarding ab und aktualisiert
     * anschließend den lokalen Runtime-Zustand.
     */
    CompletableFuture<PlayerOnboarding> complete(
            UUID minecraftUuid
    );

    /**
     * Entfernt den lokalen Onboarding-Zustand beim Verlassen
     * des Backend-Servers.
     */
    void unload(
            UUID minecraftUuid
    );

    /**
     * Anzahl aktuell geladener Onboarding-Zustände.
     */
    int loadedCount();
}
