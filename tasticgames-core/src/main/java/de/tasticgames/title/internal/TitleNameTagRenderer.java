package de.tasticgames.title.internal;

import de.tasticgames.localization.SupportedLanguage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;

/**
 * Zeigt den Netzwerk-Title als eigene Zeile unter dem Namensschild – jedem Betrachter in seiner
 * eigenen Sprache.
 *
 * <p>Dafür reitet ein {@link TextDisplay} auf dem Spieler: die Zeile bewegt sich damit ohne eigene
 * Bewegungspakete mit und sitzt immer an derselben Stelle.</p>
 *
 * <p><strong>Warum mehrere Zeilen je Spieler:</strong> eine Anzeige-Entität trägt genau einen Text
 * für alle. Wer den Title in seiner eigenen Sprache lesen soll, braucht also eine eigene Entität je
 * Sprache. Statt einer Zeile je Betrachter (das wären bei 100 Spielern 10 000 Entitäten) gibt es
 * eine Zeile je <em>tatsächlich benutzter</em> Sprache – höchstens so viele, wie das Netzwerk
 * Sprachen kennt – und jeder Betrachter bekommt alle bis auf seine eigene versteckt.</p>
 *
 * <p>Die Entitäten sind nicht persistent und tragen den Tag {@link #ENTITY_TAG}. Beim Start werden
 * übrig gebliebene Zeilen (etwa nach einem Absturz) aus allen Welten entfernt.</p>
 *
 * <p>Alle Methoden gehören auf den Hauptthread.</p>
 */
public final class TitleNameTagRenderer
        implements Listener {

    /** Scoreboard-Tag jeder von uns erzeugten Zeile. */
    public static final String ENTITY_TAG = "tastic_title";

    private final Plugin plugin;
    private final boolean enabled;
    private final float offset;
    private final float scale;
    private final float viewRange;
    private final boolean hideWhileSneaking;
    /** Sprache eines Betrachters – kommt vom Title-Service, damit hier keine Kontodaten nötig sind. */
    private final Function<Player, SupportedLanguage> languageOf;

    /** Träger -> Sprache -> Zeile. */
    private final Map<UUID, Map<SupportedLanguage, TextDisplay>> displays =
            new HashMap<>();

    /** Träger -> Sprache -> Text, damit eine Zeile nach Tod oder Weltwechsel neu entstehen kann. */
    private final Map<UUID, Map<SupportedLanguage, Component>> texts =
            new HashMap<>();

    public TitleNameTagRenderer(
            Plugin plugin,
            boolean enabled,
            double offset,
            double scale,
            double viewRange,
            boolean hideWhileSneaking,
            Function<Player, SupportedLanguage> languageOf
    ) {
        this.plugin = Objects.requireNonNull(
                plugin,
                "plugin"
        );

        this.languageOf = Objects.requireNonNull(
                languageOf,
                "languageOf"
        );

        this.enabled = enabled;
        this.offset = (float) offset;
        this.scale = (float) scale;
        this.viewRange = (float) viewRange;
        this.hideWhileSneaking = hideWhileSneaking;
    }

    public boolean enabled() {
        return enabled;
    }

    public void start() {
        if (!enabled) {
            return;
        }

        removeStaleDisplays();

        plugin.getServer()
                .getPluginManager()
                .registerEvents(
                        this,
                        plugin
                );
    }

    public void stop() {
        HandlerList.unregisterAll(
                this
        );

        clearAll();
    }

    /**
     * Zeigt die Zeile unter dem Namen von {@code player}; ein leerer Text entfernt sie wieder.
     *
     * @param byLanguage Text je Sprache – für Sprachen ohne Eintrag wird nichts angezeigt
     */
    public void render(
            Player player,
            Map<SupportedLanguage, Component> byLanguage
    ) {
        Objects.requireNonNull(
                player,
                "player"
        );

        if (!enabled) {
            return;
        }

        UUID minecraftUuid =
                player.getUniqueId();

        Map<SupportedLanguage, Component> wanted =
                new EnumMap<>(
                        SupportedLanguage.class
                );

        if (byLanguage != null) {
            byLanguage.forEach(
                    (language, text) -> {
                        if (!isBlank(
                                text
                        )) {
                            wanted.put(
                                    language,
                                    text
                            );
                        }
                    }
            );
        }

        if (wanted.isEmpty()) {
            clear(
                    minecraftUuid
            );

            return;
        }

        texts.put(
                minecraftUuid,
                wanted
        );

        Map<SupportedLanguage, TextDisplay> mine =
                displays.computeIfAbsent(
                        minecraftUuid,
                        key -> new EnumMap<>(
                                SupportedLanguage.class
                        )
                );

        // Zeilen für Sprachen, die es nicht mehr gibt, verschwinden
        mine.entrySet().removeIf(
                entry -> {
                    if (wanted.containsKey(
                            entry.getKey()
                    )) {
                        return false;
                    }

                    entry.getValue().remove();

                    return true;
                }
        );

        boolean sneaking =
                hideWhileSneaking
                        && player.isSneaking();

        for (Map.Entry<SupportedLanguage, Component> entry : wanted.entrySet()) {
            TextDisplay display =
                    mine.get(
                            entry.getKey()
                    );

            if (display == null
                    || !display.isValid()
                    || !player.equals(
                            display.getVehicle()
                    )) {
                if (display != null) {
                    display.remove();
                }

                display =
                        spawn(
                                player
                        );

                mine.put(
                        entry.getKey(),
                        display
                );
            }

            display.text(
                    sneaking
                            ? Component.empty()
                            : entry.getValue()
            );
        }

        applyVisibility(
                player
        );
    }

    /**
     * Jeder Betrachter sieht genau die Zeile seiner eigenen Sprache – und der Träger keine, so wie
     * er auch sein eigenes Namensschild nicht sieht.
     */
    private void applyVisibility(
            Player wearer
    ) {
        Map<SupportedLanguage, TextDisplay> mine =
                displays.get(
                        wearer.getUniqueId()
                );

        if (mine == null) {
            return;
        }

        for (Player viewer : plugin.getServer().getOnlinePlayers()) {
            applyVisibility(
                    wearer,
                    mine,
                    viewer
            );
        }
    }

    private void applyVisibility(
            Player wearer,
            Map<SupportedLanguage, TextDisplay> mine,
            Player viewer
    ) {
        SupportedLanguage language =
                languageOf.apply(
                        viewer
                );

        for (Map.Entry<SupportedLanguage, TextDisplay> entry : mine.entrySet()) {
            TextDisplay display =
                    entry.getValue();

            if (!display.isValid()) {
                continue;
            }

            boolean visible =
                    !viewer.equals(
                            wearer
                    )
                            && entry.getKey() == language;

            if (visible) {
                viewer.showEntity(
                        plugin,
                        display
                );
            } else {
                viewer.hideEntity(
                        plugin,
                        display
                );
            }
        }
    }

    /** Entfernt alle Zeilen eines Spielers. */
    public void clear(
            UUID minecraftUuid
    ) {
        texts.remove(
                minecraftUuid
        );

        Map<SupportedLanguage, TextDisplay> mine =
                displays.remove(
                        minecraftUuid
                );

        if (mine != null) {
            mine.values()
                    .forEach(
                            TextDisplay::remove
                    );
        }
    }

    public void clearAll() {
        displays.values()
                .forEach(
                        mine -> mine.values()
                                .forEach(
                                        TextDisplay::remove
                                )
                );

        displays.clear();
        texts.clear();
    }

    /**
     * Die Zeilen eines Spielers, damit Sichtbarkeits-Modi sie mit verstecken können.
     */
    public List<TextDisplay> displaysOf(
            UUID minecraftUuid
    ) {
        Map<SupportedLanguage, TextDisplay> mine =
                displays.get(
                        minecraftUuid
                );

        return mine == null
                ? List.of()
                : List.copyOf(
                        mine.values()
                );
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(
            PlayerQuitEvent event
    ) {
        clear(
                event.getPlayer()
                        .getUniqueId()
        );
    }

    /** Ein neuer Betrachter sieht zunächst alle Zeilen – bis auf seine eigene Sprache verschwinden sie. */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(
            PlayerJoinEvent event
    ) {
        Player viewer =
                event.getPlayer();

        plugin.getServer()
                .getScheduler()
                .runTask(
                        plugin,
                        () -> {
                            if (!viewer.isOnline()) {
                                return;
                            }

                            displays.forEach(
                                    (wearerId, mine) -> {
                                        Player wearer =
                                                plugin.getServer()
                                                        .getPlayer(
                                                                wearerId
                                                        );

                                        if (wearer != null) {
                                            applyVisibility(
                                                    wearer,
                                                    mine,
                                                    viewer
                                            );
                                        }
                                    }
                            );
                        }
                );
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(
            PlayerDeathEvent event
    ) {
        // Beim Tod verliert der Spieler seine Passagiere; die Zeilen kommen beim Respawn zurück.
        UUID minecraftUuid =
                event.getEntity()
                        .getUniqueId();

        Map<SupportedLanguage, Component> remembered =
                texts.get(
                        minecraftUuid
                );

        clear(
                minecraftUuid
        );

        if (remembered != null) {
            texts.put(
                    minecraftUuid,
                    remembered
            );
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(
            PlayerRespawnEvent event
    ) {
        reattachNextTick(
                event.getPlayer()
        );
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldChange(
            PlayerChangedWorldEvent event
    ) {
        // Ein Weltwechsel setzt den Spieler ab; die alten Zeilen bleiben in der alten Welt zurück.
        reattachNextTick(
                event.getPlayer()
        );
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onToggleSneak(
            PlayerToggleSneakEvent event
    ) {
        if (!hideWhileSneaking) {
            return;
        }

        Player player =
                event.getPlayer();

        Map<SupportedLanguage, TextDisplay> mine =
                displays.get(
                        player.getUniqueId()
                );

        Map<SupportedLanguage, Component> remembered =
                texts.get(
                        player.getUniqueId()
                );

        if (mine == null
                || remembered == null) {
            return;
        }

        mine.forEach(
                (language, display) -> {
                    if (!display.isValid()) {
                        return;
                    }

                    Component text =
                            remembered.get(
                                    language
                            );

                    display.text(
                            event.isSneaking() || text == null
                                    ? Component.empty()
                                    : text
                    );
                }
        );
    }

    private void reattachNextTick(
            Player player
    ) {
        if (!enabled
                || !plugin.isEnabled()) {
            return;
        }

        UUID minecraftUuid =
                player.getUniqueId();

        Map<SupportedLanguage, Component> remembered =
                texts.get(
                        minecraftUuid
                );

        if (remembered == null) {
            return;
        }

        Map<SupportedLanguage, Component> copy =
                new EnumMap<>(
                        remembered
                );

        clear(
                minecraftUuid
        );

        texts.put(
                minecraftUuid,
                copy
        );

        plugin.getServer()
                .getScheduler()
                .runTask(
                        plugin,
                        () -> {
                            Player online =
                                    plugin.getServer()
                                            .getPlayer(
                                                    minecraftUuid
                                            );

                            if (online == null
                                    || !online.isOnline()) {
                                return;
                            }

                            render(
                                    online,
                                    copy
                            );
                        }
                );
    }

    private TextDisplay spawn(
            Player player
    ) {
        TextDisplay display =
                player.getWorld()
                        .spawn(
                                player.getLocation(),
                                TextDisplay.class,
                                entity -> {
                                    entity.setPersistent(
                                            false
                                    );

                                    entity.addScoreboardTag(
                                            ENTITY_TAG
                                    );

                                    entity.setBillboard(
                                            Display.Billboard.CENTER
                                    );

                                    entity.setAlignment(
                                            TextDisplay.TextAlignment.CENTER
                                    );

                                    entity.setSeeThrough(
                                            false
                                    );

                                    entity.setShadowed(
                                            false
                                    );

                                    entity.setDefaultBackground(
                                            false
                                    );

                                    entity.setBackgroundColor(
                                            Color.fromARGB(
                                                    0,
                                                    0,
                                                    0,
                                                    0
                                            )
                                    );

                                    entity.setViewRange(
                                            viewRange
                                    );

                                    entity.setBrightness(
                                            new Display.Brightness(
                                                    15,
                                                    15
                                            )
                                    );

                                    entity.setTransformation(
                                            new Transformation(
                                                    new Vector3f(
                                                            0.0f,
                                                            offset,
                                                            0.0f
                                                    ),
                                                    new AxisAngle4f(),
                                                    new Vector3f(
                                                            scale,
                                                            scale,
                                                            scale
                                                    ),
                                                    new AxisAngle4f()
                                            )
                                    );
                                }
                        );

        player.addPassenger(
                display
        );

        // Das eigene Namensschild sieht man auch nicht.
        player.hideEntity(
                plugin,
                display
        );

        return display;
    }

    /** Zeilen, die ein Absturz zurückgelassen hat. */
    private void removeStaleDisplays() {
        int removed =
                0;

        for (World world : Bukkit.getWorlds()) {
            for (TextDisplay display : world.getEntitiesByClass(
                    TextDisplay.class
            )) {
                if (display.getScoreboardTags()
                        .contains(
                                ENTITY_TAG
                        )) {
                    display.remove();
                    removed++;
                }
            }
        }

        if (removed > 0) {
            plugin.getLogger().info(
                    "Removed "
                            + removed
                            + " leftover title name tags."
            );
        }
    }

    private static boolean isBlank(
            Component text
    ) {
        return text == null
                || PlainTextComponentSerializer.plainText()
                        .serialize(
                                text
                        )
                        .isBlank();
    }
}
