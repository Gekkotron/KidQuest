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

## CI and distribution

GitHub Actions (`.github/workflows/ci.yml`) runs JVM and iOS tests on every push and pull request. On push to `main`, it builds a **signed release APK**, uploads it as a workflow artifact, and distributes it to the `testers` group on Firebase App Distribution. On PRs and other branches, it builds `assembleDebug` only (no signing, no distribution).

### Firebase secrets

- Secret `FIREBASE_ANDROID_APP_ID` — the Android app ID from the Firebase console (`1:...:android:...`).
- Secret `FIREBASE_SERVICE_ACCOUNT` — the full JSON content of a service account with the *Firebase App Distribution Admin* role.
- Optional variable `FIREBASE_TESTER_GROUPS` — comma-separated group aliases (defaults to `testers`).

### Release signing

Generate a release keystore once (keep it safe — losing it means losing the ability to publish updates to the same install):

```bash
keytool -genkeypair -v \
  -keystore kidquest-release.jks \
  -alias kidquest \
  -keyalg RSA -keysize 2048 \
  -validity 10000 \
  -storetype JKS
```

Then base64-encode it and add the following GitHub secrets:

```bash
base64 -i kidquest-release.jks | pbcopy   # macOS
```

- Secret `KIDQUEST_KEYSTORE_BASE64` — the base64-encoded `.jks` contents.
- Secret `KIDQUEST_KEYSTORE_PASSWORD` — the store password you set.
- Secret `KIDQUEST_KEY_ALIAS` — the alias (`kidquest` above).
- Secret `KIDQUEST_KEY_PASSWORD` — the key password (often the same as the store password).

For local release builds, add the same keys to `local.properties` and also set `KIDQUEST_KEYSTORE_PATH` to the local `.jks` path. Without them, `assembleRelease` falls back to the debug keystore so you can still build locally.

## License

MIT © Gekkotron
