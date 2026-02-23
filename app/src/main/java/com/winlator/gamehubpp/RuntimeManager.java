package com.winlator.gamehubpp;

import android.content.Context;

import com.winlator.container.Container;
import com.winlator.container.ContainerManager;
import com.winlator.container.Shortcut;
import com.winlator.core.Callback;

/**
 * Game Hub-style runtime adapter: UI never mentions "containers."
 * Ensures a runtime (container) exists for a game and returns the container to use.
 * All launch/add-game flows use this instead of exposing container management.
 */
public final class RuntimeManager {
    private final Context context;

    public RuntimeManager(Context context) {
        this.context = context.getApplicationContext();
    }

    /**
     * Returns the container to use for the given shortcut, or null if none and creation failed.
     * For "one default container" strategy this is the shortcut's container if it exists,
     * or the default container (created if needed).
     */
    public Container getRuntimeForShortcut(Shortcut shortcut) {
        if (shortcut != null && shortcut.container != null) return shortcut.container;
        ContainerManager cm = new ContainerManager(context);
        return cm.getDefaultContainer();
    }

    /**
     * Ensures a runtime exists for the given game/shortcut. If no container exists,
     * creates the default container asynchronously and passes it to the callback.
     * Use this before launching or when adding a game.
     */
    public void ensureRuntimeForShortcutAsync(Shortcut shortcut, Callback<Container> callback) {
        if (shortcut != null && shortcut.container != null) {
            callback.call(shortcut.container);
            return;
        }
        ContainerManager cm = new ContainerManager(context);
        cm.getOrCreateDefaultContainerAsync(callback);
    }

    /**
     * Ensures a runtime exists (default container). If none exists, creates it and passes to callback.
     * Use when you have a gameId but not yet a Shortcut (e.g. before creating a shortcut).
     */
    public void ensureRuntimeAsync(Callback<Container> callback) {
        ContainerManager cm = new ContainerManager(context);
        cm.getOrCreateDefaultContainerAsync(callback);
    }

    /**
     * Returns the default container if one exists, otherwise null.
     * Does not create; use ensureRuntimeAsync when creation is acceptable.
     */
    public Container getDefaultRuntime() {
        ContainerManager cm = new ContainerManager(context);
        return cm.getDefaultContainer();
    }
}
