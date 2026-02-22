package com.winlator.gamehubpp;

import java.util.List;

/**
 * Matches game + device to a profile_id from a config pack's rules.
 * Used to stage a pack profile as Candidate for a game.
 */
public final class RulesEngine {

    /**
     * Find the first rule that matches the game and device. Returns profile_id or null.
     * gameId is used when exe_sha256 is not available (e.g. "c123_game.exe").
     */
    public static String matchProfileId(ConfigPack pack, String gameId, String exeSha256, DeviceInfo device) {
        if (pack == null || pack.rules == null) return null;
        for (ConfigPack.RuleMatch rule : pack.rules) {
            if (device.androidSdk < rule.androidMin) continue;
            if (rule.gpuFamily != null && !rule.gpuFamily.isEmpty()) {
                boolean gpuOk = false;
                for (String g : rule.gpuFamily) {
                    if (g != null && g.equalsIgnoreCase(device.gpuFamily)) { gpuOk = true; break; }
                }
                if (!gpuOk) continue;
            }
            if (rule.exeSha256 != null && !rule.exeSha256.isEmpty()) {
                if (exeSha256 == null || !exeSha256.equalsIgnoreCase(rule.exeSha256)) continue;
            }
            if (!pack.profiles.containsKey(rule.profileId)) continue;
            return rule.profileId;
        }
        return null;
    }
}
