package de.tasticgames.account;

import de.tasticgames.api.MinecraftAccount;
import de.tasticgames.service.Service;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Fachlicher Core-Service für Minecraft-Accounts.
 *
 * <p>Andere Core-Komponenten greifen über diesen Service auf
 * Accountdaten zu und verwenden nicht direkt den ApiClient.</p>
 */
public interface AccountService extends Service {

    /**
     * Registriert beziehungsweise aktualisiert einen Minecraft-Login.
     *
     * <p>Der Aufruf ist idempotent: Existiert der Account bereits,
     * werden insbesondere Benutzername und Last-Seen-Daten aktualisiert.</p>
     *
     * @param minecraftUuid UUID des Minecraft-Spielers
     * @param username aktueller Minecraft-Benutzername
     * @return zukünftiges Account-Ergebnis
     */
    CompletableFuture<MinecraftAccount> registerLogin(
            UUID minecraftUuid,
            String username
    );

    /**
     * Lädt einen bestehenden Minecraft-Account.
     *
     * @param minecraftUuid UUID des Minecraft-Spielers
     * @return zukünftiges Account-Ergebnis
     */
    CompletableFuture<MinecraftAccount> getAccount(
            UUID minecraftUuid
    );
}
