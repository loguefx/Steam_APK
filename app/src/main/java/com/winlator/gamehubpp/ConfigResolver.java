package com.winlator.gamehubpp;

import com.winlator.container.Container;
import com.winlator.container.Shortcut;

import java.util.HashMap;
import java.util.Map;

/**
 * Merges container defaults + shortcut extras + resolved Profile into effective
 * config and env for launch. Used by XServerDisplayActivity to apply all
 * compatibility options (surface_format, cpu_core_limit, vram_limit, etc.).
 */
public final class ConfigResolver {

    /** Env key for Vulkan surface format preference (if supported by runtime). */
    public static final String ENV_SURFACE_FORMAT = "GAMEHUB_SURFACE_FORMAT";
    /** Env key for CPU core limit (e.g. WINELIMITCORES used by some Wine builds). */
    public static final String ENV_CPU_CORE_LIMIT = "WINELIMITCORES";
    /** Env key for VRAM limit in MB (best-effort; runtime may support via DXVK/native). */
    public static final String ENV_VRAM_LIMIT_MB = "GAMEHUB_VRAM_LIMIT_MB";

    private ConfigResolver() {}

    /**
     * Build effective env vars from shortcut extras and resolved profile settings.
     * Container defaults are already applied by XServerDisplayActivity for
     * graphics/audio/dxwrapper; this adds profile/shortcut-derived env for
     * surface_format, cpu_core_limit, vram_limit.
     */
    public static Map<String, String> getEffectiveEnv(Container container,
                                                      Shortcut shortcut,
                                                      Resolver.ResolveResult resolveResult) {
        Map<String, String> env = new HashMap<>();
        if (container == null) return env;

        String surfaceFormat = null;
        String cpuCoreLimit = null;
        String vramLimitMb = null;

        if (resolveResult != null && resolveResult.profile != null && resolveResult.profile.settings != null) {
            if (resolveResult.profile.settings.containsKey("surface_format")) {
                Object v = resolveResult.profile.settings.get("surface_format");
                if (v != null) surfaceFormat = String.valueOf(v).trim();
            }
            if (resolveResult.profile.settings.containsKey("cpu_core_limit")) {
                Object v = resolveResult.profile.settings.get("cpu_core_limit");
                if (v != null) cpuCoreLimit = String.valueOf(v).trim();
            }
            if (resolveResult.profile.settings.containsKey("vram_limit")) {
                Object v = resolveResult.profile.settings.get("vram_limit");
                if (v != null) vramLimitMb = String.valueOf(v).trim();
            }
        }

        if (shortcut != null) {
            String s = shortcut.getExtra("surface_format", "").trim();
            if (!s.isEmpty()) surfaceFormat = s;
            s = shortcut.getExtra("cpu_core_limit", "").trim();
            if (!s.isEmpty()) cpuCoreLimit = s;
            s = shortcut.getExtra("vram_limit", "").trim();
            if (!s.isEmpty()) vramLimitMb = s;
        }

        if (surfaceFormat != null && !surfaceFormat.isEmpty()) env.put(ENV_SURFACE_FORMAT, surfaceFormat);
        if (cpuCoreLimit != null && !cpuCoreLimit.isEmpty()) env.put(ENV_CPU_CORE_LIMIT, cpuCoreLimit);
        if (vramLimitMb != null && !vramLimitMb.isEmpty()) env.put(ENV_VRAM_LIMIT_MB, vramLimitMb);

        return env;
    }
}
