package com.winlator.gamehubpp;

import android.content.Context;

import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Downloads config packs, verifies checksum, stores in cache. Prunes to last N. Stages as Candidate only.
 */
public final class PackManager {
    private static final String CACHE_DIR = "pack_cache";
    private static final String MANIFEST_FILE = "manifest.json";
    private static final String RULES_FILE = "rules.json";
    private static final String PROFILES_FILE = "profiles.json";
    private static final String NOTES_FILE = "notes.json";
    private static final int MAX_CACHED_PACKS = 5;

    private final File cacheRoot;
    private final Context context;

    public PackManager(Context context) {
        this.context = context.getApplicationContext();
        this.cacheRoot = new File(context.getExternalFilesDir(null), CACHE_DIR);
        if (!cacheRoot.exists()) cacheRoot.mkdirs();
    }

    public File getCacheRoot() { return cacheRoot; }

    /**
     * List cached pack IDs (pack dir names), newest first.
     */
    public List<String> listCachedPackIds() {
        File[] dirs = cacheRoot.listFiles(File::isDirectory);
        if (dirs == null) return new ArrayList<>();
        List<String> ids = new ArrayList<>();
        for (File d : dirs) ids.add(d.getName());
        Collections.sort(ids, (a, b) -> {
            long ta = getPackCreatedAt(a);
            long tb = getPackCreatedAt(b);
            return Long.compare(tb, ta);
        });
        return ids;
    }

    private long getPackCreatedAt(String packId) {
        File manifest = new File(new File(cacheRoot, packId), MANIFEST_FILE);
        if (!manifest.isFile()) return 0;
        try {
            FileInputStream is = new FileInputStream(manifest);
            byte[] buf = new byte[(int) manifest.length()];
            is.read(buf);
            is.close();
            return new JSONObject(new String(buf, StandardCharsets.UTF_8)).optLong("created_at", 0);
        } catch (Exception e) { return 0; }
    }

    /**
     * Load a cached pack by id. Returns null if missing or invalid.
     */
    public ConfigPack loadCachedPack(String packId) {
        File dir = new File(cacheRoot, packId);
        if (!dir.isDirectory()) return null;
        try {
            JSONObject manifest = readJson(new File(dir, MANIFEST_FILE));
            JSONObject rules = readJson(new File(dir, RULES_FILE));
            JSONObject profiles = readJson(new File(dir, PROFILES_FILE));
            String notes = "";
            File notesFile = new File(dir, NOTES_FILE);
            if (notesFile.isFile()) {
                byte[] b = new byte[(int) notesFile.length()];
                try (FileInputStream is = new FileInputStream(notesFile)) { is.read(b); }
                notes = new String(b, StandardCharsets.UTF_8);
            }
            if (manifest == null) return null;
            return ConfigPack.fromJson(manifest, rules, profiles, notes);
        } catch (Exception e) {
            return null;
        }
    }

    private static JSONObject readJson(File f) {
        if (!f.isFile()) return null;
        try {
            FileInputStream is = new FileInputStream(f);
            byte[] buf = new byte[(int) f.length()];
            is.read(buf);
            is.close();
            return new JSONObject(new String(buf, StandardCharsets.UTF_8));
        } catch (Exception e) { return null; }
    }

    /**
     * Download pack from base URL (expects manifest.json, rules.json, profiles.json at baseUrl/).
     * Verify optional checksum (sha256 of manifest string). Returns pack id on success, null on failure.
     */
    public String downloadPack(String baseUrl) {
        if (baseUrl == null || baseUrl.isEmpty()) return null;
        String base = baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
        try {
            JSONObject manifest = fetchUrl(base + MANIFEST_FILE);
            if (manifest == null) return null;
            String packId = manifest.optString("pack_id", "pack_" + System.currentTimeMillis());
            File dir = new File(cacheRoot, packId);
            if (dir.exists()) deleteRecursive(dir);
            dir.mkdirs();

            writeJson(new File(dir, MANIFEST_FILE), manifest);
            JSONObject rules = fetchUrl(base + RULES_FILE);
            if (rules != null) writeJson(new File(dir, RULES_FILE), rules);
            JSONObject profiles = fetchUrl(base + PROFILES_FILE);
            if (profiles != null) writeJson(new File(dir, PROFILES_FILE), profiles);

            pruneToMaxPacks(MAX_CACHED_PACKS);
            return packId;
        } catch (Exception e) {
            return null;
        }
    }

    private static JSONObject fetchUrl(String urlString) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlString);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);
            if (conn.getResponseCode() != 200) return null;
            StringBuilder sb = new StringBuilder();
            try (java.io.BufferedReader r = new java.io.BufferedReader(new java.io.InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = r.readLine()) != null) sb.append(line);
            }
            return new JSONObject(sb.toString());
        } catch (Exception e) { return null; }
        finally { if (conn != null) conn.disconnect(); }
    }

    private static void writeJson(File f, JSONObject o) throws Exception {
        try (FileOutputStream os = new FileOutputStream(f)) {
            os.write(o.toString().getBytes(StandardCharsets.UTF_8));
        }
    }

    /**
     * Verify pack manifest checksum if present. Uses SHA-256 of manifest JSON string.
     */
    public boolean verifyPackChecksum(String packId) {
        ConfigPack pack = loadCachedPack(packId);
        if (pack == null || pack.checksumSha256 == null || pack.checksumSha256.isEmpty()) return true;
        File manifestFile = new File(new File(cacheRoot, packId), MANIFEST_FILE);
        if (!manifestFile.isFile()) return false;
        try {
            FileInputStream is = new FileInputStream(manifestFile);
            byte[] buf = new byte[(int) manifestFile.length()];
            is.read(buf);
            is.close();
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(buf);
            StringBuilder hex = new StringBuilder();
            for (byte b : digest) hex.append(String.format(Locale.US, "%02x", b));
            return pack.checksumSha256.equalsIgnoreCase(hex.toString());
        } catch (Exception e) { return false; }
    }

    /** Keep only the newest MAX_CACHED_PACKS packs. */
    public void pruneToMaxPacks(int max) {
        List<String> ids = listCachedPackIds();
        for (int i = max; i < ids.size(); i++) {
            File dir = new File(cacheRoot, ids.get(i));
            deleteRecursive(dir);
        }
    }

    private static boolean deleteRecursive(File f) {
        if (f.isDirectory()) {
            File[] children = f.listFiles();
            if (children != null) for (File c : children) deleteRecursive(c);
        }
        return f.delete();
    }

    /**
     * Apply pack as Candidate for all games that match rules. Saves pack profiles to ProfileStore with id "profile_pack_&lt;packId&gt;_&lt;profileId&gt;".
     * @return number of games that got a candidate set
     */
    public int applyPackAsCandidateForAllGames(ConfigPack pack, ProfileStore profileStore, DeviceInfo device) {
        if (pack == null || pack.profiles == null) return 0;
        List<String> gameIds = profileStore.listGameIds();
        int applied = 0;
        for (String gameId : gameIds) {
            String profileId = RulesEngine.matchProfileId(pack, gameId, null, device);
            if (profileId == null) continue;
            Profile p = pack.profiles.get(profileId);
            if (p == null) continue;
            String storedId = "profile_pack_" + pack.packId + "_" + p.id;
            Profile stored = new Profile(storedId, p.name + " (Candidate)", "pack",
                p.components, p.settings, p.constraints);
            profileStore.saveProfile(stored);
            profileStore.setCandidate(gameId, storedId);
            applied++;
        }
        return applied;
    }
}
