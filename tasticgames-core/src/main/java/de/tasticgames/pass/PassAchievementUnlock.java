package de.tasticgames.pass;

/**
 * Ergebnis einer idempotenten Achievement-Freischaltung.
 *
 * <p>{@code unlockedNow} ist {@code false}, wenn das Achievement bereits
 * freigeschaltet war.</p>
 */
public record PassAchievementUnlock(
        boolean unlockedNow,
        String achievementKey,
        PassXpResult xp
) {
}
