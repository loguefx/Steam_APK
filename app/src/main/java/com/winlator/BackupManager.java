package com.winlator;

import android.content.Context;

import com.winlator.container.Container;
import com.winlator.container.Shortcut;
import com.winlator.xenvironment.ImageFs;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Creates a zip backup of game save/config paths under the container root.
 * Writes manifest.json inside the zip listing paths and roles.
 */
public final class BackupManager {
    public static final String BACKUP_DIR = "backups";
    public static final String MANIFEST_JSON = "manifest.json";
    public static final String ROLE_SAVES = "saves";
    public static final String ROLE_SHADER_CACHE = "shader_cache";
    public static final String ROLE_CONFIG = "config";

    private final Context context;
    private final File backupRoot;

    public BackupManager(Context context) {
        this.context = context.getApplicationContext();
        this.backupRoot = new File(context.getExternalFilesDir(null), BACKUP_DIR);
        if (!backupRoot.exists()) backupRoot.mkdirs();
    }

    public File getBackupRoot() { return backupRoot; }

    /**
     * Options for what to include in the backup. Paths are relative to container root.
     */
    public static class BackupOptions {
        public boolean includeSaves = true;
        public boolean includeShaderCache = false;
        public boolean includeConfig = false;
    }

    /**
     * Collect paths to backup under container root. Uses standard Wine user dir (xuser).
     */
    private List<String> collectPaths(File containerRoot, BackupOptions options) {
        List<String> paths = new ArrayList<>();
        String prefix = ".wine/drive_c/users/" + ImageFs.USER + "/";
        if (options.includeSaves) {
            paths.add(prefix + "Documents");
            paths.add(prefix + "AppData/Local");
            paths.add(prefix + "AppData/Roaming");
            paths.add(prefix + "Saved Games");
        }
        if (options.includeShaderCache) {
            paths.add(".wine/drive_c/ProgramData/cnc-ddraw/Shaders");
        }
        if (options.includeConfig) {
            paths.add(".wine/user.reg");
            paths.add(".wine/system.reg");
        }
        return paths;
    }

    /**
     * Create a backup zip for the given container and shortcut. Saves to backupRoot with name gameName_timestamp.zip.
     * @return the created zip file, or null on failure
     */
    public File createBackup(Container container, Shortcut shortcut, BackupOptions options) {
        if (container == null || shortcut == null) return null;
        File rootDir = container.getRootDir();
        if (rootDir == null || !rootDir.isDirectory()) return null;

        List<String> relativePaths = collectPaths(rootDir, options);
        String safeName = shortcut.name.replaceAll("[^a-zA-Z0-9._-]", "_");
        String zipName = safeName + "_" + System.currentTimeMillis() + ".zip";
        File zipFile = new File(backupRoot, zipName);

        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile))) {
            zos.setLevel(5);
            JSONArray entries = new JSONArray();
            for (String rel : relativePaths) {
                File f = new File(rootDir, rel);
                if (!f.exists()) continue;
                if (f.isDirectory()) {
                    addDirToZip(zos, f, rel, entries);
                } else {
                    addFileToZip(zos, f, rel, entries);
                }
            }
            JSONObject manifest = new JSONObject();
            manifest.put("game_name", shortcut.name);
            manifest.put("container_id", container.id);
            manifest.put("shortcut_path", shortcut.file.getPath());
            manifest.put("entries", entries);
            ZipEntry manifestEntry = new ZipEntry(MANIFEST_JSON);
            zos.putNextEntry(manifestEntry);
            zos.write(manifest.toString().getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        } catch (Exception e) {
            if (zipFile.exists()) zipFile.delete();
            return null;
        }
        return zipFile;
    }

    private void addDirToZip(ZipOutputStream zos, File dir, String basePath, JSONArray entries) throws IOException, org.json.JSONException {
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            String rel = basePath + "/" + f.getName();
            if (f.isDirectory()) {
                addDirToZip(zos, f, rel, entries);
            } else {
                addFileToZip(zos, f, rel, entries);
            }
        }
    }

    private void addFileToZip(ZipOutputStream zos, File file, String entryPath, JSONArray entries) throws IOException, org.json.JSONException {
        ZipEntry e = new ZipEntry(entryPath);
        zos.putNextEntry(e);
        try (FileInputStream is = new FileInputStream(file)) {
            byte[] buf = new byte[65536];
            int n;
            while ((n = is.read(buf)) > 0) zos.write(buf, 0, n);
        }
        zos.closeEntry();
        entries.put(new JSONObject().put("path", entryPath).put("role", ROLE_SAVES));
    }
}
