# AI Backgammon 1.0.0 release checklist

## Automated — completed in CI

- [x] Unit tests
- [x] Android Lint
- [x] Debug APK build
- [ ] Release AAB/APK workflow succeeds
- [ ] Production APK signature verifies

## Signing — owner action required

- [ ] Generate or select the permanent upload/release keystore
- [ ] Store two encrypted offline backups
- [ ] Configure `ANDROID_KEYSTORE_BASE64`
- [ ] Configure `ANDROID_KEYSTORE_PASSWORD`
- [ ] Configure `ANDROID_KEY_ALIAS`
- [ ] Configure `ANDROID_KEY_PASSWORD`
- [ ] Enable Google Play App Signing when creating the Play application

## Manual device matrix

- [ ] Android 8/9 phone
- [ ] Android 12 phone
- [ ] Android 14/15 phone
- [ ] Short ultra-wide landscape phone
- [ ] 7–11 inch tablet or foldable
- [ ] Low-memory device/process death and match restoration
- [ ] Persian/RTL system locale
- [ ] Large font/display scaling
- [ ] Sound disabled and enabled
- [ ] Haptics disabled and enabled

## Gameplay smoke test

- [ ] Blocked and open bar entry
- [ ] Hit and re-entry
- [ ] Forced use of both dice and higher die
- [ ] Direct compound move
- [ ] Four double moves
- [ ] Exact and oversized bear-off
- [ ] Gammon and backgammon scoring
- [ ] Human/AI Double, Take, Drop and Redouble
- [ ] Undo during and after AI activity
- [ ] Clock turn timeout and bank timeout
- [ ] Leave app, kill process and Continue Match
- [ ] Complete local two-player match

## Store submission

- [x] App name and launcher icon
- [x] 1024×500 feature graphic
- [x] English listing copy
- [x] Persian listing copy
- [x] Privacy policy content
- [x] Data Safety worksheet
- [ ] Host privacy page and enter its public URL
- [ ] Capture 4–8 real in-game screenshots without debug UI
- [ ] Complete content-rating questionnaire
- [ ] Set support email and website
- [ ] Confirm package ID `com.arena.backgammon` before first publication
- [ ] Upload signed AAB to Play internal testing
- [ ] Upload signed universal APK to Iranian stores
- [ ] Complete closed testing requirements applicable to the Play developer account
- [ ] Promote only after tester approval and crash-free smoke tests
