package com.winlator.gamehubpp;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Session result for CrashMonitor: launch_success, stability_pass, exit_reason.
 */
public final class SessionRecord {
    public static final int LAUNCH_SUCCESS_SECONDS = 20;
    public static final int STABILITY_PASS_SECONDS = 300;
    public static final int QUICK_CRASH_SECONDS = 60;

    public final String gameId;
    public final String profileId;
    public final long startedAt;
    public final long endedAt;
    public final String exitReason; // "normal" | "crash" | "anr" | "unknown"
    public final int runtimeSeconds;

    public SessionRecord(String gameId, String profileId, long startedAt, long endedAt,
                         String exitReason, int runtimeSeconds) {
        this.gameId = gameId;
        this.profileId = profileId;
        this.startedAt = startedAt;
        this.endedAt = endedAt;
        this.exitReason = exitReason != null ? exitReason : "unknown";
        this.runtimeSeconds = runtimeSeconds;
    }

    public boolean launchSuccess() { return runtimeSeconds >= LAUNCH_SUCCESS_SECONDS; }
    public boolean stabilityPass() { return runtimeSeconds >= STABILITY_PASS_SECONDS; }
    public boolean quickCrash() { return runtimeSeconds < QUICK_CRASH_SECONDS && !"normal".equals(exitReason); }

    public JSONObject toJson() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("game_id", gameId);
        o.put("profile_id", profileId);
        o.put("started_at", startedAt);
        o.put("ended_at", endedAt);
        o.put("exit_reason", exitReason);
        o.put("runtime_seconds", runtimeSeconds);
        return o;
    }

    public static SessionRecord fromJson(JSONObject o) throws JSONException {
        return new SessionRecord(
            o.getString("game_id"),
            o.getString("profile_id"),
            o.getLong("started_at"),
            o.getLong("ended_at"),
            o.optString("exit_reason", "unknown"),
            o.optInt("runtime_seconds", 0)
        );
    }
}
