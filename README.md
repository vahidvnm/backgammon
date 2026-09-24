# AI Backgammon for Android

A polished, completely offline backgammon game built with native Kotlin and Jetpack Compose.

## Features

- Complete standard movement rules: bar entry, hits, doubles, forced maximum dice usage, higher-die priority, bearing off and compound checker routes
- Four on-device AI personalities and five difficulty levels
- Local pass-and-play
- Standard doubling cube with ownership, Take, Drop and Redouble
- Optional coaching, match clocks, unlimited in-session Undo, automatic match restoration, sound and haptics
- Responsive landscape layouts, crafted themes, physical dice and polished checker animation
- No account, ads, analytics, tracking or internet permission

## Development build

Requires JDK 17 and Android SDK 35.

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug
```

The **Android APK** GitHub Actions workflow runs tests and lint and uploads `AI-Backgammon-Debug-APK`.

## Release build

```bash
./gradlew testReleaseUnitTest lintRelease bundleRelease assembleRelease
```

This produces an Android App Bundle and release APK. They remain unsigned unless all four signing environment variables are supplied:

```text
ANDROID_KEYSTORE_PATH
ANDROID_KEYSTORE_PASSWORD
ANDROID_KEY_ALIAS
ANDROID_KEY_PASSWORD
```

The manual **AI Backgammon Release** workflow can create signed production binaries after these repository secrets are configured:

```text
ANDROID_KEYSTORE_BASE64
ANDROID_KEYSTORE_PASSWORD
ANDROID_KEY_ALIAS
ANDROID_KEY_PASSWORD
```

Never commit the keystore or its passwords. Keep at least two secure offline backups. The same signing identity is required for future updates outside Google Play App Signing.

## Store material

- Final icons and feature graphic: `branding/store/`
- English and Persian listing copy: `store-listing/`
- Data Safety worksheet: `store-listing/data-safety.md`
- Privacy policy source and web page: `docs/`
- Third-party attribution: `THIRD_PARTY_NOTICES.md`

## Controls

Roll, tap an illuminated checker, then tap a sliding legal destination arrow. Compound destinations can consume two dice—or all available double moves—with one direct action when the complete route is legal.
