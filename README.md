# Strobily
Open source android strobe light


Not much here yet. If you use this code for something cool. Shoot me an email a bcoventry77@gmail.com


Apache 2.0 license. So feel free to do just about anything besides creating an app called Strobily and trying to impersonate this one.

## Building

The project compiles and targets Android 17 (API 37), using Android Gradle
Plugin 9.4.1, Gradle 9.6.0, and NDK 27.0.12077973. Android 5.0 (API 21) is
the minimum supported version, matching the native toolchain. Use JDK 17 or
newer (tested with JDK 21).

On Brian's machine, load the custom SDK and Gradle paths first:

```sh
source ~/.android_rc
sdkmanager "platforms;android-37.0" "build-tools;36.0.0" "ndk;27.0.12077973"
./gradlew :Strobe:assembleDebug :StrobilyAd:assembleDebug
./gradlew :Strobe:bundleRelease :StrobilyAd:bundleRelease
```

On other machines, set `ANDROID_HOME` to your SDK directory or set `sdk.dir`
in an untracked `local.properties` file. Build outputs go under `.build/`
because this legacy repository includes tracked outputs in the module `build/`
directories.

- `Strobe`: package `com.tp77.Strobe`, version 2.2.6 (55).
- `StrobilyAd`: package `com.tp77.StrobeAd`, version 2.2.8 (57).

Debug APKs are at `.build/<module>/outputs/apk/debug/`. Release bundles are at
`.build/<module>/outputs/bundle/release/`. Release signing is intentionally
unconfigured; sign with the existing Play upload key before publishing, and
ensure the version code exceeds the latest version in Play Console.

Background flashlight/strobe operation uses the `specialUse` foreground service
type; include its use-case declaration in Play Console when submitting. Music
mode also declares the microphone service type. Native libraries are built with
16 KB page-size support.

## LED brightness

The saved Strobe brightness applies to normal strobe/bursts, tacho, music,
turntable, and continuous torch illumination when both **Show brightness slider**
and **Custom brightness** are enabled. Unsupported cameras retain their original
on/off behavior. UI values remain 0–100%; positive hardware levels are scaled
once in the service and clipped to at least 1, so UI 0% selects minimum strength.
Screen flashing is independent of LED brightness.

The service accepts an optional brightness percentage array, one entry per
ON/OFF timing pair. A null array selects the live global preference. A non-null
array overrides global preferences; `-1` requests the original native/default
ON command. Existing patterns, previews, and pattern shortcuts explicitly use
`-1` arrays. Per-flash brightness is ready in the playback API; pattern storage
and editing have not changed. JNI receives already-scaled hardware levels.

Global settings update JNI state without replacing the timing sequence. A changed
hardware level marks brightness as needing an update, allowing the next global
LED ON command through the existing counter guard. Already-lit torch strength
changes call the strength API directly; returning to native/default strength
uses an OFF/ON reset. Explicit pattern arrays ignore global brightness updates.

The hardware instrumentation checks require a configured, unlocked API 33+ device
with variable torch strength and camera permission granted to the debug app:

```sh
./gradlew :Strobe:assembleDebug :Strobe:assembleDebugAndroidTest
adb install -r .build/Strobe/outputs/apk/debug/Strobe-debug.apk
adb install -r .build/Strobe/outputs/apk/androidTest/debug/Strobe-debug-androidTest.apk
adb shell am force-stop com.tp77.Strobe
adb shell am instrument -w com.tp77.Strobe.test/com.tp77.Strobe.BrightnessInstrumentation
```

The test briefly operates the LED, checks reported hardware strength and screen
pulse messages, then stops playback and restores the preferences it changed.
