# Releases

## Building the APK

This project builds a **Game Hub–style** Android app with:

- **Steam library** — Recently played, All games grid, Store/Discover (opens Steam Store in browser)
- **Device** — Lists connected game controllers
- **Performance** — Quick access to Compatibility (device info, cache, profiles)
- Dark theme and controller hints (A Launch, B Back, Y Search)

**Build:** From the project root run:

```bash
.\gradlew.bat assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`. You can copy it to `releases/` locally for testing (APKs in this folder are in .gitignore to avoid large pushes).

**Note:** Debug build is arm64-v8a. For release builds, configure a signing config and use `assembleRelease`.
