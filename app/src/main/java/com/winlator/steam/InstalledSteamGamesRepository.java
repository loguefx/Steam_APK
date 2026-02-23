package com.winlator.steam;

import android.util.Log;

import com.winlator.container.Container;
import com.winlator.core.FileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Scans the default container's Steam install to list installed games (from libraryfolders.vdf + appmanifest_*.acf).
 * Single source of truth for "is this game installed?" and "list of installed games for Home".
 */
public final class InstalledSteamGamesRepository {
    private static final String TAG = "InstalledSteamGames";

    /** StateFlags in ACF: 4 = fully installed. */
    private static final int STATE_FLAGS_INSTALLED = 4;

    private final Container container;
    private List<InstalledSteamGame> cachedGames;
    private SteamRuntimeState cachedState;

    public InstalledSteamGamesRepository(Container container) {
        this.container = container;
    }

    /**
     * Get runtime state and list of installed games. Runs scan on current thread; call from background.
     * Returns empty list and NO_CONTAINER / STEAM_NOT_INSTALLED when container or Steam is missing.
     */
    public Result getInstalledGames() {
        if (container == null) {
            return new Result(SteamRuntimeState.NO_CONTAINER, Collections.emptyList());
        }
        File steamRoot = container.getSteamRootDir();
        if (!steamRoot.isDirectory()) {
            cachedState = SteamRuntimeState.STEAM_NOT_INSTALLED;
            cachedGames = Collections.emptyList();
            return new Result(cachedState, cachedGames);
        }
        File steamExe = new File(steamRoot, "steam.exe");
        if (!steamExe.isFile()) {
            cachedState = SteamRuntimeState.STEAM_NOT_INSTALLED;
            cachedGames = Collections.emptyList();
            return new Result(cachedState, cachedGames);
        }

        List<File> steamAppsDirs = getSteamAppsDirs(steamRoot);
        List<InstalledSteamGame> games = new ArrayList<>();
        for (File steamAppsDir : steamAppsDirs) {
            scanSteamAppsDir(steamAppsDir, games);
        }
        cachedState = SteamRuntimeState.STEAM_READY;
        cachedGames = games;
        return new Result(cachedState, cachedGames);
    }

    public SteamRuntimeState getCachedState() {
        return cachedState;
    }

    public List<InstalledSteamGame> getCachedGames() {
        return cachedGames != null ? cachedGames : Collections.emptyList();
    }

    /** Invalidate cache so next getInstalledGames() does a full scan. */
    public void invalidateCache() {
        cachedGames = null;
        cachedState = null;
    }

    /** Check if a game with this appId is installed (uses cache if available). */
    public boolean isInstalled(String appId) {
        if (appId == null) return false;
        if (cachedGames != null) {
            for (InstalledSteamGame g : cachedGames) {
                if (appId.equals(g.appId)) return true;
            }
            return false;
        }
        Result r = getInstalledGames();
        for (InstalledSteamGame g : r.games) {
            if (appId.equals(g.appId)) return true;
        }
        return false;
    }

    public boolean isInstalled(int appId) {
        return isInstalled(String.valueOf(appId));
    }

    /**
     * Collect steamapps directories: Steam root's steamapps + any from libraryfolders.vdf (C: only for simplicity).
     */
    private List<File> getSteamAppsDirs(File steamRoot) {
        List<File> dirs = new ArrayList<>();
        File defaultSteamApps = new File(steamRoot, "steamapps");
        if (defaultSteamApps.isDirectory()) {
            dirs.add(defaultSteamApps);
        }
        File libraryFoldersVdf = new File(defaultSteamApps, "libraryfolders.vdf");
        if (!libraryFoldersVdf.isFile()) {
            return dirs;
        }
        try {
            String content = FileUtils.readString(libraryFoldersVdf);
            if (content == null) return dirs;
            Pattern pathPattern = Pattern.compile("\"path\"\\s+\"([^\"]+)\"");
            Matcher m = pathPattern.matcher(content);
            File rootDir = container.getRootDir();
            String driveC = rootDir.getPath() + "/.wine/drive_c";
            while (m.find()) {
                String path = m.group(1).trim().replace("\\\\", "\\");
                if (path.isEmpty()) continue;
                if (path.length() >= 3 && path.charAt(1) == ':' && (path.charAt(2) == '\\' || path.charAt(2) == '/')) {
                    char drive = Character.toUpperCase(path.charAt(0));
                    String rest = path.substring(3).replace('\\', '/');
                    if (drive == 'C') {
                        String normalized = driveC + "/" + rest;
                        File steamApps = new File(normalized, "steamapps");
                        if (steamApps.isDirectory() && !dirs.contains(steamApps)) {
                            dirs.add(steamApps);
                        }
                    }
                }
            }
        } catch (Throwable t) {
            Log.w(TAG, "Error parsing libraryfolders.vdf", t);
        }
        return dirs;
    }

    private void scanSteamAppsDir(File steamAppsDir, List<InstalledSteamGame> out) {
        File[] files = steamAppsDir.listFiles((dir, name) -> name.startsWith("appmanifest_") && name.endsWith(".acf"));
        if (files == null) return;
        for (File acf : files) {
            try {
                String name = acf.getName();
                if (name.length() < 14) continue;
                String appId = name.substring(12, name.length() - 4);
                InstalledSteamGame game = parseAppManifest(acf, appId, steamAppsDir);
                if (game != null) {
                    out.add(game);
                }
            } catch (Throwable t) {
                Log.w(TAG, "Error parsing " + acf.getName(), t);
            }
        }
    }

    private InstalledSteamGame parseAppManifest(File acfFile, String appId, File steamAppsDir) {
        List<String> lines = FileUtils.readLines(acfFile);
        if (lines == null) return null;
        String name = null;
        String installdir = null;
        int stateFlags = -1;
        long lastUpdated = 0;
        Pattern pair = Pattern.compile("\"([^\"]+)\"\\s+\"([^\"]*)\"");
        Pattern pairNum = Pattern.compile("\"([^\"]+)\"\\s+(\\d+)");
        for (String line : lines) {
            line = line.trim();
            Matcher m = pair.matcher(line);
            if (m.find()) {
                String key = m.group(1);
                String value = m.group(2);
                if ("name".equals(key)) name = value;
                else if ("installdir".equals(key)) installdir = value;
                continue;
            }
            Matcher mn = pairNum.matcher(line);
            if (mn.find()) {
                if ("StateFlags".equals(mn.group(1))) {
                    stateFlags = Integer.parseInt(mn.group(2));
                } else if ("LastUpdated".equals(mn.group(1))) {
                    try {
                        lastUpdated = Long.parseLong(mn.group(2));
                    } catch (NumberFormatException ignored) {}
                }
            }
        }
        if (stateFlags != STATE_FLAGS_INSTALLED || installdir == null || installdir.isEmpty()) {
            return null;
        }
        File gameDir = new File(new File(steamAppsDir, "common"), installdir);
        if (!gameDir.isDirectory()) return null;
        return new InstalledSteamGame(appId, name, installdir, gameDir.getPath(), lastUpdated);
    }

    public static final class Result {
        public final SteamRuntimeState state;
        public final List<InstalledSteamGame> games;

        public Result(SteamRuntimeState state, List<InstalledSteamGame> games) {
            this.state = state;
            this.games = games != null ? games : Collections.emptyList();
        }
    }
}
