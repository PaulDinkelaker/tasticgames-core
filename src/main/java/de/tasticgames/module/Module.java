package de.tasticgames.module;

import de.tasticgames.lifecycle.Lifecycle;

import java.util.Set;

/**
 * Represents an independently managed TasticCore feature.
 */
public interface Module extends Lifecycle {

    /**
     * Returns the unique identifier of this module.
     */
    String id();

    /**
     * Returns the identifiers of modules that must start before this module.
     */
    default Set<String> dependencies() {
        return Set.of();
    }
}
