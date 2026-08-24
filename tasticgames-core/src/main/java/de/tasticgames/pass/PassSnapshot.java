package de.tasticgames.pass;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Vollständiger Pass-Zustand eines Spielers.
 *
 * <p>Läuft keine Season, antwortet die API trotzdem erfolgreich und
 * {@code seasonActive} ist {@code false}; die Season-Felder sind dann
 * nicht gesetzt.</p>
 */
public record PassSnapshot(
        boolean seasonActive,
        String seasonKey,
        String seasonName,
        int level,
        int maxLevel,
        long totalXp,
        long xpIntoLevel,
        long xpForNextLevel,
        boolean premium,
        int premiumPriceCents,
        String currency,
        Instant seasonEndsAt,
        List<PassClaimSnapshot> claims,
        List<PassQuestSnapshot> quests,
        List<String> features,
        List<String> achievements,
        double xpMultiplier,
        Instant xpMultiplierUntil,
        long version
) {

    public PassSnapshot {
        claims = claims == null
                ? List.of()
                : List.copyOf(claims);

        quests = quests == null
                ? List.of()
                : List.copyOf(quests);

        features = features == null
                ? List.of()
                : List.copyOf(features);

        achievements = achievements == null
                ? List.of()
                : List.copyOf(achievements);
    }

    /**
     * Leerer Zustand ohne aktive Season.
     *
     * <p>Wird verwendet, solange der Pass nicht verfügbar ist, damit
     * aufrufende Module keine Fehlerbehandlung benötigen.</p>
     */
    public static PassSnapshot inactive() {
        return new PassSnapshot(
                false,
                null,
                null,
                0,
                0,
                0L,
                0L,
                0L,
                false,
                0,
                null,
                null,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                1.0d,
                null,
                0L
        );
    }

    /**
     * Kopie mit fortgeschriebenem Level- und XP-Stand.
     */
    public PassSnapshot withProgress(
            int level,
            long totalXp,
            long xpIntoLevel,
            long xpForNextLevel
    ) {
        return new PassSnapshot(
                seasonActive,
                seasonKey,
                seasonName,
                level,
                maxLevel,
                totalXp,
                xpIntoLevel,
                xpForNextLevel,
                premium,
                premiumPriceCents,
                currency,
                seasonEndsAt,
                claims,
                quests,
                features,
                achievements,
                xpMultiplier,
                xpMultiplierUntil,
                version
        );
    }

    /**
     * Kopie, in der die Quest mit demselben Schlüssel ersetzt oder
     * ergänzt wurde.
     */
    public PassSnapshot withQuest(
            PassQuestSnapshot quest
    ) {
        Objects.requireNonNull(
                quest,
                "quest"
        );

        List<PassQuestSnapshot> updatedQuests =
                new ArrayList<>(
                        quests.size() + 1
                );

        boolean replaced = false;

        for (PassQuestSnapshot existingQuest : quests) {
            if (existingQuest.questKey().equals(
                    quest.questKey()
            )) {
                updatedQuests.add(
                        quest
                );

                replaced = true;

                continue;
            }

            updatedQuests.add(
                    existingQuest
            );
        }

        if (!replaced) {
            updatedQuests.add(
                    quest
            );
        }

        return new PassSnapshot(
                seasonActive,
                seasonKey,
                seasonName,
                level,
                maxLevel,
                totalXp,
                xpIntoLevel,
                xpForNextLevel,
                premium,
                premiumPriceCents,
                currency,
                seasonEndsAt,
                claims,
                updatedQuests,
                features,
                achievements,
                xpMultiplier,
                xpMultiplierUntil,
                version
        );
    }

    /**
     * Kopie, die das Achievement zusätzlich enthält.
     */
    public PassSnapshot withAchievement(
            String achievementKey
    ) {
        Objects.requireNonNull(
                achievementKey,
                "achievementKey"
        );

        if (achievements.contains(
                achievementKey
        )) {
            return this;
        }

        List<String> updatedAchievements =
                new ArrayList<>(
                        achievements.size() + 1
                );

        updatedAchievements.addAll(
                achievements
        );

        updatedAchievements.add(
                achievementKey
        );

        return new PassSnapshot(
                seasonActive,
                seasonKey,
                seasonName,
                level,
                maxLevel,
                totalXp,
                xpIntoLevel,
                xpForNextLevel,
                premium,
                premiumPriceCents,
                currency,
                seasonEndsAt,
                claims,
                quests,
                features,
                updatedAchievements,
                xpMultiplier,
                xpMultiplierUntil,
                version
        );
    }
}
