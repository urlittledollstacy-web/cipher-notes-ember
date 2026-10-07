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
