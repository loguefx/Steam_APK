package com.winlator.gamehubpp;

import android.content.Context;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Resolves the active profile for launch: pinned → LKG → Candidate (if opted-in) → Safe.
 * Verifies referenced components exist; returns paths and env for Launcher.
 */
public final class Resolver {
    private final Context context;
    private final ProfileStore profileStore;
    private final ComponentManager componentManager;
    private final String defaultSafeProfileId;

    public Resolver(Context context, ProfileStore profileStore, ComponentManager componentManager, String defaultSafeProfileId) {
        this.context = context.getApplicationContext();
        this.profileStore = profileStore;
        this.componentManager = componentManager;
        this.defaultSafeProfileId = defaultSafeProfileId != null ? defaultSafeProfileId : "profile_safe_v1";
    }

    public static final class ResolveResult {
        public final String resolvedProfileId;
        public final Profile profile;
        public final Map<String, File> componentPaths;
        public final Map<String, String> env;
        public final String fallbackReason; // null if none

        public ResolveResult(String resolvedProfileId, Profile profile, Map<String, File> componentPaths,
                             Map<String, String> env, String fallbackReason) {
            this.resolvedProfileId = resolvedProfileId;
            this.profile = profile;
            this.componentPaths = componentPaths != null ? componentPaths : new HashMap<>();
            this.env = env != null ? env : new HashMap<>();
            this.fallbackReason = fallbackReason;
        }
    }

    /**
     * Resolve active profile for game. If useSafeNextRun is true, force Safe for this launch (one-time fallback).
     */
    public ResolveResult resolve(String gameId, boolean useSafeNextRun) {
        GameProfileState state = profileStore.getOrCreateGameState(gameId, defaultSafeProfileId);
        String profileIdToUse;
        String fallbackReason = null;

        if (useSafeNextRun || state.safeProfileId == null || state.safeProfileId.isEmpty()) {
            profileIdToUse = state.safeProfileId.isEmpty() ? defaultSafeProfileId : state.safeProfileId;
            if (useSafeNextRun) fallbackReason = "one_time_safe_fallback";
        } else if (state.pinned) {
            profileIdToUse = state.lkgProfileId.isEmpty() ? state.safeProfileId : state.lkgProfileId;
        } else if (!state.candidateProfileId.isEmpty()) {
            profileIdToUse = state.candidateProfileId;
        } else if (!state.lkgProfileId.isEmpty()) {
            profileIdToUse = state.lkgProfileId;
        } else {
            profileIdToUse = state.safeProfileId.isEmpty() ? defaultSafeProfileId : state.safeProfileId;
        }

        Profile profile = profileStore.loadProfile(profileIdToUse);
        if (profile == null) {
            profileIdToUse = state.safeProfileId.isEmpty() ? defaultSafeProfileId : state.safeProfileId;
            profile = profileStore.loadProfile(profileIdToUse);
            if (profile == null) profile = ProfileStore.createDefaultSafeProfile();
            fallbackReason = "profile_missing";
        }

        Map<String, File> paths = new HashMap<>();
        for (Map.Entry<String, String> e : profile.components.entrySet()) {
            String type = e.getKey();
            String id = e.getValue();
            if (id == null || id.isEmpty()) continue;
            File path = componentManager.getComponentPath(type, id);
            if (path != null) paths.put(type, path);
        }

        Map<String, String> env = new HashMap<>();
        env.put("WINEDEBUG", "-all");
        if (profile.settings.containsKey("translation_preset")) {
            String preset = String.valueOf(profile.settings.get("translation_preset"));
            if ("compatibility".equals(preset)) {
                env.put("FEX_TSOEnabled", "1");
                env.put("FEX_X87ReducedPrecision", "1");
                env.put("FEX_Multiblock", "0");
            } else if ("stable".equals(preset)) {
                env.put("FEX_TSOEnabled", "1");
                env.put("FEX_X87ReducedPrecision", "1");
            }
            // performance: no extra env (use defaults)
        }

        return new ResolveResult(profileIdToUse, profile, paths, env, fallbackReason);
    }
}
