package com.winlator.core;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Fetches component manifests from gamehub_api (or fork) for Box64, FEX, DXVK, VKD3D, GPU drivers.
 * Same component model as Game Hub — used for compatibility and optional component management.
 */
public final class ComponentApiClient {
    public static final String GAMEHUB_API_RAW_BASE = "https://raw.githubusercontent.com/gamehublite/gamehub_api/main";

    public static class ComponentEntry {
        public final int id;
        public final String name;
        public final int type;
        public final String version;
        public final String downloadUrl;
        public final String fileMd5;
        public final long fileSize;
        public final String fileName;

        ComponentEntry(int id, String name, int type, String version, String downloadUrl, String fileMd5, long fileSize, String fileName) {
            this.id = id;
            this.name = name;
            this.type = type;
            this.version = version;
            this.downloadUrl = downloadUrl;
            this.fileMd5 = fileMd5;
            this.fileSize = fileSize;
            this.fileName = fileName;
        }
    }

    public static class ManifestResult {
        public final String typeName;
        public final String displayName;
        public final int total;
        public final List<ComponentEntry> components;

        ManifestResult(String typeName, String displayName, int total, List<ComponentEntry> components) {
            this.typeName = typeName;
            this.displayName = displayName;
            this.total = total;
            this.components = components;
        }
    }

    /**
     * Fetch a component manifest from the API (e.g. box64_manifest, dxvk_manifest, drivers_manifest).
     * @param manifestName file name under components/ (e.g. "box64_manifest", "drivers_manifest")
     * @return parsed result or null on error
     */
    public static ManifestResult fetchManifest(String manifestName) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(GAMEHUB_API_RAW_BASE + "/components/" + manifestName);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            int code = conn.getResponseCode();
            if (code != 200) return null;

            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
            }

            JSONObject root = new JSONObject(sb.toString());
            if (root.optInt("code", 0) != 200) return null;
            JSONObject data = root.optJSONObject("data");
            if (data == null) return null;

            String typeName = data.optString("type_name", "");
            String displayName = data.optString("display_name", "");
            int total = data.optInt("total", 0);
            JSONArray arr = data.optJSONArray("components");
            List<ComponentEntry> components = new ArrayList<>();
            if (arr != null) {
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject o = arr.optJSONObject(i);
                    if (o == null) continue;
                    components.add(new ComponentEntry(
                        o.optInt("id", 0),
                        o.optString("name", ""),
                        o.optInt("type", 0),
                        o.optString("version", ""),
                        o.optString("download_url", ""),
                        o.optString("file_md5", ""),
                        Long.parseLong(o.optString("file_size", "0")),
                        o.optString("file_name", "")
                    ));
                }
            }
            return new ManifestResult(typeName, displayName, total, components);
        } catch (Exception e) {
            return null;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }
}
