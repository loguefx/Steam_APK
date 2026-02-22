package com.winlator.gamehubpp;

import android.content.Context;

import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

/**
 * Tracks session results and one-time fallback: if profile crashes quickly, next launch use Safe once.
 * Promotes Candidate to LKG on stability_pass; rollback on crash.
 */
public final class CrashMonitor {
    private static final String PREF_USE_SAFE_NEXT_RUN = "gamehubpp_use_safe_next_run_";
    private static final String SESSIONS_DIR = "sessions";

    private final Context context;
    private final ProfileStore profileStore;
    private final File sessionsDir;

    public CrashMonitor(Context context, ProfileStore profileStore) {
        this.context = context.getApplicationContext();
        this.profileStore = profileStore;
        this.sessionsDir = new File(context.getExternalFilesDir(null), SESSIONS_DIR);
        if (!sessionsDir.exists()) sessionsDir.mkdirs();
    }

    /** Call before launch: should we force Safe profile this run (one-time fallback)? */
    public boolean shouldUseSafeNextRun(String gameId) {
        return context.getSharedPreferences("gamehubpp", Context.MODE_PRIVATE)
            .getBoolean(PREF_USE_SAFE_NEXT_RUN + gameId, false);
    }

    /** Call when deciding to use Safe for this launch: consume the one-time flag. */
    public void clearUseSafeNextRun(String gameId) {
        context.getSharedPreferences("gamehubpp", Context.MODE_PRIVATE)
            .edit()
            .remove(PREF_USE_SAFE_NEXT_RUN + gameId)
            .apply();
    }

    /** Set the one-time "use Safe next run" flag (after a quick crash). */
    public void setUseSafeNextRun(String gameId) {
        context.getSharedPreferences("gamehubpp", Context.MODE_PRIVATE)
            .edit()
            .putBoolean(PREF_USE_SAFE_NEXT_RUN + gameId, true)
            .apply();
    }

    /** Record session end. Call when game process exits. */
    public void recordSessionEnd(String gameId, String profileId, long startedAt, String exitReason) {
        long endedAt = System.currentTimeMillis();
        int runtimeSeconds = (int) ((endedAt - startedAt) / 1000);
        SessionRecord record = new SessionRecord(gameId, profileId, startedAt, endedAt, exitReason, runtimeSeconds);

        try {
            File f = new File(sessionsDir, gameId + "_" + endedAt + ".json");
            FileOutputStream os = new FileOutputStream(f);
            os.write(record.toJson().toString().getBytes("UTF-8"));
            os.close();
        } catch (Exception ignored) {}

        GameProfileState state = profileStore.loadGameState(gameId);
        if (state == null) return;

        boolean wasCandidate = profileId != null && profileId.equals(state.candidateProfileId);

        if (record.stabilityPass()) {
            if (wasCandidate) {
                profileStore.setLkg(gameId, profileId);
                profileStore.setCandidate(gameId, null);
            }
            return;
        }

        if (record.quickCrash()) {
            setUseSafeNextRun(gameId);
            if (wasCandidate) {
                profileStore.rollbackToPreviousLkg(gameId);
                profileStore.setCandidate(gameId, null);
            }
        }
    }

    /** Determine exit reason from process exit code (simplified). */
    public static String exitReasonFromCode(int exitCode) {
        if (exitCode == 0) return "normal";
        if (exitCode == 137 || exitCode == 143) return "anr";
        return "crash";
    }
}
