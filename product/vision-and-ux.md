# Glitch: product vision and UX

Prepared October 2026 by the product and UX design role. **This is a proposal.** When this was written, `AGENTS.md` listed prompts, timers, blockers and usage tracking as non-goals, and it banned every permission. Nothing in this document is in scope until the owner approves it. Each phase below also needs `AGENTS.md`, the README and the privacy policy updated in the same change.

Related docs: `store/marketing/research.md` (what the evidence does and does not support) and `store/marketing/copy-notes.md` (brand voice). A research scientist and an Android feasibility engineer are working in parallel; where this doc says "needs feasibility check", their findings decide.

---

## 1. Vision

### Vision statement

> Your phone is designed to be tapped without thinking. Glitch is a small, kind interruption in that loop: your apps appear in a new order, so you look, then choose. It is designed using research on attention, habits and ADHD-friendly design, for people with ADHD and anyone whose attention wanders quickly. That means no lectures, no streaks and no lockouts, only gentle cues you set yourself and can switch off in one tap. Glitch gives you a moment to notice, and what you do with it is your call.

### Honest promise line (README and store)

> Glitch is just getting started. Today it shuffles your apps so you look before you tap. Next, we're adding small, optional cues that you control, designed using research on attention and habits (README only, never the store listing). It will never lecture you, lock you out or send your data anywhere.

Wording rules (from `research.md` and Play policy):
- Fine (outside the store listing): "designed using research on attention, habits and ADHD-friendly design", "a moment to choose", "cues you write yourself", "you stay in control".
- Never: "treats / fixes / helps ADHD", "improves focus or attention", "reduces screen time", "clinically proven", "beats phone addiction", or any study number next to the app's name.

---

## 2. What users actually struggle with

The research below is mostly community talk and secondary reporting, not trials. It tells us about **pain and design preferences**. It does **not** show that any feature works. Sources are listed in section 10.

| # | Finding | Source quality | What it means for design |
|---|---|---|---|
| P1 | "Just checking" turns into a long scroll. People with ADHD say phone time "vanishes in an instant" while chores drag. Coaches recommend external, visible time cues because the internal sense of time is unreliable ("time blindness"). | Coaching and clinic blogs [S8, S9, S10] | Make time visible and kind, at the right moment, without needing a timer the user has to remember to start. |
| P2 | Blockers get bypassed: force-stop, uninstall, or tapping "ignore limit" within seconds. One r/ADHD-style comment: "all the lock apps work for me for only so long (I have adhd)." | Forum threads and dev posts [S2, S3, S4] | Do not compete on how strong the lock is; we lose that arms race. Compete on being worth keeping. |
| P3 | Rigid timers break hyperfocus. A reported CHI 2026 study says fixed Pomodoro blocks cut off hard-won focus, people switched blockers off to watch calming content on purpose ("digital stimming"), and several said the apps left them feeling *more* ashamed. | Secondary reports only; primary paper not verified [S1, S5] | No fixed schedules forced on people. Allow intentional, guilt-free scrolling. Shame is a failure mode, not a feature. |
| P4 | Broken streaks, red badges and "you failed" summaries can trigger rejection sensitivity, and lead to shame, avoidance and abandonment of the app. | Developer and designer opinion, not studies [S6, S7] | No streaks, no red, no scores. A missed day is just a day. |
| P5 | Restriction causes reactance and abandonment. Lockout-style tools caused more frustration [research.md #14], a friction bundle raised stress [#13], and people paused friction tools and fell back quickly [#8]. Most people drop health and wellbeing apps within 100 days (median 70%) [S11]. | Peer-reviewed (#8, #13, #14, S11); reactance thesis [S12] | Gentle defaults, easy pause, and value in under 10 seconds. Assume people will leave, and make coming back feel welcome. |
| P6 | What people say they love: removing visual triggers ("out of sight, out of mind"), grayscale, minimal home screens, one-tap actions, and pauses that let you through slowly once you've noticed. | Store listings and forum anecdotes [S4, S13]; grayscale studies [research.md #10, #11] | Calm visuals, minimal setup, and a moment to notice, not a wall. |
| P7 | Detecting Reels or Shorts specifically is possible on Android only through an AccessibilityService that reads screen structure. It is fragile across app updates, and Google Play restricts it (prominent disclosure and consent, and not an "accessibility tool" exemption). | Open-source apps and Play policy [S14, S15, S16] | The north-star feature is real but costly in trust, policy and maintenance. Plan it as a separate, clearly opt-in step (section 7). |

**Design takeaway:** the people we're designing for don't need another warden. They need a small, kind pause at the exact moment autopilot takes over, written in their own words, and easy to ignore.

---

## 3. Personas

### Persona A: Samira, 29, adult with ADHD (diagnosed at 26), UX copywriter

- **Phone:** Samsung Galaxy, too many apps, notifications mostly off already.
- **Goal:** "I want my phone to be a tool again, not a slot machine."
- **Moments of failure:**
  - Unlocks to check the time before a 10:00 call. Instagram is in the bottom-right slot and her thumb gets there first. At 10:04 she's late, and she never did see what time it was.
  - Opens Maps to leave for the dentist, sees a Reels notification badge on the way, and loses 25 minutes. She's late again and feels the familiar shame spiral.
  - Has installed and deleted four blockers. "The first week is great, then I tap 'ignore for today' every single time, and then I just feel bad when I see the icon."
- **What she needs from Glitch:** a pause that feels like her own voice, not a teacher's; no streaks; zero setup on bad days.
- **What would make her uninstall:** being nagged when she *chose* to rest with videos, or any red "you failed" screen.

### Persona B: Jonas, 20, second-year engineering student who loses focus fast

- **Phone:** mid-range Android, YouTube, Discord, TikTok, a calculator app and the uni app.
- **Goal:** get through 45 minutes of problem sets without "just one video".
- **Moments of failure:**
  - Picks up the phone to use the calculator during homework. TikTok sits next to it in the dock. Forty minutes later the problem set is still open on the desk.
  - Opens YouTube for a lecture recording; the Shorts shelf is right under it. He watches 30 Shorts and doesn't open the lecture.
  - Tried a Pomodoro app; the 25-minute alarm went off right as a proof finally clicked, so he turned the app off for good.
- **What he needs from Glitch:** a "study set" with only the 4 apps he needs for homework, one tap to start, and no timer that cuts him off.
- **What would make him uninstall:** setup that takes longer than 1 minute, or anything that feels like parental control.

### Persona C: Elena, 41, product manager who doom-scrolls

- **Phone:** work and personal on one device, news app, LinkedIn, Reddit, email.
- **Goal:** stop the 23:00–00:30 scroll and the "inbox → news → Reddit" loop between meetings.
- **Moments of failure:**
  - Opens the phone to reply to her partner's message, sees the news app, and reads 20 minutes of bad headlines. She forgets the reply.
  - In bed at 23:10: "five minutes of Reddit". It's 00:40 when she next looks at the time.
  - Screen Time's weekly report ("+18% this week") makes her feel judged, so she turned it off.
- **What she needs from Glitch:** a calm late-evening mode, a kind nudge about time that doesn't count or grade her, and the option to say "yes, I want this" without being argued with.
- **What would make her uninstall:** reports and percentages, and any feeling of being watched.

---

## 4. Product principles

1. **Shuffle, look, then tap.** The shuffle stays the core of every screen. Every feature builds on that one moment of looking. We don't become a blocker, a launcher or a tracker.
2. **Non-judgmental.** No streaks, scores, red states, "you failed", or percentages that go up when you've done "worse". We describe, we don't grade. Copy is warm, plain and short.
3. **The user's voice, not ours.** The strongest prompts are ones the user wrote themselves. Glitch offers starter lines but encourages "write your own".
4. **Optional and off by default.** Glitch v1.0 behavior stays the default experience. Every feature is opt-in, and everything can be switched off in one place.
5. **Always an exit.** Every interruption has an honest "Open anyway" or "5 more minutes" that works on the first tap. Glitch never locks, hides by force, or argues back. (P2, P3, P5)
6. **Reversible.** Undo for every setting change, "pause everything for today" in one tap, and no penalty for pausing or coming back after weeks.
7. **Private by default.** On the device only. No accounts, no network, no analytics. If a phase needs a permission, it's requested only when the user turns that feature on, with a plain-language reason, and the core app stays usable without it.
8. **Low setup, quick win.** Useful in under 30 seconds: mark one app, done. No onboarding questionnaire. Defaults are chosen so that skipping setup is fine.
9. **Calm visuals.** Navy, white, and one gold accent. Less colour on purpose for "noisy" apps. No flashing, no countdown pressure, and reduced motion is respected.
10. **Honest claims.** We say what it does, never what it does to your brain (see section 1).

---

## 5. Feature concepts

Legend for "Needs from the phone":
- **None**: works inside Glitch, within today's rule of no permissions (only local storage).
- **Runtime permission**: a normal Android prompt, e.g. `POST_NOTIFICATIONS`.
- **Special access**: the user has to enable it in Settings, e.g. usage access or accessibility. These have high trust and policy cost.

A shared building block across concepts: **"Mark as noisy"**. The user long-presses a tile and taps "Mark as noisy" (Instagram, TikTok, news). Marked apps are the only ones that ever get extra friction or cues. Unmarked apps behave exactly as in v1.0. Wording: "noisy", never "bad" or "addictive".

### Concept 1: Anchor apps (fixed spots for the urgent few)

- **User story:** As Samira, when I need the Phone, Maps or Authenticator app *right now*, I want them in a fixed place, so the shuffle never gets in the way in an urgent moment, and everything else still shuffles.
- **Flow:**
  1. Long-press a tile and tap "Make anchor" (max 4).
  2. Anchors sit in a small fixed row at the top and never shuffle.
  3. The rest of the grid shuffles as today.
  4. Long-press an anchor and tap "Remove anchor" to undo.
- **Wireframe:**
  ```
  ┌──────────────────────────────┐
  │ Glitch                    ⋮  │
  │ ANCHORS                      │
  │ [Phone] [Maps] [Auth]        │
  │ ──────────────────────────── │
  │ [Drive] [YouTube] [Clock] [▒]│
  │ [Mail]  [Photos]  [Chrome][…]│
  │ [Camera][Insta▒]  [Files] […]│
  └──────────────────────────────┘
  ```
- **Avoids shame and abandonment:** the shuffle's real cost is slower search (research.md #5, #6), and most people prefer a fixed order. Anchors take the frustration out of urgent moments, which is the most likely reason someone would uninstall a shuffled menu. The cap of 4 keeps the shuffle meaningful.
- **Needs:** None. Anchors are stored locally. Keep the session rules: anchors are excluded from shuffle, and the rest follows existing session semantics.

### Concept 2: Calm tiles (grayscale for noisy apps)

- **User story:** As Elena, I want the apps that pull me in to look quiet and gray in Glitch, so they don't grab my eye while I look for Messages.
- **Flow:**
  1. Mark an app as noisy (long-press).
  2. Its tile turns grayscale and slightly smaller-contrast, and the label stays fully readable.
  3. It still shuffles and still opens on tap.
  4. A "Calm tiles" toggle in settings turns grayscale off for all apps at once.
- **Wireframe:**
  ```
  ┌──────────────────────────────┐
  │ Glitch                    ⋮  │
  │                              │
  │ [Maps]   (░TikTok░) [Mail]   │
  │ [Notes]  [Camera]  (░News░)  │
  │ (░Reddit░) [Phone] [Clock]   │
  │                              │
  │  (░ … ░) = shown in grayscale │
  └──────────────────────────────┘
  ```
- **Avoids shame and abandonment:** this is a visual change only, with no message and no block. It is the gentlest possible signal. Grayscale is a well-liked, low-effort strategy (P6), and here it is scoped to chosen apps instead of the whole phone. We do not claim it reduces use (research.md #11 found unlocks didn't change).
- **Needs:** None. A Compose color filter on the icon.

### Concept 3: Intention moment (a tiny pause for noisy apps)

- **User story:** As Samira, when I tap a noisy app in Glitch, I want a 2-second moment that shows a line *I wrote*, and lets me open the app or go back, so I notice whether I meant to.
- **Flow:**
  1. Tap a noisy app in Glitch.
  2. A gold card slides in with one of the user's own reminders and a soft 2-second fill bar. The buttons are active immediately; the bar is only visual.
  3. "Open TikTok" (always available) or "Not now" (returns to a freshly shuffled menu).
  4. Optional: one-word intention chips ("reply", "post", "rest") that are shown back in a later time cue (Concept 6). These are never stored beyond the day.
- **Wireframe:**
  ```
  ┌──────────────────────────────┐
  │ ░░░░░░ menu dimmed ░░░░░░░░  │
  │   ┌──────────────────────┐   │
  │   │ ◇ glitch             │   │
  │   │                      │   │
  │   │ "Is this the thing   │   │
  │   │  you picked up the   │   │
  │   │  phone for?"         │   │
  │   │          - you, Mon  │   │
  │   │ ▓▓▓▓▓▓▓░░░░░         │   │
  │   │ for: (reply)(rest)(…)│   │
  │   │ [ Not now ] [Open ▸] │   │
  │   └──────────────────────┘   │
  └──────────────────────────────┘
  ```
- **Avoids shame and abandonment:** "Open" is never disabled and "rest" is a valid intention, so watching videos on purpose is respected (P3). The reminder is the user's own words. "Not now" gives a small win without a scoreboard. In the one sec research, the explicit "dismiss/don't open" choice had the strongest effect, while a message alone did not (research.md #7). This concept adds that choice, but we still claim nothing.
- **Needs:** None, **but it only applies to apps opened through Glitch**. Opening TikTok from the home screen skips it. That's honest and in character: Glitch is the place you come to choose. Copy must say so plainly.
- **Builds the reminder library:** this is where users first write reminders. The same library powers Concepts 6, 9 and the Scroll Glitch (section 7).

### Concept 4: Focus sets (show only what this moment needs)

- **User story:** As Jonas, I want a "Study" set that shows only Calculator, Uni, Notes and Files, still shuffled, so the menu itself doesn't offer me TikTok.
- **Flow:**
  1. Tap "⋮" and choose "New set". Name it and pick apps (search plus checkboxes). Max 3 sets.
  2. A row of chips at the top switches between them: `All · Study · Evening`.
  3. In a set, only those apps show, shuffled. A quiet "Show all apps" link sits at the bottom.
  4. Leaving Glitch keeps the set for next time until the user switches back. No timer.
- **Wireframe:**
  ```
  ┌──────────────────────────────┐
  │ Glitch                    ⋮  │
  │ ( All )( ●Study )( Evening ) │
  │                              │
  │    [Notes]      [Calc]       │
  │                              │
  │    [Files]      [UniApp]     │
  │                              │
  │                              │
  │      show all apps  ›        │
  └──────────────────────────────┘
  ```
- **Avoids shame and abandonment:** the other apps aren't blocked, they're just out of sight (P6: "out of sight, out of mind"), and "Show all apps" is one tap away. There's no timer to break hyperfocus (P3). Sets can be emptied or deleted any time.
- **Needs:** None.

### Concept 5: One-tap focus (widget and Quick Settings tile)

- **User story:** As Jonas, I want one tap from my home screen to open Glitch already in my Study set, so starting is easier than finding the setting.
- **Flow:**
  1. Add the 1×1 "Glitch: Study" widget (or Quick Settings tile).
  2. Tap it. Glitch opens straight into that set, shuffled.
  3. A subtle gold dot on the widget shows which set is active. Tap and hold to switch.
- **Wireframe:**
  ```
  Home screen:           Quick Settings:
  ┌────────┐             ┌──────────────┐
  │ ◇ ◆ ◇  │             │ ◆ Glitch     │
  │ ◇ ◇ ◇  │             │   Study      │
  │ Study ●│             └──────────────┘
  └────────┘
  ```
- **Avoids shame and abandonment:** it lowers the starting cost, which is the hardest part for many people with ADHD. It has no "session failed" state because there is no session to fail.
- **Needs:** None (app widget or `TileService`; feasibility engineer to confirm). Remember that `AGENTS.md` bans a HOME intent filter, and this is not one.

### Concept 6: Kind time cues

- **User story:** As Elena, when I open a noisy app from Glitch, I want an optional, gentle "it's been 20 minutes" nudge later, phrased like a friend, so I notice the time without being told off.
- **Flow (two levels):**
  1. **In-app (no permission):** when Glitch opens, a single line under the heading: "Back at 23:42. You last came through here at 23:10." Facts only, no judgment.
  2. **Notification (opt-in):** when a noisy app is launched from Glitch, the user can choose "check in after 10 / 20 / 30 min". Glitch schedules **one** local notification for then.
  3. The notification shows the time plus their intention chip if set: "23:31 · You came for: reply to Sam." Actions: "Thanks" (dismiss) and "Back to Glitch".
  4. No repeat, no escalation. One cue per launch, at most.
- **Wireframe:**
  ```
  In Glitch:
  ┌──────────────────────────────┐
  │ Glitch                    ⋮  │
  │ back at 23:42 · last here    │
  │ 23:10                        │
  │ [ shuffled grid … ]          │

  Notification:
  ┌──────────────────────────────┐
  │ ◆ Glitch · now               │
  │ 23:31. You came for: reply   │
  │ to Sam.                      │
  │ [Thanks]   [Back to Glitch]  │
  └──────────────────────────────┘
  ```
- **Avoids shame and abandonment:** it shows the time of day, not a duration total, a score or a comparison, so there's no "+18%" moment. It's one cue per choice, the user sets the interval, and "Thanks" is the only response needed. It makes time visible, which is what coaches recommend for time blindness (P1).
- **Needs:** The in-app line needs none (the last-open time is stored locally and overwritten each time). The notification needs **`POST_NOTIFICATIONS` on Android 13+**, a runtime permission, so v1.2. It does not know whether the user is still in the app; it's just a scheduled cue. Knowing that would need usage access (v2.0).

### Concept 7: Evening wind-down set

- **User story:** As Elena, I want Glitch to switch to my "Evening" set after 22:30, with noisy apps gray and a softer palette, so bedtime scrolling has one more moment to choose.
- **Flow:**
  1. Settings, then "Evening". Pick a start time and a set (or "All apps, noisy ones calm").
  2. After that time, opening Glitch shows the darker navy "evening" palette and the chosen set.
  3. A small chip reads "Evening · until 06:00 · switch off tonight".
- **Wireframe:**
  ```
  ┌──────────────────────────────┐
  │ Glitch           ☾ Evening   │
  │ until 06:00 · off tonight ›  │
  │                              │
  │  [Books]   [Alarm]  [Notes]  │
  │  [Phone]   (░Reddit░)        │
  └──────────────────────────────┘
  ```
- **Avoids shame and abandonment:** "Off tonight" is a single tap with no reason asked for. It's time-based but not a timer: nothing is cut off.
- **Needs:** None. The check runs when Glitch opens; no alarms or background work.

### Concept 8: End-of-day "one good moment" (reflection)

- **User story:** As Samira, at the end of the day I want to see one kind, true sentence about when I chose, not a report of when I "failed".
- **Flow:**
  1. Opt in. Optionally pick a time for a reminder (that part needs a notification).
  2. Opening Glitch after that time shows one card: "Today you chose 'Not now' 3 times. Nice noticing." If the count is 0: "Today was today. Tomorrow's menu is already shuffled."
  3. One optional tap: "Write a reminder for tomorrow" (this feeds the library).
  4. The card disappears. Data from the day is erased at midnight.
- **Wireframe:**
  ```
  ┌──────────────────────────────┐
  │   ┌──────────────────────┐   │
  │   │ ◆ today              │   │
  │   │ You chose "Not now"  │   │
  │   │ 3 times. Nice        │   │
  │   │ noticing.            │   │
  │   │ [ + reminder for     │   │
  │   │   tomorrow ]  [Done] │   │
  │   └──────────────────────┘   │
  └──────────────────────────────┘
  ```
- **Avoids shame and abandonment:** it only counts positive choices made inside Glitch, never time spent. There's no history, streak or chart, and a zero day gets a warm line. Turning it off is permanent and silent.
- **Needs:** None for the in-app card. **This is a small local counter, which `AGENTS.md` currently calls "usage tracking". It needs explicit owner approval** even though it never leaves the device and resets daily. The reminder notification needs `POST_NOTIFICATIONS` (v1.2).

### Concept 9: Reminder library (the user's own words)

- **User story:** As any persona, I want to keep a short list of reminders in my own words, so whatever Glitch shows me actually lands.
- **Flow:**
  1. Settings, then "My reminders". It starts with 3 starter lines and a big "Write your own".
  2. Each reminder can be tagged with any time (morning / day / evening) and, later, any context (intention, time cue, reels).
  3. Long-press a reminder for "Retire" (it goes to a "Rested" list and is never deleted by surprise).
  4. A heart after it's shown ("this one helped") makes it come up slightly more often.
- **Wireframe:**
  ```
  ┌──────────────────────────────┐
  │ ‹ My reminders               │
  │ ┌──────────────────────────┐ │
  │ │ "Future you wanted to    │ │
  │ │  sleep before midnight." │ │
  │ │  evening · ♥ 4           │ │
  │ └──────────────────────────┘ │
  │ ┌──────────────────────────┐ │
  │ │ "Text Mum back first."   │ │
  │ └──────────────────────────┘ │
  │ [ + write your own ]         │
  │ rested (2) ›                 │
  └──────────────────────────────┘
  ```
- **Avoids shame and abandonment:** the user writes the content, so the tone is theirs. Starter lines are curious questions, not commands ("What did you open the phone for?"). There are no counts of how often they "ignored" a reminder.
- **Needs:** None.

---

## 6. Phased roadmap

Every phase needs owner approval and an `AGENTS.md` update (non-goals, privacy section) before work starts.

### v1.1: "Choose with less friction" (no new permissions)
**Why now:** these features make the shuffle more pleasant to live with, and add the first gentle cue, without touching the "Permissions: none" promise that is our strongest trust signal at launch.

- Anchor apps (Concept 1)
- Mark as noisy plus calm tiles (Concept 2)
- Intention moment for noisy apps opened via Glitch (Concept 3)
- Reminder library (Concept 9), so users start writing reminders now
- Focus sets (Concept 4) and the evening set (Concept 7)
- One-tap widget and Quick Settings tile (Concept 5), subject to the feasibility check
- In-app time line "back at … · last here …" (Concept 6, first level)

### v1.2: "Kind cues" (one optional runtime permission: `POST_NOTIFICATIONS`)
**Why now:** once people trust Glitch and have their own reminders, a single, opt-in scheduled notification makes time visible after they leave the menu. This is the part Glitch can't reach today.

- Kind time cues as notifications, one per launch (Concept 6, second level)
- End-of-day "one good moment" card and optional reminder (Concept 8; owner decision on the local counter)
- Permission asked only when the user turns one of these on. Every v1.1 feature keeps working with it denied.

### v2.0: "The Scroll Glitch" (special access, separate opt-in; future vision)
**Why now:** users will have a library of reminders that work for them and a reason to trust us, and that's the only foundation strong enough for a feature that watches for one specific moment.

- **Headline: Scroll Glitch** (section 7)
- Optional usage-access time cues that work even when the app was opened from the home screen (needs `PACKAGE_USAGE_STATS` special access; feasibility check)

---

## 7. North star (future vision): the Scroll Glitch

> **Status: future vision, not planned for any release yet.** It needs owner approval, a rewrite of the `AGENTS.md` privacy rules, a Google Play policy review, and the Android feasibility engineer's findings. Nothing here is a commitment.

### The idea
When you've been swiping through short-form video (Instagram Reels, YouTube Shorts, TikTok) for a while, Glitch notices and interrupts the feed with **a reminder you wrote yourself**, the one most likely to wake you up and get you out of the reel. It's one deliberate glitch in an endless feed.

### How today's Glitch leads here

```
 v1.0 SHUFFLE          v1.1 INTENTION          v1.2 TIME CUES          v2.0 REELS GLITCH
 ───────────────       ────────────────        ───────────────         ─────────────────
 New order each   →    Your own words     →    A kind cue after   →    Your best words,
 open: look, then      before a noisy          you leave Glitch:        at the moment the
 tap.                  app opens.              "it's 23:31".            feed takes over.

 teaches: notice       builds: reminder        builds: trust in         uses all three:
 the moment            library                 gentle interruptions     notice + your words
                                                                        + right timing
```

Each step reuses the one before it: the habit of looking (v1.0), the reminder library (v1.1), and proof that Glitch's interruptions are kind and rare (v1.2). The Scroll Glitch only works if all three are already true.

### Setup (under 1 minute, all optional)

1. **Choose where:** a list of supported apps, each with a toggle: Instagram Reels, YouTube Shorts, TikTok. Only short-form screens are watched, so the rest of Instagram or YouTube (DMs, a lecture video) is never interrupted.
2. **Choose when:** "Glitch me after [5 / 10 / 15 / 20] minutes of continuous scrolling." Default 10. Optional quiet windows ("not on Saturdays", "not 19:00–20:00, that's my rest").
3. **Your reminders:** pick from your library or write new ones. Glitch suggests writing at least 3 so they can rotate. Prompt: "What would future-you say to pull you out of a reel?" Examples (starters only): "You wanted to call Grandma today.", "The essay is due Friday. Ten minutes on it feels better than this.", "You said you'd be asleep by 00:00."
4. **Permission, explained honestly** (Play requires prominent in-app disclosure before the system screen):
   ```
   ┌──────────────────────────────┐
   │ To notice reels, Glitch needs│
   │ Android's Accessibility      │
   │ access.                      │
   │                              │
   │ • It only checks whether the │
   │   apps you picked are showing│
   │   a short-video screen.      │
   │ • It never reads your        │
   │   messages, typing or        │
   │   passwords.                 │
   │ • Nothing leaves your phone. │
   │   Glitch has no internet.    │
   │ • Turn it off any time here  │
   │   or in Android Settings.    │
   │                              │
   │ [ Not now ]  [ Continue ▸ ]  │
   └──────────────────────────────┘
   ```

### The interruption: a glitch in the feed

The feed freezes behind a navy overlay, and a short "glitch" animation runs (tiles slip out of line, the gold tile tilts in, under 400 ms, skipped if reduce motion is on). Then the user's reminder appears.

```
┌──────────────────────────────┐
│▒▒▒▒▒▒▒ reel, paused & dim ▒▒▒│
│▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒│
│ ┌──────────────────────────┐ │
│ │  □ □ □                   │ │
│ │  □ ◆ □   glitch          │ │  ← brand mark: tilted gold tile
│ │  □ □ □                   │ │
│ │                          │ │
│ │  "You said you'd be      │ │
│ │   asleep by midnight.    │ │  ← the user's own words, large
│ │   It's 23:48."           │ │
│ │              - you, Sun  │ │
│ │                          │ │
│ │  12 min of reels         │ │  ← plain fact, no judgment
│ │                          │ │
│ │ ┌──────────────────────┐ │ │
│ │ │    Close the app     │ │ │  ← primary, gold
│ │ └──────────────────────┘ │ │
│ │      5 more minutes      │ │  ← honest escape hatch, text button
│ │                          │ │
│ │  ♥ this one helped       │ │
│ └──────────────────────────┘ │
│ Glitch off for today ›       │  ← always visible, one tap
└──────────────────────────────┘
```

**Options:**
- **Close the app** (primary, gold): leaves the short-video app and opens Glitch with a fresh shuffle, so the user lands somewhere calm with choices, not on a blank home screen. A tiny "Nice." toast appears once, never counted.
- **5 more minutes** (text button, equal reachability, no confirmation, no "are you sure?"). Glitch stays silent for exactly 5 minutes, then shows one more glitch at most. The second time, the copy acknowledges the choice: "5 minutes are up. Still want to be here? That's okay too." with the options "Close the app" and "I'm resting, leave me be" (silent until the user leaves the app).
- **Glitch off for today:** one tap, no reason asked, and it resumes tomorrow.

### How reminders rotate so they don't go stale

- **Shuffle bag, Glitch's soul:** reminders are drawn from a shuffled deck with no repeats until every eligible reminder has been shown once. Then the deck reshuffles, and the last-shown card is never first in the next deck.
- **Context fit:** only reminders tagged for the current time of day (or untagged) are eligible. "Asleep by midnight" appears at night, not at 09:00.
- **Learning by hearts, not by tracking:** "♥ this one helped" gives a reminder a second card in the deck (max 2), and that's the only weighting. No hidden scoring.
- **Gentle retirement:** a reminder shown 10 times without a heart gets a quiet prompt in the library (never during an interruption): "This one's been around a while. Keep, edit or rest it?"
- **Fresh words:** at most once every 2 weeks, the end-of-day card can offer "Write a new reminder?" It can be skipped forever.
- **Variety without content from us:** the same words can render in 3 layouts (big quote, typewriter reveal, a tilted-tile frame), so the interruption looks different even if the user has only 3 reminders.

### How it avoids being annoying or uninstalled

| Risk | Design answer |
|---|---|
| Interrupts rest the user chose (P3) | Quiet windows, "I'm resting, leave me be", and "Off for today". Rest is a valid answer, never a failure. |
| Nags (P2, P5) | Default threshold of 10 minutes of continuous scrolling, hard cap of 3 glitches per hour, never more than 1 within 5 minutes, and no escalation in tone or volume. |
| Shame (P4) | No totals, history, streaks or charts. The only number shown is "12 min of reels" in this session, and it's a fact, not a grade. |
| False alarms (P7: detection is fragile) | Only fires after a sustained signal; if detection fails, it fails silent (no glitch) rather than loud. Per-app toggles so a broken app can be switched off. |
| Feels like surveillance | Only the selected apps are checked; no screen text is read or stored; no network permission in the app at all; a plain "what Glitch sees" page. |
| Arms race with bypass | We don't fight it. Disabling is one tap and easy to find. The bet is that kind, self-written interruptions are worth keeping, not hard to remove. |
| Glitch's "no permissions" identity | Ship the Scroll Glitch as a **separate opt-in add-on** (e.g. a companion app "Glitch Guard", or a separate build), so the core Glitch keeps its "Permissions: none" badge. Owner and engineer decide; this conflicts with "no extra modules" and needs an explicit exception. |

### What it needs from the phone (pending feasibility check)

- **An AccessibilityService** that watches window and view-structure changes only in the selected apps. This is how open-source Shorts and Reels blockers work [S14, S15].
- **Google Play:** this is not an `isAccessibilityTool` use case, so it needs prominent in-app disclosure and consent and the Play accessibility declaration, and it's at risk of review rejection [S16]. The engineer should confirm the current policy text.
- **Maintenance:** detection relies on other apps' UI structure and will break when Instagram, YouTube or TikTok update. Budget for ongoing fixes, and design to fail silent.
- **Conflicts with today's `AGENTS.md`:** it bans accessibility and requires no permissions. That needs a deliberate owner decision, ideally kept out of the core app.

---

## 8. Metrics we can honestly use (without analytics)

Glitch has no analytics and won't add them. To learn whether this works we rely on:
- Closed-test interviews and a short opt-in feedback form (e.g. via email, written by the user).
- Play Console's built-in aggregate stats (installs, uninstalls, ratings), which Google collects anyway.
- Questions for testers: "Did anything make you feel judged?", "Which reminder worked and why?", "When did you turn something off?"

---

## 9. Open questions for the owner

1. Approve dropping "prompts" and "timers" from the non-goals list for the v1.1 intention moment and evening set?
2. Is a local, daily-reset counter of "Not now" taps acceptable (Concept 8), or is it "usage tracking"?
3. v1.2: accept one optional runtime permission (`POST_NOTIFICATIONS`) and update the "Permissions: none" badge to "No permissions required"?
4. Scroll Glitch: separate companion app, separate build flavor, or inside Glitch? (It conflicts with "no extra modules" and "no accessibility".)
5. New dependency for storage (DataStore) or plain `SharedPreferences`? The engineer should advise.

---

## 10. Sources

User-pain sources (community and secondary; they show what people say, not what works):
- [S1] "A slower internet", The Mind Wanders (June 2026), summarising a reported CHI 2026 study on ADHD and focus apps (Pomodoro breaking hyperfocus, "digital stimming", increased shame). Primary paper not verified: https://themindwanders.com/news-20260611/
- [S2] Tildes thread, "Looking for an effective self block application for Android" (bypass by force-stop or uninstall): https://tildes.net/~tech/1vl1/looking_for_an_effective_self_block_application_for_android
- [S3] r/ADHD thread, "How to stop screen addiction" (mirror): https://lr.us.psf.lt/r/ADHD/comments/1k7eok7/how_to_stop_screen_addiction
- [S4] DEV Community, "The best free ADHD app blocker for Android" (one-tap bypass, shame is dysregulating; pauses over walls): https://dev.to/astraedus/the-best-free-adhd-app-blocker-for-android-34o2
- [S5] Repost of The Conversation article on focus apps and neurodivergent users (design recommendations; aggregator copy): https://ph.end.dedyn.io/comment/26425526
- [S6] DEV Community, "ADHD support app: no streaks. Why gamification hurts more than it helps": https://dev.to/nucleusos/adhd-support-app-no-streaks-why-gamification-hurts-more-than-it-helps-1ae0
- [S7] DEV Community, "Why streaks make ADHD worse and what actually helps": https://dev.to/nucleusos/why-streaks-make-adhd-worse-and-what-actually-helps-5147
- [S8] Beyond BookSmart, "How to manage time blindness with executive function strategies": https://beyondbooksmart.com/executive-functioning-strategies-blog/how-to-manage-time-blindness-with-executive-function-strategies
- [S9] Talkiatry, "ADHD time blindness": https://www.talkiatry.com/blog/adhd-time-blindness
- [S10] Cambridge Network, "More than just late: 5 surprising truths about ADHD time blindness": https://www.cambridgenetwork.co.uk/blog/more-just-late-5-surprising-truths-about-adhd-time-blindness

Research sources:
- [S11] "When and why adults abandon lifestyle behavior and mental health mobile apps: scoping review", *JMIR* 26 (2024), e56897 (median 70% stop within 100 days): https://www.jmir.org/2024//e56897
- [S12] Chiarle, M., thesis on digital self-control tools, Politecnico di Torino (reactance can lead to abandonment, a hypothesis): https://webthesis.biblio.polito.it/31711
- Also see `store/marketing/research.md` #7, #8, #10, #11, #13, #14 for friction, grayscale and lockout studies.

Product and platform sources:
- [S13] Minimal: Simple Home Screen (App Store listing, "out of sight, out of mind"): https://apps.apple.com/us/app/-/id6480327824 and "10 minimalist Android launchers": https://pausegadget.com/best-minimalist-launchers-android/
- [S14] Shorts Blocker (open source, AccessibilityService detection of short-form screens): https://github.com/atick-faisal/Shorts-Blocker
- [S15] AwayDoomscrollin' on F-Droid (detection fragility, false positives): https://f-droid.org/eu/packages/com.resolvecommunity.awaydoomscrollin/
- [S16] Google Play Console Help, "Use of the AccessibilityService API": https://support.google.com/googleplay/android-developer/answer/10964491?hl=en
