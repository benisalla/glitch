# Glitch: shuffle schedules (plan)

Status: **plan only, no app code.** Needs the owner's explicit approval and an AGENTS.md update (session semantics, the persisted order) before anyone builds it. Target: after the v1.1.0 home-screen work is merged, so v1.1.x or v1.2.

Baseline this plan assumes (the v1.1.0 launcher PR, not merged yet, described in AGENTS.md on `docs/home-screen-v1.1`):
- **Home mode:** reshuffle on a cold start and after the screen has turned off (a runtime-registered `ACTION_SCREEN_OFF` receiver marks the next show as a new shuffle). Returning Home (Home button, gesture, back, `onNewIntent`) never reshuffles.
- **App mode:** one shuffle per newly opened menu (`onStart` after a real `onStop`).
- **Dock:** up to 4 apps, fixed, never part of the shuffle.
- Today the shuffled order lives only in `MenuViewModel` memory; nothing about the order is persisted.

What the owner asked for: let the user decide **when** shuffling may happen (recurring windows, a master switch, a one-tap Travel pause), with no new permissions, no services, no alarms and no network.

---

## 0. Android facts this plan relies on (verified 2026-10-13)

| # | Fact | Consequence for the design | Source |
|---|---|---|---|
| F1 | `ACTION_SCREEN_OFF` / `ACTION_SCREEN_ON`: "You cannot receive this through components declared in manifests, only by explicitly registering for it with `Context.registerReceiver()`." Screen-off really means "the device becomes non-interactive". | The screen-off signal only exists while our process is alive. A cold start must be handled separately. | [Intent reference](https://developer.android.com/reference/android/content/Intent#ACTION_SCREEN_OFF) |
| F2 | `ACTION_USER_PRESENT`: "Sent when the user is present after device wakes up (e.g when the keyguard is gone)." It is not on the implicit-broadcast exception list, so since Android 8.0 a manifest receiver in an app targeting 26+ doesn't get it. It works only as a runtime receiver, exactly like screen-off. | No advantage over screen-off on reachability, and it fires *after* the keyguard is gone, which is often after the home screen is already visible. See §5.2. | [Intent reference](https://developer.android.com/reference/android/content/Intent#ACTION_USER_PRESENT), [Implicit broadcast exceptions](https://developer.android.com/develop/background-work/background-tasks/broadcasts/broadcast-exceptions) |
| F3 | `ACTION_TIME_SET` (`ACTION_TIME_CHANGED`) and `ACTION_TIMEZONE_CHANGED` are **exempt**: manifest receivers still get them "no matter what API level the apps target". The same page says "avoid registering listeners for them". `ACTION_DATE_CHANGED` is not on the list. `ACTION_TIME_TICK` is runtime-only (every minute). | We *could* listen to time and zone changes, but we don't need to, because the schedule is evaluated at decision time with the current clock and zone. **Plan: register no time receivers at all.** | [Implicit broadcast exceptions](https://developer.android.com/develop/background-work/background-tasks/broadcasts/broadcast-exceptions), [Intent reference](https://developer.android.com/reference/android/content/Intent#ACTION_TIME_TICK) |
| F4 | API 37 adds `ACTION_TIMEZONE_OFFSET_CHANGED` (offset changes without a zone change, for example DST). | Not needed and not available on minSdk 26. Wall-clock evaluation already covers DST. | [Intent reference](https://developer.android.com/reference/android/content/Intent#ACTION_TIMEZONE_OFFSET_CHANGED) |
| F5 | Android 14+: "the system can place context-registered broadcasts in a queue while the app is in the cached state", and "Multiple instances of certain broadcasts might be merged into one broadcast." The broadcasts overview names `ACTION_SCREEN_ON` as an example of a deferred broadcast. | A runtime screen-off receiver in a *cached* process can arrive late, possibly only when the activity comes back to the foreground. The design must not depend on the broadcast's timing alone (§5.2 fallbacks). | [Android 14 behavior changes](https://developer.android.com/about/versions/14/behavior-changes-all), [Broadcasts overview](https://developer.android.com/develop/background-work/background-tasks/broadcasts) |
| F6 | Process importance: cached processes are kept in a list and the system "tries to keep more useful processes, such as those hosting the user's home application". In AOSP, `ProcessList.HOME_APP_ADJ = 600` ("we want to try avoiding killing it ... because the user interacts with it so much") sits well above the cached range `CACHED_APP_MIN_ADJ = 900`. | As the default home, Glitch's process usually survives and is usually not cached, so screen-off normally arrives on time. **Usually is not always.** Samsung memory management, low-RAM devices and reboots still kill it, so the persisted state (§2.3) is mandatory. AOSP source is not an official guarantee, so it is verified on a device (§6.3). | [Processes and app lifecycle](https://developer.android.com/guide/components/activities/process-lifecycle), [AOSP ProcessList.java](https://android.googlesource.com/platform/frameworks/base/+/refs/heads/main/services/core/java/com/android/server/am/ProcessList.java) |
| F7 | Context-registered receivers "receive broadcasts as long as their registering context is valid". Register with the Application context to receive them "as long as the app runs". On API 33+ a runtime receiver needs `RECEIVER_EXPORTED` or `RECEIVER_NOT_EXPORTED`. With `NOT_EXPORTED` it still gets "some system broadcasts", but not those from privileged non-system-UID apps. | Screen-off comes from the system server, so `ContextCompat.RECEIVER_NOT_EXPORTED` is enough. The launcher PR owns this receiver. | [Broadcasts overview](https://developer.android.com/develop/background-work/background-tasks/broadcasts) |
| F8 | `PowerManager.isInteractive()` (API 20) tells whether the device is interactive. | It's a cheap fallback in `onStop`: if the activity stops *because* the screen went off, `isInteractive()` is already false (verify on a device). | [PowerManager](https://developer.android.com/reference/android/os/PowerManager#isInteractive()) |
| F9 | `java.time` (`ZonedDateTime`, `LocalTime`, `DayOfWeek`, `Clock`) is "Added in API level 26". Core-library desugaring only matters for a *subset* of `java.time` on older releases. | minSdk is 26, so we use `java.time` natively. **No desugaring and no new dependency.** | [ZonedDateTime](https://developer.android.com/reference/java/time/ZonedDateTime), [Java 8+ support](https://developer.android.com/studio/write/java8-support) |
| F10 | `ZonedDateTime` docs on local time: a **gap** (spring forward) has local times with no valid offset, and an **overlap** (autumn) has local times with two valid offsets. | We evaluate windows on the *local wall-clock* reading of "now", which is always well defined. Gaps and overlaps only change how long a window lasts in real time (§1.3). | [ZonedDateTime](https://developer.android.com/reference/java/time/ZonedDateTime) |
| F11 | When the zone changes, the framework calls `TimeZone.setDefault(null)` in every running app process (`ActivityThread.ApplicationThread.updateTimeZone()`), so the next `ZoneId.systemDefault()` sees the new zone. | Read the zone **per check** (`ZoneId.systemDefault()`), never cache it. Note that `Clock.systemDefaultZone()` freezes the zone when it is created, so we don't use it as a long-lived field. | [AOSP ActivityThread.java](https://android.googlesource.com/platform/frameworks/base/+/refs/heads/main/core/java/android/app/ActivityThread.java) |
| F12 | `Settings.Global.BOOT_COUNT` (API 24): "Boot count since the device starts running API level 24". It is readable without a permission. | It is an optional way to tell "reboot" from "process death" on a cold start (open question Q6). | [Settings.Global](https://developer.android.com/reference/android/provider/Settings.Global#BOOT_COUNT) |
| F13 | `adb shell cmd alarm set-time <epoch ms>` and `cmd alarm set-timezone <Olson id>` exist in AOSP `AlarmManagerService.ShellCmd`. | These are the commands for manual time tests (§6.3). `setprop persist.sys.timezone` alone sends no `TIMEZONE_CHANGED` and needs root on user builds, so we don't use it. | [AOSP AlarmManagerService.java](https://android.googlesource.com/platform/frameworks/base/+/refs/heads/main/apex/jobscheduler/service/java/com/android/server/alarm/AlarmManagerService.java) |

Exact-alarm permissions, `TIME_TICK`, WorkManager, foreground services and location are **not** used. No manifest change at all.

---

## 1. Behavior spec

### 1.1 Concepts

- **Shuffle on unlock (master switch):** on by default. When off, Glitch never reshuffles: the order stays as it is until the user turns it back on. The dock is fixed either way.
- **Shuffle times (windows):** recurring local wall-clock windows. Each one has a set of weekdays, a start and an end. If the end is at or before the start, the window is **overnight** and ends on the next day. The weekday is the day the window **starts** (Fri 22:00–02:00 covers Friday night into Saturday 02:00). Start is inclusive and end is exclusive. "All day" is 00:00–24:00. Several windows are allowed; "now" is allowed if **any** window contains it. Default: one window, *Every day, All day* (so behaviour is the same as v1.1 until the user changes it).
- **Travel pause:** a temporary hold that wins over the windows. Three options: *2 hours* (an absolute instant), *Until tomorrow* (= through today, local), *Until a date* (through the chosen local date). It ends by itself; no timer runs.
- **Due:** a persisted flag meaning "a new shuffle has been earned but not applied yet". In home mode, screen-off sets it. In app mode, every real `onStop` sets it (that's today's per-open session rule). A shuffle clears it. Showing the menu while not eligible **keeps** it, so it is applied on the next eligible unlock *or* show (the owner's "missed-while-locked" rule).
- **Decision time:** the only moments we decide are the launcher's `onStart` paths: cold start, a show while due, a plain show. Nothing runs in the background, and the receiver only flips a flag.

### 1.2 Decision table: "should we reshuffle now?"

Inputs: `trigger`, master switch, windows, pause, `lastShuffleAt` (null = no saved order). Rows are evaluated **top to bottom; the first match wins.** "Keep" always means: restore or keep the current grid order, reconcile it with installed or removed apps, never reshuffle.

| # | Situation | Trigger | Result | Due afterwards |
|---|---|---|---|---|
| 1 | Rotation, dark-mode or font change, multi-window resize | `CONFIG_CHANGE` | Keep | unchanged |
| 2 | No saved order (first run, data cleared, unreadable store) | any but 1 | **Shuffle** (initial order, even if off or paused) | cleared |
| 3 | Back to Home, no screen-off since the last shuffle | `PLAIN_SHOW` | Keep | false |
| 4 | Master switch **off** | `DUE_SHOW` / `COLD_START` | Keep | **cleared** (unlocks while off never pile up) |
| 5 | Travel pause **active** (2 hours not over, or today ≤ the "through" date) | `DUE_SHOW` / `COLD_START` | Keep | **kept** |
| 6 | Pause **expired** (2 h over / date passed) | same | ignore the pause and go on to rows 7 and 8; the stale pause is cleared from storage | – |
| 7 | **Inside** a window (including the after-midnight part of an overnight window) | `DUE_SHOW` / `COLD_START` | **Shuffle** | cleared |
| 8 | **Outside** every window (or no windows configured) | `DUE_SHOW` / `COLD_START` | Keep | **kept** |

Named scenarios, all derived from the table:

| Scenario | Expected |
|---|---|
| Mon–Fri 07:00–22:00. Tue 2026-10-13: lock 21:30, unlock 21:45. | Shuffle (row 7). |
| Same: lock 21:55, unlock 22:10. | Keep (row 8); due is still set. |
| …continued: unlock Wed 06:30. | Keep (row 8). |
| …continued: unlock Wed 07:05, or the first Home return after 07:00 while the phone is in use. | **Shuffle once** (row 7, missed-while-locked). Then due is cleared, and later Home returns keep the order. |
| Overnight window Fri 22:00–02:00. Unlock Sat 01:30. | Shuffle (Friday's window carries into Saturday). |
| Same: unlock Sat 02:00. | Keep (end exclusive). |
| Overnight window Sun 22:00–02:00. Unlock Mon 01:00 (Monday not selected). | Shuffle (belongs to Sunday's window). |
| Travel pause "2 hours" set at 14:00. Unlock 15:00. | Keep, due kept. |
| …unlock 16:05. | Pause expired → shuffle if inside a window. |
| "Until 20 Oct". Unlock 20 Oct 23:50. | Keep. |
| …unlock 21 Oct 00:10, inside a window. | Shuffle. |
| Reboot at 23:00 outside every window. | Cold start → Keep: the saved order is restored and reconciled with the installed apps. |
| Reboot inside a window. | Cold start → Shuffle. |
| Process killed by the system while the phone is in use, inside a window. | Cold start → Shuffle (v1.1 rule). See Q6 for a better option. |
| Flight from Paris to New York: 07:30 Paris is 01:30 New York. Window 07:00–22:00. | Evaluated in the **new** zone at the next check: 01:30 → Keep. |
| Manual clock change, forward or back. | No special case. Windows use the new wall clock. A 2-hour pause whose start is now in the future (clock set back) counts as expired. `lastShuffleAt` in the future is harmless (only null vs non-null matters). |
| Master switch turned on, or the schedule edited, while the grid is visible. | No reshuffle right now. The new rules apply at the next decision. |
| Dock app added or removed. | Never reshuffles (v1.1 rule), whatever the schedule. |

### 1.3 DST and other edge cases (Europe/Paris)

- **Spring forward, Sun 2027-03-28:** at 02:00 CET the clock jumps to 03:00 CEST, so 02:00–02:59 never happens.
  - A window `Sun 02:00–02:45` doesn't occur that day. A Sunday unlock is never inside it.
  - A window `Sun 01:30–02:30` is inside at 01:45 CET and outside at 03:00 CEST. It lasts 30 real minutes.
  - An overnight window `Sat 22:00–02:30`: Sunday 01:59 CET is inside, 03:00 CEST is outside.
  - Nothing is ever "skipped into": we only compare the current reading.
- **Fall back, Sun 2026-10-25:** at 03:00 CEST the clock goes back to 02:00 CET, so 02:00–02:59 happens twice. A window `Sun 02:00–03:00` is inside for both occurrences (2 real hours). A window ending at 02:30 is inside, then outside, then inside again for the second 02:00–02:30. That's correct for wall-clock rules: at most one extra eligible unlock.
- **2-hour pause across a DST change:** it is an `Instant`, so it lasts exactly 2 real hours. The UI shows the end in local time ("until 02:30").
- **Date pause across zones:** "through 20 Oct" is a `LocalDate` judged against the *current* local date, so it follows the traveller's wall clock.
- **Clock set back before a 2-hour pause started:** the pause counts as expired, so shuffling resumes. Failing towards shuffling is harmless and the user can pause again. We don't use a monotonic clock, because it resets at reboot.
- **Unlock while a delayed screen-off broadcast (F5) is still queued:** covered by the `onStop` fallback (§5.2). Worst case, one unlock is missed. We never reshuffle a grid the user is looking at.

### 1.4 App mode (Glitch not the default home)

Same table. Every real `onStop` sets due, so opening Glitch inside a window shuffles as today, and outside a window it shows the saved order. Proposed in Q5.

---

## 2. Data model and storage

### 2.1 Pure Kotlin model (`menu/ShuffleSchedule.kt`, no Android imports)

```kotlin
/** A recurring window. Minutes since local midnight. endMinute <= startMinute means overnight. */
data class ShuffleWindow(
    val days: Set<DayOfWeek>,      // non-empty; the day the window STARTS
    val startMinute: Int,          // 0..1439
    val endMinute: Int,            // 1..1440 (1440 = 24:00)
) {
    init {
        require(days.isNotEmpty())
        require(startMinute in 0..1439 && endMinute in 1..1440 && startMinute != endMinute)
    }
    val isOvernight: Boolean get() = endMinute <= startMinute
    val isAllDay: Boolean get() = startMinute == 0 && endMinute == 1440

    /** Wall-clock containment. Start inclusive, end exclusive. */
    fun contains(local: LocalDateTime): Boolean {
        val m = local.hour * 60 + local.minute
        val today = local.dayOfWeek
        val yesterday = today.minus(1)
        return if (!isOvernight) today in days && m >= startMinute && m < endMinute
        else (today in days && m >= startMinute) || (yesterday in days && m < endMinute)
    }
}

data class ShuffleSchedule(
    val enabled: Boolean = true,                         // master "Shuffle on unlock"
    val windows: List<ShuffleWindow> = listOf(EVERY_DAY_ALL_DAY),
) {
    fun allows(local: LocalDateTime): Boolean = windows.any { it.contains(local) }
    companion object {
        val EVERY_DAY_ALL_DAY = ShuffleWindow(DayOfWeek.entries.toSet(), 0, 1440)
        const val MAX_WINDOWS = 6
    }
}

sealed interface TravelPause {
    data object None : TravelPause
    /** "2 hours": absolute, immune to zone/DST changes. */
    data class Until(val startedAt: Instant, val until: Instant) : TravelPause
    /** "Until tomorrow" = Through(today); "Until a date" = Through(date). Local, inclusive. */
    data class Through(val lastDay: LocalDate) : TravelPause

    fun isActive(now: ZonedDateTime): Boolean = when (this) {
        None -> false
        is Until -> { val i = now.toInstant(); !i.isBefore(startedAt) && i.isBefore(until) }
        is Through -> !now.toLocalDate().isAfter(lastDay)
    }

    companion object {
        fun twoHours(now: ZonedDateTime) = Until(now.toInstant(), now.toInstant().plus(Duration.ofHours(2)))
        fun untilTomorrow(now: ZonedDateTime) = Through(now.toLocalDate())
        fun through(date: LocalDate, now: ZonedDateTime): Through {
            require(!date.isBefore(now.toLocalDate()) && !date.isAfter(now.toLocalDate().plusDays(365)))
            return Through(date)
        }
    }
}

enum class ShuffleTrigger { COLD_START, DUE_SHOW, PLAIN_SHOW, CONFIG_CHANGE }

sealed interface ShuffleDecision {
    data class Shuffle(val reason: Reason) : ShuffleDecision
    data class Keep(val reason: Reason, val stillDue: Boolean) : ShuffleDecision
    enum class Reason { NO_SAVED_ORDER, CONFIG_CHANGE, NOT_DUE, SWITCHED_OFF, PAUSED, IN_WINDOW, OUTSIDE_WINDOWS }
}
```

Notes:
- Minutes, not `LocalTime`, because `LocalTime` can't express the 24:00 end of an all-day window.
- Seconds are ignored (minute precision). 21:59:59 is inside a window ending at 22:00, and 22:00:00 is outside.
- Validation lives in the model (`require`). The editor prevents invalid input, and the store drops invalid rows instead of crashing.

### 2.2 Pure decision function (`menu/ShufflePolicy.kt`)

```kotlin
object ShufflePolicy {
    fun shouldReshuffle(
        now: ZonedDateTime,           // ZonedDateTime.now(clock.withZone(ZoneId.systemDefault())) at call time
        lastShuffleAt: Instant?,      // null = no saved order exists
        schedule: ShuffleSchedule,
        pause: TravelPause,
        trigger: ShuffleTrigger,
    ): ShuffleDecision
}
```

Exact semantics, in this order:
1. `trigger == CONFIG_CHANGE` → `Keep(CONFIG_CHANGE, stillDue = <unchanged>)`. The caller doesn't touch the flag.
2. `lastShuffleAt == null` → `Shuffle(NO_SAVED_ORDER)`.
3. `trigger == PLAIN_SHOW` → `Keep(NOT_DUE, stillDue = false)`.
4. `!schedule.enabled` → `Keep(SWITCHED_OFF, stillDue = false)`.
5. `pause.isActive(now)` → `Keep(PAUSED, stillDue = true)`.
6. `schedule.allows(now.toLocalDateTime())` → `Shuffle(IN_WINDOW)`.
7. otherwise → `Keep(OUTSIDE_WINDOWS, stillDue = true)`.

Properties: total (it never throws), deterministic, no I/O. `lastShuffleAt` is only checked for null (rule 2). It is also stored so the editor can show "Last shuffled 07:42". A future or past value never changes the result, which is what makes manual clock changes safe. Expired pauses are reported by a second pure helper, `TravelPause.expiredOrNone(now)`, so the caller can clear them. The caller (ViewModel) maps the decision to state changes:
- `Shuffle` → shuffle the grid (dock excluded), set `lastShuffleAt = clock.instant()`, `due = false`, persist the order.
- `Keep(stillDue)` → restore or reconcile, `due = stillDue` (except for `CONFIG_CHANGE`).

A companion pure helper for the UI and status line, `ShufflePolicy.nextChange(now, schedule, pause): ZonedDateTime?`, gives the next local time at which the answer to "is now allowed?" flips. It scans day by day for up to 8 days and resolves gap times with `ZonedDateTime.of(local, zone)`, which shifts them forward. It is only used for text like "Shuffling resumes at 07:00", never for timers.

### 2.3 Storage: SharedPreferences, no new dependency

One private file `glitch_shuffle` (`Context.MODE_PRIVATE`; `allowBackup="false"` already keeps it on the device). It holds plain primitive keys, so no JSON library is needed. If the launcher PR adds a dock store, the shuffle state can go in its own file next to it.

| Key | Type | Meaning |
|---|---|---|
| `schema` | Int | `1` |
| `enabled` | Boolean | master switch (default true) |
| `windows` | String | one window per line, `days;start;end`. `days` is a 7-bit mask (bit 0 = Monday), so `31;420;1320` = Mon–Fri 07:00–22:00. Missing key = default all-day window. Empty string = no windows. |
| `pause_kind` | String | `none` / `until` / `through` |
| `pause_started` / `pause_until` | Long | epoch ms (kind `until`) |
| `pause_through` | String | ISO `LocalDate` (kind `through`) |
| `order` | String | the **grid** order (dock excluded) as `AppEntry.key` values joined by `\n` (keys are `package/activity` and contain no newline) |
| `last_shuffle_at` | Long | epoch ms. Absent = null. |
| `due` | Boolean | the pending-shuffle flag |
| `last_boot_count` | Int | only if Q6 is approved |

Write rules:
- `order` + `last_shuffle_at` + `due` are written together in one `edit().apply()` after every shuffle, and `order` again after every reconcile (installs and removals).
- `due = true` from the screen-off receiver uses `commit()`. It's a tiny synchronous write, so the flag survives if the process dies right after the screen goes off.
- Restore on a cold start: `MenuOrder.restore(savedKeys, latest)`. This is new and pure: it maps keys to the latest entries, drops missing ones, appends new apps in catalog order and excludes dock keys. Equivalently, it's `reconcile(savedKeys.mapNotNull(latestByKey::get), latest)`.
- A corrupt or unknown value falls back to the default for that key only. A corrupt `order` means `lastShuffleAt` is treated as null, so we shuffle (row 2).

Privacy: the order is a list of package names that stays in app-private storage and never leaves the device, the same data class the dock already stores. No permission and no Data safety change.

---

## 3. UI

### 3.1 Where it lives

- The grid's title row (`MenuTitle`, "Glitch") gets a trailing **⋮** button (48 dp target). Its menu: **Shuffle schedule**, plus whatever the launcher PR adds there (for example "Change home app"). Long-pressing an empty area is *not* used, because it's hard to discover and easy to trigger by accident on a home screen.
- The editor is a **full-screen Compose destination inside `MainActivity`**: a simple `var screen by rememberSaveable`, no Navigation dependency. Opening it doesn't stop the activity, so it never counts as a new session and never reshuffles. Back returns to the grid with the same order.
- A calm **status line** under the title, shown only when the order is being held. It is plain text and tapping it opens the editor:
  - `Order kept · shuffling resumes at 07:00`
  - `Paused for travel · until 16:05`
  - `Shuffle is off`

### 3.2 Wireframes

**A. Shuffle schedule (main screen)**
```
┌──────────────────────────────────────┐
│ ←  Shuffle schedule                  │
│                                      │
│  Shuffle on unlock            [ ●━ ] │
│  New order each time you unlock,     │
│  during the times below.             │
│                                      │
│  ┌────────────────────────────────┐  │
│  │ ✈  Travel pause                │  │
│  │    Keep your order for a while │  │
│  │                      [ Pause ] │  │
│  └────────────────────────────────┘  │
│                                      │
│  Shuffle times                       │
│  ┌────────────────────────────────┐  │
│  │ Mon–Fri · 07:00 – 22:00     ›  │  │
│  ├────────────────────────────────┤  │
│  │ Fri, Sat · 22:00 – 02:00    ›  │  │
│  │ Ends next day                  │  │
│  └────────────────────────────────┘  │
│  [ + Add time ]                      │
│                                      │
│  Right now: shuffling is on.         │
│  Your dock never moves.              │
└──────────────────────────────────────┘
```
- With the master switch off, the pause card and the list are dimmed and disabled, and the footer reads "Shuffle is off. Your order stays as it is."
- During a pause the card shows the remaining time and a **Resume now** button instead of **Pause** (see D).
- With no windows: "No shuffle times. Your order stays as it is until you add one."
- "Add time" is hidden at `MAX_WINDOWS` (6).

**B. Add / edit a time**
```
┌──────────────────────────────────────┐
│ ×  Shuffle time               Save   │
│                                      │
│  Days                                │
│  (M) (T) (W) (T) (F) ( S ) ( S )     │
│   ●   ●   ●   ●   ●                  │
│  Every day · Weekdays · Weekends     │
│                                      │
│  All day                     [ ━○ ]  │
│                                      │
│  From                        07:00 › │
│  Until                       22:00 › │
│                                      │
│  Mon–Fri, 07:00 to 22:00             │
│                                      │
│              [ Delete this time ]    │
└──────────────────────────────────────┘
```
- The day chips are `FilterChip`s, each with a full-name content description ("Monday, selected"). Week order follows the locale's first day (`WeekFields.of(locale)`). The quick picks fill the chips.
- From and Until open the Material 3 `TimePicker` in a dialog. It honours the 24h/12h system setting.
- When Until ≤ From, the summary reads "Fri, 22:00 to Sat, 02:00 (ends next day)".
- Validation stays inline and calm. Save is disabled with "Pick at least one day" or "Start and end can't be the same". There are no error dialogs.
- Delete is only shown when editing. It asks for no confirmation; an undo snackbar appears on screen A.

**C. Travel pause sheet (modal bottom sheet)**
```
┌──────────────────────────────────────┐
│               ───                    │
│  Travel pause                        │
│  Your apps stay where they are.      │
│  The dock never moves.               │
│                                      │
│  ○ For 2 hours        until 16:05    │
│  ○ Until tomorrow     until 00:00    │
│  ○ Until a date…      pick a date    │
│                                      │
│        [ Cancel ]   [ Pause ]        │
└──────────────────────────────────────┘
```
- The end times are computed live from the injected clock, in the local zone.
- "Until a date…" opens the Material 3 `DatePicker`, limited to today through +365 days. The row then reads "Through Tue, 20 Oct".

**D. Pause active (card on screen A)**
```
│  ┌────────────────────────────────┐  │
│  │ ✈  Paused for travel           │  │
│  │    1 h 42 min left · until 16:05│ │
│  │                  [ Resume now ]│  │
│  └────────────────────────────────┘  │
```
- For a date pause: "Through Tue, 20 Oct · 6 days left".
- The remaining time comes from a `produceState` that re-reads the clock **only while the screen is visible**, once a minute (a UI tick, not a background timer). "Resume now" clears the pause and doesn't reshuffle immediately. The next unlock decides.

### 3.3 Copy (English source strings; all go to `strings.xml`)

| Key | Text |
|---|---|
| `schedule_title` | Shuffle schedule |
| `schedule_master` | Shuffle on unlock |
| `schedule_master_sub` | New order each time you unlock, during the times below. |
| `schedule_off_footer` | Shuffle is off. Your order stays as it is. |
| `schedule_times` | Shuffle times |
| `schedule_add` | Add time |
| `schedule_empty` | No shuffle times. Your order stays as it is until you add one. |
| `schedule_now_on` | Right now: shuffling is on. |
| `schedule_now_held` | Right now: your order is kept until %1$s. |
| `schedule_dock_note` | Your dock never moves. |
| `window_title` | Shuffle time |
| `window_all_day` | All day |
| `window_from` / `window_until` | From / Until |
| `window_next_day` | Ends next day |
| `window_err_days` | Pick at least one day |
| `window_err_same` | Start and end can't be the same |
| `window_delete` / `window_deleted` / `undo` | Delete this time / Time removed / Undo |
| `pause_title` | Travel pause |
| `pause_sub` | Your apps stay where they are. The dock never moves. |
| `pause_2h` / `pause_tomorrow` / `pause_date` | For 2 hours / Until tomorrow / Until a date… |
| `pause_active` | Paused for travel |
| `pause_left` | %1$s left · until %2$s |
| `pause_through` | Through %1$s |
| `pause_resume` | Resume now |
| `status_held` | Order kept · shuffling resumes at %1$s |
| `status_paused` | Paused for travel · until %1$s |
| `status_off` | Shuffle is off |

Tone: no "focus", no "discipline", no streaks and no claims. The copy describes what the app does.

---

## 4. Pure-function contract summary

| Function | Pure? | Injected time |
|---|---|---|
| `ShuffleWindow.contains(LocalDateTime)` | yes | caller |
| `ShuffleSchedule.allows(LocalDateTime)` | yes | caller |
| `TravelPause.isActive(ZonedDateTime)`, `twoHours/untilTomorrow/through` | yes | caller |
| `ShufflePolicy.shouldReshuffle(...)` | yes | caller |
| `ShufflePolicy.nextChange(...)` | yes | caller |
| `ScheduleCodec.encode/decode` (prefs strings ↔ model) | yes | none |
| `MenuOrder.restore(keys, latest)` | yes | none |

The ViewModel gets `clock: java.time.Clock = Clock.systemUTC()` and `zone: () -> ZoneId = ZoneId::systemDefault`, and builds `ZonedDateTime.now(clock).withZoneSameInstant(zone())` on every check. Tests pass `Clock.fixed(...)` or a mutable fake clock plus a fixed zone.

---

## 5. Integration with the launcher code (no new modules, no DI)

### 5.1 Changes, file by file

| File | Change |
|---|---|
| `menu/ShuffleSchedule.kt` (new) | The model from §2.1 and `ScheduleCodec`. |
| `menu/ShufflePolicy.kt` (new) | `shouldReshuffle` and `nextChange`. |
| `menu/ShuffleStore.kt` (new) | A thin SharedPreferences wrapper behind a small interface `ShuffleStateStore` (load/save schedule, pause, order, `lastShuffleAt`, due). Unit tests use an in-memory fake. This is the only Android-touching new class in `menu/`. If the launcher PR already has a prefs store for the dock, extend that one instead of adding a second class. |
| `menu/MenuOrder.kt` | Add `restore(savedKeys, latest)`. |
| `menu/MenuViewModel.kt` | Replace the `needsShuffle = true` session flag with a call to the policy. Add `onScreenOff()` (marks due), the schedule/pause intents for the editor, `scheduleState: StateFlow` and `statusLine: StateFlow`. Constructor gains `store`, `clock`, `zone` (with defaults in the factory). |
| `menu/ScheduleScreen.kt` (new) | Screens A, B, C, D. Stateless composables that take state and callbacks, so Compose tests need no ViewModel. |
| `menu/MenuScreen.kt` | The ⋮ button, the status line, and switching between the grid and the editor. |
| `MainActivity.kt` | Wiring only. It already forwards screen-off (launcher PR). It adds the fallback in `onStop` (below) and passes `isInteractive`. |
| Manifest, Gradle | **No change.** No permission, no receiver declaration, no dependency. |

### 5.2 Trigger mapping in `MenuViewModel.onMenuStarted()`

```
if (!orderInMemory)              trigger = COLD_START      // process start: restore or shuffle
else if (store.due)              trigger = DUE_SHOW        // screen-off since last shuffle (home) / real stop (app mode)
else                             trigger = PLAIN_SHOW      // back to Home
// CONFIG_CHANGE never reaches here: the existing sessionActive guard skips it.
```

How `due` gets set (any one is enough; setting it twice is harmless):
1. **Screen-off receiver** (launcher PR, runtime-registered, `RECEIVER_NOT_EXPORTED`, Application or activity scope) → `viewModel.onScreenOff()` → `store.due = true` (commit). The schedule is **not** evaluated at screen-off; it's evaluated at the next show, which is when the user actually sees the grid.
2. **`onStop` fallback:** `onMenuStopped(changingConfigurations, interactive = powerManager.isInteractive)`. If the launcher itself was on screen when the phone locked, `interactive == false` → due. This covers a late or merged broadcast (F5) in the most common case: locking from the home screen.
3. **App mode:** every real `onStop` sets due (today's per-open session, unchanged in effect).
4. **Cold start:** treated as due by the policy (`COLD_START`).

**Why screen-off and not `ACTION_USER_PRESENT`:** both are runtime-only (F1, F2), so reach is the same. Screen-off fires *before* the next show, so the decision is ready when `onStart` runs and the user never sees an order change. `USER_PRESENT` fires *after* the keyguard is dismissed, by which time the home activity has often already started (the lock screen sits on top of it). Acting on it would either reshuffle a visible grid, which breaks the session rule, or have to wait for the next show anyway. Screen-off also counts a wake-and-glance at the lock screen as an "unlock", which is fine: the next show reshuffles once. `USER_PRESENT` adds nothing, so **don't register it.**

**Why no time receivers:** windows and pauses are evaluated at each decision with the current clock and zone (F3, F11). A `TIME_SET` or `TIMEZONE_CHANGED` receiver would only matter for a timer we don't have. The editor and status line recompute while visible.

**What we accept:** if the process was cached *and* the screen went off while another app was in front *and* the broadcast was queued past the next show, that one unlock is missed (keep, not shuffle). The next unlock catches up. We never shuffle a visible grid, and we never add a service to close this gap.

### 5.3 AGENTS.md updates needed (in the same PR as the code)

- Session semantics: "In home mode, a screen-off makes the next show *due*. A due show reshuffles only if the shuffle schedule allows it at that moment; otherwise it stays due. A cold start restores the saved order unless the schedule allows a shuffle."
- Architecture: the persisted grid order and schedule live in `menu/` storage on the device.
- Tests: add the schedule invariants from §6.

---

## 6. Test plan

### 6.1 JVM unit tests (`app/src/test/`, no Android, fixed clocks, zone `Europe/Paris` unless noted)

Fixtures: `W1 = Mon–Fri 07:00–22:00`, `W2 = Fri 22:00–02:00`, `W3 = Sun 22:00–02:00`, `W4 = Thu 23:00–01:00`. `at("2026-10-13T07:00")` builds a `ZonedDateTime` in Paris. Weekdays: 2026-10-13 is a Tuesday, 10-16 Fri, 10-17 Sat, 10-18 Sun, 10-19 Mon, 10-25 Sun, 2026-12-31 Thu, 2027-03-28 Sun.

**`ShuffleWindow` / `ShuffleSchedule`**
1. W1 Tue 2026-10-13 06:59 → false; 07:00 → true; 21:59 → true; 21:59:59 → true; 22:00 → false.
2. W1 Sat 2026-10-17 10:00 → false; Sun 10-18 10:00 → false.
3. W2 Fri 2026-10-16 21:59 → false; 22:00 → true; 23:59 → true; Sat 10-17 00:00 → true; 01:59 → true; 02:00 → false.
4. W2 Sat 2026-10-17 23:00 → false (Saturday not selected); Thu 10-15 23:00 → false.
5. W3 crossing the week boundary: Sun 2026-10-18 23:00 → true; Mon 10-19 01:00 → true (Mon not selected); Mon 02:00 → false; Mon 23:00 → false.
6. W4 crossing the year: Thu 2026-12-31 23:30 → true; Fri 2027-01-01 00:30 → true; 01:00 → false.
7. All-day window: 00:00 → true, 23:59 → true, on every weekday. The window is never overnight.
8. End at 24:00 (`20:00–24:00`, Tue): Tue 23:59 → true, Wed 00:00 → false.
9. Several windows: W1 + W2, Fri 2026-10-16 21:59 (W1) → true, 22:00 (W2) → true; the two windows touch, and the union has no gap.
10. Empty window list → `allows` is false for any time.
11. Construction rejects empty days, start == end, start 1440, end 0, and negative minutes.
12. **DST spring forward**, Paris Sun 2027-03-28. Window `Sun 01:30–02:30`: `at(01:45)` → true; `ZonedDateTime.of(2027-03-28T02:30, Paris)` resolves to 03:30 CEST → false; 03:00 CEST → false. Window `Sun 02:00–02:45`: every instant that day → false. Overnight `Sat 22:00–02:30`: Sun 01:59 CET → true, 03:00 CEST → false. Window `Sun 02:30–04:00`: 03:10 CEST → true.
13. **DST fall back**, Paris Sun 2026-10-25. Window `Sun 02:00–03:00`: `02:30.withEarlierOffsetAtOverlap()` (+02:00) → true; `02:30.withLaterOffsetAtOverlap()` (+01:00) → true; 03:00 CET → false. Window `Sun 01:00–02:30`: earlier 02:15 → true, earlier 02:45 → false, later 02:15 → true. This documents the deliberate wall-clock semantics.
14. **Zone change, same instant:** `2026-10-13T05:30Z` → Paris 07:30 Tue → W1 true; `America/New_York` 01:30 → false; `Asia/Tokyo` 14:30 Tue → true.
15. Zone change across the date line: `2026-10-16T21:30Z` → Paris Fri 23:30 → W2 true; Tokyo Sat 06:30 → W2 false.

**`TravelPause`**

16. `twoHours(at 2026-10-13T14:00)`: 15:59:59 → active; 16:00 → inactive; 13:59 (clock set back) → inactive.
17. `twoHours` across fall back: started at 2026-10-25T01:30+02:00 (23:30Z) → active at 02:15+01:00 (01:15Z), inactive at 02:30+01:00 (01:30Z).
18. `untilTomorrow(at 2026-10-13T23:30)` → `Through(2026-10-13)`: active at 23:59, inactive at 2026-10-14T00:00.
19. `through(2026-10-20)`: active at 2026-10-20T23:59 Paris; in New York at the same instant (17:59) → active; at `2026-10-20T17:00Z` in Tokyo (Oct 21 02:00) → inactive; Paris 2026-10-21T00:00 → inactive.
20. `through` rejects yesterday and today+366; accepts today and today+365.
21. `None` is never active.

**`ShufflePolicy.shouldReshuffle`**: one test per row of §1.2, then combinations.

22. `CONFIG_CHANGE` → Keep for every combination of enabled/paused/in-window, even when `lastShuffleAt` is null.
23. `lastShuffleAt == null` + `PLAIN_SHOW`, off, paused, outside → `Shuffle(NO_SAVED_ORDER)`.
24. `PLAIN_SHOW` inside a window → `Keep(NOT_DUE, stillDue=false)` (back Home never reshuffles).
25. Off + `DUE_SHOW` inside a window → `Keep(SWITCHED_OFF, false)`.
26. Paused + `DUE_SHOW` inside a window → `Keep(PAUSED, true)`; the pause wins over the window.
27. Expired 2-hour pause + `DUE_SHOW` inside a window → `Shuffle(IN_WINDOW)`.
28. `DUE_SHOW` Tue 2026-10-13 21:45 (W1) → Shuffle; 22:10 → `Keep(OUTSIDE_WINDOWS, true)`.
29. Missed-while-locked sequence (fake clock, applied through a tiny state reducer): screen off 21:55 → show 22:10 Keep(due) → show Wed 06:30 Keep(due) → show Wed 07:05 Shuffle → show 07:20 `PLAIN_SHOW` Keep. Assert exactly one Shuffle.
30. `COLD_START` outside with a saved order → `Keep(OUTSIDE_WINDOWS, true)`; inside → Shuffle.
31. `lastShuffleAt` far in the future (clock set back a year) → same results as 28 to 30.
32. No windows + `DUE_SHOW` → `Keep(OUTSIDE_WINDOWS, true)`.
33. Overnight W2: `DUE_SHOW` at Sat 2026-10-17 01:30 → Shuffle; Sat 02:00 → Keep.
34. Property test with a fixed `Random(seed)`: 10,000 random (schedule, pause, trigger, instant) tuples in four zones. Assert the result is total (no throw) and is Shuffle ⇔ (row-2 condition) ∨ (due trigger ∧ enabled ∧ ¬paused ∧ allowed). The seed is fixed and nothing about randomness is asserted.

**`nextChange`**

35. W1 only: Tue 22:10 → Wed 07:00; Fri 22:10 → Mon 07:00. W1 + W2: Fri 21:00 → Sat 02:00 (the union stays allowed across 22:00, so the next flip is the end of W2).
36. Window `Sun 02:00–04:00` on 2027-03-28 at 01:00 → 03:00 CEST (gap shifted forward).
37. Paused for 2 hours, inside a window → returns the pause end.

**Codec and store**

38. Round-trip encode/decode for: all-day, overnight, 6 windows, every day mask, all pause kinds, an order with 300 keys.
39. A corrupt `windows` row is dropped and the others are kept. An unknown `pause_kind` → `None`. A garbage `order` → the order is treated as missing (`lastShuffleAt` = null). Nothing throws.
40. Missing keys (an upgrade from v1.1) → enabled, all-day window, no pause, due = false, no saved order → the first cold start shuffles.

**`MenuOrder.restore`**

41. Saved `[c, a, b]`, latest `[a, b, c, d]` → `[c, a, b, d]`; saved key `x` that was uninstalled → dropped; no duplicates; the result is a permutation of latest minus dock.
42. Dock keys in the saved order (dock changed while the process was dead) are excluded from the grid.

**`MenuViewModel`** (fake catalog, fake store, mutable clock, `Random(42)`, `StandardTestDispatcher`)

43. Cold start, outside a window, saved order present → the state equals `restore(saved, latest)` exactly, and the shuffle was never called (spy `Random`).
44. Cold start inside a window → a permutation of the catalog minus the dock; `lastShuffleAt` = clock; due = false; the order is persisted.
45. `onScreenOff()` → stop → start at an in-window time → one reshuffle; a second stop/start without screen-off → same order.
46. `onScreenOff()` outside → start → same order, due still true in the store. Move the clock to 07:00 → stop/start → reshuffled.
47. `onMenuStopped(changing=false, interactive=false)` without `onScreenOff()` → the next start is due (fallback path).
48. Config change (`onMenuStopped(changing=true)` + start) → identical order in every combination.
49. Package installed while visible outside a window → the new app is appended, the others keep their order, and the persisted order is updated.
50. Edit the schedule or toggle the master switch while visible → the order is unchanged; the status line updates.
51. Pause set while due → keep. Clock passes the pause end → the next start shuffles, and the stale pause is cleared from the store.
52. The dock is never in the grid and never changes position, across all of the above.
53. App mode (not default home): every real stop/start inside a window shuffles; outside, it shows the saved order.

### 6.2 Compose UI tests (`app/src/androidTest/`, stateless screens, fixed clock)

1. Screen A renders the master switch, the pause card and the default "Every day · All day" row. Toggling the switch calls `onEnabledChange(false)`, and the list becomes disabled (`assertIsNotEnabled`).
2. "Add time" opens B. Tapping chips M, T, W, T, F toggles selection (assert `isSelected` and the content description "Monday, selected"). "Weekends" selects only Sat and Sun.
3. From 22:00, Until 02:00 → the summary shows "ends next day"; Save emits `ShuffleWindow(FRI, 1320, 120)`.
4. From == Until → Save is disabled and the message "Start and end can't be the same" is shown. No days → "Pick at least one day".
5. Edit an existing row → its values are prefilled; Delete → back on A, the row is gone, a snackbar shows "Time removed", and Undo restores it.
6. "Add time" is hidden at 6 windows.
7. Pause sheet with the clock fixed at 2026-10-13T14:00 Paris: three options with "until 16:00" and "until 00:00". Choosing 2 hours → the card shows "2 h left · until 16:00". Advance the fake clock 18 min → "1 h 42 min left". "Resume now" calls `onResume`.
8. Date option opens the DatePicker; past dates are not selectable; choosing 20 Oct → "Through Tue, 20 Oct". (Use the locale-formatted expected string from the same formatter.)
9. Status line: given `Keep(OUTSIDE_WINDOWS)` + nextChange 07:00 → "Order kept · shuffling resumes at 07:00"; tapping it opens A.
10. Font scale 2.0 and a 360 dp width: no clipped text in A–D (`assertIsDisplayed` on the last item after scroll); every touch target is at least 48 dp.
11. Opening and closing the editor from the grid does not change the grid order (`MenuContent` test with the real ViewModel and a fake catalog).

### 6.3 Emulator and Samsung manual tests

Setup (emulator API 26, 34 and 36; Samsung One UI device):
```
adb shell settings put global auto_time 0          # stop network time overriding our clock
adb shell settings put global auto_time_zone 0
adb shell cmd alarm set-timezone Europe/Paris      # sends TIMEZONE_CHANGED like the Settings app
adb shell cmd alarm set-time 1791921570000         # 2026-10-13 21:59:30 +02:00
adb shell date                                     # confirm
adb shell input keyevent KEYCODE_SLEEP             # screen off
adb shell input keyevent KEYCODE_WAKEUP
adb shell wm dismiss-keyguard                      # with no secure lock / swipe lock
adb shell input keyevent KEYCODE_HOME
adb shell am kill com.benisalla.glitch             # process death (while another app is in front)
adb reboot
```
More epoch values: 2026-10-13 06:59:30 Paris = `1791867570000`; 2026-10-16 23:59:30 = `1792187970000`; 2026-12-31 23:59:30 = `1798757970000`; 2026-10-25 01:58 CEST = `1792886280000`, 02:58 CEST = `1792889880000` (one minute before fall back); 2027-03-28 01:58 CET = `1806195480000`. Before using `cmd alarm` on a new build, check it with `adb shell cmd alarm help`. If a vendor build lacks it, use Settings → Date & time manually. Don't use `setprop persist.sys.timezone`: it needs root and sends no broadcast.

| # | Steps | Expected |
|---|---|---|
| M1 | Schedule Mon–Fri 07:00–22:00. Set 21:59:30 Tue, sleep, wait 20 s, wake + unlock (now 22:00:0x). | Order unchanged; status line "Order kept · shuffling resumes at 07:00". |
| M2 | Set 06:59:30 Wed, sleep, wake at 07:00:0x. | Reshuffled once; Home → same order. |
| M3 | Outside a window, sleep/wake twice, then set the clock into the window **while the phone is unlocked in another app**, press Home. | Reshuffles once on that Home (missed-while-locked), then stable. |
| M4 | Overnight Fri 22:00–02:00; set 2026-10-16 23:59:30, sleep, wake after midnight. | Reshuffled (Sat 00:00 is still in the Friday window). |
| M5 | Travel pause 2 h, sleep/wake ×3. Then `set-time` +2 h 1 min, sleep/wake. | Order kept 3 times; then reshuffled and the card returns to "Pause". |
| M6 | Pause "Until a date" tomorrow; `set-timezone America/New_York`, sleep/wake. | Still paused (date judged in New York). |
| M7 | Inside a window in Paris at 07:30 → `set-timezone America/New_York` (01:30) → sleep/wake. | Order kept (now outside). Editor footer shows the New York time. |
| M8 | Fall back: Paris 02:58 CEST Sun 2026-10-25, window Sun 02:00–03:00; sleep/wake every few minutes through 03:00 → 02:00 CET → 03:00 CET. | Shuffles inside both 02:xx passes, keeps after 03:00 CET. |
| M9 | Spring forward: Paris 2027-03-28 01:58 CET, window Sun 01:30–02:30; sleep/wake at 01:59 and after the jump (03:00 CEST). | Shuffle at 01:59, keep at 03:00. |
| M10 | Outside a window, note the order, `adb reboot`, unlock. | **Identical** order (persisted), dock unchanged. |
| M11 | Inside a window, `adb reboot`. | Reshuffled on first show. |
| M12 | Master off, sleep/wake ×3 inside a window. | Never reshuffles; turning it on doesn't reshuffle until the next sleep/wake. |
| M13 | Memory pressure: open about 10 heavy apps, return Home, lock from the home screen, wait 5 min, unlock. | Next unlock reshuffles (screen-off receiver or the onStop fallback). Compare the screen-off receipt time with `onStart` in `adb logcat`. |
| M14 | Samsung: Settings → Battery → Background usage limits, with Glitch as default home. Lock 30 min, then unlock. | Reshuffled if inside a window; no "app is not responding"; process alive or cold-started cleanly. Repeat with "Put unused apps to sleep" on. |
| M15 | App mode (Samsung One UI Home as default): open Glitch outside a window, then inside. | Saved order outside, shuffled inside. |
| M16 | `adb shell dumpsys package com.benisalla.glitch \| grep -i permission` and check the merged manifest. | Still no `uses-permission` beyond the AndroidX dynamic-receiver one; no new receivers declared. |
| M17 | TalkBack on: walk through screens A–D. | Every control is announced with a role and state; chips announce full day names. |

---

## 7. Effort, risks, open questions

### 7.1 Effort (one engineer, after the launcher PR is merged)

| Part | Estimate |
|---|---|
| Model, policy, codec, `restore` + unit tests 1–42 | 1.5 days |
| Store, ViewModel integration, onStop fallback + tests 43–53 | 1.5 days |
| Editor A–D, status line, strings + Compose tests | 2–2.5 days |
| Manual matrix M1–M17 (emulator + Samsung), fixes | 1 day |
| AGENTS.md / README / ROADMAP / store copy updates | 0.5 day |
| **Total** | **≈ 6.5–7 days** |

### 7.2 Risks

| Risk | Mitigation |
|---|---|
| Late or merged screen-off delivery on Android 14+ (F5) → a missed shuffle | The `onStop` + `isInteractive` fallback, cold-start handling, and the due flag that carries over. We accept one missed shuffle at worst and never reshuffle a visible grid. |
| Process killed (Samsung memory management, reboot) | The order, due flag and schedule are persisted; a cold start restores outside windows. |
| A shuffle on the first Home return after a window opens, while the phone is in use, feels surprising | It's the owner's requested rule. It happens once, before the grid is drawn. Alternative in Q3. |
| Users don't understand why there was no shuffle | The status line under the title, with "resumes at …". |
| Scope: AGENTS.md lists "timers" as a non-goal | This is a user-set *permission window* with no timer, no background work and no blocking. Owner sign-off and an AGENTS.md wording update are still needed. |
| Persisting the order adds stored data | Package names only, app-private, `allowBackup=false`, no permission; Data safety unchanged ("not collected" because nothing leaves the device). |
| DST wall-clock semantics give at most one extra or one missing eligible hour twice a year | Documented and tested (13, M8, M9). |
| Users change the clock to force or avoid a shuffle | Allowed. Glitch is not a lock. |

### 7.3 Open questions for the owner

1. **Default:** ship with *On, Every day, All day* (no behaviour change on upgrade, **recommended**), or suggest a window such as *Mon–Fri 07:00–22:00* the first time the editor opens? A suggested window silently changes v1.1 behaviour for people who never open the editor.
2. **Master switch meaning:** "Shuffle on unlock: off = never reshuffle" (**recommended**: one switch for "stop", windows for "when", pause for "for now"), or "Use a schedule: off = shuffle on every unlock"?
3. **Carry-over:** apply a missed shuffle on the next eligible *unlock or Home return* (as asked), or only on the next *unlock*? The first catches up sooner. The second never reshuffles while the phone is in use.
4. **"Until tomorrow":** ends at local midnight (through today, **proposed**; simple, but only 30 min if chosen at 23:30), or at a fixed morning hour such as 06:00 tomorrow?
5. **App mode:** should the schedule also govern opening Glitch as a normal app (**proposed: yes**, one consistent rule), or only the home screen?
6. **Cold start after process death:** v1.1 reshuffles on *any* cold start. Since the order is now persisted, we could reshuffle only after a **reboot** (detected with `Settings.Global.BOOT_COUNT`, no permission) or when due, so a background kill while the phone is in use no longer reshuffles. Change the v1.1 rule (**recommended**), or keep it?
7. **Limits:** max 6 windows, and date pause up to 365 days. OK?
8. **Placement:** a ⋮ on the title row shared with "Change home app". OK, or does the launcher PR already have a settings entry to reuse?

## Owner decisions (2026-10-13)

1. Default schedule: shuffle all day, every day. Nothing changes on upgrade until the user adds windows.
2. "Until tomorrow" ends at midnight.
3. A background process kill while the phone is in use does not reshuffle. A cold start reshuffles only after a reboot, or when a shuffle is due (screen-off / unlock).
4. A missed shuffle is applied at the next unlock or Home return once shuffling is allowed again.
5. Remaining open questions take the recommended answers: master switch off means never reshuffle; the schedule also applies in normal-app mode; at most 6 windows and date pauses up to 365 days; the ⋮ menu is shared with "Change home app".
