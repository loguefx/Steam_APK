package com.winlator.gamehubpp;

import android.content.Context;

/**
 * Orchestrates launch: resolve profile, validate, track session for CrashMonitor.
 * Use before starting XServerDisplayActivity and when game process exits.
 */
public final class LaunchCoordinator {
    private static final String DEFAULT_SAFE_PROFILE_ID = "profile_safe_v1";

    private static volatile LaunchCoordinator instance;

    public static LaunchCoordinator get(Context context) {
        if (instance == null) {
            synchronized (LaunchCoordinator.class) {
                if (instance == null) instance = new LaunchCoordinator(context.getApplicationContext());
            }
        }
        return instance;
    }

    private final Context context;
    private final ProfileStore profileStore;
    private final ComponentManager componentManager;
    private final CrashMonitor crashMonitor;
    private final DeviceInfo deviceInfo;
    private final Resolver resolver;
    private final Validator validator;

    private String currentGameId;
    private String currentProfileId;
    private long sessionStartedAt;
    private Resolver.ResolveResult currentResolveResult;

    public LaunchCoordinator(Context context) {
        this.context = context.getApplicationContext();
        this.profileStore = new ProfileStore(this.context);
        this.componentManager = new ComponentManager(this.context);
        this.crashMonitor = new CrashMonitor(this.context, profileStore);
        this.deviceInfo = DeviceInfoCollector.collect(this.context);
        this.resolver = new Resolver(this.context, profileStore, componentManager, DEFAULT_SAFE_PROFILE_ID);
        this.validator = new Validator(this.context, componentManager, deviceInfo);
    }

    public ProfileStore getProfileStore() { return profileStore; }
    public ComponentManager getComponentManager() { return componentManager; }
    public CrashMonitor getCrashMonitor() { return crashMonitor; }
    public Resolver getResolver() { return resolver; }
    public Validator getValidator() { return validator; }
    public DeviceInfo getDeviceInfo() { return deviceInfo; }

    /**
     * Prepare for launch. Call before starting the game activity.
     * @param gameId unique game id (e.g. containerId + "_" + exe name)
     * @return resolve result; if validation blocks, result.profile is still set (Safe fallback)
     */
    public Resolver.ResolveResult prepareLaunch(String gameId) {
        profileStore.ensureDefaultSafeProfile();
        boolean useSafe = crashMonitor.shouldUseSafeNextRun(gameId);
        if (useSafe) crashMonitor.clearUseSafeNextRun(gameId);
        Resolver.ResolveResult result = resolver.resolve(gameId, useSafe);
        Validator.ValidationResult vr = validator.validate(result.profile, result);
        if (vr.isBlock()) {
            result = resolver.resolve(gameId, true);
        }
        currentGameId = gameId;
        currentProfileId = result.resolvedProfileId;
        sessionStartedAt = System.currentTimeMillis();
        currentResolveResult = result;
        return result;
    }

    /** Last resolve result for the current launch (so the runner can apply env/paths). Null after onGameExit/onLaunchAborted. */
    public Resolver.ResolveResult getCurrentResolveResult() {
        return currentResolveResult;
    }

    /** Call when game process exits (e.g. from termination callback). */
    public void onGameExit(int exitCode) {
        if (currentGameId == null) return;
        String reason = CrashMonitor.exitReasonFromCode(exitCode);
        crashMonitor.recordSessionEnd(currentGameId, currentProfileId, sessionStartedAt, reason);
        currentGameId = null;
        currentProfileId = null;
        currentResolveResult = null;
    }

    /** Call when activity is destroyed without normal exit (e.g. user backed out). */
    public void onLaunchAborted() {
        currentGameId = null;
        currentProfileId = null;
        currentResolveResult = null;
    }
}
