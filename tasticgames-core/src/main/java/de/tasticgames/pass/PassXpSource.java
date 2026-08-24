package de.tasticgames.pass;

/**
 * Quelle vergebener Pass-XP.
 *
 * <p>Die API führt pro Quelle ein eigenes Tageslimit; TasticCore
 * bündelt eingehende XP je Spieler und Quelle, bevor sie gesendet
 * werden.</p>
 */
public enum PassXpSource {

    COOKIE_CLICKS,

    COOKIE_PURCHASE,

    COOKIE_PRESTIGE,

    COOKIE_GOLDEN,

    COOKIE_ZONE,

    QUEST,

    ACHIEVEMENT,

    PLAYTIME,

    EVENT,

    ADMIN,

    SURVIVAL,

    DUELS,

    CREATIVE
}
