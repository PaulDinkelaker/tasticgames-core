package de.tasticgames.pass;

/**
 * Ergebnis einer gemeldeten Quest-Fortschrittsmeldung.
 *
 * <p>{@code quest} ist nicht gesetzt, wenn die API keinen Fortschritt
 * führen konnte; {@code xp} nur, wenn der Abschluss XP vergeben hat.</p>
 */
public record PassQuestUpdate(
        PassQuestSnapshot quest,
        boolean completedNow,
        PassXpResult xp
) {
}
