<p align="center">
	<img src="logo.png" width="376" height="128" alt="Steam APK Logo" />
</p>

# Steam APK

An Android app to run Windows (x86_64) games and applications with Wine and Box86/Box64. Built with a **Game Hub–style** UI: Steam library, controller support, and a clean dark interface.

## Features

- **Steam library** — See your owned games, recently played, and launch titles. Open the Steam Store in-app to browse and buy.
- **Device** — View connected gamepads and controllers.
- **Performance** — Quick access to compatibility settings, cache, and profiles.
- **Controller-friendly** — Navigation and hints (A Launch, B Back, Y Search, LB/RB tabs).

## Installation

1. Open **[Releases](https://github.com/loguefx/Steam_APK/releases)** and download the latest **app-debug.apk** (or the release you want).
2. On your Android phone (arm64-v8a), install the APK (enable "Install from unknown sources" if prompted).
3. Launch the app and complete any first-run setup (containers, etc.).

**No release yet?** Run the **Build and release APK** workflow: go to the [Actions](https://github.com/loguefx/Steam_APK/actions) tab → **Build and release APK** → **Run workflow**. When it finishes, the new release and APK will appear under [Releases](https://github.com/loguefx/Steam_APK/releases).

## Building

From the project root:

```bash
.\gradlew.bat assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`. For a signed release build, configure a signing config and run `assembleRelease`.

## Useful Tips

- If you see performance issues, try changing the Box64 preset to **Performance** in Container Settings → Advanced.
- For .NET Framework apps, install **Wine Mono** from Start Menu → System Tools → Installers.
- For older games that don't start, add `MESA_EXTENSION_MAX_YEAR=2003` in Container Settings → Environment Variables.
- Use shortcuts on the home screen to set per-game options (Box64 preset, exec args, etc.).
- For low-resolution games, enable **Force Fullscreen** in shortcut settings.
- For Unity games, try Box64 preset **Stability** or add exec argument `-force-gfx-direct`.

## Credits and third-party

This project builds on the following:

- [Winlator](https://github.com/brunodev85/winlator) — base Android/Wine/Box86/Box64 integration
- GLIBC patches — [Termux Pacman](https://github.com/termux-pacman/glibc-packages)
- [Wine](https://www.winehq.org/)
- [Box86/Box64](https://github.com/ptitSeb) by ptitseb
- [Mesa](https://www.mesa3d.org) (Turnip/Zink/VirGL)
- [DXVK](https://github.com/doitsujin/dxvk), [VKD3D](https://gitlab.winehq.org/wine/vkd3d), [CNC DDraw](https://github.com/FunkyFr3sh/cnc-ddraw)

Thanks to everyone involved in these projects.
