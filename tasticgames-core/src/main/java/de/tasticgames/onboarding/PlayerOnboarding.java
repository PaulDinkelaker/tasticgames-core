package de.tasticgames.onboarding;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class PlayerOnboarding {

    private final long accountId;
    private final UUID minecraftUuid;

    private OnboardingStep currentStep;
    private boolean languageSelected;
    private boolean completed;

    private Instant languageSelectedAt;
    private Instant completedAt;

    public PlayerOnboarding(
            long accountId,
            UUID minecraftUuid,
            OnboardingStep currentStep,
            boolean languageSelected,
            boolean completed,
            Instant languageSelectedAt,
            Instant completedAt
    ) {
        if (accountId <= 0) {
            throw new IllegalArgumentException(
                    "accountId must be positive."
            );
        }

        this.accountId = accountId;

        this.minecraftUuid = Objects.requireNonNull(
                minecraftUuid,
                "minecraftUuid"
        );

        this.currentStep = Objects.requireNonNull(
                currentStep,
                "currentStep"
        );

        this.languageSelected = languageSelected;
        this.completed = completed;
        this.languageSelectedAt = languageSelectedAt;
        this.completedAt = completedAt;
    }

    public long accountId() {
        return accountId;
    }

    public UUID minecraftUuid() {
        return minecraftUuid;
    }

    public OnboardingStep currentStep() {
        return currentStep;
    }

    public boolean languageSelected() {
        return languageSelected;
    }

    public boolean completed() {
        return completed;
    }

    public Instant languageSelectedAt() {
        return languageSelectedAt;
    }

    public Instant completedAt() {
        return completedAt;
    }

    public void apply(
            OnboardingStep currentStep,
            boolean languageSelected,
            boolean completed,
            Instant languageSelectedAt,
            Instant completedAt
    ) {
        this.currentStep = Objects.requireNonNull(
                currentStep,
                "currentStep"
        );

        this.languageSelected = languageSelected;
        this.completed = completed;
        this.languageSelectedAt = languageSelectedAt;
        this.completedAt = completedAt;
    }
}
