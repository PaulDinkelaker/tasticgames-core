package de.tasticgames.pass;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Die aktive Season mit ihrer vollständigen Belohnungsstrecke, dem
 * Quest-Katalog und den Tageslimits je XP-Quelle.
 *
 * <p>TasticCore hält immer nur die ACTIVE Season vorrätig und
 * aktualisiert sie zyklisch im Hintergrund.</p>
 */
public record PassSeasonSnapshot(
        String key,
        String displayName,
        int maxLevel,
        int xpBase,
        int xpGrowth,
        int premiumPriceCents,
        String currency,
        Instant startsAt,
        Instant endsAt,
        List<PassTierSnapshot> tiers,
        List<PassQuestDefinition> quests,
        Map<PassXpSource, Integer> dailyXpCaps
) {

    public PassSeasonSnapshot {
        Objects.requireNonNull(
                key,
                "key"
        );

        tiers = tiers == null
                ? List.of()
                : List.copyOf(tiers);

        quests = quests == null
                ? List.of()
                : List.copyOf(quests);

        dailyXpCaps = dailyXpCaps == null
                ? Map.of()
                : Map.copyOf(dailyXpCaps);
    }
}
