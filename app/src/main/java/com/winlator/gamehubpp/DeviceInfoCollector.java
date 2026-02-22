package com.winlator.gamehubpp;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;

/**
 * Collects device fingerprint for compatibility gating.
 */
public final class DeviceInfoCollector {
    public static DeviceInfo collect(Context context) {
        Context app = context.getApplicationContext();
        String model = Build.MODEL != null ? Build.MODEL : "";
        int sdk = Build.VERSION.SDK_INT;
        String abi = Build.SUPPORTED_ABIS != null && Build.SUPPORTED_ABIS.length > 0
            ? Build.SUPPORTED_ABIS[0] : "arm64-v8a";
        String gpuFamily = inferGpuFamily();
        long ramMb = 0;
        try {
            ActivityManager am = (ActivityManager) app.getSystemService(Context.ACTIVITY_SERVICE);
            if (am != null) {
                ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
                am.getMemoryInfo(mi);
                ramMb = mi.totalMem / (1024 * 1024);
            }
        } catch (Exception ignored) {}
        return new DeviceInfo(model, sdk, gpuFamily, ramMb, abi);
    }

    private static String inferGpuFamily() {
        String brand = Build.BRAND != null ? Build.BRAND.toLowerCase() : "";
        String board = Build.BOARD != null ? Build.BOARD.toLowerCase() : "";
        String device = Build.DEVICE != null ? Build.DEVICE.toLowerCase() : "";
        if (board.contains("adreno") || device.contains("adreno") || board.contains("qualcomm")) return "adreno_7xx";
        if (board.contains("mali") || device.contains("mali")) return "mali";
        return "unknown";
    }
}
