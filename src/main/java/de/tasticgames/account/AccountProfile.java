package de.tasticgames.account;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class AccountProfile {

    private final long accountId;
    private final UUID minecraftUuid;

    private String username;
    private String language;
    private String accountStatus;

    private final Instant firstSeenAt;
    private Instant lastSeenAt;

    private final Instant createdAt;
    private Instant updatedAt;

    public AccountProfile(
            long accountId,
            UUID minecraftUuid,
            String username,
            String language,
            String accountStatus,
            Instant firstSeenAt,
            Instant lastSeenAt,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.accountId = accountId;
        this.minecraftUuid = Objects.requireNonNull(minecraftUuid);

        this.username = Objects.requireNonNull(username);
        this.language = Objects.requireNonNull(language);
        this.accountStatus = Objects.requireNonNull(accountStatus);

        this.firstSeenAt = Objects.requireNonNull(firstSeenAt);
        this.lastSeenAt = Objects.requireNonNull(lastSeenAt);

        this.createdAt = Objects.requireNonNull(createdAt);
        this.updatedAt = Objects.requireNonNull(updatedAt);
    }

    public long accountId() {
        return accountId;
    }

    public UUID minecraftUuid() {
        return minecraftUuid;
    }

    public String username() {
        return username;
    }

    public String language() {
        return language;
    }

    public String accountStatus() {
        return accountStatus;
    }

    public Instant firstSeenAt() {
        return firstSeenAt;
    }

    public Instant lastSeenAt() {
        return lastSeenAt;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    public void username(String username) {
        this.username = Objects.requireNonNull(username);
    }

    public void language(String language) {
        this.language = Objects.requireNonNull(language);
    }

    public void accountStatus(String accountStatus) {
        this.accountStatus = Objects.requireNonNull(accountStatus);
    }

    public void lastSeenAt(Instant lastSeenAt) {
        this.lastSeenAt = Objects.requireNonNull(lastSeenAt);
    }

    public void updatedAt(Instant updatedAt) {
        this.updatedAt = Objects.requireNonNull(updatedAt);
    }
}
