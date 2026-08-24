package de.tasticgames.command;

import de.tasticgames.api.ApiClient;
import de.tasticgames.player.PlayerManager;
import de.tasticgames.player.TasticPlayer;
import de.tasticgames.service.ServiceRegistry;
import de.tasticgames.settings.PlayerSettingsService;
import de.tasticgames.settings.SettingKey;
import de.tasticgames.settings.SettingRegistry;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class TasticCoreCommand
        implements CommandExecutor {

    private final PlayerManager playerManager;
    private final PlayerSettingsService playerSettingsService;
    private final SettingRegistry settingRegistry;
    private final ServiceRegistry serviceRegistry;
    private final ApiClient apiClient;

    public TasticCoreCommand(
            PlayerManager playerManager,
            PlayerSettingsService playerSettingsService,
            SettingRegistry settingRegistry,
            ServiceRegistry serviceRegistry,
            ApiClient apiClient
    ) {
        this.playerManager = Objects.requireNonNull(
                playerManager,
                "playerManager"
        );

        this.playerSettingsService = Objects.requireNonNull(
                playerSettingsService,
                "playerSettingsService"
        );

        this.settingRegistry = Objects.requireNonNull(
                settingRegistry,
                "settingRegistry"
        );

        this.serviceRegistry = Objects.requireNonNull(
                serviceRegistry,
                "serviceRegistry"
        );

        this.apiClient = Objects.requireNonNull(
                apiClient,
                "apiClient"
        );
    }

    @Override
    public boolean onCommand(
            CommandSender sender,
            Command command,
            String label,
            String[] args
    ) {
        if (!sender.hasPermission("tasticcore.admin")) {
            sendColored(
                    sender,
                    NamedTextColor.RED,
                    "You do not have permission to use this command."
            );

            return true;
        }

        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }

        String subcommand =
                args[0].toLowerCase();

        return switch (subcommand) {
            case "status" ->
                    handleStatusCommand(
                            sender,
                            args
                    );

            case "flush" ->
                    handleFlushCommand(
                            sender,
                            args
                    );

            case "player" ->
                    handlePlayerCommand(
                            sender,
                            args
                    );

            case "setting" ->
                    handleSettingCommand(
                            sender,
                            args
                    );

            case "settings" ->
                    handleSettingsCommand(
                            sender,
                            args
                    );

            default -> {
                sendUsage(sender);
                yield true;
            }
        };
    }

    private boolean handlePlayerCommand(
            CommandSender sender,
            String[] args
    ) {
        if (args.length != 2) {
            sendColored(
                    sender,
                    NamedTextColor.RED,
                    "Usage: /tasticcore player <name>"
            );

            return true;
        }

        Player player =
                Bukkit.getPlayerExact(args[1]);

        if (player == null) {
            sendColored(
                    sender,
                    NamedTextColor.RED,
                    "Player not online."
            );

            return true;
        }

        Optional<TasticPlayer> runtime =
                playerManager.find(
                        player.getUniqueId()
                );

        if (runtime.isEmpty()) {
            sendColored(
                    sender,
                    NamedTextColor.RED,
                    "No runtime loaded."
            );

            return true;
        }

        TasticPlayer tasticPlayer =
                runtime.get();

        sendBlank(sender);
        sendColored(
                sender,
                NamedTextColor.GOLD,
                "=== TasticPlayer ==="
        );

        sendValue(
                sender,
                "Account ID",
                tasticPlayer.accountId()
        );

        sendValue(
                sender,
                "Minecraft UUID",
                tasticPlayer.minecraftUuid()
        );

        sendValue(
                sender,
                "Username",
                tasticPlayer.username()
        );

        sendValue(
                sender,
                "Language",
                tasticPlayer.language()
        );

        sendValue(
                sender,
                "Account Status",
                tasticPlayer.accountStatus()
        );

        sendValue(
                sender,
                "State",
                tasticPlayer.state()
        );

        sendValue(
                sender,
                "Ready",
                tasticPlayer.ready()
        );

        sendValue(
                sender,
                "Server",
                tasticPlayer.runtime().serverName()
        );

        sendValue(
                sender,
                "Loaded At",
                tasticPlayer.runtime().loadedAt()
        );

        sendValue(
                sender,
                "Loaded Duration",
                formatDuration(
                        tasticPlayer.runtime()
                                .loadedDuration()
                )
        );

        sendValue(
                sender,
                "Unloading",
                tasticPlayer.runtime().unloading()
        );

        sendValue(
                sender,
                "Settings Dirty",
                tasticPlayer.settings().dirty()
        );

        sendValue(
                sender,
                "First Seen",
                tasticPlayer.firstSeenAt()
        );

        sendValue(
                sender,
                "Last Seen",
                tasticPlayer.lastSeenAt()
        );

        sendValue(
                sender,
                "Created At",
                tasticPlayer.createdAt()
        );

        sendValue(
                sender,
                "Updated At",
                tasticPlayer.updatedAt()
        );

        sendBlank(sender);

        return true;
    }

    private boolean handleFlushCommand(
            CommandSender sender,
            String[] args
    ) {
        if (args.length != 2) {
            sendColored(
                    sender,
                    NamedTextColor.RED,
                    "Usage: /tasticcore flush <player>"
            );

            return true;
        }

        Player player =
                Bukkit.getPlayerExact(args[1]);

        if (player == null) {
            sendColored(
                    sender,
                    NamedTextColor.RED,
                    "Player not online."
            );

            return true;
        }

        Optional<TasticPlayer> runtime =
                playerManager.find(
                        player.getUniqueId()
                );

        if (runtime.isEmpty()) {
            sendColored(
                    sender,
                    NamedTextColor.RED,
                    "Runtime not loaded."
            );

            return true;
        }

        TasticPlayer tasticPlayer =
                runtime.get();

        boolean dirtyBefore =
                tasticPlayer.settings().dirty();

        playerSettingsService
                .flush(player.getUniqueId())
                .whenComplete((ignored, throwable) -> {
                    if (throwable != null) {
                        Throwable cause =
                                unwrap(throwable);

                        Bukkit.getScheduler().runTask(
                                Bukkit.getPluginManager()
                                        .getPlugin("TasticCore"),
                                () -> sendColored(
                                        sender,
                                        NamedTextColor.RED,
                                        "Settings flush failed: "
                                                + safeMessage(cause)
                                )
                        );

                        return;
                    }

                    Bukkit.getScheduler().runTask(
                            Bukkit.getPluginManager()
                                    .getPlugin("TasticCore"),
                            () -> {
                                sendColored(
                                        sender,
                                        NamedTextColor.GREEN,
                                        "Flushed settings for "
                                                + tasticPlayer.username()
                                );

                                sendValue(
                                        sender,
                                        "Dirty Before",
                                        dirtyBefore
                                );

                                sendValue(
                                        sender,
                                        "Dirty After",
                                        tasticPlayer.settings().dirty()
                                );
                            }
                    );
                });

        return true;
    }

    private Throwable unwrap(
            Throwable throwable
    ) {
        Throwable current = throwable;

        while ((current instanceof java.util.concurrent.CompletionException
                || current instanceof java.util.concurrent.ExecutionException)
                && current.getCause() != null) {
            current = current.getCause();
        }

        return current;
    }

    private String safeMessage(
            Throwable throwable
    ) {
        String message = throwable.getMessage();

        if (message == null || message.isBlank()) {
            return throwable
                    .getClass()
                    .getSimpleName();
        }

        return message;
    }

    private boolean handleSettingCommand(
            CommandSender sender,
            String[] args
    ) {
        if (args.length != 4) {
            sendColored(
                    sender,
                    NamedTextColor.RED,
                    "Usage: /tasticcore setting "
                            + "<player> <setting> <value>"
            );

            return true;
        }

        Player player =
                Bukkit.getPlayerExact(args[1]);

        if (player == null) {
            sendColored(
                    sender,
                    NamedTextColor.RED,
                    "Player not online."
            );

            return true;
        }

        Optional<TasticPlayer> runtime =
                playerManager.find(
                        player.getUniqueId()
                );

        if (runtime.isEmpty()) {
            sendColored(
                    sender,
                    NamedTextColor.RED,
                    "Runtime not loaded."
            );

            return true;
        }

        String settingId =
                args[2];

        SettingKey<?> key;

        try {
            key = settingRegistry.require(
                    settingId
            );
        } catch (IllegalArgumentException exception) {
            sendColored(
                    sender,
                    NamedTextColor.RED,
                    "Unknown setting: "
                            + settingId
            );

            return true;
        }

        try {
            Object parsedValue =
                    parseSettingValue(
                            key,
                            args[3]
                    );

            setUntyped(
                    runtime.get(),
                    key,
                    parsedValue
            );

            sendColored(
                    sender,
                    NamedTextColor.GREEN,
                    "Updated "
                            + key.id()
                            + " = "
                            + parsedValue
            );

            sendColored(
                    sender,
                    NamedTextColor.GRAY,
                    "Settings dirty: "
                            + runtime.get()
                            .settings()
                            .dirty()
            );

        } catch (IllegalArgumentException exception) {
            sendColored(
                    sender,
                    NamedTextColor.RED,
                    exception.getMessage()
            );
        }

        return true;
    }

    private boolean handleStatusCommand(
            CommandSender sender,
            String[] args
    ) {
        if (args.length != 1) {
            sendColored(
                    sender,
                    NamedTextColor.RED,
                    "Usage: /tasticcore status"
            );

            return true;
        }

        sendBlank(sender);
        sendColored(
                sender,
                NamedTextColor.GOLD,
                "=== TasticCore Status ==="
        );

        sendValue(
                sender,
                "API Enabled",
                apiClient.enabled()
        );

        sendValue(
                sender,
                "Loaded Players",
                playerManager.onlinePlayers().size()
        );

        sendValue(
                sender,
                "Registered Settings",
                settingRegistry.size()
        );

        sendValue(
                sender,
                "Registered Services",
                serviceRegistry.size()
        );

        long dirtyPlayers =
                playerManager.onlinePlayers()
                        .stream()
                        .filter(player ->
                                player.settings().dirty()
                        )
                        .count();

        sendValue(
                sender,
                "Dirty Player Settings",
                dirtyPlayers
        );

        long unloadingPlayers =
                playerManager.onlinePlayers()
                        .stream()
                        .filter(player ->
                                player.runtime().unloading()
                        )
                        .count();

        sendValue(
                sender,
                "Unloading Players",
                unloadingPlayers
        );

        sendBlank(sender);

        return true;
    }

    private boolean handleSettingsCommand(
            CommandSender sender,
            String[] args
    ) {
        if (args.length != 2) {
            sendColored(
                    sender,
                    NamedTextColor.RED,
                    "Usage: /tasticcore settings <player>"
            );

            return true;
        }

        Player player =
                Bukkit.getPlayerExact(args[1]);

        if (player == null) {
            sendColored(
                    sender,
                    NamedTextColor.RED,
                    "Player not online."
            );

            return true;
        }

        Optional<TasticPlayer> runtime =
                playerManager.find(
                        player.getUniqueId()
                );

        if (runtime.isEmpty()) {
            sendColored(
                    sender,
                    NamedTextColor.RED,
                    "Runtime not loaded."
            );

            return true;
        }

        TasticPlayer tasticPlayer =
                runtime.get();

        Map<String, Object> snapshot =
                playerSettingsService.snapshot(
                        tasticPlayer
                );

        sendBlank(sender);
        sendColored(
                sender,
                NamedTextColor.GOLD,
                "=== Settings: "
                        + tasticPlayer.username()
                        + " ==="
        );

        snapshot.entrySet()
                .stream()
                .sorted(
                        Map.Entry.comparingByKey()
                )
                .forEach(entry ->
                        sender.sendMessage(
                                Component.text(
                                                entry.getKey(),
                                                NamedTextColor.YELLOW
                                        )
                                        .append(
                                                Component.text(
                                                        " = ",
                                                        NamedTextColor.GRAY
                                                )
                                        )
                                        .append(
                                                Component.text(
                                                        String.valueOf(
                                                                entry.getValue()
                                                        ),
                                                        NamedTextColor.WHITE
                                                )
                                        )
                        )
                );

        sendBlank(sender);

        sendValue(
                sender,
                "Dirty",
                tasticPlayer.settings().dirty()
        );

        sendValue(
                sender,
                "Registered Settings",
                settingRegistry.size()
        );

        sendBlank(sender);

        return true;
    }

    private Object parseSettingValue(
            SettingKey<?> key,
            String rawValue
    ) {
        Objects.requireNonNull(
                key,
                "key"
        );

        Objects.requireNonNull(
                rawValue,
                "rawValue"
        );

        if (key.type() == Boolean.class) {
            if (!rawValue.equalsIgnoreCase("true")
                    && !rawValue.equalsIgnoreCase("false")) {
                throw new IllegalArgumentException(
                        "Value for setting '"
                                + key.id()
                                + "' must be true or false."
                );
            }

            return Boolean.parseBoolean(
                    rawValue
            );
        }

        if (key.type() == Integer.class) {
            try {
                return Integer.parseInt(
                        rawValue
                );
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException(
                        "Value for setting '"
                                + key.id()
                                + "' must be an integer."
                );
            }
        }

        if (key.type() == String.class) {
            return rawValue;
        }

        throw new IllegalArgumentException(
                "Unsupported setting type: "
                        + key.type().getSimpleName()
        );
    }

    @SuppressWarnings({
            "unchecked",
            "rawtypes"
    })
    private void setUntyped(
            TasticPlayer player,
            SettingKey<?> key,
            Object value
    ) {
        SettingKey rawKey =
                key;

        Object validatedValue =
                rawKey.validate(value);

        playerSettingsService.set(
                player,
                rawKey,
                validatedValue
        );
    }

    private void sendValue(
            CommandSender sender,
            String name,
            Object value
    ) {
        sender.sendMessage(
                Component.text(
                                name + ": ",
                                NamedTextColor.YELLOW
                        )
                        .append(
                                Component.text(
                                        String.valueOf(value),
                                        NamedTextColor.WHITE
                                )
                        )
        );
    }

    private void sendColored(
            CommandSender sender,
            NamedTextColor color,
            String message
    ) {
        sender.sendMessage(
                Component.text(
                        String.valueOf(message),
                        color
                )
        );
    }

    private void sendBlank(
            CommandSender sender
    ) {
        sender.sendMessage(
                Component.empty()
        );
    }

    private void sendUsage(
            CommandSender sender
    ) {
        sendBlank(sender);

        sendColored(
                sender,
                NamedTextColor.GOLD,
                "=== TasticCore Commands ==="
        );

        sendColored(
                sender,
                NamedTextColor.YELLOW,
                "/tasticcore player <name>"
        );

        sendColored(
                sender,
                NamedTextColor.YELLOW,
                "/tasticcore settings <name>"
        );

        sendColored(
                sender,
                NamedTextColor.YELLOW,
                "/tasticcore flush <name>"
        );

        sendColored(
                sender,
                NamedTextColor.YELLOW,
                "/tasticcore status"
        );

        sendColored(
                sender,
                NamedTextColor.YELLOW,
                "/tasticcore setting "
                        + "<name> <setting> <value>"
        );

        sendBlank(sender);
    }

    private String formatDuration(
            Duration duration
    ) {
        Objects.requireNonNull(
                duration,
                "duration"
        );

        long totalSeconds =
                Math.max(
                        0,
                        duration.toSeconds()
                );

        long days =
                totalSeconds / 86_400;

        long hours =
                (totalSeconds % 86_400) / 3_600;

        long minutes =
                (totalSeconds % 3_600) / 60;

        long seconds =
                totalSeconds % 60;

        if (days > 0) {
            return "%dd %02dh %02dm %02ds".formatted(
                    days,
                    hours,
                    minutes,
                    seconds
            );
        }

        if (hours > 0) {
            return "%dh %02dm %02ds".formatted(
                    hours,
                    minutes,
                    seconds
            );
        }

        if (minutes > 0) {
            return "%dm %02ds".formatted(
                    minutes,
                    seconds
            );
        }

        return seconds + "s";
    }
}
