# Glitch

<p align="center">
  <img src="store/graphics/feature-graphic.png" width="720" alt="Glitch logo: a white ring broken by a gold tile. Glitch the autopilot. Look, then tap.">
</p>

**Glitch the autopilot. Look, then tap.**

[![CI](https://github.com/benisalla/glitch/actions/workflows/ci.yml/badge.svg)](https://github.com/benisalla/glitch/actions/workflows/ci.yml)
![Android 8.0+](https://img.shields.io/badge/Android-8.0%2B-3DDC84?logo=android&logoColor=white)
![Permissions: none](https://img.shields.io/badge/permissions-none-brightgreen)
[![License: MIT](https://img.shields.io/badge/license-MIT-blue)](LICENSE)

Glitch is an Android app for breaking the autopilot on your phone. Its first feature is the shuffle: your installed apps in a new random order every time you open Glitch, so you look instead of tapping by position.

<p align="center">
  <a href="https://github.com/benisalla/glitch/releases/latest/download/glitch.apk"><b>⬇ Download Glitch for Android</b></a>
  &nbsp;·&nbsp; <a href="https://benisalla.github.io/glitch/">Website</a>
  &nbsp;·&nbsp; <a href="https://apps.obtainium.imranr.dev/redirect?r=obtainium://add/https://github.com/benisalla/glitch">Get updates with Obtainium</a>
</p>

## Vision
Your phone is designed to be tapped without thinking. Glitch is a small, kind interruption in that loop: today it shuffles your apps so you look before you tap. Next, it grows into gentle cues you set yourself, designed using research on attention, habits and ADHD-friendly design. No lectures, no streaks, no lockouts, and nothing leaves your phone. Read the full [vision](VISION.md).

### Coming next
- **v1.1, intention moment:** mark the apps that pull you in, get a calm "Still want this?" pause with your own if-then plans, anchor your essential apps, and switch to focus sets. Still no permissions.
- **v1.2, gentle cues:** an optional kind time reminder, a focus widget and a Quick Settings tile.
- **v2.0, time awareness:** optional on-device usage awareness, and a first Scroll Glitch that steps in with *your own* reminder after long short-video sessions.
- **Future, the Scroll Glitch add-on:** detects endless Reels and Shorts scrolling and offers you a way out, as a separate opt-in companion app.

See the [roadmap](ROADMAP.md) and the research behind it in [`product/`](product/). Glitch is not a medical device and does not treat ADHD or any condition.

## What it does
- Shows every launchable app on the device (icon and name) in a shuffled order.
- Shuffles once each time you open the menu. The order stays the same while the menu is on screen, including when you rotate the phone.
- Opens the app you tap, normally. Coming back to the menu afterwards gives a new order.
- If an app is installed or removed while the menu is open, the list updates without reshuffling the other entries.

## What it does not do
- It does **not** replace or rearrange Samsung's swipe-up app drawer, and it is not a home-screen (launcher) replacement. It is a separate app, "Glitch", that you open from your home screen.
- No reminders, blockers, timers, usage tracking or accounts.
- It is not a medical device and makes no claim to treat ADHD or improve focus, memory or intelligence. Today it only changes the order of the list.

## Permissions
None. The app does not request internet access or any other permission. To see which apps are installed it declares a narrow [`<queries>`](https://developer.android.com/training/package-visibility) entry for the standard launcher intent (`MAIN` / `LAUNCHER`), which lets it see only apps that appear in a launcher. It does not use `QUERY_ALL_PACKAGES`, and it has no analytics or network code.

## Build from source
Prerequisites:
- JDK 17
- Android SDK with platform 37 (`platforms;android-37.0`) and build-tools 36.0.0
- `ANDROID_HOME` set to the SDK path, or a `local.properties` file in the repo root containing `sdk.dir=/path/to/Android/Sdk`

Commands (run from the repo root):
```
./gradlew :app:assembleDebug              # builds app/build/outputs/apk/debug/app-debug.apk
./gradlew :app:testDebugUnitTest          # unit tests
./gradlew :app:lintDebug                  # lint
./gradlew :app:connectedDebugAndroidTest  # instrumented tests; needs a running emulator or connected device
```

## Install on a phone (including Samsung Galaxy)
The steps below are standard Android procedures; menu names can differ slightly between One UI versions.

**Easiest: download the APK**
1. On your phone, open [glitch.apk](https://github.com/benisalla/glitch/releases/latest/download/glitch.apk) (always the latest release) or scan the QR code on the [website](https://benisalla.github.io/glitch/#install).
2. Open the downloaded file. When Android asks, allow "Install unknown apps" for your browser or My Files, then tap Install.
3. Android or Play Protect may warn you because Glitch is not on the Play Store yet. Glitch has no permissions and no internet access, and its source code is right here.

For automatic updates, add `https://github.com/benisalla/glitch` in [Obtainium](https://obtainium.imranr.dev/).

**Option A: with adb (from a computer)**
1. On the phone, open Settings > About phone > Software information and tap "Build number" seven times to enable Developer options.
2. Open Settings > Developer options and turn on "USB debugging".
3. Connect the phone by USB and accept the "Allow USB debugging?" prompt.
4. Build the APK (above), then run:
   ```
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

**Option B: download the APK built by CI**
1. On GitHub, open the repository's Actions tab, pick a successful workflow run, and download the artifact named `glitch-debug-apk`.
2. Unzip it to get `app-debug.apk` and copy the APK to the phone (or download it on the phone directly).
3. Open the APK with My Files or your browser. When asked, allow "Install unknown apps" for that app, then tap Install.

**Put it on the home screen**
Open the app drawer, long-press "Glitch", and choose "Add to Home". Open the menu from that icon whenever you want a shuffled list.

## Known limitations
- It does not replace the One UI app drawer or home screen; both keep working as before.
- Apps in a work profile or Samsung Secure Folder are not listed.
- Release builds are signed only when you provide your own upload key in `keystore.properties` (see `store/README.md`); without it `bundleRelease` produces an unsigned bundle.
- Tested on an Android emulator (API 35) only; not yet tested on a physical Samsung device.
- The built manifest contains one entry added automatically by AndroidX Core: `com.benisalla.glitch.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`. It is signature-level and private to this app, is never shown to the user, and grants no access to anything. The app requests no user-facing permissions.
- Lint reports 2 warnings and no errors: `OldTargetApi` (targetSdk is 36 while compileSdk is 37) and `DataExtractionRules` (backup is disabled with `allowBackup="false"`; the app stores no user data).

## Release / Play Store
Listing text, icon and feature graphic, Data safety answers and a step-by-step release checklist are in [`store/`](store/README.md). The privacy policy is [`docs/privacy-policy.md`](docs/privacy-policy.md), which must be hosted at a public URL (see step 5 of the checklist). The launcher icon and store graphics are generated by `python3 store/tools/make_graphics.py`.

## Contributing
See [CONTRIBUTING.md](CONTRIBUTING.md). Agent and contributor rules are in [AGENTS.md](AGENTS.md).

## License
Released under the [MIT License](LICENSE).
