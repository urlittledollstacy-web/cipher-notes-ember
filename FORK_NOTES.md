# Cipher Ember — fork notes

A look-and-feel fork of [CipherApps/cipher-notes](https://github.com/CipherApps/cipher-notes)
(MIT). The visual language is "Ember Archive": warm dark paper, ember accent,
brass for sealed notes, Fraunces / Hanken Grotesk / JetBrains Mono type.

## What changed

Visual:
- `ui/theme/CipherTheme.kt` — Ember Archive color scheme and typography.
- `res/values/colors.xml`, `res/values/themes.xml` — window / status bar colors.
- `res/font/` — Fraunces, Hanken Grotesk, JetBrains Mono (OFL).
- `ui/screens/ListScreen.kt` — app bar, "no network" ribbon, title/subtitle.
- `ui/components/EncryptDialog.kt` — clearer sealing copy.
- `ui/components/NoteCard.kt` — `SEALED` tag instead of a padlock emoji.
- `widget/Widget.kt` — Ember palette.

Security-hardening (small, deliberate, reviewable):
- `widget/Widget.kt` — the home-screen widget never renders note bodies. Only
  the title is shown, and only when the user opts in. Previously plaintext note
  content could be previewed on the home screen.
- `ui/screens/NoteEditorViewModel.kt` — unlock attempts use exponential
  backoff after 5 failures, to slow down passphrase guessing.
- New notes prompt to be sealed on creation by default
  (`ui/screens/SettingsViewModel.kt`, `ListScreen.kt`, `CipherMainApp.kt`,
  `SettingsScreen.kt`).

Untouched, as intended:
- `crypto/CryptoManager.kt` — AES-256-GCM, PBKDF2, EncryptedSharedPreferences.
- No new permissions, no network access, no telemetry.

## Build

Requires JDK 21 and Android SDK 36.

```bash
./gradlew assembleDebug     # APK at build/outputs/apk/debug/
./gradlew testDebugUnitTest # widget privacy rules
```

## Getting a testable APK

`.github/workflows/android-ci.yml` runs on every push and pull request. It
runs the unit tests and builds a debug APK, uploaded as the
`CipherEmber-debug-apk` artifact (30 day retention).

Grab it from the Actions tab: open the latest run, then Artifacts ->
`CipherEmber-debug-apk`. Artifact downloads require being signed in to
GitHub. Unzip and install:

```bash
adb install CipherEmber-debug.apk
```

The release variant is deliberately not built here: `assembleRelease`
produces an *unsigned* APK (no signing config in `build.gradle.kts`), which
Android will not install. Signing a release build needs a keystore, which
is a decision for the maintainer, not CI.

## Gotchas hit during the reskin

- `SavedStateHandle.get()` is **not** observable. State read this way during
  composition does not recompose when it changes (`markAuthenticated()` set
  the handle and the lock screen never advanced). Use `mutableStateOf`, or
  `getStateFlow` / `getLiveData` if the handle must be the source of truth.
- Async reads must not be guessed with `collectAsState(initial = ...)`.
  DataStore/Room have not emitted on the first frame after a rotation, so the
  `initial` value is what gets painted. Guessing `dynamicColors = true` flashed
  the Material You theme over a custom one; guessing `isAppLockEnabled = false`
  composed the notes before the lock state arrived (fail-open). Collect with
  `initial = null` and render only the window background until known.
- Fixing the gate in `MainActivity` is **not enough**. Any screen that
  re-reads the same DataStore keys with its own `initial` guesses re-creates
  the bug below the gate. `SettingsScreen` did: seven guessed keys made the
  theme radio draw "Ember Archive" (`ThemeMode.DEFAULT`) and the dynamic
  colours toggle draw on, for a frame, on rotation. Resolve the screen's
  settings as one snapshot and gate on that, rather than one `initial` per key.
- `ThemeMode.DEFAULT == EMBER` ("Ember Archive"), the first enum entry. It is
  what a guessed theme value shows.
- The unit tests are pure logic (hashing, throttle backoff, theme constants,
  widget text). They do not exercise Compose, so state-reactivity and layout
  regressions are invisible to them; those need a device.
