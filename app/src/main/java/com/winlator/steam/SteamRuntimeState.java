package com.winlator.steam;

/**
 * State of Steam runtime in the default container.
 */
public enum SteamRuntimeState {
    /** No default container exists. */
    NO_CONTAINER,
    /** Container exists but Steam is not installed (no steam.exe in Steam root). */
    STEAM_NOT_INSTALLED,
    /** Steam is installed and ready; can launch Steam and scan installed games. */
    STEAM_READY
}
