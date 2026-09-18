# Backgammon for Android

A polished, completely offline Backgammon game built with native Kotlin and Jetpack Compose.

## Features
- Complete standard move rules: bar entry, hits, doubles, forced maximum dice usage, higher-die priority, bearing off, and wins
- Local pass-and-play and five-level on-device AI
- Five responsive board themes, three checker styles, and three dice styles
- Animated dice, forgiving touch targets, legal destination hints, sound and haptic feedback
- Persistent preferences and a portrait-first glass interface

## Build
Requires JDK 17 and Android SDK 35.

```bash
./gradlew testDebugUnitTest assembleDebug
```

The GitHub Actions **Android APK** workflow builds on pushes to `main` and manual dispatches. Download its `Backgammon-Android-APK` artifact.

## Controls
Roll, tap one of your checkers, then tap a glowing legal destination. Checkers on the bar must re-enter first. The status line shows the active side and remaining dice.
