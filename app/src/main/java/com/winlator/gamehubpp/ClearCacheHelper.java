package com.winlator.gamehubpp;

import android.content.Context;

import com.winlator.core.FileUtils;
import com.winlator.xenvironment.ImageFs;

import java.io.File;

/**
 * Non-destructive cache cleanup: temp only, shader cache only. Does not clear all data.
 */
public final class ClearCacheHelper {

    /** Clear app temp dir (e.g. /tmp under imagefs). */
    public static void clearTempCache(Context context) {
        try {
            ImageFs fs = ImageFs.find(context);
            if (fs != null) FileUtils.clear(fs.getTmpDir());
        } catch (Exception ignored) {}
    }

    /** Clear known shader cache paths under wine prefix (cnc-ddraw/Shaders, mesa/vk cache if present). */
    public static void clearShaderCache(Context context) {
        try {
            ImageFs fs = ImageFs.find(context);
            if (fs == null) return;
            File root = fs.getRootDir();
            File winePrefix = new File(root, ImageFs.WINEPREFIX);
            File shadersCnc = new File(winePrefix, "drive_c/ProgramData/cnc-ddraw/Shaders");
            if (shadersCnc.isDirectory()) FileUtils.delete(shadersCnc);
            File cacheDir = new File(root, ImageFs.CACHE_PATH);
            if (cacheDir.isDirectory()) FileUtils.clear(cacheDir);
        } catch (Exception ignored) {}
    }
}
