# LanxX Game Booster

Android Native Java project. Minimum Android 10 (API 29), target SDK 35.

## Features
- Floating bubble overlay (drag to move, tap to open panel).
- Add/remove installed app shortcuts from the main screen.
- Launch saved apps from the floating panel.
- Simple session timer.
- Android settings shortcut.
- GitHub Actions workflow to build a debug APK.

## Build with GitHub Actions
1. Upload the complete project to a GitHub repository.
2. Open **Actions**.
3. Run **Build LanxX Game Booster APK** (or push to `main`).
4. Open the completed workflow run.
5. Download artifact **LanxX-Game-Booster-APK**.

## Build locally
Use JDK 17 and Gradle 8.9:
`gradle assembleDebug`

## First launch
1. Open LanxX Game Booster.
2. Tap **AKTIFKAN FLOATING WINDOW**.
3. Allow **Display over other apps** in Android settings.
4. Return to LanxX and tap activate again if needed.
5. Add apps using **TAMBAH APLIKASI**.
6. Tap the floating `LX` bubble to open shortcuts.

## Notes
- This is a shortcut/utility overlay, not a hardware overclock tool.
- Android controls which apps can be launched and whether overlays are allowed.
- It does not modify game files, inject code, automate gameplay, or bypass anti-cheat.
- `QUERY_ALL_PACKAGES` is used to populate the installed-app picker in this sideloadable project. Review Google Play policy before publishing.
