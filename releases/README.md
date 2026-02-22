# Releases

## winlator-gamehub-debug.apk

Debug build of Winlator with Game Hub-style UI:

- **Steam library**: Recently played, All games grid, Store/Discover (opens Steam Store in browser)
- **Device**: Lists connected game controllers
- **Performance**: Quick access to Compatibility (device info, cache, profiles)
- Dark theme, controller hints (A Launch, B Back, Y Search)

**Build:** From the project root run `.\gradlew.bat assembleDebug`. The APK is written to `app/build/outputs/apk/debug/app-debug.apk`. You can copy it to `releases/` locally for testing (that folder is in .gitignore for APKs to avoid large pushes).

**Note:** Debug build is arm64-v8a. For release builds, configure a signing config and use `assembleRelease`.
