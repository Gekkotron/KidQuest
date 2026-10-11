# KidQuest

A standalone family chore-points app for Android and iOS, built with Kotlin Multiplatform and Compose Multiplatform.

- One-tap quick buttons (+10, +20, +100, −10) and a configurable list of point actions, with undo
- Solo or shared goals with progress bars, confetti when reached, and redemption with an adjustable split
- Levels from lifetime points and daily streaks
- All data stays on the device (Room). French and English.

## Build

Requirements: JDK 17, Android SDK 36. iOS needs Xcode and [XcodeGen](https://github.com/yonaskolb/XcodeGen).

```bash
./gradlew :shared:jvmTest :composeApp:jvmTest   # all logic + UI tests on the JVM
./gradlew :androidApp:installDebug              # run on a connected device / emulator
```

iOS:

```bash
brew install xcodegen
cd iosApp && xcodegen && open KidQuest.xcodeproj
```

## Layout

- `shared/` — domain calculators and the Room data layer (Android, iOS, JVM)
- `composeApp/` — Compose Multiplatform UI, view models, Koin DI
- `androidApp/` — Android application entry point
- `iosApp/` — SwiftUI host for the Compose UI

Design: `docs/superpowers/specs/2026-10-11-kidquest-design.md`

## License

MIT © Gekkotron
