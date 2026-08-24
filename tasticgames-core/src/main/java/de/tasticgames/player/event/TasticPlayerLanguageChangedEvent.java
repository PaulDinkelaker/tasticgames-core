package de.tasticgames.player.event;

import de.tasticgames.localization.SupportedLanguage;
import de.tasticgames.player.TasticPlayer;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.Objects;

public final class TasticPlayerLanguageChangedEvent
        extends Event {

    private static final HandlerList HANDLERS =
            new HandlerList();

    private final TasticPlayer tasticPlayer;
    private final SupportedLanguage previousLanguage;
    private final SupportedLanguage newLanguage;

    public TasticPlayerLanguageChangedEvent(
            TasticPlayer tasticPlayer,
            SupportedLanguage previousLanguage,
            SupportedLanguage newLanguage
    ) {
        super(false);

        this.tasticPlayer = Objects.requireNonNull(
                tasticPlayer,
                "tasticPlayer"
        );

        this.previousLanguage = Objects.requireNonNull(
                previousLanguage,
                "previousLanguage"
        );

        this.newLanguage = Objects.requireNonNull(
                newLanguage,
                "newLanguage"
        );

        if (previousLanguage == newLanguage) {
            throw new IllegalArgumentException(
                    "Previous and new language must be different."
            );
        }
    }

    public TasticPlayer tasticPlayer() {
        return tasticPlayer;
    }

    public SupportedLanguage previousLanguage() {
        return previousLanguage;
    }

    public SupportedLanguage newLanguage() {
        return newLanguage;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
