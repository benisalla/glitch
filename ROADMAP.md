# Glitch roadmap

This is a plan, not a promise of dates. Each item is built only after the owner approves it, and it ships with tests. The research behind each step is in [`product/`](product/):

- [research-adhd-focus.md](product/research-adhd-focus.md): the evidence
- [vision-and-ux.md](product/vision-and-ux.md): personas, principles and wireframes
- [feasibility-android.md](product/feasibility-android.md): what Android and Google Play allow

Evidence ratings come from the research review: **moderate** means supported by field or lab studies in the general population; **weak** means inferred and not yet tested in this form. No feature has been tested with people with ADHD, and none of them treats ADHD.

## v1.0: Shuffle (today)
- Your apps in a new random order every time you open Glitch.
- Light and dark themes, no permissions, no internet, no tracking.

## v1.1: Intention moment (no new permissions)
Why now: these features make the shuffle easier to live with and add the first gentle cue, without giving up "Permissions: none".

| Feature | What it does | Evidence |
|---|---|---|
| Mark as noisy | Mark the apps that pull you in. Their tiles turn calm grayscale in Glitch. | Weak |
| "Still want this?" pause | Before a noisy app opens from Glitch: a big **Close** button and **Open anyway**. Never a lockout. | Moderate |
| Your own if-then plans | Write lines like "If I open Instagram, then I first reply to Sara". One is shown on the pause, and they rotate. | Moderate (if-then plans); weak (in a pause) |
| Anchor apps | Up to 4 essentials (phone, maps) keep a fixed spot; everything else shuffles. Changes the core shuffle, so it needs an AGENTS.md update. | Weak |
| Focus sets | One tap to show only a chosen set, e.g. *Study* or *Evening*. | Weak |
| "Away time" card | Coming back to Glitch shows "You left 23 min ago for YouTube". Only from Glitch's own launches. | Weak |
| Glitch shortcuts | Put "Instagram via Glitch" on your home screen so the pause also works from there. | Feasibility-checked |

## v1.2: Gentle cues (one optional permission: notifications)
- **Kind time cue:** one notification a while after you open a noisy app from Glitch, showing your own reason. At most one per launch. Off by default.
- **Focus widget and Quick Settings tile:** start a focus set from the home screen.

## v2.0: Time awareness (opt-in: usage access, a foreground service and notifications)
- See time spent in your noisy apps today. It is computed on the phone and never leaves it.
- **Scroll Glitch, app level:** after 10–15 minutes of continuous use of a short-video app, show your own reminder with **Close**, **5 more minutes** and **Pause today**. One reminder plus one follow-up per session, with a daily cap.

## Future: Scroll Glitch, in-app detection (separate opt-in add-on)
- Tells Reels or Shorts apart from messages and the rest of the app, so only endless scrolling triggers a reminder.
- It needs Android's AccessibilityService, so it will ship as a **separate companion app ("Glitch Guard")**. Glitch itself never gets accessibility access, and its shuffle keeps working with every permission denied.
- Screen content is reduced on the phone to a yes/no and is never stored or sent. Glitch has no internet access.
- Known costs: a stricter Google Play review, and detection that breaks when Instagram, YouTube or TikTok change their screens.

## What we will not build
Lockouts, streaks, shame-based messages, cloud accounts, ads, selling data, or claims that Glitch treats or fixes ADHD.
