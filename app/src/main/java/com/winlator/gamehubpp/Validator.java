package com.winlator.gamehubpp;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Validates profile + device before launch. Blocks invalid combos.
 * Returns OK, WARN with suggestion, or BLOCK with "Apply Safe Mode".
 */
public final class Validator {
    public static final int RESULT_OK = 0;
    public static final int RESULT_WARN = 1;
    public static final int RESULT_BLOCK = 2;

    private final Context context;
    private final ComponentManager componentManager;
    private final DeviceInfo deviceInfo;

    public Validator(Context context, ComponentManager componentManager, DeviceInfo deviceInfo) {
        this.context = context.getApplicationContext();
        this.componentManager = componentManager;
        this.deviceInfo = deviceInfo;
    }

    public static final class ValidationResult {
        public final int result; // RESULT_OK, RESULT_WARN, RESULT_BLOCK
        public final String message;
        public final String suggestedAction; // e.g. "Apply Safe Mode"

        public ValidationResult(int result, String message, String suggestedAction) {
            this.result = result;
            this.message = message != null ? message : "";
            this.suggestedAction = suggestedAction != null ? suggestedAction : "";
        }

        public boolean isOk() { return result == RESULT_OK; }
        public boolean isBlock() { return result == RESULT_BLOCK; }
    }

    /**
     * Validate that the resolved profile is compatible with device and components exist.
     */
    public ValidationResult validate(Profile profile, Resolver.ResolveResult resolveResult) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        if (profile.constraints.containsKey("android_min")) {
            int minSdk = ((Number) profile.constraints.get("android_min")).intValue();
            if (deviceInfo.androidSdk < minSdk) {
                errors.add("Android " + deviceInfo.androidSdk + " below required " + minSdk);
            }
        }

        if (profile.constraints.containsKey("gpu_family")) {
            Object gpu = profile.constraints.get("gpu_family");
            if (gpu instanceof List) {
                List<?> families = (List<?>) gpu;
                boolean found = false;
                for (Object f : families) if (deviceInfo.gpuFamily.equals(String.valueOf(f))) { found = true; break; }
                if (!found) warnings.add("GPU " + deviceInfo.gpuFamily + " not in profile list");
            }
        }

        for (Map.Entry<String, String> e : profile.components.entrySet()) {
            String type = e.getKey();
            String id = e.getValue();
            if (id == null || id.isEmpty()) continue;
            if (!resolveResult.componentPaths.containsKey(type)) {
                ComponentManifest manifest = componentManager.getManifest(type, id);
                if (manifest == null) {
                    errors.add("Missing component " + type + ":" + id);
                } else {
                    if (!manifest.abis.isEmpty() && !manifest.abis.contains(deviceInfo.abi)) {
                        errors.add("Component " + id + " ABI " + manifest.abis + " does not match " + deviceInfo.abi);
                    }
                    if (manifest.minAndroid > deviceInfo.androidSdk) {
                        errors.add("Component " + id + " requires Android " + manifest.minAndroid);
                    }
                    if (!manifest.supportedGpus.isEmpty() && !manifest.supportedGpus.contains(deviceInfo.gpuFamily)) {
                        warnings.add("GPU " + deviceInfo.gpuFamily + " not in component " + id + " supported list");
                    }
                }
            }
        }

        if (!errors.isEmpty()) {
            return new ValidationResult(RESULT_BLOCK, "Invalid configuration: " + String.join("; ", errors), "Apply Safe Mode");
        }
        if (!warnings.isEmpty()) {
            return new ValidationResult(RESULT_WARN, String.join("; ", warnings), "");
        }
        return new ValidationResult(RESULT_OK, "", "");
    }
}
