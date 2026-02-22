package com.winlator;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Stores Steam sign-in state (Steam ID and signed_in flag).
 * Used by LaunchGateActivity to gate access to the main launcher.
 */
public final class SteamAuthPrefs {
    private static final String PREFS_NAME = "steam_auth";
    private static final String KEY_STEAM_ID = "steam_id";
    private static final String KEY_SIGNED_IN = "signed_in";
    private static final String KEY_DISPLAY_NAME = "display_name";
    private static final String KEY_WEB_API_KEY = "steam_web_api_key";

    private final SharedPreferences prefs;

    public SteamAuthPrefs(Context context) {
        this.prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public boolean isSignedIn() {
        return prefs.getBoolean(KEY_SIGNED_IN, false);
    }

    public String getSteamId() {
        return prefs.getString(KEY_STEAM_ID, null);
    }

    public String getDisplayName() {
        return prefs.getString(KEY_DISPLAY_NAME, null);
    }

    public void setSignedIn(String steamId, String displayName) {
        prefs.edit()
            .putBoolean(KEY_SIGNED_IN, true)
            .putString(KEY_STEAM_ID, steamId)
            .putString(KEY_DISPLAY_NAME, displayName != null ? displayName : "")
            .apply();
    }

    public void signOut() {
        prefs.edit()
            .remove(KEY_SIGNED_IN)
            .remove(KEY_STEAM_ID)
            .remove(KEY_DISPLAY_NAME)
            .apply();
    }

    public String getWebApiKey() {
        return prefs.getString(KEY_WEB_API_KEY, "");
    }

    public void setWebApiKey(String key) {
        prefs.edit().putString(KEY_WEB_API_KEY, key != null ? key : "").apply();
    }
}
