# Agent instructions: Glitch

## Product scope
- One purpose: show the installed, launchable apps (icon + label) in a random order, so the user has to look instead of tapping by position. Tapping an app opens it normally. It works as a normal app or, if the user chooses, as the phone's home screen.
- Platform boundary (changed with owner approval on 2026-10-13, for v1.1.0): Glitch is an **optional** home-screen launcher. It declares a `MAIN` activity intent filter with the `HOME` and `DEFAULT` categories so the user can pick it as the default home app, and it keeps working as a normal app opened from another launcher. It is never forced: no auto-switching, no nagging, no tricks to hold the HOME role. The user selects it through Android's own default-home chooser or Settings, and the app always offers a "Change home app" way back. It never modifies Samsung One UI's own launcher or app drawer. That is the only way Android lets an app control where the user's apps appear.
- Home-screen scope (v1.1.0): the shuffled app grid, a fixed dock of up to 4 apps, long-press actions ("Add to dock" / "Remove from dock", "App info"), a "Make Glitch your home screen" card, and a "Change home app" entry. No widgets, wallpaper or search in this version.
- Vision: Glitch grows into gentle, user-controlled cues for people with ADHD and attention that wanders quickly. See `VISION.md`, `ROADMAP.md` and the research in `product/`.
- Planned, not yet approved: intention prompts, time cues, usage awareness (see `ROADMAP.md`). The fixed dock replaces the earlier "anchor apps" idea and is approved as part of v1.1.0. Build a roadmap item only after the owner explicitly approves that item, and update this file in the same change.
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
- `MainActivity.kt`: wiring only. It is both the launcher (`MAIN`/`LAUNCHER`) activity and the optional home (`MAIN` + `HOME` + `DEFAULT`) activity. It forwards lifecycle events (`onStart`, `onStop` with `isChangingConfigurations`), screen-off events and the "is Glitch the default home" state to `MenuViewModel`, and opens Android's default-home chooser or settings when the user asks.
- The dock (up to 4 app keys) and other user choices are stored only on the device. Screen-off detection uses a broadcast receiver registered at runtime, which needs no permission.

Session semantics (keep these; updated 2026-10-13 for the home-screen mode):
- **Normal app mode** (Glitch is not the default home): shuffle exactly once per newly opened menu, meaning `onStart` after a stop that was not a configuration change. Returning after launching an app counts as a new session.
- **Home mode** (Glitch is the default home): reshuffle only on a cold start and after the screen has turned off (i.e. the next time the user unlocks or wakes the phone). Never reshuffle when the user returns Home (Home button, gesture, back from an app, `onNewIntent`), on recomposition, or on a configuration change. The order stays steady while the phone is in use.
- **Dock:** up to 4 apps, chosen by the user (defaults: phone, messages, camera, browser, where installed). Dock apps keep their fixed spots and are excluded from the shuffle and from the shuffled grid. Adding or removing a dock app does not reshuffle the grid. A dock app that is uninstalled is dropped from the dock.
- Order stays stable while the menu is visible in both modes: no reshuffle on recomposition, rotation/configuration change, or incidental refresh.
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
- Assert invariants instead: the result is a permutation of the input (same keys, no duplicates, nothing lost); order is unchanged across config changes, refreshes and (in home mode) returns to Home; home mode reshuffles after screen-off and not otherwise; dock apps never appear in the shuffled grid and the dock never exceeds 4; reconciliation keeps survivor order and adds/drops only the changed entries.
- New behavior in `menu/` or `apps/` comes with tests.

## Privacy requirements
- No permissions in the manifest, also in home mode (the `HOME` intent filter is not a permission). In particular no `INTERNET`, `QUERY_ALL_PACKAGES`, accessibility, overlay, usage access, or anything needing root.
- Package visibility comes only from the narrow `<queries>` entry for the `MAIN`/`LAUNCHER` intent.
- No tracking, analytics, crash reporting SDKs or network code. App data never leaves the device.

## Git workflow
- After v0.1, work on feature branches and open pull requests to `main`. CI must pass before merging.
- No force pushes to `main`. Do not rewrite shared history.
- Never commit `.claude/settings.local.json`, `CLAUDE.local.md`, `local.properties`, keystores, secrets, or build output (`build/`, `.gradle/`, APKs).
