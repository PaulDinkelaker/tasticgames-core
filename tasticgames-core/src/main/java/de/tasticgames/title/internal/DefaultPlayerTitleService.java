package de.tasticgames.title.internal;

import de.tasticgames.TasticCorePlugin;
import de.tasticgames.api.ApiClient;
import de.tasticgames.localization.LocalizationService;
import de.tasticgames.localization.SupportedLanguage;
import de.tasticgames.player.PlayerManager;
import de.tasticgames.title.NetworkTitle;
import de.tasticgames.title.PlayerTitleService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Standardimplementierung des {@link PlayerTitleService}.
 *
 * <p>Der Title wird beim Join einmal aus der API geladen und danach nur noch
 * bei einer Änderung neu gesetzt – die Lobby meldet sie direkt über
 * {@link #apply}, jeder andere Weg über {@link #refresh}.</p>
 *
 * <p>Der Service bricht nie ab: ist die API deaktiviert oder nicht
 * erreichbar, bleibt der Title {@link NetworkTitle#none()} und es wird nichts
 * angezeigt.</p>
 */
public final class DefaultPlayerTitleService
        implements PlayerTitleService {

    private final TasticCorePlugin plugin;
    private final ApiClient apiClient;
    private final PlayerManager playerManager;
    private final LocalizationService localizationService;
    private final TitleNameTagRenderer renderer;
    /** Netzwerk-Titles insgesamt ({@code titles.enabled} in core.yml). */
    private final boolean enabled;

    private final ConcurrentMap<UUID, NetworkTitle> titles =
            new ConcurrentHashMap<>();

    public DefaultPlayerTitleService(
            TasticCorePlugin plugin,
            ApiClient apiClient,
            PlayerManager playerManager,
            LocalizationService localizationService,
            TitleNameTagRenderer renderer,
            boolean enabled
    ) {
        this.plugin = Objects.requireNonNull(
                plugin,
                "plugin"
        );

        this.apiClient = Objects.requireNonNull(
                apiClient,
                "apiClient"
        );

        this.playerManager = Objects.requireNonNull(
                playerManager,
                "playerManager"
        );

        this.localizationService = Objects.requireNonNull(
                localizationService,
                "localizationService"
        );

        this.renderer = Objects.requireNonNull(
                renderer,
                "renderer"
        );

        this.enabled = enabled;
    }

    @Override
    public String id() {
        return "player-title-service";
    }

    @Override
    public void start() {
        if (!enabled) {
            plugin.getLogger().info(
                    "Network titles are disabled (titles.enabled=false)."
            );

            return;
        }

        renderer.start();

        plugin.getLogger().info(
                "Network titles are enabled (name tag line: "
                        + renderer.enabled()
                        + ")."
        );
    }

    @Override
    public void stop() {
        renderer.stop();
        titles.clear();
    }

    @Override
    public CompletableFuture<NetworkTitle> load(
            UUID minecraftUuid
    ) {
        Objects.requireNonNull(
                minecraftUuid,
                "minecraftUuid"
        );

        if (!enabled
                || !apiClient.enabled()) {
            return CompletableFuture.completedFuture(
                    NetworkTitle.none()
            );
        }

        return apiClient
                .getNetworkTitle(
                        minecraftUuid
                )
                .handle(
                        (title, throwable) -> {
                            if (throwable != null) {
                                plugin.getLogger().fine(
                                        "Could not load the title of "
                                                + minecraftUuid
                                                + ": "
                                                + rootMessage(
                                                throwable
                                        )
                                );

                                return NetworkTitle.none();
                            }

                            return title;
                        }
                )
                .thenApply(
                        title -> {
                            apply(
                                    minecraftUuid,
                                    title
                            );

                            return title;
                        }
                );
    }

    @Override
    public CompletableFuture<NetworkTitle> refresh(
            UUID minecraftUuid
    ) {
        return load(
                minecraftUuid
        );
    }

    @Override
    public void apply(
            UUID minecraftUuid,
            NetworkTitle title
    ) {
        Objects.requireNonNull(
                minecraftUuid,
                "minecraftUuid"
        );

        if (!enabled) {
            return;
        }

        titles.put(
                minecraftUuid,
                title == null
                        ? NetworkTitle.none()
                        : title
        );

        renderLater(
                minecraftUuid
        );
    }

    @Override
    public Optional<NetworkTitle> title(
            UUID minecraftUuid
    ) {
        return Optional.ofNullable(
                titles.get(
                        minecraftUuid
                )
        );
    }

    @Override
    public NetworkTitle titleOrNone(
            UUID minecraftUuid
    ) {
        return title(
                minecraftUuid
        ).orElseGet(
                NetworkTitle::none
        );
    }

    @Override
    public void unload(
            UUID minecraftUuid
    ) {
        titles.remove(
                minecraftUuid
        );

        if (!plugin.isEnabled()) {
            return;
        }

        runOnMainThread(
                () -> renderer.clear(
                        minecraftUuid
                )
        );
    }

    @Override
    public boolean enabled() {
        return enabled;
    }

    @Override
    public List<TextDisplay> nameTags(
            UUID minecraftUuid
    ) {
        return renderer.displaysOf(
                minecraftUuid
        );
    }

    private void renderLater(
            UUID minecraftUuid
    ) {
        if (!plugin.isEnabled()) {
            return;
        }

        runOnMainThread(
                () -> render(
                        minecraftUuid
                )
        );
    }

    private void render(
            UUID minecraftUuid
    ) {
        Player player =
                plugin.getServer()
                        .getPlayer(
                                minecraftUuid
                        );

        if (player == null
                || !player.isOnline()) {
            return;
        }

        NetworkTitle title =
                titleOrNone(
                        minecraftUuid
                );

        Map<SupportedLanguage, Component> byLanguage =
                new EnumMap<>(
                        SupportedLanguage.class
                );

        for (SupportedLanguage language : SupportedLanguage.values()) {
            String text =
                    title.text(
                            language
                    );

            if (text.isBlank()) {
                continue;
            }

            try {
                byLanguage.put(
                        language,
                        MiniMessage.miniMessage()
                                .deserialize(
                                        text
                                )
                );
            } catch (RuntimeException exception) {
                plugin.getLogger().warning(
                        "Title of "
                                + minecraftUuid
                                + " is not valid MiniMessage in "
                                + language.code()
                                + " and is not shown: "
                                + text
                );
            }
        }

        if (byLanguage.isEmpty()) {
            renderer.clear(
                    minecraftUuid
            );

            return;
        }

        renderer.render(
                player,
                byLanguage
        );
    }

    /**
     * Die Sprache eines Betrachters. Jede Zeile gibt es je Sprache einmal, deshalb liest jeder den
     * Title in seiner eigenen – auch den Title anderer Spieler.
     */
    public SupportedLanguage languageOf(
            Player viewer
    ) {
        return playerManager
                .find(
                        viewer.getUniqueId()
                )
                .map(
                        localizationService::languageOf
                )
                .orElseGet(
                        localizationService::defaultLanguage
                );
    }

    private void runOnMainThread(
            Runnable task
    ) {
        if (plugin.getServer().isPrimaryThread()) {
            task.run();

            return;
        }

        plugin.getServer()
                .getScheduler()
                .runTask(
                        plugin,
                        task
                );
    }

    private static String rootMessage(
            Throwable throwable
    ) {
        Throwable current =
                throwable;

        while (current.getCause() != null
                && current.getCause() != current) {
            current =
                    current.getCause();
        }

        return current.getMessage() == null
                ? current.getClass().getSimpleName()
                : current.getMessage();
    }
}
