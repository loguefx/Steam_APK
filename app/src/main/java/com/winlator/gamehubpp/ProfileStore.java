package com.winlator.gamehubpp;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Persists per-game profiles (Safe/Candidate/LKG), pinning, and history.
 * On first run, Safe baseline is used if no LKG exists.
 */
public final class ProfileStore {
    private static final String PROFILES_DIR = "profiles";
    private static final String GAME_STATE_PREFIX = "game_";
    private static final String PROFILE_PREFIX = "profile_";
    private static final int MAX_HISTORY = 10;

    private final File profilesRoot;
    private final Context context;

    public ProfileStore(Context context) {
        this.context = context.getApplicationContext();
        this.profilesRoot = new File(context.getExternalFilesDir(null), PROFILES_DIR);
        if (!profilesRoot.exists()) profilesRoot.mkdirs();
    }

    /** Ensure the default Safe profile exists on disk. Call on first use. */
    public void ensureDefaultSafeProfile() {
        if (loadProfile("profile_safe_v1") != null) return;
        saveProfile(ProfileStore.createDefaultSafeProfile());
    }

    /** Get or create game state for gameId. Safe profile id is required for new games. */
    public GameProfileState getOrCreateGameState(String gameId, String defaultSafeProfileId) {
        ensureDefaultSafeProfile();
        GameProfileState state = loadGameState(gameId);
        if (state != null) return state;
        GameProfileState newState = new GameProfileState(
            gameId,
            defaultSafeProfileId,
            "",
            defaultSafeProfileId,
            false,
            new ArrayList<>()
        );
        saveGameState(newState);
        return newState;
    }

    /** List all game IDs that have stored state (for applying pack rules). */
    public List<String> listGameIds() {
        List<String> out = new ArrayList<>();
        File[] files = profilesRoot.listFiles((d, name) -> name.startsWith(GAME_STATE_PREFIX) && name.endsWith(".json"));
        if (files == null) return out;
        for (File f : files) {
            String name = f.getName();
            out.add(name.substring(GAME_STATE_PREFIX.length(), name.length() - 5));
        }
        return out;
    }

    public GameProfileState loadGameState(String gameId) {
        File f = new File(profilesRoot, GAME_STATE_PREFIX + gameId + ".json");
        if (!f.isFile()) return null;
        try {
            FileInputStream is = new FileInputStream(f);
            byte[] buf = new byte[(int) f.length()];
            is.read(buf);
            is.close();
            JSONObject o = new JSONObject(new String(buf, "UTF-8"));
            return GameProfileState.fromJson(o);
        } catch (Exception e) {
            return null;
        }
    }

    public void saveGameState(GameProfileState state) {
        try {
            File f = new File(profilesRoot, GAME_STATE_PREFIX + state.gameId + ".json");
            JSONObject o = state.toJson();
            FileOutputStream os = new FileOutputStream(f);
            os.write(o.toString().getBytes("UTF-8"));
            os.close();
        } catch (Exception ignored) {}
    }

    /** Set LKG for game and optionally add to history. */
    public void setLkg(String gameId, String profileId) {
        GameProfileState state = loadGameState(gameId);
        if (state == null) return;
        List<String> history = new ArrayList<>(state.history);
        if (!history.isEmpty() && history.get(0).equals(profileId)) { /* already latest */ }
        else {
            history.add(0, profileId);
            if (history.size() > MAX_HISTORY) history = history.subList(0, MAX_HISTORY);
        }
        GameProfileState updated = new GameProfileState(
            gameId, state.safeProfileId, state.candidateProfileId, profileId, state.pinned, history
        );
        saveGameState(updated);
    }

    /** Set Candidate for game (from pack). Clear candidate if null. */
    public void setCandidate(String gameId, String profileId) {
        GameProfileState state = loadGameState(gameId);
        if (state == null) return;
        GameProfileState updated = new GameProfileState(
            gameId, state.safeProfileId, profileId != null ? profileId : "", state.lkgProfileId, state.pinned, state.history
        );
        saveGameState(updated);
    }

    /** Rollback: set LKG to previous in history and clear candidate. */
    public void rollbackToPreviousLkg(String gameId) {
        GameProfileState state = loadGameState(gameId);
        if (state == null) return;
        List<String> history = new ArrayList<>(state.history);
        String newLkg = state.safeProfileId;
        if (history.size() > 1) {
            history.remove(0);
            newLkg = history.get(0);
        }
        GameProfileState updated = new GameProfileState(
            gameId, state.safeProfileId, "", newLkg, state.pinned, history
        );
        saveGameState(updated);
    }

    /** Set pinned. */
    public void setPinned(String gameId, boolean pinned) {
        GameProfileState state = loadGameState(gameId);
        if (state == null) return;
        GameProfileState updated = new GameProfileState(
            gameId, state.safeProfileId, state.candidateProfileId, state.lkgProfileId, pinned, state.history
        );
        saveGameState(updated);
    }

    /** Load a profile by id from profiles dir. */
    public Profile loadProfile(String profileId) {
        File f = new File(profilesRoot, PROFILE_PREFIX + profileId + ".json");
        if (!f.isFile()) return null;
        try {
            FileInputStream is = new FileInputStream(f);
            byte[] buf = new byte[(int) f.length()];
            is.read(buf);
            is.close();
            return Profile.fromJson(new JSONObject(new String(buf, "UTF-8")));
        } catch (Exception e) {
            return null;
        }
    }

    public void saveProfile(Profile profile) {
        try {
            File f = new File(profilesRoot, PROFILE_PREFIX + profile.id + ".json");
            FileOutputStream os = new FileOutputStream(f);
            os.write(profile.toJson().toString().getBytes("UTF-8"));
            os.close();
        } catch (Exception ignored) {}
    }

    /** Returns the default Safe profile (bundled baseline). Caller can create from assets or defaults. */
    public static Profile createDefaultSafeProfile() {
        Map<String, String> comp = new HashMap<>();
        comp.put("compat_layer", "");
        comp.put("gpu_driver", "");
        comp.put("translator", "");
        Map<String, Object> set = new HashMap<>();
        set.put("translation_preset", "stable");
        set.put("surface_format", "BGRA8");
        return new Profile("profile_safe_v1", "Safe (Stable)", "safe", comp, set, new HashMap<>());
    }
}
