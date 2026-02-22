package com.winlator.gamehubpp;

import android.os.Build;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Device fingerprint for compatibility gating (no prediction).
 * Used by Validator and Resolver.
 */
public final class DeviceInfo {
    public final String deviceModel;
    public final int androidSdk;
    public final String gpuFamily;
    public final long ramMb;
    public final String abi;

    public DeviceInfo(String deviceModel, int androidSdk, String gpuFamily, long ramMb, String abi) {
        this.deviceModel = deviceModel != null ? deviceModel : "";
        this.androidSdk = androidSdk;
        this.gpuFamily = gpuFamily != null ? gpuFamily : "unknown";
        this.ramMb = ramMb;
        this.abi = abi != null ? abi : "arm64-v8a";
    }

    public JSONObject toJson() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("device_model", deviceModel);
        o.put("android_sdk", androidSdk);
        o.put("gpu_family", gpuFamily);
        o.put("ram_mb", ramMb);
        o.put("abi", abi);
        return o;
    }

    public static DeviceInfo fromJson(JSONObject o) throws JSONException {
        return new DeviceInfo(
            o.optString("device_model", ""),
            o.optInt("android_sdk", Build.VERSION.SDK_INT),
            o.optString("gpu_family", "unknown"),
            o.optLong("ram_mb", 0),
            o.optString("abi", "arm64-v8a")
        );
    }
}
