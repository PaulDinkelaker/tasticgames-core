package de.tasticgames.player.event;

import de.tasticgames.player.TasticPlayer;
import de.tasticgames.settings.PlayerSettingChange;
import de.tasticgames.settings.SettingKey;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.Objects;

public final class TasticPlayerSettingChangedEvent
        extends Event {

    private static final HandlerList HANDLERS =
            new HandlerList();

    private final PlayerSettingChange<?> change;

    public TasticPlayerSettingChangedEvent(
            PlayerSettingChange<?> change
    ) {
        super(false);

        this.change = Objects.requireNonNull(
                change,
                "change"
        );

        if (!change.changed()) {
            throw new IllegalArgumentException(
                    "Player setting change must contain different values."
            );
        }
    }

    public PlayerSettingChange<?> change() {
        return change;
    }

    public TasticPlayer tasticPlayer() {
        return change.player();
    }

    public SettingKey<?> key() {
        return change.key();
    }

    public Object previousValue() {
        return change.previousValue();
    }

    public Object newValue() {
        return change.newValue();
    }

    public boolean concerns(
            SettingKey<?> settingKey
    ) {
        Objects.requireNonNull(
                settingKey,
                "settingKey"
        );

        return change.key()
                .id()
                .equals(
                        settingKey.id()
                );
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
