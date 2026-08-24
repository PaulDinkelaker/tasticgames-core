package de.tasticgames.chat;

import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/**
 * Der Chat gehört dem Proxy: TasticProxy rendert jede Zeile einmal für das ganze Netzwerk und
 * schickt sie an alle Server. Damit dieselbe Nachricht nicht zusätzlich lokal ausgegeben wird,
 * bricht dieser Listener sie auf dem Backend ab.
 *
 * <p>Warum hier und nicht auf dem Proxy: seit 1.19.1 lässt sich eine signierte Chatnachricht auf
 * dem Proxy nicht mehr abbrechen – Velocity trennt den Spieler dann mit "a proxy plugin caused an
 * illegal protocol state". Auf dem Backend ist der Abbruch dagegen ganz normal möglich.</p>
 *
 * <p>Schalter: {@code chat.handled-by-proxy} in core.yml. Steht er auf {@code false}, verhält sich
 * der Server wieder wie Vanilla – dann sehen Spieler jede Nachricht doppelt, solange der globale
 * Chat des Proxys läuft.</p>
 */
public final class ProxyChatListener
        implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChat(
            AsyncChatEvent event
    ) {
        event.setCancelled(
                true
        );
    }
}
