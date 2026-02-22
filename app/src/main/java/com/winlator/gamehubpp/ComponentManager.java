package com.winlator.gamehubpp;

import android.content.Context;

import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages side-by-side components (compat, driver, translator).
 * Never overwrites an existing component ID (immutable).
 */
public final class ComponentManager {
    private static final String DIR_COMPAT = "compat";
    private static final String DIR_DRIVER = "driver";
    private static final String DIR_TRANSLATOR = "translator";
    private static final String MANIFEST_FILE = "manifest.json";

    private final File componentsRoot;

    public ComponentManager(Context context) {
        this.componentsRoot = new File(context.getExternalFilesDir(null), "components");
    }

    public File getComponentsRoot() { return componentsRoot; }

    public File getCompatDir() { return new File(componentsRoot, DIR_COMPAT); }
    public File getDriverDir() { return new File(componentsRoot, DIR_DRIVER); }
    public File getTranslatorDir() { return new File(componentsRoot, DIR_TRANSLATOR); }

    /** Get component root for type (e.g. compat, driver, translator). */
    public File getDirForType(String type) {
        if (ComponentManifest.TYPE_COMPAT.equals(type)) return getCompatDir();
        if (ComponentManifest.TYPE_DRIVER.equals(type)) return getDriverDir();
        if (ComponentManifest.TYPE_TRANSLATOR.equals(type)) return getTranslatorDir();
        return null;
    }

    /** List installed component IDs for a type. Each subdir is one component. */
    public List<String> listInstalledIds(String type) {
        File dir = getDirForType(type);
        List<String> ids = new ArrayList<>();
        if (dir == null || !dir.isDirectory()) return ids;
        File[] children = dir.listFiles();
        if (children == null) return ids;
        for (File f : children) {
            if (f.isDirectory()) {
                File manifest = new File(f, MANIFEST_FILE);
                if (manifest.isFile()) ids.add(f.getName());
            }
        }
        return ids;
    }

    /** Get path to an installed component by type and id. Returns null if not found. */
    public File getComponentPath(String type, String id) {
        if (id == null || id.isEmpty()) return null;
        File dir = getDirForType(type);
        if (dir == null) return null;
        File path = new File(dir, id);
        return path.isDirectory() && new File(path, MANIFEST_FILE).isFile() ? path : null;
    }

    /** Load manifest for a component. Returns null on error. */
    public ComponentManifest getManifest(String type, String id) {
        File path = getComponentPath(type, id);
        if (path == null) return null;
        File manifestFile = new File(path, MANIFEST_FILE);
        if (!manifestFile.isFile()) return null;
        try {
            FileInputStream is = new FileInputStream(manifestFile);
            byte[] buf = new byte[(int) manifestFile.length()];
            int n = is.read(buf);
            is.close();
            if (n <= 0) return null;
            JSONObject o = new JSONObject(new String(buf, 0, n, "UTF-8"));
            return ComponentManifest.fromJson(o);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Ensure component directory exists and write manifest.
     * Does not overwrite existing component (immutable). Returns false if id already installed.
     */
    public boolean installComponent(ComponentManifest manifest) {
        File dir = getDirForType(manifest.type);
        if (dir == null) return false;
        File path = new File(dir, manifest.id);
        if (path.exists()) return false; // immutable
        if (!path.mkdirs()) return false;
        try {
            JSONObject o = manifest.toJson();
            FileOutputStream os = new FileOutputStream(new File(path, MANIFEST_FILE));
            os.write(o.toString().getBytes("UTF-8"));
            os.close();
            return true;
        } catch (Exception e) {
            deleteRecursive(path);
            return false;
        }
    }

    /** Remove a component by type and id. */
    public boolean removeComponent(String type, String id) {
        File path = getComponentPath(type, id);
        if (path == null) return false;
        return deleteRecursive(path);
    }

    private static boolean deleteRecursive(File f) {
        if (f.isDirectory()) {
            File[] children = f.listFiles();
            if (children != null) for (File c : children) deleteRecursive(c);
        }
        return f.delete();
    }
}
