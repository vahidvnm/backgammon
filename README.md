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

## Flavors

The app ships two marketplace flavors of the same game, selected with the `store` flavor dimension:

| Flavor | Store | Package queried for store actions |
| --- | --- | --- |
| `myket` | مایکت | `ir.mservices.market` |
| `bazaar` | کافه‌بازار | `com.farsitel.bazaar` |

Every Gradle task therefore carries the flavor name. Both flavors must always be built and tested together, because they only differ in `BuildConfig` store metadata.

## Development build

Requires JDK 17 and Android SDK 35.

```bash
./gradlew testMyketDebugUnitTest testBazaarDebugUnitTest lintMyketDebug lintBazaarDebug assembleMyketDebug assembleBazaarDebug
```

The **Android APK** GitHub Actions workflow runs both flavors' tests and lint and uploads `Smart-Backgammon-Marketplace-Debug-APKs`. CI debug builds use the repository's dedicated non-production `app/ci-debug.p12` key, so debug APKs from future runs can update one another. This key is public by design and must never be used for a store release.

## Release build

```bash
./gradlew testMyketReleaseUnitTest testBazaarReleaseUnitTest lintMyketRelease lintBazaarRelease bundleMyketRelease bundleBazaarRelease assembleMyketRelease assembleBazaarRelease
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

Never commit the keystore or its passwords. Keep at least two secure offline backups. The same signing identity is required for future updates outside Google Play App Signing. `.gitignore` rejects `*.jks`, `*.keystore`, `*.p12` and `keystore.properties` so a signing key can never be committed by accident.

## Continuous integration

| Workflow | Trigger | Purpose |
| --- | --- | --- |
| `android-build.yml` | pull requests into `main`, pushes to `main`, pushes to `arena/**`, manual | Unit tests, lint and debug APKs for both flavors |
| `android-emulator-test.yml` | pull requests into `main`, pushes to `main`, manual | Instrumented game journeys on an API 35 emulator |
| `android-release.yml` | tags matching `v*`, manual dispatch | Signed release bundles, APKs and the Cafe Bazaar BIN |

Pull requests must be green before merging. The release workflow never receives a branch push: production binaries are only produced from a version tag or an explicit manual run. When the signing secrets are absent it still builds and uploads unsigned validation binaries and marks the run with a warning instead of failing.

## Store material

- Final icons and feature graphic: `branding/store/`
- English and Persian listing copy: `store-listing/`
- Data Safety worksheet: `store-listing/data-safety.md`
- Privacy policy source and web page: `docs/`
- Third-party attribution: `THIRD_PARTY_NOTICES.md`

## Controls

Roll, tap an illuminated checker, then tap a sliding legal destination arrow. Compound destinations can consume two dice—or all available double moves—with one direct action when the complete route is legal.
