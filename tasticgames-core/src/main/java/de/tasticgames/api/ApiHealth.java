package de.tasticgames.api;

public record ApiHealth(
        String status,
        String service,
        String version
) {
}
