package de.tasticgames;

import de.tasticgames.api.TasticCoreApi;
import de.tasticgames.bootstrap.CoreBootstrap;
import org.bukkit.plugin.java.JavaPlugin;

public final class TasticCorePlugin extends JavaPlugin {

    private CoreBootstrap bootstrap;
    private static TasticCorePlugin instance;

    @Override
    public void onEnable() {
        instance = this;
        try {
            bootstrap = new CoreBootstrap(this);
            bootstrap.start();
        } catch (Exception exception) {
            getLogger().severe("TasticCore failed to start.");
            exception.printStackTrace();

            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        if (bootstrap == null) {
            return;
        }

        try {
            bootstrap.stop();
        } catch (Exception exception) {
            getLogger().severe("TasticCore failed to stop cleanly.");
            exception.printStackTrace();
        } finally {
            bootstrap = null;
        }
        instance = null;
    }

    public static TasticCorePlugin instance() {
        TasticCorePlugin current = instance;

        if (current == null) {
            throw new IllegalStateException(
                    "TasticCore is not enabled."
            );
        }

        return current;
    }

    public TasticCoreApi api() {
        if (bootstrap == null) {
            throw new IllegalStateException(
                    "TasticCore bootstrap is not initialized."
            );
        }

        return bootstrap.coreApi();
    }
}
