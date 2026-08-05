package de.tasticgames.service;

import de.tasticgames.lifecycle.Lifecycle;

/**
 * Represents reusable functionality that can be consumed by other services and modules.
 */
public interface Service extends Lifecycle {

    /**
     * Returns the unique identifier of this service.
     */
    String id();
}
