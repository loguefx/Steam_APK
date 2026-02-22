package com.winlator.gamehubpp;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Config pack: manifest, rules, profiles. Downloaded from cloud, staged as Candidate only.
 */
public final class ConfigPack {
    public final String packId;
    public final long createdAt;
    public final int minAppVersion;
    public final int profilesVersion;
    public final String checksumSha256;
    public final List<RuleMatch> rules;
    public final Map<String, Profile> profiles;
    public final String notes;

    public ConfigPack(String packId, long createdAt, int minAppVersion, int profilesVersion,
                      String checksumSha256, List<RuleMatch> rules, Map<String, Profile> profiles, String notes) {
        this.packId = packId;
        this.createdAt = createdAt;
        this.minAppVersion = minAppVersion;
        this.profilesVersion = profilesVersion;
        this.checksumSha256 = checksumSha256;
        this.rules = rules != null ? rules : new ArrayList<>();
        this.profiles = profiles != null ? profiles : new HashMap<>();
        this.notes = notes != null ? notes : "";
    }

    public static final class RuleMatch {
        public final String exeSha256;   // optional, empty = any
        public final List<String> gpuFamily;
        public final int androidMin;
        public final String requiredCompatLayer; // optional
        public final String profileId;

        public RuleMatch(String exeSha256, List<String> gpuFamily, int androidMin, String requiredCompatLayer, String profileId) {
            this.exeSha256 = exeSha256 != null ? exeSha256 : "";
            this.gpuFamily = gpuFamily != null ? gpuFamily : new ArrayList<>();
            this.androidMin = androidMin;
            this.requiredCompatLayer = requiredCompatLayer != null ? requiredCompatLayer : "";
            this.profileId = profileId;
        }
    }

    public static ConfigPack fromJson(JSONObject manifest, JSONObject rulesJson, JSONObject profilesJson, String notes) throws JSONException {
        String packId = manifest.optString("pack_id", "");
        long createdAt = manifest.optLong("created_at", System.currentTimeMillis());
        int minAppVersion = manifest.optInt("min_app_version", 1);
        int profilesVersion = manifest.optInt("profiles_version", 1);
        String checksum = "";
        if (manifest.has("checksum")) checksum = manifest.optJSONObject("checksum").optString("sha256", "");

        List<RuleMatch> rules = new ArrayList<>();
        JSONArray arr = rulesJson != null ? rulesJson.optJSONArray("matches") : null;
        if (arr != null) {
            for (int i = 0; i < arr.length(); i++) {
                JSONObject m = arr.optJSONObject(i);
                if (m == null) continue;
                JSONObject game = m.optJSONObject("game");
                JSONObject device = m.optJSONObject("device");
                JSONObject requires = m.optJSONObject("requires");
                String exeSha = game != null ? game.optString("exe_sha256", "") : "";
                List<String> gpu = new ArrayList<>();
                if (device != null && device.has("gpu_family")) {
                    JSONArray a = device.optJSONArray("gpu_family");
                    if (a != null) for (int j = 0; j < a.length(); j++) gpu.add(a.optString(j, ""));
                }
                int androidMin = device != null ? device.optInt("android_min", 26) : 26;
                String reqCompat = requires != null ? requires.optString("compat_layer", "") : "";
                String profileId = m.optString("profile_id", "");
                if (!profileId.isEmpty()) rules.add(new RuleMatch(exeSha, gpu, androidMin, reqCompat, profileId));
            }
        }

        Map<String, Profile> profiles = new HashMap<>();
        if (profilesJson != null) {
            JSONObject data = profilesJson.optJSONObject("profiles");
            if (data == null) data = profilesJson;
            java.util.Iterator<String> it = data.keys();
            while (it.hasNext()) {
                String key = it.next();
                JSONObject p = data.optJSONObject(key);
                if (p != null) {
                    try {
                        if (!p.has("id")) p.put("id", key);
                        Profile prof = Profile.fromJson(p);
                        profiles.put(prof.id, prof);
                    } catch (Exception ignored) {}
                }
            }
        }

        return new ConfigPack(packId, createdAt, minAppVersion, profilesVersion, checksum, rules, profiles, notes);
    }
}
