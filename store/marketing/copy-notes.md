# Store copy notes (en-US)

The listing text itself lives in `store/fastlane/metadata/android/en-US/`. This file explains the choices and keeps material for later.

## Current listing at a glance
| Field | Text | Chars (limit) |
|---|---|---|
| Title | Shuffle Menu: Random App Order | 30 (30) |
| Short description | Your apps in a new order every time you open it. Look, then tap. No permissions. | 80 (80) |
| Full description | see `full_description.txt` | 2616 (4000) |
| What's new (1.0.0) | see `changelogs/default.txt` | 426 (500) |

Counts are from `wc -m` on files with no trailing newline.

Note: the title on Play is now "Shuffle Menu: Random App Order", which matches the name under the icon ("Shuffle Menu") and the feature graphic. When creating the app in Play Console (`store/README.md`, step 4), use this title instead of "Shuffle App Menu".

## Keywords and why
Play ranks mostly on the title, then the short description, then natural use of words in the full description. No keyword list, no repetition for its own sake.

| Keyword / phrase | Where | Why |
|---|---|---|
| shuffle | title, description | The brand word and the core action. Matches what the icon and screenshots show. |
| random app order / random order | title, description | The plain search phrase for what the app does. "Random" is a common search word; "order" says exactly what is randomized. |
| menu | title, description | Matches the app's name and the screenshots ("every time you open the menu"). |
| app list / apps | description | Natural phrasing people use for the list of their apps. |
| look, then tap | short description, description, screenshot 3 | The memorable hook. Ties copy to the "Look, then tap" screenshot. |
| habit, by position, from memory | description ("Why shuffle?") | Explains the idea in everyday words, without claiming an outcome. |
| no permissions, no internet, no ads, no tracking, private | short description, description, screenshot 5 | A real differentiator for a utility that reads the app list. Matches the Data safety label ("No data collected"). |
| light and dark | description, changelog | Matches screenshot 4. |

Deliberately not used:
- "App drawer" and "launcher" in the title or short description. The app is not a launcher or drawer replacement, and those words would attract people expecting one (and leave bad reviews). They appear in the full description only to say what the app is not.
- Any focus, memory, productivity, mindfulness, detox or mental-health wording. Not allowed by AGENTS.md and risky under Play's metadata policy.
- Brand names (phone makers, other apps), superlatives, "free" in the title, emoji, ALL CAPS.

## Hook for research (later)
The "Why shuffle?" section has one sentence designed to carry a research link or citation once the teammate's review is done, without changing the claim:

> Most of us open apps by position. The thumb knows where an icon lives and taps before we have really decided.

This describes a common behavior (tapping by position out of habit). It does not promise any result. If research is added, keep it as background ("research on habits describes..."), not as an outcome of using this app.

## Short description alternatives for A/B tests
For Play Console store listing experiments (Grow > Store presence > Store listing experiments). Test one change at a time against the current text.

| Variant | Text | Chars |
|---|---|---|
| Current | Your apps in a new order every time you open it. Look, then tap. No permissions. | 80 |
| A (habit angle) | A fresh app order each time you open it, so you look before you tap. | 68 |
| B (privacy angle) | Your app list, shuffled on every open. No permissions, no ads, no tracking. | 75 |

What each tests: A leads with the idea of looking before tapping; B leads with privacy for people who are wary of utilities that read app lists. Run each for at least 7 days or until Play reports a clear result.

## Promo text (friends and testers)
Use for messages, social posts, or recruiting the 12+ closed testers Play requires.

> I made a small Android app called Shuffle Menu. It shows your apps in a new random order every time you open it, so you look at the list and choose instead of tapping by habit. Tap an app and it opens like normal. It needs no permissions, has no internet access, no ads and no tracking, and it doesn't replace your home screen. I need testers for a 14-day closed test on Google Play before it can go public: if you have an Android phone, send me the email address you use for Play, install it from the link I send you, and keep it installed for two weeks. Feedback is very welcome.
