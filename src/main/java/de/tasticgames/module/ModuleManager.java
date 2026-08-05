package de.tasticgames.module;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Registers and manages TasticCore modules.
 *
 * <p>The insertion order is preserved to keep startup and shutdown
 * behavior deterministic.</p>
 */
public final class ModuleManager {

    private final Map<String, Module> modules = new LinkedHashMap<>();

    /**
     * Registers a module.
     *
     * @throws IllegalStateException if a module with the same ID already exists
     */
    public void register(Module module) {
        Objects.requireNonNull(module, "module");

        String moduleId = validateId(module.id());

        if (modules.containsKey(moduleId)) {
            throw new IllegalStateException(
                    "Module is already registered: " + moduleId
            );
        }

        modules.put(moduleId, module);
    }

    /**
     * Returns a required module by ID.
     *
     * @throws IllegalStateException if the module is not registered
     */
    public Module require(String moduleId) {
        String validatedId = validateId(moduleId);
        Module module = modules.get(validatedId);

        if (module == null) {
            throw new IllegalStateException(
                    "Required module is not registered: " + validatedId
            );
        }

        return module;
    }

    /**
     * Returns a module when registered.
     */
    public Optional<Module> optional(String moduleId) {
        return Optional.ofNullable(modules.get(validateId(moduleId)));
    }

    /**
     * Checks whether a module is registered.
     */
    public boolean contains(String moduleId) {
        return modules.containsKey(validateId(moduleId));
    }

    /**
     * Removes a module without stopping it.
     */
    public Optional<Module> unregister(String moduleId) {
        return Optional.ofNullable(
                modules.remove(validateId(moduleId))
        );
    }

    /**
     * Returns an immutable snapshot of all registered modules.
     */
    public Collection<Module> modules() {
        return List.copyOf(modules.values());
    }

    public int size() {
        return modules.size();
    }

    public void clear() {
        modules.clear();
    }

    private String validateId(String moduleId) {
        Objects.requireNonNull(moduleId, "moduleId");

        String normalizedId = moduleId.trim().toLowerCase();

        if (normalizedId.isBlank()) {
            throw new IllegalArgumentException("Module ID must not be blank.");
        }

        if (!normalizedId.matches("[a-z0-9._-]+")) {
            throw new IllegalArgumentException(
                    "Invalid module ID: " + moduleId
            );
        }

        return normalizedId;
    }
}
