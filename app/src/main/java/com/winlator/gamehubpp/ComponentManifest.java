package com.winlator.gamehubpp;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Component manifest schema (stored per component in manifest.json).
 * Immutable once installed.
 */
public final class ComponentManifest {
    public static final String TYPE_COMPAT = "compat_layer";
    public static final String TYPE_DRIVER = "gpu_driver";
    public static final String TYPE_TRANSLATOR = "translator";

    public final String id;
    public final String type;
    public final String version;
    public final String channel;
    public final List<String> abis;
    public final int minAndroid;
    public final List<String> supportedGpus;
    public final String sha256;

    public ComponentManifest(String id, String type, String version, String channel,
                             List<String> abis, int minAndroid, List<String> supportedGpus, String sha256) {
        this.id = id;
        this.type = type;
        this.version = version;
        this.channel = channel != null ? channel : "stable";
        this.abis = abis != null ? abis : new ArrayList<>();
        this.minAndroid = minAndroid;
        this.supportedGpus = supportedGpus != null ? supportedGpus : new ArrayList<>();
        this.sha256 = sha256;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("id", id);
        o.put("type", type);
        o.put("version", version);
        o.put("channel", channel);
        JSONArray a = new JSONArray();
        for (String abi : abis) a.put(abi);
        o.put("abi", a);
        o.put("min_android", minAndroid);
        a = new JSONArray();
        for (String gpu : supportedGpus) a.put(gpu);
        o.put("supported_gpus", a);
        if (sha256 != null) o.put("checksum", new JSONObject().put("sha256", sha256));
        return o;
    }

    public static ComponentManifest fromJson(JSONObject o) throws JSONException {
        List<String> abis = new ArrayList<>();
        JSONArray arr = o.optJSONArray("abi");
        if (arr != null) for (int i = 0; i < arr.length(); i++) abis.add(arr.getString(i));
        List<String> gpus = new ArrayList<>();
        arr = o.optJSONArray("supported_gpus");
        if (arr != null) for (int i = 0; i < arr.length(); i++) gpus.add(arr.getString(i));
        String sha = null;
        if (o.has("checksum")) sha = o.getJSONObject("checksum").optString("sha256", null);
        return new ComponentManifest(
            o.getString("id"),
            o.getString("type"),
            o.optString("version", ""),
            o.optString("channel", "stable"),
            abis,
            o.optInt("min_android", 26),
            gpus,
            sha
        );
    }
}
