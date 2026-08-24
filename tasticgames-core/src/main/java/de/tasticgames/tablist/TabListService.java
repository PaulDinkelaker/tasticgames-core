package de.tasticgames.tablist;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;

import java.util.Objects;

/**
 * Empties the vanilla player list on every server: pressing TAB still opens the list, but there is no entry
 * in it - not even the player's own.
 * <p>
 * This uses {@link Player#unlistPlayer(Player)}, which only removes the entry from the list. The players stay
 * fully visible in the world (unlike {@code hidePlayer}), keep their skins and can be interacted with; only
 * the overlay is empty. Header and footer are cleared as well, so nothing is left behind.
 * <p>
 * A plugin that writes the player list itself (TAB, for example) sends its own entries and wins - switch its
 * player list feature off, or set {@code tablist.hidden: false} in {@code core.yml} and let it do the work.
 */
public final class TabListService implements Listener {

    private final Plugin plugin;
    private final boolean hidden;
    private boolean registered;

    public TabListService(
            Plugin plugin,
            boolean hidden
    ) {
        this.plugin =
                Objects.requireNonNull(
                        plugin,
                        "plugin"
                );

        this.hidden = hidden;
    }

    public boolean hidden() {
        return hidden;
    }

    /** Registers the listener and empties the list for everyone who is already online. */
    public void start() {
        if (!hidden || registered) {
            return;
        }

        plugin.getServer()
                .getPluginManager()
                .registerEvents(
                        this,
                        plugin
                );

        registered = true;

        for (Player online : Bukkit.getOnlinePlayers()) {
            hideFor(
                    online
            );
        }

        plugin.getLogger().info(
                "Player list is hidden on this server (TAB shows no entries)."
        );
    }

    /** Restores the list, so a reload does not leave players with an empty TAB. */
    public void stop() {
        if (!registered) {
            return;
        }

        HandlerList.unregisterAll(
                this
        );

        registered = false;

        for (Player online : Bukkit.getOnlinePlayers()) {
            for (Player other : Bukkit.getOnlinePlayers()) {
                online.listPlayer(
                        other
                );
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(
            PlayerJoinEvent event
    ) {
        Player joined = event.getPlayer();

        hideFor(
                joined
        );

        for (Player online : Bukkit.getOnlinePlayers()) {
            if (!online.equals(
                    joined
            )) {
                // the joining player must disappear from every list that is already open
                online.unlistPlayer(
                        joined
                );
            }
        }
    }

    /** Removes every entry (including the player's own) from this player's list. */
    private void hideFor(
            Player player
    ) {
        for (Player other : Bukkit.getOnlinePlayers()) {
            player.unlistPlayer(
                    other
            );
        }

        player.sendPlayerListHeaderAndFooter(
                Component.empty(),
                Component.empty()
        );
    }
}
