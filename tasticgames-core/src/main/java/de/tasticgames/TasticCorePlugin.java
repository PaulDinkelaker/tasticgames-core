package de.tasticgames;

import de.tasticgames.api.TasticCoreApi;
import de.tasticgames.bootstrap.CoreBootstrap;
import org.bukkit.plugin.java.JavaPlugin;

public final class TasticCorePlugin
        extends JavaPlugin {

    private static TasticCorePlugin instance;

    private CoreBootstrap bootstrap;

    @Override
    public void onEnable() {
        try {
            CoreBootstrap newBootstrap =
                    new CoreBootstrap(
                            this
                    );

            newBootstrap.start();

            bootstrap =
                    newBootstrap;

            instance =
                    this;

        } catch (Exception exception) {
            getLogger().severe(
                    "TasticCore failed to start."
            );

            exception.printStackTrace();

            instance =
                    null;

            bootstrap =
                    null;

            getServer()
                    .getPluginManager()
                    .disablePlugin(
                            this
                    );
        }
    }

    @Override
    public void onDisable() {
        try {
            CoreBootstrap currentBootstrap =
                    bootstrap;

            if (currentBootstrap != null) {
                currentBootstrap.stop();
            }

        } catch (Exception exception) {
            getLogger().severe(
                    "TasticCore failed to stop cleanly."
            );

            exception.printStackTrace();

        } finally {
            bootstrap =
                    null;

            instance =
                    null;
        }
    }

    public static TasticCorePlugin instance() {
        TasticCorePlugin current =
                instance;

        if (current == null
                || !current.isEnabled()) {
            throw new IllegalStateException(
                    "TasticCore is not enabled."
            );
        }

        return current;
    }

    public TasticCoreApi api() {
        CoreBootstrap currentBootstrap =
                bootstrap;

        if (currentBootstrap == null
                || !currentBootstrap.isRunning()) {
            throw new IllegalStateException(
                    "TasticCore bootstrap is not running."
            );
        }

        return currentBootstrap.coreApi();
    }
}
