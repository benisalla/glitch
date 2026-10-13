# Two-week launch plan

Goal: get Glitch into the hands of a few hundred people who care about FOSS and privacy, collect honest feedback, and land in F-Droid. Nothing here needs money.

Day 0 is the day all prerequisites are met. Pick a **Tuesday** for Day 1 if you can, so Show HN and Product Hunt fall on weekdays.

## Before Day 1 (prerequisites; see `distribution.md` section 0)
- [ ] Repo is public. History checked for secrets.
- [ ] GitHub Release `v1.0.0` with a signed `glitch.apk`, its SHA-256 in the release notes, and short install steps.
- [ ] Website live at https://benisalla.github.io/glitch/, with download, Obtainium and source links.
- [ ] Dependency Info block disabled (`dependenciesInfo { includeInApk = false; includeInBundle = false }`).
- [ ] Fastlane metadata moved to the root `fastlane/metadata/android/en-US/`, with `images/` and `changelogs/1.txt`.
- [ ] README states the AI-assistance disclosure. Add an Obtainium badge (self-hosted image) and the "Add to Obtainium" redirect link.
- [ ] GitHub issue templates: keep `bug_report.md`, and add a **feedback** template ("What did you expect? What felt annoying? What felt useful? Phone + Android version").
- [ ] Turn on GitHub Discussions (optional): one "Feedback on v1.0" thread to link from posts.
- [ ] Install the release APK on at least two physical devices, ideally one Samsung, and make sure a fresh install works.

## Feedback collection (all launch long)
- **Primary:** GitHub issues with labels `feedback`, `bug` and `idea`. Every post links to the issues page.
- **Email:** `ismailbenalla52@gmail.com` (already public in the privacy policy) for people without a GitHub account. Use a Gmail label `glitch-feedback`.
- **Threads:** answer every comment on HN, Reddit and Product Hunt within a few hours on launch days. Copy useful points into a GitHub issue and link back to the thread.
- **No analytics.** Measure only what is public: GitHub stars, release download counts (`gh release view v1.0.0 --json assets`), issues opened, and F-Droid status. Keep a simple log in a pinned GitHub issue "Launch notes".
- **Daily 20 minutes:** triage new issues, thank people, and close duplicates.

## Day by day

| Day | Channel / task | Details |
|---|---|---|
| **1 (Tue)** | GitHub release goes public. **AlternativeTo** submission. **F-Droid merge request** opened. | AlternativeTo and F-Droid queues are slow, so start them first. Submit the F-Droid MR with the `metadata/com.benisalla.glitch.yml` from `distribution.md` after `fdroid lint`/`build` pass locally. |
| **2 (Wed)** | **r/fossdroid** (Application Release flair, MIT line). | Morning, US Eastern. Stay around for 2–3 hours to reply. |
| **3 (Thu)** | **Show HN.** | Post 8–10am US Eastern with the repo URL, then add the first comment at once. Be available all day. Don't share the link asking for votes. |
| **4 (Fri)** | Triage and fix. | Fix any quick bugs from Days 2–3. If a fix ships, publish `v1.0.1`: versionCode 2, `changelogs/2.txt`, and a new release. Note it in the threads. |
| **5 (Sat)** | **X/Twitter** launch post. Mastodon (e.g. floss.social, fosstodon.org) with the same text plus `#FOSS #Android`. | The weekend is fine for social. Reply to everyone. |
| **6 (Sun)** | Rest / light replies. | Update the "Launch notes" issue with numbers and themes. |
| **7 (Mon)** | **LinkedIn** post. | Weekday morning, local time. |
| **8 (Tue)** | **Product Hunt** launch. | 12:01am Pacific. Maker comment right away. Upload gallery images. Say "Android only, APK from GitHub". No vote asking. |
| **9 (Wed)** | **r/droidappshowcase** post. Check the r/digitalminimalism and r/nosurf sidebars and **message their mods** to ask whether a disclosed developer post is OK. | Post only where it is allowed. Never post in r/androidapps or r/ADHD. |
| **10 (Thu)** | r/digitalminimalism **or** r/nosurf, **only if** the mods said yes. Use the discussion-first draft. | One sub per day, never cross-post the same text on the same day. |
| **11 (Fri)** | **dev.to / blog** article (outline in `posts.md`). | Share it on X/Mastodon/LinkedIn as the "how it's built" follow-up. This counts as content, not a second launch. |
| **12 (Sat)** | Follow up on F-Droid. | Answer packager comments on the MR. If reproducible builds fail, fall back to F-Droid signing. |
| **13 (Sun)** | Write a feedback summary. | Group issues and emails into: bugs, annoyances, requests. Do not commit to roadmap items without the owner's approval (AGENTS.md). |
| **14 (Mon)** | Wrap-up post. | A short "what I learned from 2 weeks of feedback" on GitHub Discussions, X and LinkedIn. Thank contributors by name (with their consent). Decide the next step: the Play closed test (12 testers, 14 days) using the promo text in `store/marketing/copy-notes.md`, and developer verification before 2027. |

## Guardrails during the launch
- One post per community, no reposting, and always disclose that you're the developer.
- No claims beyond what the app does (AGENTS.md). If a commenter says "this cured my ADHD", thank them, but don't quote or repost it as a testimonial.
- If someone asks about AI use, answer plainly (see `posts.md`).
- If a community removes a post, accept it, don't argue, and don't repost elsewhere to get around the rules.
- No paid promotion, no vote exchanges, no sockpuppets.
