package com.winlator.gamehubpp;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Profile: component IDs + settings. Used by ProfileStore and Resolver.
 */
public final class Profile {
    public final String id;
    public final String name;
    public final String source; // "local" | "pack" | "safe"
    public final Map<String, String> components; // compat_layer, gpu_driver, translator
    public final Map<String, Object> settings;   // translation_preset, fex flags, surface_format, etc.
    public final Map<String, Object> constraints; // device, android_min, gpu_family

    public Profile(String id, String name, String source,
                   Map<String, String> components, Map<String, Object> settings, Map<String, Object> constraints) {
        this.id = id;
        this.name = name;
        this.source = source != null ? source : "local";
        this.components = components != null ? components : new HashMap<>();
        this.settings = settings != null ? settings : new HashMap<>();
        this.constraints = constraints != null ? constraints : new HashMap<>();
    }

    public JSONObject toJson() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("id", id);
        o.put("name", name);
        o.put("source", source);
        JSONObject c = new JSONObject();
        for (Map.Entry<String, String> e : components.entrySet()) c.put(e.getKey(), e.getValue());
        o.put("components", c);
        c = new JSONObject();
        for (Map.Entry<String, Object> e : settings.entrySet()) c.put(e.getKey(), e.getValue());
        o.put("settings", c);
        c = new JSONObject();
        for (Map.Entry<String, Object> e : constraints.entrySet()) c.put(e.getKey(), e.getValue());
        o.put("constraints", c);
        return o;
    }

    public static Profile fromJson(JSONObject o) throws JSONException {
        Map<String, String> comp = new HashMap<>();
        JSONObject c = o.optJSONObject("components");
        if (c != null) {
            java.util.Iterator<String> it = c.keys();
            while (it.hasNext()) { String key = it.next(); comp.put(key, c.optString(key, "")); }
        }
        Map<String, Object> set = new HashMap<>();
        c = o.optJSONObject("settings");
        if (c != null) {
            java.util.Iterator<String> it = c.keys();
            while (it.hasNext()) { String key = it.next(); set.put(key, c.opt(key)); }
        }
        Map<String, Object> cons = new HashMap<>();
        c = o.optJSONObject("constraints");
        if (c != null) {
            java.util.Iterator<String> it = c.keys();
            while (it.hasNext()) { String key = it.next(); cons.put(key, c.opt(key)); }
        }
        return new Profile(
            o.getString("id"),
            o.optString("name", ""),
            o.optString("source", "local"),
            comp,
            set,
            cons
        );
    }
}
