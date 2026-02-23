package com.winlator.steam;

/**
 * One game installed in the container's Steam library (from appmanifest_*.acf scan).
 */
public final class InstalledSteamGame {
    public final String appId;
    public final String name;
    /** Folder name under steamapps/common (installdir from ACF). */
    public final String installDir;
    /** Full path to game directory in container, if available. */
    public final String installPath;
    /** Last updated timestamp from ACF, or 0. */
    public final long lastUpdated;

    public InstalledSteamGame(String appId, String name, String installDir, String installPath, long lastUpdated) {
        this.appId = appId != null ? appId : "";
        this.name = name != null ? name : ("App " + appId);
        this.installDir = installDir != null ? installDir : "";
        this.installPath = installPath != null ? installPath : "";
        this.lastUpdated = lastUpdated;
    }

    public int getAppIdInt() {
        try {
            return Integer.parseInt(appId);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
