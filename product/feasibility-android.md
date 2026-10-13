# Glitch: Android and Google Play feasibility of candidate features

Status: research note for the owner. Nothing here is approved scope. AGENTS.md still lists prompts, blockers, timers and usage tracking as non-goals, and HOME as forbidden. Any feature below needs the owner's explicit sign-off and an AGENTS.md update first.

Baseline (from `rename/glitch`): a standalone menu, minSdk 26, targetSdk 36, `allowBackup="false"`. The source manifest declares **zero `<uses-permission>`** (the merged manifest only adds the AndroidX app-private DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION), and package visibility comes only from a `<queries>` entry for `MAIN`/`LAUNCHER`. No network code. Data safety: nothing collected or shared.

Verified 2026-10-13 against developer.android.com and the Play Console Help Center (sources at the end). Anything not confirmed in an official doc is marked **(unverified)**.

## Permission tiers used in this document

| Tier | What it means | Effect on the "no permissions" promise |
|---|---|---|
| **T0** | No `<uses-permission>` at all. Local storage only. | Promise intact. Badge stays "permissions: none". |
| **T1** | A runtime permission the user can decline in a normal dialog (`POST_NOTIFICATIONS`, API 33+), or a normal install-time permission (`FOREGROUND_SERVICE*`). | Promise weakens to "no sensitive permissions". The feature must degrade gracefully when the permission is denied. |
| **T2** | Special app access granted only in Settings: Usage access, Display over other apps, Alarms and reminders, Notification access. | The promise is broken for users who opt in. Needs an in-app disclosure, and on Android 13+ sideloaded builds are gated by "restricted settings". |
| **T3** | AccessibilityService. | Highest privacy cost and highest Play review risk: Play Console declaration, video, prominent disclosure and consent. |

Data safety note: data that never leaves the device is not "collected" for the Play Data safety form, so T0 to T2 features can keep "No data collected" as long as nothing is transmitted. The permissions themselves are still visible on the store listing and in Settings.

---

## 1. Intention prompt (inside Glitch only)

Before opening an app the user marked "distracting", Glitch shows a sheet: "What are you here for?" with an optional one-line answer, then "Open" or "Not now".

- **APIs and permissions:** none. A Compose bottom sheet or dialog in `MenuScreen`, and a flag per `AppEntry.key` in local storage. T0.
- **Play policy:** none. No declaration.
- **Privacy cost:** none if the typed intention is not stored. If it is stored (to show later or for feature 8), it stays on device and is covered by export/delete (feature 10).
- **Effort:** S. State machine in `MenuViewModel` (tap, then gate, then launch) plus one composable.
- **Risks:** only gates launches made from Glitch. Users can still open the app from the home screen or drawer. Be honest in copy: it is a speed bump the user chose, not a blocker. A sheet or dialog does not stop the activity, so the session semantics hold: no `onStop`, no reshuffle.
- **Recommendation: do now** (after owner approval). It is the core T0 feature.

## 2. Delay or breathe screen before distracting apps

### 2a. Inside Glitch only
- **APIs and permissions:** none. A full-screen composable with a 3 to 10 s countdown or breathing animation, then launch. T0.
- **Play policy:** none.
- **Privacy cost:** none.
- **Effort:** S. Shares the gate flow with feature 1. Use an injected clock so tests can run without real delays.
- **Risks:** same coverage gap as 1.
- **Recommendation: do now**, combined with 1 as one "gate" setting per distracting app (none / intention / pause).

### 2b-lite. Pinned shortcut "Instagram via Glitch" (no permission)
A no-permission way to catch launches from the home screen. Glitch offers to pin a home-screen shortcut (`ShortcutManager.requestPinShortcut`, API 26, which matches minSdk). The shortcut opens Glitch's gate, then the real app. The user removes the original icon themselves.
- **APIs and permissions:** `ShortcutManager` pinned shortcuts. No permission. T0. The launcher shows its own confirmation dialog.
- **Play policy:** none.
- **Privacy cost:** none.
- **Effort:** S to M. Needs a small gate activity or an intent extra on `MainActivity`. The target must be revalidated against `AppCatalog` (only launchable entries, by `AppEntry.key`) so Glitch cannot be used as a generic launch proxy. Keep the target activity non-exported if pinned shortcuts allow it **(unverified, confirm on device)**.
- **Risks:** it does not catch launches from the app drawer, notifications or Recents. Some launchers (including One UI) may badge the shortcut with Glitch's icon.
- **Recommendation: do soon after 1/2a.** It is the best coverage available without permissions.

### 2b. Intercepting launches from anywhere (one sec style)
Two technical routes:

| Route | How | Permissions | Notes |
|---|---|---|---|
| Usage access polling | Foreground service polls `UsageStatsManager.queryEvents` for `ACTIVITY_RESUMED` of target packages, then shows an overlay or starts Glitch's activity | `PACKAGE_USAGE_STATS` (T2) + `SYSTEM_ALERT_WINDOW` (T2, needed to start activities from the background) + `FOREGROUND_SERVICE` / `FOREGROUND_SERVICE_SPECIAL_USE` + `POST_NOTIFICATIONS` | Slower and less reliable. one sec calls it its "legacy approach": it is killed by the system more often and has to poll actively. |
| AccessibilityService | `TYPE_WINDOW_STATE_CHANGED` events filtered with `android:packageNames` to the user's chosen apps, then shows a `TYPE_ACCESSIBILITY_OVERLAY` window (no `SYSTEM_ALERT_WINDOW` needed) or starts an activity | Accessibility (T3). one sec additionally asks for overlay permission. | Event-driven, fast, reliable. Highest Play scrutiny. |

- **Play policy (Accessibility):** Glitch is not an accessibility tool. Only apps whose *primary purpose* is supporting people with disabilities may set `isAccessibilityTool=true`. "Cognitive or learning" is a listed disability type, but a general focus app marketed to everyone does not qualify, so **do not set the flag**. Glitch would need to:
  - complete the Accessibility declaration in Play Console, including a video of the disclosure and of the service in use;
  - show a prominent in-app disclosure, separate from the privacy policy, that describes what is accessed and why, and requires affirmative consent;
  - document the API use in the Play listing.

  Play also says apps "must use more narrowly scoped APIs and permissions in lieu of the Accessibility API when possible". Detecting *which app is in front* can be done with usage access, so that alone is a weak justification for accessibility.
- **Play policy (Usage access):** no dedicated Play declaration form for `PACKAGE_USAGE_STATS` was found. The general User Data and "Permissions and APIs" rules apply: request in context, disclose, respect refusal.
- **Privacy cost:** large. Accessibility can technically read everything on screen. Usage access exposes the full app-usage history.
- **Effort:** L for either route: onboarding, OEM battery quirks (Samsung "sleeping apps"), restricted settings for sideloaded GitHub APKs, and Play declaration work.
- **Risks:** Play rejection or delay, a broken privacy promise, and an Android 13+ "Restricted setting" block for anyone installing the CI APK (Android 15 reportedly extends this to usage access and overlay, **unverified** in official docs). It also directly contradicts today's "no blockers" non-goal.
- **Recommendation: later at the earliest, only as an explicit opt-in tier.** Prefer the usage-access route first, and only consider accessibility together with feature 11 (which is the only case that needs in-app screen content).

## 3. Mark apps as distracting, hide them, grayscale their icons (in the Glitch menu)
- **APIs and permissions:** none. Grayscale is a Compose `ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })` on the icon. Marks are stored per `AppEntry.key`. T0.
- **Play policy:** none.
- **Privacy cost:** none (local list of package names).
- **Effort:** S. Long-press menu on an entry, plus a small "Hidden apps" list so hidden apps can be restored.
- **Risks:**
  - Hiding must not break shuffle invariants. Filter *after* the shuffle and reconciliation so the order of the visible apps is unaffected by hiding or unhiding.
  - Marks for uninstalled apps should be pruned lazily.
  - Keys use `package/activity`, so an app with two launcher activities is marked twice. That is acceptable and consistent with the model.
- **Recommendation: do now.** This is the foundation for 1, 2a and 2b-lite.

## 4. Time-spent awareness (today's usage per app)
- **APIs and permissions:** `UsageStatsManager.queryAndAggregateUsageStats` / `queryEvents` with `PACKAGE_USAGE_STATS`. The docs say "declaring the permission implies intention to use the API and the user of the device still needs to grant permission through the Settings application" (`Settings.ACTION_USAGE_ACCESS_SETTINGS`). This is T2, a special app access, not a runtime dialog. Results are `null` while the user is locked (Android 11+). Note that `queryEventsForSelf` needs no permission but only covers Glitch itself, which is useless here.
- **Play policy:** no dedicated declaration found. Prominent disclosure and consent per the User Data policy is expected because app usage is personal data. Data safety can still say "not collected" if it stays on device.
- **Privacy cost:** high relative to today. Glitch would see every app's foreground time, including apps the user never opens through Glitch.
- **Effort:** M: permission onboarding, revocation handling (`AppOpsManager.unsafeCheckOpNoThrow(OPSTR_GET_USAGE_STATS, ...)`), an aggregation function that can be unit-tested, and UI.
- **Risks:**
  - It duplicates Samsung/Google Digital Wellbeing, which already shows this with no extra trust needed.
  - Numbers can disagree with Digital Wellbeing, because the two count differently.
  - Shame-driven stats may backfire for an ADHD audience (for the research teammate to confirm).
- **Recommendation: later, opt-in.** A T0 alternative: one button that opens the system's Digital Wellbeing screen (intent availability varies by OEM, **unverified**).

## 5. Gentle session timer ("you've been in X for 10 minutes")
Accurate tracking of time *in X* needs to know when the user leaves X:

| Variant | Permissions | Accuracy |
|---|---|---|
| **5a. Launch-based nudge (T1).** When X is opened *from Glitch*, post a notification using `setUsesChronometer(true)` (the system renders the elapsed time, no service needed) and schedule a gentle reminder at +N minutes with an inexact alarm (`AlarmManager.set`/`setAndAllowWhileIdle`, no permission) or WorkManager. Cancel both when Glitch is opened again. | `POST_NOTIFICATIONS` (runtime, API 33+, declinable). | Approximate. It does not know if the user already left X, so the copy must say "You opened X about 10 min ago. Still on purpose?" Inexact alarms can be late, never early. |
| 5b. True foreground tracking | Usage access (T2) + foreground service (`specialUse`, Play Console justification required) + notifications | Accurate to the polling interval. Battery cost from polling. |
| 5c. Accessibility | T3 | Accurate, but not justified for this alone (narrow-API rule). |

- **Play policy:** 5a needs nothing special. 5b needs the foreground-service type declared in Play Console (App content) with a free-form `specialUse` justification that reviewers assess. Exact alarms (`SCHEDULE_EXACT_ALARM`) are not pre-granted for apps targeting API 33+, and `USE_EXACT_ALARM` is restricted to alarm and calendar apps. Avoid both. A nudge does not need to be exact.
- **Privacy cost:** 5a is low (Glitch already knows which app it launched). 5b is high.
- **Effort:** 5a is S to M. 5b is L.
- **Risks:** notification fatigue, OEM battery killers delaying WorkManager or alarms, and a nudge that fires after the user has already left X (mitigated by soft wording).
- **Recommendation: 5a later (v2), opt-in, off by default. Avoid 5b and 5c** unless feature 11 (phase A) lands anyway.

## 6. Focus session / Pomodoro with home-screen widget and Quick Settings tile
- **APIs and permissions:**
  - Widget: Jetpack Glance (`androidx.glance:glance-appwidget`). A new dependency needs a stated reason, per AGENTS.md. No permission.
  - Quick Settings tile: `TileService` guarded by `BIND_QUICK_SETTINGS_TILE`, which the system holds, so the user grants nothing. Android 13+ can offer to add it via `StatusBarManager.requestAddTileService`.
  - Timer display: avoid a ticking service. Show "Focus until 14:25" in the widget, tile subtitle and an ongoing notification with `setChronometerCountDown(true)` (T1 for the notification).
  - End-of-session alert: an inexact alarm can be late. `setWindow` windows under 10 minutes are clipped to 10 minutes on Android 12+. If exact end time matters, the honest options are `SCHEDULE_EXACT_ALARM` (T2, user-granted) or a `specialUse` foreground service (Play justification). Neither is worth it for v2.
  - Optional "quiet during focus": `ACCESS_NOTIFICATION_POLICY` (T2) or `AutomaticZenRule` / Modes. The T0 alternative is a button that opens system Do Not Disturb or Modes settings.
- **Play policy:** none for widget and tile. A foreground service or exact alarm would add declarations.
- **Privacy cost:** none to low.
- **Effort:** M. Widget plus tile plus session state persisted locally so it survives process death.
- **Risks:**
  - Scope creep into a generic Pomodoro app, which is a crowded category.
  - Widget update limits: `updatePeriodMillis` of 30 min or more, so show the end time rather than a live countdown.
  - Contradicts "no timers" in AGENTS.md.
- **Recommendation: later (v2/v3).** The widget alone (a "Glitch" widget that opens the shuffled menu, or shows today's intention) is T0 and a cheap first step.

## 7. Optional HOME launcher (role HOME)
- **APIs and permissions:** an activity with `MAIN` + `HOME` + `DEFAULT` intent filter. Request with `RoleManager.createRequestRoleIntent(RoleManager.ROLE_HOME)` (API 29+; on 26 to 28 the user picks the default home in Settings). No dangerous permission. `MAIN`/`LAUNCHER` `<queries>` is enough for visibility, so `QUERY_ALL_PACKAGES` is not needed.
- **Play policy:** no declaration. Launchers explicitly are *not* accessibility tools.
- **Privacy cost:** none in permissions. The trust cost is large, because the app becomes the phone's front door.
- **Effort:** L to XL: home pages, widgets host (`AppWidgetHost`), wallpaper, gestures, Recents interplay, default-launcher edge cases, One UI-specific behaviour, and robust crash recovery (a crashing launcher bricks the phone experience).
- **Value:** every launch could go through gates 1 and 2 without accessibility, which is the strongest no-permission coverage possible.
- **Risks:** It is the top uninstall driver for launcher apps: users lose their Samsung home screen and widgets. It also changes the product identity, and AGENTS.md forbids it.
- **Recommendation: avoid.** 2b-lite pinned shortcuts give most of the value at a fraction of the risk. Revisit only if user research shows that pinned shortcuts are not enough.

## 8. Daily "first app of the day" intention / reflection (local)
- **APIs and permissions:** none. On the first `onMenuStarted` of a local calendar day (`java.time.LocalDate`; minSdk 26 has `java.time`), show "What's one thing you want to do today?" and store it on device. Optionally show it at the top of the menu all day. Evening reflection could be a T1 notification, or just shown on the next open (T0).
- **Limitation:** without permissions Glitch cannot detect the phone's first unlock. Manifest-registered `USER_PRESENT` receivers no longer fire for third-party apps since Android 8 **(verify)**. So this is "first time you open Glitch today".
- **Play policy:** none.
- **Privacy cost:** low. Free text is personal, but local only, and is covered by export/delete.
- **Effort:** S.
- **Risks:** it must not block the menu. One tap to skip. It must not count as a reshuffle trigger.
- **Recommendation: do now**, behind a setting.

## 9. Notification batching / quiet mode (NotificationListenerService)
- **APIs and permissions:** a `NotificationListenerService` declared with `BIND_NOTIFICATION_LISTENER_SERVICE`. The user grants Notification access in Settings (`ACTION_NOTIFICATION_LISTENER_SETTINGS`). This is T2. Batching can be done with `snoozeNotification(key, durationMs)` and `cancelNotification`. Glitch cannot repost another app's notification, only post its own summary.
- **Play policy:** no dedicated declaration form found. The User Data policy applies: notification content is personal and sensitive, so prominent disclosure and consent are required. Android 13+ restricted settings block it for sideloaded installs.
- **Privacy cost:** very high. Glitch would read every notification, including messages and 2FA codes.
- **Effort:** L, with many edge cases: ongoing/foreground notifications, calls, alarms, OEM grouping.
- **Risks:** it duplicates Android's Do Not Disturb, Modes (Android 15), notification cooldown and One UI's own scheduling. Bugs here mean missed important messages, which is a trust disaster.
- **Recommendation: avoid.** Offer a T0 "Open Do Not Disturb settings" shortcut inside a focus session instead.

## 10. Local-only stats, storage, export and delete
- **APIs and permissions:** none.
  - **Storage:** Jetpack DataStore (Preferences) for marks, settings, today's intention and small counters, for example "gates shown / apps opened anyway / backed out" per day. Room only if a per-event history is really needed. Start without it. Both are new dependencies that need the stated reason.
  - **Export:** Storage Access Framework (`ActivityResultContracts.CreateDocument("application/json")`). No storage permission.
  - **Delete:** a "Delete all data" button that clears the store. Uninstall removes everything. `allowBackup="false"` already keeps it out of cloud backups. Keep it that way, or add explicit `dataExtractionRules` if backup is ever reconsidered.
- **Play policy:** none. Data safety stays "no data collected" because nothing is transmitted. The privacy policy needs a sentence on what is stored locally.
- **Privacy cost:** none relative to today (local only).
- **Effort:** S (DataStore + delete) to M (export plus a simple stats screen).
- **Risks:** stats can drift into shaming. Prefer neutral counts ("you paused 4 times today") over scores.
- **Recommendation: do now** (storage + delete), with export soon after.

---

## 11. Reels detector (future vision; no code)

Goal: when the user is scrolling short-form video (Instagram Reels, YouTube Shorts, TikTok, Facebook Reels), Glitch interrupts with a reminder **written by the user** ("You said you'd finish the report").

### 11a. App-level only (no in-app distinction)
- **How:** detect that a target *package* is in the foreground. TikTok is essentially all short-form, so app-level detection is a good proxy there. For Instagram, YouTube and Facebook, Reels/Shorts usually live inside the same activity as DMs and the main feed, so app-level detection cannot tell "watching Reels" from "replying to a friend". It can only say "you've been in Instagram for 15 min".
- **APIs:** `UsageStatsManager.queryEvents` (`ACTIVITY_RESUMED` / `ACTIVITY_PAUSED`) with usage access (T2).
  - **Real time:** needs a foreground service polling every few seconds (`specialUse`, Play justification, persistent notification).
  - **Battery-friendly:** WorkManager periodic checks (minimum 15 min), so reminders are coarse. A middle ground is to poll only during a user-started focus session.
- **Fragility:** low. Package names are stable (TikTok ships under more than one package name by region).
- **Battery:** a polling foreground service costs a small but constant amount. Session-scoped polling costs close to zero.
- **Privacy:** Glitch learns which apps are in the foreground and when. No screen content.
- **Play risk:** moderate. Usage access needs disclosure. A `specialUse` foreground service is reviewed.

### 11b. In-app screen detection via AccessibilityService
- **How:**
  - Declare the service with `android:packageNames` limited to the target apps (the system then sends no events from any other app), event types `typeWindowStateChanged|typeWindowContentChanged`, `canRetrieveWindowContent=true`, and a `notificationTimeout` of about 500 ms or more to throttle the very chatty content events of video feeds.
  - On an event, inspect the active window's node tree for markers of the short-video player: view resource IDs (requires `flagReportViewIds`), content descriptions, or text such as a "Reels"/"Shorts" tab being selected.
  - Compute one boolean, `isShortFormFeed`, then debounce it (for example, true for 20 s or more of continuous scrolling) before interrupting.
- **Fragility:** high. Resource IDs and content descriptions are internal to Meta, Google and ByteDance, and change without notice. Server-side A/B tests mean two users of the same version can see different layouts. Play listings of existing blockers show user reviews like "still shows reels in the main feed", "stopped working after a few days" and "crashes Instagram". Mitigations:
  - keep detection rules in one small, testable matcher per app;
  - prefer several weak signals over one ID;
  - add a "Didn't catch it? / False alarm?" local feedback toggle;
  - fall back to 11a when no rule matches.

  Updating rules requires an app release, since there is no network by design.
- **Battery:** event-driven, so it is cheaper than polling. But a scrolling feed fires many content-change events. With `packageNames` scoping, throttling, and walking the tree only on window-state changes or at most every N ms, cost should be modest. Measure it with Battery Historian before shipping.
- **Privacy:**
  - What can be promised: screen content is read in memory inside the service, reduced to one boolean, never stored, never logged, never transmitted. Glitch has no `INTERNET` permission, so it physically cannot transmit. Events from non-target apps are never delivered because of `packageNames`.
  - The cost, stated honestly: the permission itself grants the ability to read the screen of the selected apps, and Android's warning dialog will say so.
- **Play policy (current):**
  - Glitch is not an accessibility tool. Do **not** set `isAccessibilityTool=true`; ADHD-adjacent marketing does not make a general focus app's primary purpose disability support.
  - Required steps: the Play Console Accessibility declaration (purpose, data collected or shared, video of the disclosure and the service in use); a prominent in-app disclosure and consent screen; and documenting the use in the store listing.
  - The "narrowly scoped API" rule is satisfied *only* for the in-app Reels/Shorts distinction, because no other API exposes it. App-level detection alone would not justify accessibility.
  - The API may not be used to "work around Android built-in platform security controls, privacy controls and notifications" or to stop users from disabling apps. A reminder screen that the user can dismiss is fine. Never block uninstalling Glitch or disabling the service.
- **Precedents on Play:** many Reels/Shorts blockers are live on Google Play using the Accessibility API: No Shorts, NoScroll, Shortstop, Block Scroll, StopScroll, Reels Blocker, UnReel, ReelsOff. one sec uses Accessibility plus overlay, with usage access as a fallback. This shows the category is approvable with the declaration route. Their review history is not public. Expect a slower first review and possible requests for a better video or disclosure.

### 11c. How to interrupt

| Option | Permission | Pros | Cons |
|---|---|---|---|
| Overlay `TYPE_APPLICATION_OVERLAY` | `SYSTEM_ALERT_WINDOW` (T2, Settings grant; restricted for sideloads) | Appears instantly over the feed | Extra special permission. Android may reposition or limit overlays. |
| `TYPE_ACCESSIBILITY_OVERLAY` | none beyond the accessibility service | Instant, no extra permission (11b only) | Only available with 11b. Must be clearly dismissible. |
| Full-screen activity (Glitch's own screen, then Back to feed or Home) | Starting activities from the background needs an exemption, for example `SYSTEM_ALERT_WINDOW` | Strong "snap out" moment, and the user's words fill the screen | Background launch restrictions. Feels most like a blocker. |
| Notification (heads-up) | `POST_NOTIFICATIONS` (T1) | Least intrusive. Works with 11a. | Easy to ignore while scrolling. `USE_FULL_SCREEN_INTENT` is only auto-granted to alarm and calling apps on Android 14+, so do not rely on it. |

Recommended: a heads-up notification for phase A. In phase B, an accessibility overlay that shows the user's own sentence, with two buttons, "Back to [my reason]" (sends the user Home) and "Keep watching 5 min" (snoozes). No forced Back action and no lockout.

### Recommended phased path for 11
1. **v1.x (now, T0):** distracting marks, intention, pause and pinned shortcuts (1, 2a, 2b-lite, 3), plus a "reminder sentence" the user writes and sees in the gate. This tests whether self-authored reminders help, with zero permissions.
2. **v2 (T1/T2 opt-in):** app-level timer (11a / 5b) for TikTok, Instagram, YouTube and Facebook, only during a user-started focus session or with session-scoped polling. Interrupt via notification. Usage access and onboarding.
3. **v3 (T3 opt-in):** in-app Reels/Shorts detection via accessibility (11b), with the overlay interrupt. Ship it only after:
   - per-app matchers with fixtures-based unit tests;
   - a measured battery budget;
   - the Play declaration and video;
   - a privacy policy update.

   Strongly consider shipping v3 as a **separate companion app or product flavor** ("Glitch Guard") so the core Glitch listing keeps "permissions: none" and its fast, clean review. Opening the companion stays a deliberate user choice. *(Superseded by AGENTS.md: the Accessibility feature ships only as a companion app in its own repository.)*

---

## Summary table

| # | Feature | Permissions (tier) | Play policy risk | Effort | Recommendation |
|---|---|---|---|---|---|
| 1 | Intention prompt in Glitch | none (T0) | none | S | **Do now** |
| 2a | Pause/breathe screen in Glitch | none (T0) | none | S | **Do now** |
| 2b-lite | Pinned "via Glitch" shortcuts | none (T0) | none | S-M | **Do soon** |
| 2b | Intercept launches anywhere | Usage access + overlay + FGS (T2) or Accessibility (T3) | High (Accessibility declaration, disclosure) | L | Later, opt-in tier only |
| 3 | Mark/hide/grayscale in menu | none (T0) | none | S | **Do now** |
| 4 | Today's usage per app | Usage access (T2) | Medium (disclosure) | M | Later, opt-in |
| 5a | Launch-based gentle nudge | `POST_NOTIFICATIONS` (T1) | Low | S-M | Later (v2), off by default |
| 5b/c | Accurate in-app session timer | Usage access + FGS `specialUse` (T2) / Accessibility (T3) | Medium-High | L | Avoid (unless 11 lands) |
| 6 | Focus session + widget + QS tile | none for widget/tile (T0); notification T1; exact alarm or FGS T2 | Low (High-ish if FGS/exact alarm) | M | Later; widget first |
| 7 | Optional HOME launcher | none, but `ROLE_HOME` | Low policy, very high product risk | L-XL | **Avoid** |
| 8 | First-open-of-day intention | none (T0) | none | S | **Do now** |
| 9 | Notification batching | Notification access (T2) | Medium-High (sensitive data) | L | **Avoid**; link to DND/Modes |
| 10 | Local storage, export, delete | none (T0) | none | S-M | **Do now** |
| 11a | Reels detector, app-level | Usage access + FGS or WorkManager (T2) + T1 | Medium | M-L | v2, opt-in |
| 11b | Reels detector, in-app | Accessibility (T3) (+ overlay optional) | High but precedented | L | v3, separate companion app (AGENTS.md) |

**Features that keep "no permissions" fully intact:** 1, 2a, 2b-lite (pinned shortcuts), 3, 8, 10, and the widget/tile part of 6.

---

## Architecture sketch

The aim is to fit the current single `:app` module and its `model/`, `apps/` and `menu/` packages, without frameworks or extra modules.

```
com.benisalla.glitch
├── MainActivity.kt          wiring only; also reads an optional "gate for <key>" extra (2b-lite)
├── model/
│   ├── AppEntry.kt          unchanged
│   ├── Contracts.kt         AppCatalog, AppLauncher, LaunchResult unchanged
│   └── Prefs.kt             NEW: data class AppMark(key, distracting, hidden, gate: Gate)
│                                 enum Gate { NONE, INTENTION, PAUSE }
│                                 interface LocalStore { marks(): Flow<Map<String, AppMark>>; setMark(..);
│                                   todayIntention(..); exportJson(): String; clearAll() }
│                                 (the interface exists only so MenuViewModel stays JVM-testable with a fake)
├── apps/
│   ├── PackageManagerAppCatalog.kt, IntentAppLauncher.kt   unchanged
│   ├── PinnedShortcuts.kt   NEW (2b-lite): requestPinShortcut for an AppEntry
│   └── DataStoreLocalStore.kt NEW: LocalStore backed by Preferences DataStore
└── menu/
    ├── MenuOrder.kt          unchanged shuffle/reconcile; hidden filtering applied AFTER order
    ├── MenuViewModel.kt      + marks flow combined into Ready(apps, marks)
    │                         + gate state: Idle | Asking(entry, gate) | Pausing(entry, endsAt)
    │                         + injected clock (like the injected Random) for pause tests
    ├── MenuScreen.kt         + long-press actions, grayscale ColorFilter, hidden-apps sheet
    ├── GateScreen.kt         NEW: intention sheet + pause screen (pure UI)
    └── DailyIntention.kt     NEW: first-open-of-day check (LocalDate in, Boolean out), pure
```

Rules this keeps:
- **Session semantics unchanged.** Gates are in-activity UI, so they cause no `onStop` and no reshuffle. Launching from a gate goes through the same `AppLauncher`, so "return after launch = new session" still holds.
- **Contract change discipline.** Adding `Prefs.kt` to `model/` means adding a fake `LocalStore` to the existing `MenuViewModel` tests in the same change.
- **Tests (no flakiness):**
  - gate state transitions with a fake clock;
  - hidden filtering preserves the permutation invariant of the visible subset;
  - a mark on a removed app is ignored and pruned;
  - first-of-day logic with fixed dates;
  - export/clear round-trip on the fake.
- **Dependencies to justify:** `androidx.datastore:datastore-preferences` (needed for 1, 3, 8, 10). Later, `androidx.glance:glance-appwidget` (6) and `androidx.work:work-runtime` (5a/11a).
- **Future permissioned tiers** (4, 5a, 11) would go into a new `focus/` package. They should touch the menu only through `LocalStore` and `AppEntry.key`, so the T0 core never depends on them. If 11b ships, put it in a product flavor or a companion app, so the default build's merged manifest still has zero `<uses-permission>`. CI can assert that with a merged-manifest check. *(Superseded by AGENTS.md: the Accessibility feature ships only as a companion app in its own repository.)*

---

## Sources
- Play: Use of the AccessibilityService API (isAccessibilityTool, qualifying apps, declaration, disclosure): https://support.google.com/googleplay/android-developer/answer/10964491
- Play: Permissions and APIs that Access Sensitive Information (Accessibility API rules, narrow-API rule, QUERY_ALL_PACKAGES, special permissions via Settings): https://support.google.com/googleplay/android-developer/answer/16558241 and https://support.google.com/googleplay/android-developer/answer/9888170
- Play: isMonitoringTool / Stalkerware policy (monitoring *another* person; not applicable to self-use): https://support.google.com/googleplay/android-developer/answer/12955211
- Play: Foreground service and full-screen intent requirements: https://support.google.com/googleplay/android-developer/answer/13392821
- Android 14 behaviour changes (full-screen intent, FGS types): https://developer.android.com/about/versions/14/behavior-changes-14
- UsageStatsManager (PACKAGE_USAGE_STATS granted in Settings, queryEventsForSelf, null when locked): https://developer.android.com/reference/android/app/usage/UsageStatsManager
- Foreground service types (`specialUse` reviewed in Play Console, `shortService` ~3 min): https://developer.android.com/develop/background-work/services/fgs/service-types
- Schedule alarms (exact alarm permissions, not pre-granted on API 33+, 10-min window clipping): https://developer.android.com/develop/background-work/services/alarms/schedule
- Background activity start restrictions and exemptions: https://developer.android.com/guide/components/activities/background-starts
- WindowManager.LayoutParams (`TYPE_ACCESSIBILITY_OVERLAY`, `TYPE_APPLICATION_OVERLAY` requires SYSTEM_ALERT_WINDOW): https://developer.android.com/reference/android/view/WindowManager.LayoutParams
- AccessibilityServiceInfo (`isAccessibilityTool`, `packageNames`, `notificationTimeout`): https://developer.android.com/reference/android/accessibilityservice/AccessibilityServiceInfo
- NotificationListenerService (BIND_NOTIFICATION_LISTENER_SERVICE, snoozeNotification): https://developer.android.com/reference/android/service/notification/NotificationListenerService
- RoleManager.ROLE_HOME: https://developer.android.com/reference/android/app/role/RoleManager
- Quick Settings tiles (BIND_QUICK_SETTINGS_TILE, requestAddTileService): https://developer.android.com/develop/ui/views/quicksettings-tiles
- Package visibility `<queries>`: https://developer.android.com/training/package-visibility/declaring
- one sec on Android (Accessibility + overlay, usage-access fallback and its downsides): https://feedback.one-sec.app/help/articles/5486811-one-sec-for-android and https://tutorials.one-sec.app/en/articles/3148866
- Reels/Shorts blockers live on Play (Accessibility-based; reviews cite breakage): https://play.google.com/store/apps/details?id=com.app.noshorts , https://play.google.com/store/apps/details?id=com.newswarajya.noswipe.reelshortblocker , https://play.google.com/store/apps/details?id=com.ladeon.shortstop , https://play.google.com/store/apps/details?id=com.arktechplugins.blockscroll , https://play.google.com/store/apps/details?id=com.swt.reelsblocker , https://play.google.com/store/apps/details?id=com.unreel.noscroll
- Open-source references: https://github.com/atick-faisal/Shorts-Blocker , https://f-droid.org/packages/com.resolvecommunity.awaydoomscrollin/
- Android 13/15 restricted settings for sideloaded apps (secondary sources, not official docs): https://androidauthority.com/android-15-restricted-settings-sideloading-3481098 , https://9to5google.com/2024/09/12/android-15-sideloaded-apps-restrictions/
