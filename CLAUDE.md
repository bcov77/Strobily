# Strobily repository guide

This is a legacy Java Android strobe-light app with a native C timing engine.
The notes below describe the code and Brian's development setup inspected in
November 2026. Check the actual Gradle files and attached devices if they change.

## Brian's Android environment

**Source `~/.android_rc` before running Gradle, SDK tools, or adb.** This machine
intentionally uses directories outside the standard Android/Gradle locations:

| Variable | Value |
| --- | --- |
| `ANDROID_HOME` | `$HOME/android_dev/sdk` |
| `ANDROID_USER_HOME` | `$HOME/android_dev/android` |
| `ANDROID_EMULATOR_HOME` | `$HOME/android_dev/emulator` |
| `ANDROID_AVD_HOME` | `$HOME/android_dev/emulator/avd` |
| `GRADLE_USER_HOME` | `$HOME/android_dev/gradle` |
| `STUDIO_PROPERTIES` | `$HOME/android_dev/studio/idea.properties` |

The rc file adds SDK `platform-tools`, `emulator`, and
`cmdline-tools/latest/bin` to `PATH`. Its comment says the environment settings
also belong in `~/Library/LaunchAgents/com.user.loginenv.plist` for GUI apps.
Do not assume the SDK is in `~/Library/Android/sdk`, or Gradle caches in
`~/.gradle`.

The shell used during this session already had JetBrains Runtime 21 configured.
Check `java -version` and `/usr/libexec/java_home -V` if Java is missing or wrong;
`~/.android_rc` itself does not select a JDK. A local fallback is:

```sh
export JAVA_HOME="$(/usr/libexec/java_home -v 21)"
export PATH="$JAVA_HOME/bin:$PATH"
```

Use the checked-in `./gradlew`; a system `gradle` command is not required.
On other machines, set `ANDROID_HOME` or use an untracked `local.properties`
containing `sdk.dir`.

## Build configuration and commands

Configured toolchain:

- Android compile/target API 37 (Android 17); minimum API 21 (Android 5.0).
- Android Gradle Plugin 9.4.1; Gradle wrapper 9.6.0, with distribution checksum.
- JDK 17 minimum for AGP; this project was built and tested with JDK 21.
- SDK Build Tools 36.0.0; NDK 27.0.12077973 pinned in `StrobeLib/build.gradle`.
- Dependency/plugin repositories are configured in root `settings.gradle`:
  Google Maven, Maven Central, and the Gradle plugin portal as appropriate.

```sh
source ~/.android_rc
# Only needed if these SDK components are missing:
sdkmanager "platforms;android-37.0" "build-tools;36.0.0" "ndk;27.0.12077973"

./gradlew :Strobe:assembleDebug :StrobilyAd:assembleDebug
./gradlew :Strobe:lintDebug :StrobilyAd:lintDebug
./gradlew :Strobe:bundleRelease :StrobilyAd:bundleRelease
```

**New module outputs go in `.build/<module>/`, not `<module>/build/`.** Root
`build.gradle` redirects them because the original repository contains tracked,
stale build artifacts. Do not treat those old artifacts as current source or
validation results. `.gitignore` excludes new build/cache directories, `.cxx`,
local SDK configuration, and Android Studio project files.

- APK: `.build/Strobe/outputs/apk/debug/Strobe-debug.apk` (and the equivalent
  `StrobilyAd` path).
- Bundle: `.build/<module>/outputs/bundle/release/<module>-release.aab`.
- Lint reports: `.build/<module>/reports/lint-results-debug.html`.
- Release signing is unconfigured. Publishing requires the existing upload key
  and a version code higher than the current Play Console version.

There are existing deprecated Java/Android APIs and AGP option warnings. Do not
turn a focused task into a broad AndroidX or legacy-code migration just to remove
warnings. `gradle.properties` currently retains non-final resource IDs and
transitive R classes for the legacy code.

## Modules and entry points

| Module | Role |
| --- | --- |
| `Strobe` | App package `com.tp77.Strobe`; launcher `StrobeActivity`. |
| `StrobilyAd` | Separate app package `com.tp77.StrobeAd`; launcher `StrobeAdActivity`. |
| `StrobeLib` | Shared activities, tabs, resources, service, pattern storage, and native engine. Most feature work belongs here. |
| `PASupportV4` | Bundled modified support library. Source packages and Gradle namespace are `android.support.v42`. |

Both launcher activities immediately open `com.tp77.StrobeLib.MainActivity` and
finish. The shared activity detects the app variant from its package name.
The app modules own their manifests and package-specific FileProvider authorities;
check both manifests when changing permissions or services.

The project also depends on `com.android.support:support-v4:28.0.0`. It coexists
with the modified `v42` library. Giving `PASupportV4` the standard
`android.support.v4` namespace causes a duplicate-namespace build failure.

## Shared Java code

Java sources are under `StrobeLib/src/main/java/com/tp77/StrobeLib/`; layouts are
under `StrobeLib/src/main/res/layout/`.

- `MainActivity`: current app controller, tab navigation, service binding,
  camera selection/setup, LED/screen selection, torch, shortcuts, and common
  start/stop/update methods.
- `MyFragment`: shared tab base class. `updateSliders(...)` refreshes controls;
  it runs on resume and when a tab becomes visible. `refreshFragments()` on the
  activity recreates tabs for settings that affect layout.
- `StrobeFragment`, `TachoFragment`, `MusicFragment`, `TurntableFragment`:
  mode-specific controls. Main layouts use `frag_*_fragment.xml` names.
- `SettingsFragment`, `AdvancedFragment`: preferences and configuration.
  Preferences use `PreferenceManager.getDefaultSharedPreferences(...)`, with
  keys usually defined beside the feature. Existing helpers include
  `MainActivity.apply(Editor)` and `SettingsFragment.togglePref(...)`.
- `PatternFragment`: pattern editing, preview, and playback.
- `WidgetFragment`: creates pattern shortcuts; execution comes back through
  `MainActivity.doWidget()`.
- `TutorialDialog` and `EveryDialog`: setup and other dialogs.
- `DiagnosticFragment`/`DiagnosticView`: native timing diagnostics.
- Custom controls include `VerticalSeekbar`, `ReversedSeekBar`, `TitleScroller`,
  and `ViewPagerFixed`. Ordinary SeekBars and the reversed duty-cycle control
  are distinct widgets.

`S_STROBE`, `S_TACHO`, `S_OTHER`, and `S_WIDGET` identify playback sources.
**`S_OTHER` is shared by music, turntable, and patterns**, not a unique music
identifier. For example, it prevents volume-button frequency changes in these
modes. Music operation also has a separate `mMusicing` flag.

## Service and native timing engine

`StrobeService` is the current Android service, not a network server. It owns
playback state, wake locks, foreground notifications, music processing, and a
high-priority thread entering JNI.

Typical path:

```text
Fragment -> MainActivity start/update methods -> StrobeService.controlUpdate()
         -> native update() -> native timing loop -> Android camera calls
```

Music is an important exception: its beat-processing worker lives inside
`StrobeService` and calls `controlUpdate()` directly. Changes only in the activity
can miss that path.

- Timing arrays contain alternating ON/OFF durations **in microseconds**.
  The current engine supports up to 10,000 duration entries / 5,000 flashes.
- Native `update()` publishes a timing sequence using two buffers and resets
  playback position/diagnostics when the loop adopts it.
- `settingsUpdate()` calls native `setSettings()` for settings that can change
  during playback without replacing the sequence.
- `StrobeService` loads **`NDK3`**, built from **`src/main/jni/new.c`**. That is
  the current timing engine; do not assume `native.c` is the active one.
- Legacy `StrobeLibActivity` and `StrobeLibService` remain in the tree/manifest.
  The old service loads `NDK2`, built from `native.c`. Current launchers use
  `MainActivity`/`StrobeService`, though some old constants/classes are reused.
- `Android.mk` builds both native libraries with ndk-build. `Application.mk`
  selects API 21 and armeabi-v7a, arm64-v8a, x86, and x86_64, with flexible
  page-size support for 16 KB devices.

There are two camera paths: a legacy opened `android.hardware.Camera` with
flash-mode parameters, and a newer `CameraManager` path that controls the torch
without opening a camera capture session. Native code calls Java camera APIs
through JNI. Screen flashing is separate: native callbacks post messages through
`StrobeService.mScreenPost` to update the activity background on the UI thread.

The current engine shares simple settings between JNI callers and the timing
thread. Brian has explicitly preferred avoiding new mutexes/deadlock risks for
simple live integer updates; preserve that context when proposing changes.

## AUX audio output

AUX uses Java `AuxAudioOutput` (`AudioTrack`) and the sample-clock renderer
`AuxWaveform`. It consumes the same timing arrays from `StrobeService.controlUpdate()`
as other outputs, including direct music-worker calls, but runs independently of
camera/screen timing. **Keep AUX out of the JNI engine.** Voltage preferences are
signed hundredths in [-100, 100]; stereo is left/both/right (0/1/2).
The visible/enabled global custom-brightness setting scales both AUX levels live
(0% mutes, 100% preserves the configured high/low amplitudes).

AUX optionally saves and raises media volume for active playback, then restores it
unless the user changed volume or the route changed. Max volume defaults on; the
option for volume buttons to adjust media volume defaults off (normal frequency
controls remain the default). The headphone guard defaults on and accepts wired/USB
audio outputs; it confirms the actual route with silent preroll before emitting the
waveform or boosting volume. Bluetooth and built-in speakers do not qualify. Focus loss or an output disconnect latches AUX off until an explicit
stop/start or AUX toggle. A finite sequence drains before release; explicit Stop
pauses and flushes immediately. Native and audio burst completion are coordinated
in Java so native completion cannot truncate queued AUX samples. Persistent AUX-only
playback uses the `mediaPlayback` foreground-service type in both app variants.

## Pattern persistence

`PatternStorage` owns SQLite `patterns.db`, table `patterns`, currently database
version 1. `PatPart` represents pattern parts and expands them into timing arrays,
including referenced/nested patterns.

The stored pattern payload is a **Base64-encoded ObjectOutputStream** containing
part count, `PatPart.VERSION` (currently 2; version-1 payloads remain readable), and each part's explicitly written
fields. This is not simply one SQL column per part property. Future format
changes need to consider both the SQLite schema and this serialized format,
including existing saved patterns and import/export compatibility.

## Testing on Brian's phone

**Use `StrobilyAd` (`com.tp77.StrobeAd`) for device testing and normal development
installs. Do not install `Strobe` (`com.tp77.Strobe`) unless Brian explicitly asks
for that variant.** Build both variants when shared code changes. The shared
Android Studio configuration **Run Strobily** targets `StrobilyAd`.

The session used a USB-connected **Pixel 8 Pro running API 37**. Discover devices
each time rather than hardcoding its serial or assuming it is still connected:

```sh
source ~/.android_rc
adb devices -l
./gradlew :StrobilyAd:assembleDebug
adb install -r .build/StrobilyAd/outputs/apk/debug/StrobilyAd-debug.apk
adb shell am start -W -n com.tp77.StrobeAd/.StrobeAdActivity
```

Use `adb -s <serial>` if multiple devices are attached. Reinstall with `-r` to
preserve data. First-run setup and runtime permission dialogs can block UI tests.
Inspect app-specific logcat output and device state; launching successfully alone
does not establish that JNI, camera access, or background operation works.

There is a dependency-free hardware instrumentation runner at
`Strobe/src/androidTest/java/com/tp77/Strobe/BrightnessInstrumentation.java`.
Both app variants use this runner; `StrobilyAd` shares the test sources from
`Strobe/src/androidTest/java`. The runner class retains its `com.tp77.Strobe`
Java package, but the test target below is **`com.tp77.StrobeAd`**. This is a
custom instrumentation runner rather than a standard JUnit suite.
Its checks require a configured, unlocked API 33+ device with adjustable torch
strength and camera permission. It briefly flashes the LED, then stops playback
and restores the preferences it changed:

```sh
source ~/.android_rc
./gradlew :StrobilyAd:assembleDebug :StrobilyAd:assembleDebugAndroidTest
adb install -r .build/StrobilyAd/outputs/apk/debug/StrobilyAd-debug.apk
adb install -r .build/StrobilyAd/outputs/apk/androidTest/debug/StrobilyAd-debug-androidTest.apk
adb shell am force-stop com.tp77.StrobeAd
adb shell am instrument -w com.tp77.StrobeAd.test/com.tp77.Strobe.BrightnessInstrumentation
```

The instrumentation runner also exercises AUX output selection and silent real
`AudioTrack` playback: live updates, short bursts, focus loss, volume handling,
rapid restart, and background/rebind behavior. It temporarily raises media volume
while sending zero-valued PCM and restores its test settings. Pure waveform tests
run without a device:

```sh
source ~/.android_rc
./gradlew :StrobeLib:testDebugUnitTest
```

Run checks appropriate to the edited paths. Build both app variants for shared
code/manifest changes. Modern foreground-service declarations and runtime
permissions matter when testing background operation. `MainActivity` also applies
system-bar/cutout insets for current Android edge-to-edge requirements.

## Editing notes

Many existing Java, XML, and Gradle files use CRLF line endings; preserve them
when making focused edits. `git -c core.whitespace=cr-at-eol diff --check` avoids
reporting preserved carriage returns as newly introduced trailing whitespace.
Keep generated outputs and machine-specific IDE files out of commits. See
`README.md` for user-facing build instructions.
