# Cipher Notes (Ember)

A reskin fork of [`CipherApps/cipher-notes`](https://github.com/CipherApps/cipher-notes),
tracked at `urlittledollstacy-web/cipher-notes-ember`.

**Scope: UI and polish only. The encryption and core logic stay untouched.**
Title text is deliberately left visible (plaintext) so notes stay findable in
the list; only the body is sealed.

Remotes: `fork` = the reskin repo (push here). `origin` = upstream `CipherApps`.

## Build and test

Toolchain: **JDK 21**, **Android SDK platform 36 + build-tools 35.0.0**,
Gradle **8.10**, AGP **8.5.2**, Kotlin **2.0.21**, Room **2.6.1**.
`compileSdk`/`targetSdk` 36, `minSdk` 26.

```bash
export JAVA_HOME=/tmp/tools/jdk21
export ANDROID_HOME=/tmp/tools/sdk
export ANDROID_SDK_ROOT=/tmp/tools/sdk
./gradlew assembleDebug testDebugUnitTest
```

Results land in `build/test-results/testDebugUnitTest/`; the APK in
`build/outputs/apk/debug/CipherNotes-debug.apk`. CI is
`.github/workflows/android-ci.yml` (JDK 21, drives `sdkmanager` directly
because `setup-android` fails on this image).

### The SDK is not persisted

`/tmp/tools` is wiped by environment resets, so the toolchain often needs
reinstalling before the first build. Grab it with curl:

```bash
curl -sL -o /tmp/tools/jdk21.tar.gz \
  "https://api.adoptium.net/v3/binary/latest/21/ga/linux/x64/jdk/hotspot/normal/eclipse"
curl -sL -o /tmp/tools/cmdline-tools.zip \
  "https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip"
```

`unzip` is not installed; extract with Python (`zipfile`) and `chmod +x` the
`cmdline-tools/latest/bin` scripts. `local.properties` already points
`sdk.dir` at `/tmp/tools/sdk`. Run Gradle with the exports in one shell
(`export ...; ./gradlew ...`) since env does not carry into a backgrounded
`bash -c`.

## Testing

`testDebugUnitTest` is pure JVM JUnit: **48 tests**, covering the widget text
helper, lockout/pin/throttle crypto, and lock layout/theme. There is **no
Robolectric or instrumentation infrastructure** and no device/emulator in CI,
so anything needing an Android runtime (Room migrations, DataStore, Compose
behaviour) cannot be unit-tested here. Say so rather than implying coverage.

Prefer verifying DB changes by reproducing both schemas in plain SQLite and
diffing `PRAGMA table_info` against Room's generated `NoteDatabase_Impl`.

## Workflow and conventions

* **Never push to `main`/`master` without explicit user consent.**
* Experimental branches are treated as draft PRs based on their parent branch.
* The user tests each change on a real device and replies `(no work)` when the
  result is not yet resolved; a plain confirmation means it is verified.
* Merges need explicit user consent. Merge with the squash-merge flow used on
  previous PRs.
* Comments and commit messages should explain *why*, not restate the diff.

## Landmarks

Encryption lives in `crypto/`: `CryptoManager` seals as `salt + iv +
ciphertext` (AES-GCM) then Base64. `LockoutController` + `DataStoreLockout-
StateStore` back the escalating note-unlock delay (capped ~320s). Room schema
is in `data/NoteDatabase.kt`; DI in `di/AppModule.kt`. Both the editor and the
to-do checklist unlock through `NoteEditorViewModel.unlock()`.

## Fix history on `main`

| Commit | Change |
| --- | --- |
| `513b73d` | Swipe-delete commits immediately; closing the app can no longer revive a deleted note. Device-verified. |
| `b773b3c` | Settings DataStore survives a corrupt file instead of locking the user out (#18). |
| `eb2bd5a` | Appearance: opt-in dynamic colors, always-light Light, readable switches (#17). |
| `f970be3` | One Unlock tap = one password attempt (#16). |
| `f85c8d4` | Note lock remembers wrong passwords (#15). |

### Pending

* **PR #19** — tell a damaged note apart from a wrong password (SHA-256
  ciphertext fingerprint + `Migration(1, 2)` adding `ciphertextHash`). Draft,
  CI green, **not merged**. It predates `513b73d`, so it needs a rebase onto
  `main` before it can merge.
