package de.tasticgames.account.internal;

import de.tasticgames.account.AccountService;
import de.tasticgames.api.ApiClient;
import de.tasticgames.api.MinecraftAccount;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class DefaultAccountService
        implements AccountService {

    private final ApiClient apiClient;

    public DefaultAccountService(
            ApiClient apiClient
    ) {
        this.apiClient = Objects.requireNonNull(
                apiClient,
                "apiClient"
        );
    }

    @Override
    public String id() {
        return "account-service";
    }

    @Override
    public void start() {
        if (!apiClient.enabled()) {
            throw new IllegalStateException(
                    "Cannot start AccountService because ApiClient is disabled."
            );
        }
    }

    @Override
    public void stop() {
        // Aktuell besitzt der Service keine eigenen Ressourcen.
    }

    @Override
    public CompletableFuture<MinecraftAccount> registerLogin(
            UUID minecraftUuid,
            String username
    ) {
        Objects.requireNonNull(
                minecraftUuid,
                "minecraftUuid"
        );

        Objects.requireNonNull(
                username,
                "username"
        );

        return apiClient.registerLogin(
                minecraftUuid,
                username
        );
    }

    @Override
    public CompletableFuture<MinecraftAccount> getAccount(
            UUID minecraftUuid
    ) {
        Objects.requireNonNull(
                minecraftUuid,
                "minecraftUuid"
        );

        return apiClient.getPlayer(
                minecraftUuid
        );
    }
}
