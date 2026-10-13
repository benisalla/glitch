# Agent instructions: Glitch

## Product scope
- One purpose: show the installed, launchable apps (icon + label) in a random order, so the user has to look instead of tapping by position. Tapping an app opens it normally.
- Platform boundary: this is a standalone menu app that the user opens from their existing home screen. It does not modify Samsung One UI's app drawer, is not a home-screen replacement, and must never declare a HOME intent filter or request the HOME role.
- Vision: Glitch grows into gentle, user-controlled cues for people with ADHD and attention that wanders quickly. See `VISION.md`, `ROADMAP.md` and the research in `product/`.
- Planned, not yet approved: intention prompts, time cues, usage awareness (see `ROADMAP.md`). Build a roadmap item only after the owner explicitly approves that item, and update this file in the same change.
- Never: lockouts or blockers you can't escape, streaks, shame-based copy, analytics, accounts, backend, ads, AI.
- Claims: Glitch is not a medical device. Never claim it treats, fixes or reduces ADHD or any condition, or that it improves focus, memory or intelligence. Allowed outside the store listing: "designed using research on attention, habits and ADHD-friendly design" and descriptions of what the app does. The store listing never mentions ADHD or any condition, and never advertises features that are not shipped yet.
- Any feature needing AccessibilityService (e.g. the in-app Scroll Glitch) belongs in a separate companion app in its own repository or project, never in this app (not as a module or build flavor either).
- The Privacy requirements below stay binding until an approved roadmap item changes them in the same PR.
- Do not expand scope without the owner's explicit approval.

## Architecture
Single Gradle module `:app`, Kotlin + Jetpack Compose, package `com.benisalla.glitch` (`app/src/main/java/com/benisalla/glitch/`).
- `model/`: shared contracts: `AppEntry` (key = `package/activity`), `AppCatalog`, `AppLauncher`, `LaunchResult`.
- `apps/`: `PackageManagerAppCatalog` (discovers launcher activities, loads icons, emits package changes) and `IntentAppLauncher`.
- `menu/`: shuffle logic, session state (`MenuViewModel`), and Compose UI (`MenuScreen`, `GlitchTheme`).
- `MainActivity.kt`: wiring only. It calls `onMenuStarted()` from `onStart` and `onMenuStopped(isChangingConfigurations)` from `onStop`.

Session semantics (keep these):
- Shuffle exactly once per newly opened menu, meaning `onStart` after a stop that was not a configuration change. Returning after launching an app counts as a new session.
- Order stays stable while the menu is visible: no reshuffle on recomposition, rotation/configuration change, or incidental refresh.
- When apps are installed or removed while the menu is open: keep the current order of surviving entries, drop removed ones, add new ones without reshuffling the rest.

No DI frameworks, no extra modules, no needless abstractions.

## Commands
Requires JDK 17 and the Android SDK (platform 37, build-tools 36.0.0), with `ANDROID_HOME` or `sdk.dir` in `local.properties`.
```
./gradlew :app:assembleDebug              # build debug APK
./gradlew :app:testDebugUnitTest          # JVM unit tests
./gradlew :app:lintDebug                  # Android lint
./gradlew :app:connectedDebugAndroidTest  # instrumented tests; needs a running emulator or device
```
Keep runs targeted (e.g. `:app:testDebugUnitTest --tests '...'`). Fix failures; never disable tests, lint checks or CI to get green.

## Ownership areas
- Build and platform files (`*.gradle.kts`, `gradle/**`, `gradle.properties`, `gradlew*`, `AndroidManifest.xml`, `res/**`), `model/**`, `MainActivity.kt`, `.gitignore`: changes to contracts or dependencies need an explicit reason.
- `apps/**` and its tests: app discovery and launching.
- `menu/**` and its tests: shuffle, session, UI.
- `.github/**`: CI only.
- `AGENTS.md`, `CLAUDE.md`, `README.md`, `CONTRIBUTING.md`, `.claude/settings.json`: docs and agent config.
Changing a shared contract in `model/` means updating every implementation and test in the same change.

## Tests
- Unit tests live in `app/src/test/`, instrumented tests in `app/src/androidTest/`.
- No flaky shuffle tests: inject `Random` and use a fixed seed. Never assert that an order "looks random" or differs from the previous one by chance alone.
- Assert invariants instead: the result is a permutation of the input (same keys, no duplicates, nothing lost); order is unchanged across config changes and refreshes; reconciliation keeps survivor order and adds/drops only the changed entries.
- New behavior in `menu/` or `apps/` comes with tests.

## Privacy requirements
- No permissions in the manifest. In particular no `INTERNET`, `QUERY_ALL_PACKAGES`, accessibility, overlay, usage access, or anything needing root.
- Package visibility comes only from the narrow `<queries>` entry for the `MAIN`/`LAUNCHER` intent.
- No tracking, analytics, crash reporting SDKs or network code. App data never leaves the device.

## Git workflow
- After v0.1, work on feature branches and open pull requests to `main`. CI must pass before merging.
- No force pushes to `main`. Do not rewrite shared history.
- Never commit `.claude/settings.local.json`, `CLAUDE.local.md`, `local.properties`, keystores, secrets, or build output (`build/`, `.gradle/`, APKs).
