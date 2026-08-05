package de.tasticgames.service;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stores and resolves shared TasticCore services by their public service type.
 *
 * <p>Each service type may only be registered once.</p>
 */
public final class ServiceRegistry {

    private final Map<Class<? extends Service>, Service> services =
            new ConcurrentHashMap<>();

    /**
     * Registers a service implementation under its public service type.
     *
     * @param serviceType public service interface or class
     * @param service implementation instance
     * @param <T> service type
     * @throws IllegalStateException if the type is already registered
     */
    public <T extends Service> void register(
            Class<T> serviceType,
            T service
    ) {
        Objects.requireNonNull(serviceType, "serviceType");
        Objects.requireNonNull(service, "service");

        Service existingService = services.putIfAbsent(serviceType, service);

        if (existingService != null) {
            throw new IllegalStateException(
                    "Service is already registered: " + serviceType.getName()
            );
        }
    }

    /**
     * Returns the registered service.
     *
     * @param serviceType requested service type
     * @param <T> service type
     * @return registered service
     * @throws IllegalStateException if no service is registered
     */
    public <T extends Service> T require(Class<T> serviceType) {
        Objects.requireNonNull(serviceType, "serviceType");

        Service service = services.get(serviceType);

        if (service == null) {
            throw new IllegalStateException(
                    "Required service is not registered: " + serviceType.getName()
            );
        }

        return serviceType.cast(service);
    }

    /**
     * Returns the registered service when available.
     */
    public <T extends Service> Optional<T> optional(Class<T> serviceType) {
        Objects.requireNonNull(serviceType, "serviceType");

        return Optional.ofNullable(services.get(serviceType))
                .map(serviceType::cast);
    }

    /**
     * Checks whether a service type is registered.
     */
    public boolean contains(Class<? extends Service> serviceType) {
        Objects.requireNonNull(serviceType, "serviceType");

        return services.containsKey(serviceType);
    }

    /**
     * Removes and returns a registered service.
     */
    public <T extends Service> Optional<T> unregister(Class<T> serviceType) {
        Objects.requireNonNull(serviceType, "serviceType");

        return Optional.ofNullable(services.remove(serviceType))
                .map(serviceType::cast);
    }

    /**
     * Returns the number of registered services.
     */
    public int size() {
        return services.size();
    }

    /**
     * Removes all registered services.
     *
     * <p>This method does not call {@link Service#stop()}.</p>
     */
    public void clear() {
        services.clear();
    }
}
