# Store copy notes (en-US)

The listing text itself lives in `store/fastlane/metadata/android/en-US/`. This file explains the choices and keeps material for later.

## Current listing at a glance
| Field | Text | Chars (limit) |
|---|---|---|
| Title | Glitch: Break the Autopilot | 27 (30) |
| Short description | Glitch the autopilot. Your apps, shuffled on every open. Look, then tap. | 72 (80) |
| Full description | see `full_description.txt` | 2638 (4000) |
| What's new (1.0.0) | see `changelogs/default.txt` | 427 (500) |

Counts are from `wc -m` on files with no trailing newline.

Tagline: "Glitch the autopilot. Look, then tap." It opens the full description and starts the short description.

Note: the app is now called Glitch (name under the icon), and the Play title is "Glitch: Break the Autopilot". When creating the app in Play Console (`store/README.md`, step 4), use this title.

## Name and tone
"Glitch" plays on the icon: a white ring, the loop of autopilot, broken by a tilted gold tile, one piece out of place. The tone is confident and a little edgy: the app interrupts tapping on autopilot. Keep the edge in the wording, not in the claims. "Autopilot" refers only to tapping apps by position, which the research supports (`research.md`, #4, #5, #6). It never means phone use, attention or behavior in general.

## Keywords and why
Play ranks mostly on the title, then the short description, then natural use of words in the full description. No keyword list, no repetition for its own sake.

"Glitch" on its own is a common word (games, video effects, bug reports) and will not rank by itself. Every place that carries search weight pairs it with a descriptive phrase.

| Keyword / phrase | Where | Why |
|---|---|---|
| glitch | title, short description, description | The brand word. Always next to "Break the Autopilot" or a description of the shuffle, never alone. |
| break the autopilot | title | The promise of the whole app, not just its first feature. |
| shuffled, random app order | short description, description | Describes the shipped first feature for search. |
| random order / random app order | description, changelog | The plain search phrase for what the app does. |
| shuffled / reshuffled | short description, description | The core action, matches screenshots. |
| app list / apps | description | Natural phrasing for the list of installed apps. |
| look, then tap | short description, description, screenshot 3 | The memorable hook. Ties copy to the "Look, then tap" screenshot. |
| autopilot, by position, from memory | short description, description ("Why glitch?") | Explains the idea in everyday words, without claiming an outcome. |
| no permissions, no internet, no ads, no tracking, private | description, changelog, screenshot 5 | A real differentiator for a utility that reads the app list. Matches the Data safety label ("No data collected"). |
| light and dark | description, changelog | Matches screenshot 4. |

Deliberately not used:
- "Glitch" alone in the title. Without "Break the Autopilot" it would compete with games and effects apps. Glitch is more than a menu: the shuffle is only its first feature, so "menu" stays out of the title.
- "App drawer" and "launcher" in the title or short description. The app is not a launcher or drawer replacement, and those words would attract people expecting one. They appear in the full description only to say what the app is not.
- Any focus, attention, memory, productivity, mindfulness, detox, screen-time, addiction or mental-health wording, and no mention of any condition. Not allowed by AGENTS.md and risky under Play's metadata policy.
- Film or pop-culture references to "breaking out" of a simulation, and their trademarked names. The "autopilot" wording carries the vibe on its own.
- Brand names (phone makers, other apps), superlatives, "free" in the title, emoji, ALL CAPS.

## Hook for research (later)
The "Why glitch?" section has one sentence designed to carry a research link or citation, without changing the claim:

> Most of us open apps by position. The thumb knows where an icon lives and taps before we have really decided.

This describes a common behavior (tapping by position out of habit). It does not promise any result. If research is added, keep it as background ("research on habits describes..."), not as an outcome of using this app.

## Short description alternatives for A/B tests
For Play Console store listing experiments (Grow > Store presence > Store listing experiments). Test one change at a time against the current text.

| Variant | Text | Chars |
|---|---|---|
| Current (tagline angle) | Glitch the autopilot. Your apps, shuffled on every open. Look, then tap. | 72 |
| A (plain habit angle) | Apps in a new random order on every open, so you look before you tap. | 69 |
| B (privacy angle) | Your apps, shuffled on every open. No permissions, no internet, no ads. | 71 |

What each tests: the current text leads with the brand attitude; A says plainly what happens and why, for people who skim past the tagline; B leads with privacy for people who are wary of utilities that read app lists. Run each for at least 7 days or until Play reports a clear result.

## Promo text (friends and testers)
Use for messages, social posts, or recruiting the 12 closed testers Play requires.

> I made a small Android app called Glitch. It shows your apps in a new random order every time you open it, so you can't tap by muscle memory: you look, then tap. Tap an app and it opens like normal. It needs no permissions, has no internet access, no ads and no tracking, and it doesn't replace your home screen. Before it can go public on Google Play, I need 12 testers for a 14-day closed test. If you have an Android phone, send me the email address you use for Google Play, install Glitch from the link I send you, and keep it installed for the full two weeks. Feedback, good or bad, is very welcome.
