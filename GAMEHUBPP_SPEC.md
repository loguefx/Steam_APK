# GameHub++ (Stable PC Games on Android) — Full Spec

**Project:** GameHub++  
**Goal:** Build a GameHub-style Android app that launches PC games via Wine/Proton, x86→ARM translation (FEX), and GPU drivers (Turnip) with:

- No random breakage when cloud configs or components update
- Deterministic stability (no prediction or dynamic tuning)
- Known-good profiles, rollback, pinning
- Side-by-side component versions (Proton/Wine/Turnip/FEX), never forced overwrite
- Offline mode (cached packs + pinned LKG)
- Validation to block invalid combos before launch
- One-time fallback after crash (Standard→Safe), never endless tweaking
- Preserve existing core logic; add orchestration + packaging + safety

## High-level concept

GameHub++ is a **runtime orchestrator + content system**.

- **Runtimes:** Proton/Wine builds, FEX builds, Turnip drivers
- **Profiles:** Game settings (compat layer, translation preset, driver, surface format, etc.)
- **Packs:** Versioned signed "config packs" from cloud (optional), safely staged
- **Local system** always maintains **LKG (Last Known Good)** per game. We do not "optimize" dynamically; we only select known-good bundles and roll back if something breaks.

## Key definitions

**Game identity:** Uniquely identified by `exe_path` (relative within prefix), `exe_sha256`, optional `product_name` from PE, optional game version.

**Components (side-by-side):** `compat_layer` (wine/proton), `translator` (fex), `gpu_driver` (turnip); optional dxvk/vkd3d, audio driver packs, input packs.

**Profiles:** Select component IDs + settings: compat layer ID, turnip driver ID, translation preset ID + overrides, surface format, audio driver, dinput priority, toggles (e.g. skip video decode).

**Safe / Candidate / LKG (per game):**
- **Safe:** Minimal compatibility profile (bundled in app)
- **Candidate:** Newest downloaded profile not yet trusted
- **LKG:** Last confirmed stable profile

## Non-goals

- No ML, no prediction, no continuous dynamic scaling
- No invasive background killing beyond a small allowlisted list
- No forced component updates that overwrite working installs

## Architecture modules

1. **ComponentManager** — Install, list, validate, remove component versions; store side-by-side; channels (stable/beta/custom); dependency metadata (ABI, Android min, GPU family); **never replace** an existing component ID (immutable). Storage: `components/compat/`, `components/driver/`, `components/translator/` under app files.
2. **ProfileStore** — Persist per-game profiles (Safe/Candidate/LKG); profile pinning; history of last N profiles for rollback.
3. **PackManager** — Download config packs from backend; verify signature; store in cache; stage as **Candidate only**; support offline (cached packs + bundled baselines).
4. **Resolver** — Determine active profile for launch: if pinned → use pinned (usually LKG); else if Candidate exists and opted-in/validated → Candidate; else LKG; else Safe. Verify components exist and match device constraints; output resolved profile + component paths + env + launch args.
5. **Validator** — Block invalid combos before launch: component ABI vs device, GPU driver vs GPU family, compat/translator compatibility, profile constraints, known-bad denylist. Return OK / WARN+suggested fix / BLOCK+"Apply Safe Mode".
6. **Launcher** — Build process env, apply translation params, launch game process, monitor exit/ANR/crash; keep existing logic, feed it resolved plan.
7. **CrashMonitor + Promotion/Rollback** — Detect crash signals; session result (launch_success > Xs, stability_pass > Y min); if Candidate and stability_pass → promote to LKG; if Candidate and crash → rollback to previous LKG. **One-time fallback:** if current profile crashes quickly (<60s), next launch use Safe once; if Safe also crashes → "Hard incompatibility" screen. No infinite loops.

## Implementation milestones

- **Milestone 1 — Local-only stability:** ComponentManager (install/list by folder), ProfileStore with Safe baseline + LKG, Resolver + Validator (no cloud), CrashMonitor + session records + one-time fallback.
- **Milestone 2 — Config packs (cloud but safe):** PackManager with signature checks, Rules engine, stage as Candidate only, promotion/rollback from session stability.
- **Milestone 3 — Offline-first polish:** Cache packs (last N), pinning UI, export/import profile bundles, clear cache tools (shader-only, temp-only).

## Technical details

- **Device fingerprint:** Android version, SoC/GPU family, device model, Vulkan version, RAM (for gating only).
- **Component immutability:** Once installed, component ID = fixed directory; never overwrite.
- **Safe baseline:** Ship `safe_profile.json` (and optionally per-GPU) in app assets; on first run copy to ProfileStore.
- **Time thresholds (constants):** `launch_success_seconds = 20`, `stability_pass_seconds = 300`.

## Deliverables (files)

- **Core (Java):** `gamehubpp/ComponentManager.java`, `ProfileStore.java`, `PackManager.java`, `Resolver.java`, `Validator.java`, `CrashMonitor.java`; models: `ComponentManifest.java`, `Profile.java`, `GameProfileState.java`, `DeviceInfo.java`, `SessionRecord.java`.
- **UI:** Game Settings Compatibility tab, Translation Params screen, Cloud Config screen, Offline Mode screen.
