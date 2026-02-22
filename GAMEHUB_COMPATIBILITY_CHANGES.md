# Game Hub Compatibility Layer – Implementation Notes

This document describes the changes added to Winlator to align with the Game Hub–style launcher plan: Steam sign-in gate, component API, and a path toward the full compatibility/stability layer.

## Completed

### 1. Steam sign-in (first-load gate)
- **LaunchGateActivity** – New launcher activity. If the user is not signed in with Steam, it shows a WebView with Steam OpenID login. After sign-in, it starts MainActivity.
- **SteamAuthPrefs** – Stores `steam_id` and `signed_in` in SharedPreferences.
- **Sign out** – In Settings, a "Sign out" button clears the stored credential and returns to the Steam sign-in screen.
- **Callback** – The app handles `gamehub-open://steam/callback` to receive the Steam OpenID redirect and extract the Steam ID.

### 2. Component API client
- **ComponentApiClient** – Fetches component manifests from gamehub_api (e.g. `box64_manifest`, `dxvk_manifest`, `drivers_manifest`) and parses them into `ManifestResult` and `ComponentEntry`.
- **ComponentsFragment** – New "Components" item in the main menu. Shows a "Refresh component list" button that loads Box64, DXVK, and Drivers manifest counts from the API.

## GameHub++ (Milestone 1 — local-only stability)

See **GAMEHUBPP_SPEC.md** for the full GameHub++ spec. Implemented:

- **Models:** `gamehubpp/DeviceInfo.java`, `ComponentManifest.java`, `Profile.java`, `GameProfileState.java`, `SessionRecord.java`.
- **ComponentManager** — Side-by-side components under `components/compat`, `components/driver`, `components/translator`; list/install by folder; immutable IDs.
- **ProfileStore** — Per-game Safe/Candidate/LKG; default Safe profile on first run; pinning, history, setLkg/setCandidate/rollback.
- **Resolver** — Resolves active profile (useSafeNextRun → Safe; pinned → LKG; else LKG → Candidate → Safe); returns paths + env.
- **Validator** — Validates profile + device (ABI, Android min, GPU family); returns OK / WARN / BLOCK + "Apply Safe Mode".
- **CrashMonitor** — Session records; one-time "use Safe next run" after quick crash; promote Candidate→LKG on stability_pass; rollback on crash.
- **LaunchCoordinator** — Singleton; prepareLaunch(gameId) before start, onGameExit(status) in termination callback, onLaunchAborted() on user exit.
- **DeviceInfoCollector** — Device fingerprint (model, SDK, ABI, GPU family, RAM).
- **Wiring:** `XServerDisplayActivity` calls `LaunchCoordinator.get(this).prepareLaunch(gameId)` when launching with a shortcut; termination callback calls `onGameExit(status)`; `exit()` calls `onLaunchAborted()`.

## Resolver wired into launch path

- **LaunchCoordinator** now stores **currentResolveResult** and exposes **getCurrentResolveResult()**.
- **XServerDisplayActivity** applies the resolved profile’s **env** in `setupXEnvironment()`: after container/shortcut env, it merges `resolved.env` (e.g. WINEDEBUG, FEX_*) so the game process runs with the compatibility profile’s environment.

## Compatibility UI

- **Compatibility** item in the main menu opens **CompatibilityFragment**: shows device info (model, Android, GPU family) and a short explanation (Safe/LKG/Candidate, one-time fallback, pin from shortcut menu).
- **Shortcut popup menu:** new action **Pin LKG for offline** (and toggles to Unpin). Uses the same gameId as launch; calls ProfileStore.getOrCreateGameState + setPinned and shows a toast.

## Milestone 2 — Config packs (cloud but safe)

- **ConfigPack** — Model: pack_id, created_at, min_app_version, profiles_version, checksum, rules (RuleMatch: exe_sha256, gpu_family, android_min, profile_id), profiles map, notes.
- **PackManager** — Download pack from base URL (manifest.json, rules.json, profiles.json), verify SHA-256 checksum, store under `pack_cache/<packId>/`, list cached pack IDs (newest first), loadCachedPack(id), pruneToMaxPacks(5), applyPackAsCandidateForAllGames(pack, profileStore, device).
- **RulesEngine** — matchProfileId(pack, gameId, exeSha256, device): first matching rule returns profile_id.
- **CloudConfigFragment** — Menu: Cloud config. UI: “Check for updated config” (download from DEFAULT_PACK_BASE_URL), list cached packs, “Install as Candidate” (verify checksum, applyPackAsCandidateForAllGames, toast count).

## Milestone 3 — Offline-first polish

- **Cache last N:** PackManager.pruneToMaxPacks(5) after each download.
- **Clear cache tools:** ClearCacheHelper.clearTempCache(context), clearShaderCache(context). Buttons in Compatibility: “Clear temporary cache”, “Clear shader cache”; toast “Cache cleared”.
- **Offline mode UI:** OfflineModeFragment (menu: Offline mode) shows “Use cached configs only” and “Offline profile locked (LKG pinned)”.
- **Pinning:** Already in shortcut menu (Pin LKG for offline).
- **Export/import profile:** Compatibility: “Export profile” (writes profile_safe_v1 to exported_safe_profile.json in app files), “Import profile bundle” (OPEN_DOCUMENT, parse JSON as Profile, save to ProfileStore).

## UI — Translation Params and menus

- **TranslationParamsFragment** — Menu: Translation params. Preset spinner (Compatible/Stable/Performance), “Reset to preset”, “Unlock advanced” (toggles advanced section).
- **Menus:** Main menu has Compatibility, Cloud config, Offline mode, Translation params, Components, Settings, About.

## Next steps (from the plan)

1. **Wire resolved component paths into the runner** — Use Resolver’s componentPaths/env when starting the game (today the existing Box64/Wine path is unchanged; Resolver runs in parallel).
2. **Config Packs (Milestone 2)** — PackManager, signature checks, Rules engine, Candidate staging.
3. **Offline polish (Milestone 3)** — Cache last N packs, pinning UI, clear cache tools.
4. **UI** — Game Settings Compatibility tab, Translation Params screen, Cloud Config, Offline Mode (per GAMEHUBPP_SPEC.md).

## Building

This is the brunodev85 Winlator project. Use Android Studio or the Gradle wrapper (if added) to build. The project has no `gradlew` in the repo root; use your IDE or install Gradle and run from the project root.

## Files added/edited

- `app/src/main/java/com/winlator/LaunchGateActivity.java` (new)
- `app/src/main/java/com/winlator/SteamAuthPrefs.java` (new)
- `app/src/main/java/com/winlator/ComponentsFragment.java` (new)
- `app/src/main/java/com/winlator/core/ComponentApiClient.java` (new)
- `app/src/main/java/com/winlator/MainActivity.java` (launcher intent moved to LaunchGateActivity; added Components menu case)
- `app/src/main/java/com/winlator/SettingsFragment.java` (Sign out button)
- `app/src/main/AndroidManifest.xml` (LaunchGateActivity as launcher; intent-filter for gamehub-open)
- `app/src/main/res/layout/activity_launch_gate.xml` (new)
- `app/src/main/res/layout/components_fragment.xml` (new)
- `app/src/main/res/layout/settings_fragment.xml` (Sign out section)
- `app/src/main/res/menu/main_menu.xml` (Components item)
- `app/src/main/res/values/strings.xml` (Steam and Components strings)
