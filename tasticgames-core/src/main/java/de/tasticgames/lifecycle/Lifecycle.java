package de.tasticgames.lifecycle;

/**
 * Defines the controlled startup and shutdown lifecycle of a TasticCore component.
 */
public interface Lifecycle {

    /**
     * Starts this component.
     *
     * @throws Exception when startup fails
     */
    void start() throws Exception;

    /**
     * Stops this component and releases its resources.
     *
     * @throws Exception when shutdown fails
     */
    void stop() throws Exception;
}
