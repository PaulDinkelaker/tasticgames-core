package de.tasticgames.player.internal;

import de.tasticgames.account.AccountProfile;
import de.tasticgames.player.PlayerRuntimeContext;
import de.tasticgames.player.PlayerState;
import de.tasticgames.player.TasticPlayer;
import de.tasticgames.settings.PlayerSettings;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class PlayerSession implements TasticPlayer {

    private final AccountProfile accountProfile;
    private final PlayerSettings settings;
    private final PlayerRuntimeContext runtimeContext;

    private volatile PlayerState state;

    public PlayerSession(
            AccountProfile accountProfile,
            PlayerSettings settings,
            PlayerRuntimeContext runtimeContext
    ) {
        this.accountProfile = Objects.requireNonNull(
                accountProfile,
                "accountProfile"
        );

        this.settings = Objects.requireNonNull(
                settings,
                "settings"
        );

        this.runtimeContext = Objects.requireNonNull(
                runtimeContext,
                "runtimeContext"
        );

        this.state = PlayerState.LOADING;
    }

    public AccountProfile accountProfile() {
        return accountProfile;
    }

    @Override
    public PlayerRuntimeContext runtime() {
        return runtimeContext;
    }

    @Override
    public long accountId() {
        return accountProfile.accountId();
    }

    @Override
    public UUID minecraftUuid() {
        return accountProfile.minecraftUuid();
    }

    @Override
    public String username() {
        return accountProfile.username();
    }

    @Override
    public String language() {
        return accountProfile.language();
    }

    @Override
    public String accountStatus() {
        return accountProfile.accountStatus();
    }

    @Override
    public Instant firstSeenAt() {
        return accountProfile.firstSeenAt();
    }

    @Override
    public Instant lastSeenAt() {
        return accountProfile.lastSeenAt();
    }

    @Override
    public Instant createdAt() {
        return accountProfile.createdAt();
    }

    @Override
    public Instant updatedAt() {
        return accountProfile.updatedAt();
    }

    @Override
    public PlayerSettings settings() {
        return settings;
    }

    @Override
    public PlayerState state() {
        return state;
    }

    @Override
    public boolean ready() {
        return state == PlayerState.READY;
    }

    public void state(
            PlayerState state
    ) {
        PlayerState validatedState = Objects.requireNonNull(
                state,
                "state"
        );

        this.state = validatedState;

        if (validatedState == PlayerState.UNLOADING) {
            runtimeContext.beginUnload();
        }
    }
}
