package de.tasticgames.api;

import de.tasticgames.onboarding.OnboardingStep;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record OnboardingSnapshot(
        long accountId,
        UUID minecraftUuid,
        OnboardingStep currentStep,
        boolean languageSelected,
        boolean completed,
        Instant languageSelectedAt,
        Instant completedAt
) {

    public OnboardingSnapshot {
        if (accountId <= 0) {
            throw new IllegalArgumentException(
                    "accountId must be positive."
            );
        }

        Objects.requireNonNull(
                minecraftUuid,
                "minecraftUuid"
        );

        Objects.requireNonNull(
                currentStep,
                "currentStep"
        );
    }
}
