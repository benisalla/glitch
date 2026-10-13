# Glitch v1.1.0 launcher: test plan

Scope: the optional home-screen mode (HOME role), the dock, the home-screen card, and the
existing app mode, which must keep working. Package `com.benisalla.glitch`.

Columns: **How** is `A` (automated: JVM unit test, instrumented test or `qa/launcher_smoke.sh`)
or `M` (manual). Each case has one expected result. A case passes only if the expected result
holds exactly.

## 0. Setup

- Build and install: `./gradlew :app:installDebug` (or `adb install -r app/build/outputs/apk/debug/app-debug.apk`).
  If an install fails with `INSTALL_FAILED_UPDATE_INCOMPATIBLE`, a differently signed build is
  installed: `adb uninstall com.benisalla.glitch` first (this wipes the saved dock).
- Record the current home app: `adb shell cmd role get-role-holders android.app.role.HOME`.
- Set Glitch as home: `adb shell cmd role add-role-holder android.app.role.HOME com.benisalla.glitch`.
- Restore afterwards: `adb shell cmd role add-role-holder android.app.role.HOME <previous holder>`.
- Rotation without the accelerometer: `adb shell settings put system accelerometer_rotation 0`,
  then `adb shell settings put system user_rotation 1` (landscape) or `0` (portrait).
- Screen off/on: `adb shell input keyevent KEYCODE_SLEEP`, then `KEYCODE_WAKEUP`, then
  `adb shell wm dismiss-keyguard`.
- Devices: the `shuffle_api35` emulator (stock Pixel launcher, about 19 launchable apps), plus at
  least one physical Samsung One UI phone, because One UI handles default-home changes and
  the keyguard differently.

"Order" below means the order of the grid tiles. "Visible order" is what is on screen when the
grid is scrolled to the top. The dock is never part of the grid.

## 1. Automated coverage (to exist before release)

| # | Level | Test | Expected |
|---|-------|------|----------|
| 1.1 | Unit | Shuffle of the grid with a seeded `Random` | Result is a permutation of the input minus the dock entries: same keys, no duplicates, nothing lost |
| 1.2 | Unit | Session: `onMenuStopped(changingConfigurations = true)` then `onMenuStarted()` | Order unchanged |
| 1.3 | Unit | Launcher session: start, stop (app launched, screen still on), start | Order unchanged |
| 1.4 | Unit | Launcher session: screen-off signal, then start | Exactly one reshuffle; with a fixed seed the new order equals the expected seeded permutation |
| 1.5 | Unit | Several screen-off signals before the next start | Still exactly one reshuffle |
| 1.6 | Unit | App mode (not default home): stop (not config change), start | One reshuffle, as in v1.0 |
| 1.7 | Unit | Catalog refresh with one app added and one removed | Survivors keep their relative order; removed entry gone; new entry present once; no reshuffle |
| 1.8 | Unit | Dock: defaults resolve to phone, messages, camera, browser when installed | Dock has those apps in that order; missing defaults are skipped, no placeholder |
| 1.9 | Unit | Dock: add a 5th app | Rejected; dock still has 4 entries, unchanged |
| 1.10 | Unit | Dock: remove an app | It leaves the dock and appears in the grid exactly once |
| 1.11 | Unit | Dock app uninstalled (catalog no longer has it) | Dropped from the displayed dock; remaining dock order unchanged; no crash |
| 1.12 | Unit | Dock persistence: save, recreate the store from the same storage | Same dock, same order |
| 1.13 | Instrumented | Compose: dock row and grid render from a fixed state | Dock tiles and grid tiles have distinct semantics (testTag) and content descriptions = app labels |
| 1.14 | Instrumented | Long-press a grid tile | Menu shows "Add to dock" and "App info"; on a dock tile "Remove from dock" and "App info" |
| 1.15 | Instrumented | Merged manifest (or `aapt dump permissions`) | No `uses-permission` entries other than the AndroidX `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` signature permission the library adds; no INTERNET, QUERY_ALL_PACKAGES |
| 1.16 | Script | `qa/launcher_smoke.sh` on the emulator | Exit 0, every line PASS (see section 9) |

## 2. Becoming the home app and leaving it

| # | How | Steps | Expected |
|---|-----|-------|----------|
| 2.1 | A | `cmd package query-activities -a android.intent.action.MAIN -c android.intent.category.HOME` | Lists `com.benisalla.glitch/...` (HOME + DEFAULT filter present) |
| 2.2 | A | `cmd role add-role-holder android.app.role.HOME com.benisalla.glitch`, then `get-role-holders` | Holder is `com.benisalla.glitch` |
| 2.3 | M | Settings > Apps > Default apps > Home app > Glitch | Glitch selectable; after choosing it, Home shows Glitch |
| 2.4 | M | Glitch not default: open Glitch from the old launcher | "Make Glitch your home screen" card shown at the top; nothing else changed |
| 2.5 | M | Tap the card | System role request dialog (or Default home settings) opens; choosing Glitch makes it home; the card disappears on return |
| 2.6 | M | Tap the card, then cancel the dialog | Glitch stays a normal app; card still shown; no crash, no repeated dialog |
| 2.7 | M | Glitch is default: find "Change home app" | Opens the system Default home app settings; choosing the previous launcher works |
| 2.8 | M | Switch home: Glitch, old launcher, Glitch, old launcher (4 times quickly) | Each switch takes effect on the next Home press; no crash; no duplicate Glitch tasks in Recents; dock unchanged |
| 2.9 | M | Uninstall Glitch while it is the home app | System falls back to another launcher; the device is never left without a home |
| 2.10 | M | Reboot with Glitch as home | Glitch is shown after boot (after unlock) with a fresh order; dock unchanged |
| 2.11 | M | Glitch is home, open Glitch from Recents or via `am start -n com.benisalla.glitch/.MainActivity` | Same single task; no second copy of the home screen in Recents |

## 3. Reshuffle rules as the home screen

| # | How | Steps | Expected |
|---|-----|-------|----------|
| 3.1 | A | Press Home, note order; open Settings; press Home | Same order |
| 3.2 | M | Repeat 3.1 with an app launched from a grid tile and from a dock tile | Same order in both cases |
| 3.3 | A | Screen off, screen on, dismiss keyguard (no lock) | New grid order; dock unchanged |
| 3.4 | M | Same as 3.3 with a PIN lock, unlock with PIN | New order after unlock, shown once; it does not change again on the next Home press |
| 3.5 | M | Screen off while another app is in front; on, unlock, then press Home | Home shows a new order (the reshuffle waits for the next time Home is shown) |
| 3.6 | M | Screen times out by itself (not the power button) | Same as 3.3 |
| 3.7 | M | Screen off and on twice in a row before looking at Home | One new order; no visible double shuffle or flicker |
| 3.8 | A | Rotate to landscape and back on the home screen | Order unchanged; dock unchanged |
| 3.9 | M | Other config changes: dark mode toggle, font size, display size, language, split screen/freeform resize | Order unchanged after each |
| 3.10 | A | Press Home while Glitch is already shown and at the top | Order unchanged |
| 3.11 | M | Scroll the grid down, press Home | Grid scrolls (or jumps) to the top; order unchanged |
| 3.12 | A | Press Back on the home screen | Nothing happens: Glitch stays, order unchanged, no exit animation |
| 3.13 | M | Back gesture (gesture navigation) on the home screen | Same as 3.12 |
| 3.14 | M | Open notification shade or quick settings over Glitch, close it | Order unchanged |
| 3.15 | M | Incoming call or alarm screen over Glitch, dismiss | Order unchanged (screen did not turn off); if the screen turned off, a new order is allowed |
| 3.16 | M | Recents (overview) from Glitch, then back to Glitch | Order unchanged |

## 4. App mode (Glitch is not the default home)

| # | How | Steps | Expected |
|---|-----|-------|----------|
| 4.1 | A | Open Glitch from the old launcher; leave to Settings; open Glitch again | New order (v1.0 behaviour) |
| 4.2 | A | Rotate while open | Order unchanged |
| 4.3 | M | Tap an app, return with Back | New order (returning from an app is a new session in app mode) |
| 4.4 | M | Back on Glitch in app mode | Glitch closes like a normal app (Back is only swallowed when Glitch is home) |
| 4.5 | M | Dock in app mode | Dock is shown and behaves as on the home screen (same contents) |

## 5. Dock

| # | How | Steps | Expected |
|---|-----|-------|----------|
| 5.1 | M | Fresh install (`pm clear com.benisalla.glitch`) on a device with phone, messages, camera, browser | Dock: phone, messages, camera, browser, in that order; those 4 are not in the grid |
| 5.2 | M | Fresh install on the emulator (some defaults may be missing) | Only resolved defaults shown; no empty slots, no crash |
| 5.3 | A | Through all of section 3 | Dock contents and order identical in every snapshot |
| 5.4 | M | Long-press a grid tile > Add to dock (dock has 3) | App moves to the end of the dock; it disappears from the grid; other grid tiles keep their relative order |
| 5.5 | M | Dock full (4), long-press a grid tile | "Add to dock" disabled or explains the dock is full; nothing changes |
| 5.6 | M | Long-press a dock tile > Remove from dock | App leaves the dock and appears once in the grid; the other dock tiles keep their order |
| 5.7 | M | Long-press any tile > App info | System App info page for that app opens |
| 5.8 | M | Change the dock, then `adb shell am kill com.benisalla.glitch` (with Glitch in the background) and press Home | Dock change kept |
| 5.9 | M | Change the dock, then `adb shell am force-stop com.benisalla.glitch`, then reboot | Dock change kept |
| 5.10 | M | Uninstall a dock app (`adb uninstall <pkg>`) while Glitch is shown | It leaves the dock immediately; the other dock apps keep their order; grid order unchanged |
| 5.11 | M | Reinstall that app | It appears once, in the grid (not back in the dock unless the design says otherwise); grid survivors keep their order |
| 5.12 | M | Empty the dock (remove all 4) | No dock row or an empty dock; grid unaffected; adding back works |
| 5.13 | M | Disable a dock app (`pm disable-user <pkg>`) and re-enable | Same as 5.10 and 5.11 |
| 5.14 | M | Long-press with TalkBack on | Actions are reachable as custom accessibility actions |

## 6. Package changes while shown

| # | How | Steps | Expected |
|---|-----|-------|----------|
| 6.1 | M | Glitch shown as home; `adb install` a new app | New tile appears once; every other tile keeps its relative order |
| 6.2 | M | Glitch shown; `adb uninstall` a grid app | Tile disappears; others keep their order |
| 6.3 | M | Update an installed app (`adb install -r`) | No reshuffle; tile stays where it was |
| 6.4 | M | Tap a tile whose app was just uninstalled (race) | Snackbar "... is no longer available"; no crash |

## 7. Process death and robustness

| # | How | Steps | Expected |
|---|-----|-------|----------|
| 7.1 | M | Home shown; open Settings; `adb shell am kill com.benisalla.glitch`; press Home | Glitch comes back; order may be new (process died) but dock is the saved one; no crash. Record whether order is kept and confirm with the launcher engineer that it matches the design |
| 7.2 | M | Developer options > Don't keep activities; launch an app, press Home | Order unchanged (activity recreated, ViewModel/saved state keeps the session) |
| 7.3 | M | `am crash` or force a crash in another app, return Home | Glitch unaffected |
| 7.4 | M | Low-memory: open many heavy apps, press Home | Glitch restarts quickly; no blank screen longer than the loading indicator |
| 7.5 | M | Glitch is home and crashes on start (simulate with a broken debug build) | System offers another launcher; document recovery steps for users in the README |

## 8. Display, accessibility and scale

| # | How | Steps | Expected |
|---|-----|-------|----------|
| 8.1 | M | Dark mode on and off | Tiles, dock, card and menus readable; contrast passes; order unchanged |
| 8.2 | M | Font size max + display size max | Labels wrap to 2 lines with ellipsis; dock tiles not clipped; long-press menu fits |
| 8.3 | M | Landscape on a phone | Dock still visible (bottom or side); grid fills the rest; nothing hidden under the nav bar |
| 8.4 | M | Tablet / foldable (inner screen) | Grid uses more columns; dock stays at most 4 |
| 8.5 | M | 150+ apps (install dummies, see note) | Grid scrolls smoothly; icons load lazily; first frame under 1 s on a mid-range phone; Home scrolls to top |
| 8.6 | M | 150+ apps: unlock 5 times | New order each time; no ANR |
| 8.7 | M | TalkBack: swipe through dock and grid | Each tile reads its full label; dock announced separately from the grid |
| 8.8 | M | Gesture navigation and 3-button navigation | Home and Back behave as in 3.10 to 3.13 in both |
| 8.9 | M | RTL language (Arabic) | Grid and dock mirror; order semantics unchanged |
| 8.10 | M | Work profile apps | Shown or omitted consistently with v1.0; no crash |

Note for 8.5: generate dummy apps with any minimal APK built under 150 different application ids,
or use a device that already has many apps. Do not add test APKs to this repo.

## 9. Privacy

| # | How | Steps | Expected |
|---|-----|-------|----------|
| 9.1 | A | `aapt2 dump permissions app/build/outputs/apk/debug/app-debug.apk` | No user-facing permission (no INTERNET, QUERY_ALL_PACKAGES, overlay, usage access, accessibility); only the library signature permission `com.benisalla.glitch.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` |
| 9.2 | A | Merged manifest `<queries>` | Only the MAIN/LAUNCHER intent |
| 9.3 | M | `adb shell dumpsys package com.benisalla.glitch | grep -A3 "requested permissions"` | Same as 9.1 |
| 9.4 | M | Airplane mode | Everything in sections 2 to 8 works the same |
| 9.5 | M | Manifest does not request `android.permission.SET_PREFERRED_APPLICATIONS` or similar; the role change goes through the system dialog | Confirmed |

## 10. Smoke script (`qa/launcher_smoke.sh`)

Run after installing the debug APK on a connected emulator or device:

```
qa/launcher_smoke.sh                 # full run
qa/launcher_smoke.sh --parse-only    # just open Glitch and print the parsed dock and grid
ANDROID_SERIAL=emulator-5554 KEEP_DUMPS=1 qa/launcher_smoke.sh
DOCK_RID=dock qa/launcher_smoke.sh   # if tiles carry testTags exposed as resource ids
```

What it checks, in order: Glitch can hold the HOME role; Home shows Glitch; dock has 1 to 4
apps and none of them is in the grid; Back on home keeps Glitch and the order; Home while on
Glitch keeps the order; Settings then Home keeps the order; screen off/on gives a new order
(up to `SHUFFLE_TRIES`=3 attempts); landscape and back keep the order; the dock is identical in
every snapshot. Then app mode with the previous home restored: rotation keeps the order and
reopening gives a new order. It restores the previous home role holder and rotation on exit,
including on failure or Ctrl-C.

Exit codes: 0 all PASS, 1 any FAIL, 2 not a launcher build (no HOME filter; launcher checks
are SKIPped, app-mode checks still run), 3 setup problem (no device, several devices without
`ANDROID_SERIAL`, Glitch not installed).

How it reads the order: `uiautomator dump`, then every node of `com.benisalla.glitch` with a
non-empty content-desc is a tile (Compose merges icon and label into one node whose description
is the label). Tiles are sorted top-to-bottom, left-to-right. Dock tiles are the ones whose
resource-id contains `DOCK_RID` if set; otherwise the ones outside the scrollable grid; otherwise
(grid not scrollable) the ones in the bottom `DOCK_ZONE_PCT` (15%) of the window. Only the
visible part of the grid can be compared, so orders are compared as "the shorter visible list
is a prefix of the longer one", which handles rotation changing the number of visible tiles.

About chance: a correct reshuffle can repeat the visible order by luck. For N shuffled apps and
k visible tiles this happens with probability (N-k)!/N! per attempt (1/19! = 8e-18 on the stock
emulator; 1/24 if only 4 apps are in the grid). The script prints this number on every repeat
and only fails after 3 identical attempts, so a FAIL means the reshuffle is missing, not bad
luck, unless the device has 3 or fewer grid apps.

Known limits: the dock heuristic needs `DOCK_RID` if the dock sits inside the grid's scroll
area; RTL layouts sort left-to-right, so run the script in an LTR language; on a device with a
secure lock screen, `wm dismiss-keyguard` cannot unlock it, so run the script on a device with
no lock or Swipe only.

## 11. Exit criteria for v1.1.0

- Section 1 tests exist and pass in CI (`:app:testDebugUnitTest`, `:app:lintDebug`) and
  `:app:connectedDebugAndroidTest` passes on the emulator.
- `qa/launcher_smoke.sh` exits 0 on `shuffle_api35` and on one Samsung One UI phone.
- Every manual case in sections 2 to 9 passed on at least one device, and sections 2, 3 and 5
  on the Samsung phone; any deviation has an issue filed and a release decision recorded.
