# Distribution: free channels for Glitch

Checked on 2026-10-13. Policies change, so re-check each link before you submit. Sources are listed under each section.

Glitch facts used below: package `com.benisalla.glitch`, versionName `1.0.0`, versionCode `1`, MIT license, minSdk 26, no permissions, no network code. Stable APK link: `https://github.com/benisalla/glitch/releases/latest/download/glitch.apk`. Website: `https://benisalla.github.io/glitch/`.

## 0. Blockers to fix before any channel (state of `main` today)

| # | Blocker | Why it matters | Who / where |
|---|---|---|---|
| 1 | **The GitHub repo is private** (`gh repo view` reports `PRIVATE`). | Every FOSS catalog, Obtainium, Show HN and r/fossdroid need a public repo. The stable APK link returns 404 for everyone else. | Owner: Settings > General > Change visibility. Check the history for secrets first. |
| 2 | **No GitHub Release yet.** Tag `v1.0.0` exists, but no release has `glitch.apk` attached. | The `releases/latest/download/glitch.apk` link, IzzyOnDroid and Obtainium all read the latest release. | Release engineer: publish a signed release APK named `glitch.apk` on a release for tag `v1.0.0`. |
| 3 | **The website is not live** (`https://benisalla.github.io/glitch/` returns 404). | Posts and catalogs link to it. | Web designer: enable GitHub Pages. |
| 4 | **The APK will contain Google's encrypted "Dependency Info" signing block.** AGP adds it unless you turn it off. | IzzyOnDroid's scanner flags it and asks developers to remove it. F-Droid is discussing a policy, and the block can break reproducible-build checks. | `app/build.gradle.kts` (owner area, so it needs an explicit reason): add `dependenciesInfo { includeInApk = false; includeInBundle = false }` inside `android { }`. Play does not need the block. |
| 5 | **The fastlane metadata is in `store/fastlane/`.** | F-Droid only looks in fixed places at the repo root: `fastlane/metadata/android/<locale>/`, `metadata/<locale>/` or `src/<flavor>/fastlane/...`. IzzyOnDroid accepts any path inside the repo. | Move it to `fastlane/metadata/android/en-US/` at the root and point `store/` docs there. A symlink would also work, but moving it is cleaner. |
| 6 | **The fastlane folder has no images, and the changelog name is wrong for F-Droid.** | F-Droid and IzzyOnDroid read `images/icon.png` (512 px), `images/featureGraphic.png` (1024x500) and `images/phoneScreenshots/*.png`. They read changelogs as `changelogs/<versionCode>.txt`, so `default.txt` is ignored. | Copy `store/graphics/icon-512.png`, `store/graphics/feature-graphic.png` and `store/screenshots/*.png` into `images/`. Add `changelogs/1.txt` (500 bytes max, plain ASCII). |
| 7 | **AI-assisted code needs a decision on disclosure.** 25 of the 39 commits carry `Co-Authored-By: Claude`. | IzzyOnDroid rejects apps whose code is fully or partly written by generative AI (section 1). F-Droid has no formal rule yet, but an interim proposal encourages disclosure. HN and Reddit readers will look at the commit history. | Owner: decide how to describe it. My recommendation is a short, honest line in the README, for example "Built with AI coding assistance (Claude Code); every change is reviewed and tested by the maintainer". Never hide it. |

## 1. IzzyOnDroid (F-Droid-compatible repo)

**What it is:** a large third-party F-Droid repo (`apt.izzysoft.de/fdroid`) that is built into the Neo Store and Droid-ify clients and can be added to the F-Droid client. It does not build from source: it pulls the developer-signed APK from your tagged GitHub release and scans it.

**Inclusion criteria** (App Inclusion Policy, current version):
- FOSS license approved by OSI/FSF. **MIT is fine.**
- Public source code on Codeberg, GitLab or GitHub. **Blocked until the repo is public.**
- No proprietary components or trackers. **Fine** (see the dependency audit in section 6).
- Fastlane metadata in the repo, with a short description of 80 characters or fewer, a full description, an icon and screenshots. The path is configured per app. **The text is in place, but the images are missing** (blocker 6).
- APK signed with a release key, not `debuggable` or `testOnly`. The APK has to be attached to a **tagged GitHub release**, with tag names that match versionName or versionCode (`v1.0.0` is fine). Maximum about 30 MB.
- **AI policy:** "We are strongly opposed to apps which are fully or in part created by generative AI tools." "Vibe-coded apps will be rejected." LLM-written README and changelog text is allowed, but code is not. "Any lack of transparency ... can lead to the project being degraded to rejected state."

**Does Glitch qualify?** Technically yes, once blockers 1, 2, 4 and 6 are fixed. **Under the AI policy, it most likely does not:** the app code was written with an AI coding assistant. Do not submit it while leaving that out. If the owner wants to try, say so openly in the request and accept a likely "no". Effort is better spent on F-Droid, Obtainium and GitHub Releases.

**Steps (if you submit):**
1. Fix blockers 1, 2, 4 and 6.
2. Open an issue at https://codeberg.org/IzzyOnDroid/repodata/issues. The inclusion queue moved from GitLab to Codeberg, and the old GitLab project is archived and read-only. Title it `[AppRequest] Glitch`. Include the repo URL, the license, a one-line description, and a sentence on how AI tools were used.
3. Reply to the reviewer. Requests get an `approved` label after review and may wait for `needs/on-device-testing`.

Sources: [IzzyOnDroid App Inclusion Policy](https://izzyondroid.org/docs/general/AppInclusionPolicy/), [New app inclusions](https://izzyondroid.org/contributing/NewAppInclusions/), [Fastlane notes](https://izzyondroid.org/docs/general/Fastlane/), [APK checks / signing-block scan](https://android.izzysoft.de/articles/named/iod-scan-apkchecks?lang=en), [AGP `DependenciesInfo`](https://developer.android.com/reference/tools/gradle-api/com/android/build/api/dsl/DependenciesInfo).

## 2. F-Droid (main repository)

**What it is:** the main FOSS Android store. F-Droid builds the app itself from your source, on its own build server, and by default signs it with F-Droid's key.

**Requirements:**
- Everything must be FLOSS: the app, its dependencies and the whole toolchain, built with Debian-packaged tools and no proprietary build tools. Binaries from Google Maven are allowed if they are freely licensed. No Play Services, Firebase or Crashlytics.
- Public repo with a license file. **Blocked until the repo is public.**
- Tag every release commit. A `versionName` of `1.0.0` maps to tag `v1.0.0`, which fits.
- Metadata: F-Droid reads fastlane metadata from the repo root (blockers 5 and 6). It also needs a recipe file `metadata/com.benisalla.glitch.yml` in the `fdroiddata` repo.
- **Reproducible builds are optional.** Without them, F-Droid ships its own build signed with its own key, so users cannot update between your GitHub APK and the F-Droid APK. With them, you add `Binaries:` (pointing at your GitHub APK) and `AllowedAPKSigningKeys:` (your certificate's SHA-256) to the recipe. F-Droid then publishes **your** signed APK whenever its own build matches it. Known pitfall: F-Droid's docs say APKs signed with `apksigner` from build-tools 35.0.0-rc1 or later fail its verification, and recommend build-tools 34 for signing. This repo uses build-tools 36.0.0, so test before you rely on reproducibility. Blocker 4 also has to be fixed.
- Build toolchain: AGP 9.4.1, Kotlin 2.4.21 and JDK 17. The F-Droid build server has JDK 17 (Bookworm) and JDK 21/25 (Trixie). Platform 37 is fetched through `sdkmanager`. Nothing proprietary.
- AI: there is no formal F-Droid policy yet. An interim proposal (adapted from Debian) asks for real human review of AI-generated material and encourages, but does not require, disclosure. Disclose anyway (blocker 7).

**Does Glitch qualify?** Yes, once blockers 1, 2, 5 and 6 are fixed. The dependencies are all FOSS (section 6). Anti-features: none expected.

**Option A, Request For Packaging (easiest, slow):** open an issue at https://gitlab.com/fdroid/rfp/-/issues using the "New app" template, with the repo URL, license and a description. A volunteer packager writes the recipe when they have time. This can take weeks or months.

**Option B, merge request (faster, recommended):**
1. Fork https://gitlab.com/fdroid/fdroiddata and create the branch `com.benisalla.glitch`.
2. Add `metadata/com.benisalla.glitch.yml`. Starting draft (verify it with `fdroid lint`):
   ```yaml
   Categories:
     - System
   License: MIT
   AuthorName: Ismail Ben Alla
   WebSite: https://benisalla.github.io/glitch/
   SourceCode: https://github.com/benisalla/glitch
   IssueTracker: https://github.com/benisalla/glitch/issues
   Changelog: https://github.com/benisalla/glitch/releases

   AutoName: Glitch

   RepoType: git
   Repo: https://github.com/benisalla/glitch.git

   Builds:
     - versionName: 1.0.0
       versionCode: 1
       commit: v1.0.0
       subdir: app
       gradle:
         - yes

   AutoUpdateMode: Version
   UpdateCheckMode: Tags
   CurrentVersion: 1.0.0
   CurrentVersionCode: 1
   ```
   If you choose reproducible builds, add `Binaries: https://github.com/benisalla/glitch/releases/download/v%v/glitch.apk` and `AllowedAPKSigningKeys: <sha256 of your signing cert, lowercase, no colons>`. Note that the `%v` URL needs a **versioned** asset path, not `latest`.
3. In the fdroidserver build container (`/build`), run `fdroid readmeta`, `fdroid rewritemeta com.benisalla.glitch`, `fdroid checkupdates --allow-dirty com.benisalla.glitch`, `fdroid lint com.benisalla.glitch` and `fdroid build com.benisalla.glitch`.
4. Commit with the message `New App: com.benisalla.glitch`, push, and open a merge request against `fdroiddata`. Answer packager questions quickly.
5. After the merge, the app appears in the client after the next build cycle, usually within a few days.

Sources: [F-Droid Inclusion Policy](https://f-droid.org/docs/Inclusion_Policy/), [Submitting Quick Start Guide](https://f-droid.org/docs/Submitting_to_F-Droid_Quick_Start_Guide/), [Reproducible Builds](https://f-droid.org/docs/Reproducible_Builds/), [Fastlane locations](https://izzyondroid.org/docs/general/Fastlane/), [F-Droid RFP tracker](https://gitlab.com/fdroid/rfp/-/issues), [Interim GenAI policy proposal](https://gitlab.com/fdroid/admin/-/work_items/699), [Forum: F-Droid and AI](https://forum.f-droid.org/t/f-droid-policy-on-libre-ai/33279).

## 3. Obtainium (install and update from GitHub Releases)

**What it is:** an open-source Android app that installs and updates apps straight from their release pages. There is nothing to submit and no approval step. It works as soon as the repo is public and a release has an APK.

**How users add Glitch:** Obtainium > Add App > paste `https://github.com/benisalla/glitch` > Add. Obtainium picks the APK from the latest release and checks for updates.

**One-click link (official deep-link format):**
- Raw: `obtainium://add/https://github.com/benisalla/glitch`
- Clickable on the web, with a fallback page for people without Obtainium (use this in the README and on the website): `https://apps.obtainium.imranr.dev/redirect?r=obtainium://add/https://github.com/benisalla/glitch`
- Badge: use `assets/graphics/badge_obtainium.png` from the Obtainium repo, **hosted in our repo** and not hotlinked. Display it at about 161x48.

**Optional:** add a config to the crowdsourced catalog (https://apps.obtainium.imranr.dev) with a PR to https://github.com/ImranR98/apps.obtainium.imranr.dev.

**Needed from the repo:** public repo, and one APK per release. Keep a single APK asset, or give it a stable name like `glitch.apk`, so Obtainium does not ask which file to pick.

Sources: [Obtainium README](https://github.com/ImranR98/Obtainium), [Obtainium deep links](https://wiki.obtainium.imranr.dev/deep_links/).

## 4. Other free channels

| Channel | What it is | Glitch qualifies? | Steps | Pros | Cons |
|---|---|---|---|---|---|
| **GitHub Releases + website** (primary) | The APK on a tagged release, linked from the website. | Yes, after blockers 1–3. | Publish the release, link the stable URL, add install steps and the SHA-256 of the APK. | Full control, no review, works with Obtainium. | Users see the "unknown sources" prompt. No discovery. |
| **AlternativeTo** | A catalog of "alternatives to X". Listing only, no hosting. | Yes. | Create an account and verify the email, then User icon > "Suggest new application". Fill in platforms, license (open source), description and tags, and put links only in the URL fields. It goes into a review backlog. An optional $5 fee buys priority review. | Good SEO, and people searching "app drawer shuffle" or "open source launcher alternatives" find it. | A slow queue. No promo in the profile, no links in the description, no vote incentives. |
| **Uptodown** | A large APK store with a free developer console (uptodown.dev). | Probably. | Sign up at https://www.uptodown.dev/, then upload the signed APK. I could not read the current terms (the page did not render), so read them before you upload. | A big audience in Spanish- and Portuguese-speaking markets. | Download pages carry ads, and it is not a FOSS-focused audience. Uptodown is not on Google's list of verification "participating stores" today, but the 2027 global rollout covers all apps on certified devices. |
| **APKMirror** | Mirrors APKs, mostly of Play apps, and is curated. | **Unlikely for now.** | The FAQ says it is selective: new apps with no track record are likely to be rejected, and it must be able to verify the APK's legitimacy. Revisit after Google Play launch. | Trusted name, with signature checks. | Not a place to launch. |
| **r/fossdroid, Lemmy and Mastodon FOSS communities** | Discovery, not distribution. | Yes. | See `posts.md`. | The right audience. | They expect a public repo and a license line. |

Sources: [AlternativeTo FAQ](https://alternativeto.net/faq/), [Uptodown developer console](https://www.uptodown.dev/), [APKMirror FAQ](https://www.apkmirror.com/faq).

## 5. Google Android developer verification (sideloaded apps)

**Status as of 2026-10-13, from Google's official pages:**
- **Timeline:** early access November 2025. Verification opened to all developers in March 2026. In August 2026 the limited-distribution accounts and the "advanced flow" for power users launched. **Enforcement started on 30 September 2026** in **Brazil, Indonesia, Singapore and Thailand**, on certified devices running Android 7+. **Global rollout is planned for 2027**, for all apps on certified devices.
- **Scope today:** enforcement only covers installs **from participating stores**: Google Play, Samsung Galaxy Store, Xiaomi GetApps, HONOR App Market, OPPO App Market, vivo V-Appstore and Transsion Palm Store. The FAQ, updated 15 July 2026, says: "If you distribute your app through other stores, or if users sideload your app directly, these new verification requirements won't apply to your app yet." So **today a GitHub APK, F-Droid, IzzyOnDroid and Obtainium install normally**, including in the four countries.
- **adb** installs are always allowed without verification.
- **Advanced flow** (for users installing apps from unverified developers): a one-time setup. Turn on developer mode, confirm you are not being coached, restart and reauthenticate, wait **24 hours**, then confirm with biometrics or PIN. It can stay on for 7 days or indefinitely. A warning with "Install anyway" still appears.

**Cost and account types:**
- **Full distribution, Android Developer Console** (for apps outside Play only): **$25 one-time fee**. Individuals verify with a **government-issued photo ID and a proof of address**, plus a private email and phone number confirmed by OTP. Google says it "typically takes about 10 minutes" to submit.
- **Google Play Console:** if you publish on Play, you register through Play Console (the $25 Play fee). Apps that use Play App Signing are claimed automatically. **Because Glitch is going to Play later, one Play Console account covers both channels. Do not pay twice.**
- **Limited distribution:** free and needs no ID, but it only covers **up to 20 devices**. That suits testers, not public release. You can upgrade from limited to full, but not the other way.
- Package registration: register `com.benisalla.glitch` and prove ownership by uploading an APK signed with your key and adding the key's SHA-256 fingerprint. You can register several keys per package. **If you lose the signing key, you cannot register the package. Back it up.**
- **Open-source stores that re-sign** (F-Droid without reproducible builds): register your own key, then download the F-Droid-signed APK after publication and add **its** SHA-256 to the same package. For an existing app with 50+ installs, Google asks for an `adi-registration.properties` snippet in `assets/`. If F-Droid ships your own signed APK (reproducible builds), there is nothing extra to do. Registration is optional for open-source platforms, but without it users need the advanced flow.

**What the owner should do:**
1. Nothing is blocking now. A GitHub/F-Droid launch in October 2026 is not affected.
2. Before the 2027 global rollout: register the developer identity once, through Play Console when the Play listing is created, or through the Android Developer Console ($25) if Play is still far off. Then register `com.benisalla.glitch` with the release key's SHA-256, plus F-Droid's key if F-Droid re-signs.
3. Keep the release keystore backed up in two places.
4. Re-check https://developer.android.com/developer-verification before each release. Dates and the list of countries have changed several times.

Sources: [Android developer verification (overview and timeline)](https://developer.android.com/developer-verification), [Guides: countries and participating stores](https://developer.android.com/developer-verification/guides), [FAQ](https://developer.android.com/developer-verification/guides/faq), [Full distribution: identity documents](https://developer.android.com/developer-verification/guides/full-distribution), [Open source app registration](https://developer.android.com/developer-verification/guides/open-source-app-registration), [Play Help: Understanding Android developer verification ($25 fee, 20-device limited accounts)](https://support.google.com/android-developer-console/answer/16561738?hl=en). Secondary: [Android Authority timeline](https://www.androidauthority.com/android-sideloading-changes-timeline-3679204/), [TechRepublic on the 24-hour wait](https://www.techrepublic.com/article/news-google-android-sideloading-24-hour-wait/).

## 6. Dependency audit (`gradle/libs.versions.toml`, `app/build.gradle.kts`)

| Dependency | Source | License | FOSS? |
|---|---|---|---|
| Android Gradle Plugin 9.4.1 (`com.android.application`) | Google Maven | Apache-2.0 | Yes (build tool) |
| Kotlin 2.4.21 + Compose compiler plugin | Maven Central | Apache-2.0 | Yes |
| androidx.core:core-ktx 1.19.1 | Google Maven | Apache-2.0 | Yes |
| androidx.activity:activity-compose 1.13.0 | Google Maven | Apache-2.0 | Yes |
| androidx.lifecycle runtime-compose / viewmodel-compose 2.11.0 | Google Maven | Apache-2.0 | Yes |
| Compose BOM 2026.09.00: ui, ui-graphics, ui-tooling(-preview), material3 | Google Maven | Apache-2.0 | Yes |
| Test only: JUnit 4.13.2 (EPL-1.0), kotlinx-coroutines-test 1.11.0 (Apache-2.0), androidx.test ext-junit / runner, compose ui-test (Apache-2.0) | Maven Central / Google Maven | as listed | Yes, and not shipped in the APK |

No Play Services, Firebase, Crashlytics, analytics, ads or network libraries. **Result: all dependencies are FOSS.** The only APK-level items to clean up are the Dependency Info block (blocker 4) and the `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` that AndroidX Core adds. That permission is signature-level, harmless, and already explained in the README.

## 7. Recommended order

1. **GitHub Releases + website + Obtainium link.** Available as soon as blockers 1–3 are fixed, and needs no approval.
2. **F-Droid merge request.** Fix blockers 4–6, then submit in launch week 1. Approval takes days to weeks.
3. **AlternativeTo listing.** Submit on day 1, because the queue is slow.
4. Later: Uptodown (optional), Google Play (with verification), APKMirror only after Play.
5. **IzzyOnDroid: skip** unless the owner decides to make a fully transparent request that will probably be declined under its AI policy.
