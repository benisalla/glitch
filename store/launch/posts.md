# Launch posts (drafts)

Ready to paste once `distribution.md` blockers 1–3 are fixed: the repo is public, a release has `glitch.apk`, and the website is live.

## Rules for every post
- Say what the app does today. Plans are labelled as plans, never as features.
- Never say Glitch improves focus or attention, reduces screen time, or treats, fixes or reduces ADHD or any condition. Outside the store listing, the only allowed research line is: **"designed using research on attention, habits and ADHD-friendly design."** Use it sparingly, and leave it out wherever ADHD would read as a sales hook.
- Not a medical device. No outcome promises, no "science-backed", no "digital detox".
- Always say you are the developer.
- Be open about AI assistance if anyone asks (see `distribution.md`, blocker 7). A suggested line: *"I built it with an AI coding assistant (Claude Code). I review and test every change, and the code is all public."*
- Never ask for upvotes, in public or in private.
- Feedback goes to GitHub issues (`https://github.com/benisalla/glitch/issues`) or the email in the privacy policy (`ismailbenalla52@gmail.com`).

Links used below:
- Repo: https://github.com/benisalla/glitch
- APK: https://github.com/benisalla/glitch/releases/latest/download/glitch.apk
- Website: https://benisalla.github.io/glitch/
- Obtainium: https://apps.obtainium.imranr.dev/redirect?r=obtainium://add/https://github.com/benisalla/glitch

---

## Reddit

### Community rules (checked 2026-10-13; re-read each sidebar before posting)
| Subreddit | Self-promotion status | What to do |
|---|---|---|
| r/androidapps | **Not allowed.** A mod post from 27 May 2026 updated Rule 2: all self-promotion, including tester requests and feedback requests, must go to **r/droidappshowcase**. The rule is "zero tolerance", and fake "I found this app" posts are called out. | Post in **r/droidappshowcase** instead. Do not post in r/androidapps. If someone in r/androidapps asks for exactly this kind of app, reply once, say you are the developer, and link the repo. |
| r/fossdroid | Allowed for FOSS apps. Use the **"Application Release"** flair for an app you created, and **list the open-source license** at the top or bottom of the post. | Post the draft below with the license line. |
| r/digitalminimalism | Rules not verified (the sidebar could not be loaded). Many wellbeing subs ban promotion. | Read the sidebar first. If there is no explicit rule, message the mods to ask before posting. If promotion is not allowed, skip it. |
| r/nosurf | Rules not verified (same reason). The sub often discusses tools, but that does not mean promotion is allowed. | Same as above: check the sidebar, then ask the mods. Use the discussion-first draft. |
| r/ADHD | **Not allowed.** Rule 8 is "No Advertising/Self Promotion", and research recruitment needs approval (Rule 7). Related subs (r/AutisticWithADHD) explicitly ban free-app promotion and beta-tester requests. | **Do not post.** No workaround posts either. If a mod invites tool suggestions in an official thread, a single disclosed comment may be fine. Ask the mods first. |

Sources: [r/androidapps mod post, 27 May 2026 (mirror)](https://rl.klein.ruhr/r/AndroidApps), [r/FOSSdroid posting guidelines](https://fossdroid.org/reddit/posting-guidelines.html), [r/ADHD rules (mirror)](https://reddit.birdcat.cafe/r/ADHD/wiki/rules).

### r/fossdroid (flair: Application Release)
**Title:** Glitch: an open-source app that shows your apps in a new random order every time you open it

**Body:**
> **License: MIT**
>
> Hi, I'm the developer. Glitch is a small Android app I made because my thumb kept opening apps before I had decided to.
>
> What it does:
> - Shows your installed apps (icon + name) in a random order, reshuffled every time you open it.
> - The order stays put while it's on screen, even when you rotate the phone.
> - Tap an app and it opens normally. Come back and the order changes again.
> - It does **not** replace your launcher or app drawer. It's a separate app you put on your home screen.
>
> Privacy: no permissions, no internet access, no ads, no analytics, no accounts. It sees only apps that show up in a launcher, through a narrow `<queries>` entry rather than `QUERY_ALL_PACKAGES`.
>
> Stack: Kotlin + Jetpack Compose, AndroidX only, no proprietary dependencies. Android 8.0+.
>
> - Source: https://github.com/benisalla/glitch
> - APK: https://github.com/benisalla/glitch/releases/latest
> - Obtainium: add `https://github.com/benisalla/glitch`
> - F-Droid: planned
>
> It's v1.0, so it's deliberately tiny. I'd love feedback on the idea and the code, and bug reports. Issues are open on GitHub.

### r/droidappshowcase (the place for r/androidapps-style posts)
**Title:** [Dev] Glitch: your apps in a new order every time, so you look before you tap (free, open source, no permissions)

**Body:**
> I'm the developer. Glitch shows your installed apps in a random order that changes every time you open it. Your thumb can't tap a spot from memory, so you read the list and pick what you actually came for. Tap an app and it opens like normal.
>
> - Free, MIT licensed, no ads, no in-app purchases
> - No permissions and no internet access
> - Doesn't replace your launcher or One UI's app drawer. It's a separate app.
> - Light and dark themes
>
> Yes, finding an app takes a second longer. That's the point.
>
> Download: https://github.com/benisalla/glitch/releases/latest (or add the repo in Obtainium)
> Source: https://github.com/benisalla/glitch
>
> What would make this more useful to you? Honest criticism welcome.

### r/digitalminimalism / r/nosurf (only if the rules allow it or a mod approves; lead with the discussion)
**Title:** I made a tiny app that shuffles my app icons every time I open it. Curious if anyone else has tried "position friction"

**Body:**
> Disclosure: I'm the developer, and it's free and open source, with nothing to sell.
>
> I noticed I'd open apps by position: the thumb goes to the bottom-right corner and I'm in before I've decided anything. So I built Glitch. It shows your apps in a new random order every time you open it, so you have to read the names before you tap.
>
> It doesn't block anything, there are no timers, and it doesn't track anything (no permissions, no internet). It just removes the "same spot" shortcut. I don't know yet whether it helps anyone beyond me, and I'm not claiming it reduces screen time.
>
> Have you tried anything similar, like grayscale, moving icons around, or hiding apps in folders? What worked, and what did you end up turning off?
>
> Link if you want to try it: https://github.com/benisalla/glitch

### r/ADHD
**Do not post.** Rule 8 bans advertising and self-promotion. See the table above.

---

## Hacker News: Show HN

Rules (from the Show HN guidelines): it must be something people can try, with no signup wall. Never ask friends to upvote or comment. Stay in the thread and answer questions. Post on a weekday morning, US Eastern time.

**Title (up to 80 characters):**
`Show HN: Glitch – an Android app that shuffles your app list on every open`

**URL:** `https://github.com/benisalla/glitch` (the repo, so people see the code first)

**First comment (post right away):**
> Hi HN, I'm Ismail. Glitch is a small Android app that shows your installed apps in a random order, reshuffled each time you open it. The idea: most of us open apps by position, and the thumb taps before we've decided. When the icons are rarely in the same place, you read the names and choose.
>
> Some details that might interest people here:
>
> - No permissions at all, no INTERNET. To list apps it uses a narrow `<queries>` entry for MAIN/LAUNCHER intents instead of QUERY_ALL_PACKAGES.
> - Session semantics were the fiddly part: shuffle exactly once per "newly opened" menu (onStart after a non-config-change onStop), keep the order stable across rotation and recomposition, and when a package is installed or removed while the menu is open, keep the survivors' order and only add or drop the changed entries.
> - Tests inject a seeded Random and assert invariants (it's a permutation, the order is stable across config changes, reconciliation keeps survivors in place) instead of "looks random".
> - Kotlin + Compose, AndroidX only, MIT.
> - I built it with an AI coding assistant (Claude Code). I reviewed and tested every change, and the history is public.
>
> It isn't a launcher replacement and doesn't touch the system app drawer. The next ideas (an optional "still want this?" pause for apps you mark yourself) are on the roadmap, not shipped.
>
> APK: https://github.com/benisalla/glitch/releases/latest. You can also add it in Obtainium. I'd especially like feedback on whether the friction is annoying or useful after a few days.

---

## Product Hunt

Notes: the tagline limit is 60 characters (third-party guides; confirm in the form). No vote rings. Post the maker comment right away. Gallery images: use `store/screenshots/*` and `store/graphics/feature-graphic.png`. Category: Android / Productivity / Open Source.

**Name:** Glitch

**Tagline (37 characters):** `Glitch the autopilot. Look, then tap.`
Alternative (55 characters): `Your Android apps in a new order every time you open it`

**Description (up to 260 characters):**
> Glitch shows your installed Android apps in a random order, reshuffled every time you open it. You read before you tap instead of tapping by position. Free and open source (MIT). No permissions, no internet, no ads, no tracking.

**Topics:** Android, Open Source, Privacy, Productivity

**Links:** website, GitHub repo, APK release page. Product Hunt is a web/iOS-heavy audience, so say "Android only" clearly.

**First comment (maker):**
> Hey Product Hunt, I'm Ismail, the maker.
>
> I kept opening apps on autopilot: unlock, thumb goes to the same corner, and I'm in before I've decided anything. Glitch is my small answer. It shows your apps in a new random order every time you open it, so you have to look before you tap. Tap an app and it opens normally.
>
> What it is not: a blocker, a timer or a launcher replacement. It doesn't take away your phone. It just removes the "same spot" shortcut. It also asks for no permissions, has no internet access, and collects nothing.
>
> It's free and open source (MIT), Android 8.0+, and you install it as an APK from GitHub for now. Google Play is coming later.
>
> Glitch is designed using research on attention, habits and ADHD-friendly design, and I'd love to hear from you: is the shuffle useful friction or just annoying? What would you want next? Issues are open at github.com/benisalla/glitch.

---

## X / Twitter

**Launch post (under 280 characters):**
> I made Glitch: a free, open-source Android app that shows your apps in a new random order every time you open it.
>
> Your thumb can't tap by memory, so you look, then tap.
>
> No permissions. No internet. No ads.
>
> https://github.com/benisalla/glitch

**Thread follow-ups (optional):**
> 2/ It's not a launcher and doesn't touch your app drawer. Add it to your home screen and open it when you want to choose instead of autopilot.
>
> 3/ Privacy: zero permissions, no INTERNET. It only sees apps that appear in a launcher. MIT licensed, Kotlin + Compose.
>
> 4/ v1.0 is deliberately tiny. Tell me what's annoying or useful, here or in GitHub issues. APK: https://github.com/benisalla/glitch/releases/latest

Hashtags (at most 2): `#Android #OpenSource`

---

## LinkedIn

> I shipped a small side project: **Glitch**, a free, open-source Android app.
>
> It does one thing. It shows your installed apps in a new random order every time you open it. Most of us open apps by position, and the thumb taps before we've decided. When the icons move, you read the names and choose. Tap an app and it opens normally.
>
> A few choices I'm happy with:
> • No permissions and no internet access. Nothing can leave the phone.
> • No ads, accounts or analytics.
> • A narrow package-visibility query instead of reading every installed package.
> • Deterministic tests for a random feature (a seeded Random, plus invariant checks).
>
> Glitch is designed using research on attention, habits and ADHD-friendly design. It's not a medical device and it doesn't promise outcomes. It's a small moment of friction that you control.
>
> It's Android only, MIT licensed, and the APK is on GitHub (Play later). Feedback is very welcome, especially from people who try it for a week:
> https://github.com/benisalla/glitch
>
> #Android #OpenSource #Kotlin #JetpackCompose #Privacy

---

## dev.to / blog post outline

**Title:** "Building a random feature you can test: shuffling an Android app list with Compose"
**Tags:** android, kotlin, jetpackcompose, opensource

1. **The itch (2 paragraphs).** Opening apps by position, and the idea of breaking that with a shuffle. State clearly that this is a tiny tool, not a productivity promise.
2. **Seeing apps without permissions.** Android 11+ package visibility; why a `<queries>` entry for `MAIN`/`LAUNCHER` instead of `QUERY_ALL_PACKAGES`; what the user sees in the manifest (nothing).
3. **When exactly to shuffle.** Session semantics: shuffle in `onStart` after a real stop, not after a configuration change (`isChangingConfigurations`). Why "returning from a launched app" counts as a new session.
4. **Live updates without reshuffling.** Reconciling package-change events: keep the survivors' order, drop removed apps, append new ones. Include a code snippet.
5. **Testing randomness without flaky tests.** Inject `Random`, use a fixed seed, and assert invariants (permutation, stability, reconciliation) instead of "looks random".
6. **Privacy as a feature.** No INTERNET permission, no analytics, the Data safety answers, and why that matters for an app that reads your app list.
7. **Shipping free, outside Play.** GitHub Releases, Obtainium, F-Droid, and a short, factual note on Google's developer verification (enforced in 4 countries since 30 Sep 2026, for participating stores only; global in 2027).
8. **How it was built.** A short, honest note on using an AI coding assistant, and on how review and tests kept it in check.
9. **What's next and how to help.** The roadmap is labelled as plans. Ask for feedback through GitHub issues.

Canonical URL: publish on the website first if it has a blog, then cross-post to dev.to with `canonical_url`.
