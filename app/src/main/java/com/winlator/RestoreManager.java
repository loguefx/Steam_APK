package com.winlator;

import android.content.Context;

import com.winlator.container.Container;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Lists backup zips and restores them into a container (unzip to container root, merge/replace).
 */
public final class RestoreManager {
    private final Context context;

    public RestoreManager(Context context) {
        this.context = context.getApplicationContext();
    }

    /**
     * List backup zip files in the default backup directory that contain a valid manifest.
     * Newest first by name (timestamp).
     */
    public List<File> listBackups() {
        File backupRoot = new File(context.getExternalFilesDir(null), BackupManager.BACKUP_DIR);
        if (!backupRoot.isDirectory()) return new ArrayList<>();
        File[] files = backupRoot.listFiles((d, name) -> name.endsWith(".zip"));
        if (files == null) return new ArrayList<>();
        List<File> list = new ArrayList<>();
        for (File f : files) list.add(f);
        list.sort((a, b) -> Long.compare(b.lastModified(), a.lastModified()));
        return list;
    }

    /**
     * Read manifest from zip (without fully extracting). Returns null if invalid.
     */
    public JSONObject readManifest(File backupZip) {
        if (backupZip == null || !backupZip.isFile()) return null;
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(backupZip))) {
            ZipEntry e;
            while ((e = zis.getNextEntry()) != null) {
                if (BackupManager.MANIFEST_JSON.equals(e.getName())) {
                    StringBuilder sb = new StringBuilder();
                    byte[] buf = new byte[4096];
                    int n;
                    while ((n = zis.read(buf)) > 0) sb.append(new String(buf, 0, n, StandardCharsets.UTF_8));
                    return new JSONObject(sb.toString());
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    /**
     * Restore backup zip into the given container. Extracts all entries (except manifest) into container root.
     * Overwrites existing files. Uses container from manifest if containerId matches; otherwise uses provided container.
     */
    public boolean restore(File backupZip, Container container) {
        if (backupZip == null || !backupZip.isFile() || container == null) return false;
        File rootDir = container.getRootDir();
        if (rootDir == null || !rootDir.isDirectory()) return false;

        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(backupZip))) {
            ZipEntry e;
            byte[] buf = new byte[65536];
            while ((e = zis.getNextEntry()) != null) {
                if (BackupManager.MANIFEST_JSON.equals(e.getName())) continue;
                String path = e.getName();
                if (path.contains("..")) continue;
                File out = new File(rootDir, path);
                if (e.isDirectory()) {
                    out.mkdirs();
                } else {
                    File parent = out.getParentFile();
                    if (parent != null) parent.mkdirs();
                    try (FileOutputStream fos = new FileOutputStream(out)) {
                        int n;
                        while ((n = zis.read(buf)) > 0) fos.write(buf, 0, n);
                    }
                }
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
