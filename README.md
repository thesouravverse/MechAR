# MechAR

Solo AR target-practice game for Android. Detect a floor → tap to deploy a
mech robot in your room → tap the mech to fire and bring it down.

**v1 scope**: one mech, single player, no AI, no multiplayer.
**v2 onward**: walking AI, custom Blender-made mechs, mech-vs-mech, multiplayer.

## Stack

- Kotlin + Jetpack Compose + Material 3
- **ARCore 1.45** + **SceneView 2.2** (Filament-based) for AR
- minSdk 24, targetSdk 35, JDK 17
- Cloud-built via GitHub Actions — no local Android SDK needed

## Project layout

```
MechAR/
  app/src/main/
    java/com/thesouravverse/mechar/
      MechARApp.kt
      MainActivity.kt          # permission gate + entry
      ui/
        GameScreen.kt          # HUD overlay
        MechARScene.kt         # ARCore + Filament scene
        GameViewModel.kt       # HP / score state
        theme/Theme.kt
    res/                       # icons, theme, strings
    assets/models/mech.glb     # ← drop your mech model here (see folder README)
    AndroidManifest.xml
  .github/workflows/android.yml
```

## Iteration loop (no local Android SDK)

1. Edit Kotlin in VS Code.
2. `git push` → GitHub Actions builds debug APK (~5 min).
3. Download `MechAR-debug-apk` artifact from the Actions run.
4. Transfer APK to your phone (Drive / USB / email).
5. Tap to install.

## Release build

```bash
# Bump versionCode + versionName in app/build.gradle.kts, then:
git tag v1.0.0
git push --tags
```

Actions builds and signs `MechAR-release-aab`. Download → upload to
Play Console internal testing.

## Required GitHub Secrets (release builds only)

- `KEYSTORE_B64` — base64 of your `.jks`
- `KEYSTORE_PASSWORD`
- `KEY_ALIAS`
- `KEY_PASSWORD`

## Adding a real mech model

See [app/src/main/assets/models/README.md](app/src/main/assets/models/README.md).
Until you drop one in, the app falls back to a red metallic cube.

## Phone requirements

- Android 7.0+ (API 24+)
- **ARCore supported** — Samsung S23 Ultra ✓. Full list:
  https://developers.google.com/ar/devices
- Camera permission (the app asks on first launch)
