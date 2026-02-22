package com.winlator.gamehubpp;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Per-game profile state: Safe, Candidate, LKG, pinning, history.
 */
public final class GameProfileState {
    public final String gameId;
    public final String safeProfileId;
    public final String candidateProfileId;
    public final String lkgProfileId;
    public final boolean pinned;
    public final List<String> history;

    public GameProfileState(String gameId, String safeProfileId, String candidateProfileId,
                            String lkgProfileId, boolean pinned, List<String> history) {
        this.gameId = gameId;
        this.safeProfileId = safeProfileId;
        this.candidateProfileId = candidateProfileId;
        this.lkgProfileId = lkgProfileId;
        this.pinned = pinned;
        this.history = history != null ? history : new ArrayList<>();
    }

    public JSONObject toJson() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("game_id", gameId);
        o.put("safe", safeProfileId);
        o.put("candidate", candidateProfileId);
        o.put("lkg", lkgProfileId);
        o.put("pinned", pinned);
        JSONArray a = new JSONArray();
        for (String h : history) a.put(h);
        o.put("history", a);
        return o;
    }

    public static GameProfileState fromJson(JSONObject o) throws JSONException {
        List<String> hist = new ArrayList<>();
        JSONArray arr = o.optJSONArray("history");
        if (arr != null) for (int i = 0; i < arr.length(); i++) hist.add(arr.getString(i));
        return new GameProfileState(
            o.getString("game_id"),
            o.optString("safe", ""),
            o.optString("candidate", ""),
            o.optString("lkg", ""),
            o.optBoolean("pinned", false),
            hist
        );
    }
}
