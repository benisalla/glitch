# Play Store release kit

Everything needed to publish Glitch on Google Play, plus a step-by-step checklist.

## What is in this folder
| Path | What it is |
|---|---|
| `fastlane/metadata/android/en-US/title.txt` | App name on Play (max 30 chars). |
| `fastlane/metadata/android/en-US/short_description.txt` | Short description (max 80 chars). |
| `fastlane/metadata/android/en-US/full_description.txt` | Full description (max 4000 chars). |
| `fastlane/metadata/android/en-US/changelogs/default.txt` | "What's new" text for 1.0.0 (max 500 chars). |
| `graphics/icon-512.png` | Hi-res app icon, 512 x 512, 32-bit PNG. |
| `graphics/feature-graphic.png` | Feature graphic, 1024 x 500, 24-bit PNG. |
| `brand/glitch-icon.png` | The Glitch logo: the single source for every icon and graphic. |
| `graphics/marketing/*.png` | The 5 phone screenshots to upload, built by `tools/make_screenshots.py`. |
| `tools/make_graphics.py` | Builds the launcher icon and the graphics above from the logo. |
| `data-safety.md` | Exact answers for Data safety, App content, content rating and category. |
| `../docs/privacy-policy.md` | Privacy policy (needs to be hosted at a public URL, see step 5). |

The text files use the [fastlane `supply`](https://docs.fastlane.tools/actions/supply/) layout, so they can be uploaded automatically later. For a first release, copy and paste them into Play Console by hand; fastlane is not required.

The Play title is "Glitch: Break the Autopilot" (see `fastlane/metadata/android/en-US/title.txt`); the name under the icon on the phone is "Glitch" (from `strings.xml`).

### Regenerating the icon and graphics
```
python3 store/tools/make_graphics.py
python3 store/tools/make_screenshots.py   # run after make_graphics.py: it reads glitch-mark.png
```
Both scripts need Python 3, Pillow and the Noto Sans fonts (`fonts-noto` on Debian/Ubuntu). To change the logo, replace `store/brand/glitch-icon.png` and run both scripts. `make_graphics.py` writes:
- `app/src/main/res/mipmap-*/ic_launcher_{foreground,monochrome}.png`: the adaptive-icon layers for every density. The mark is kept inside the 66 dp safe zone, and the monochrome layer is for Android 13+ themed icons.
- `ic_launcher_background` in `app/src/main/res/values/colors.xml`: the logo navy.
- `store/brand/glitch-mark.png`, `store/graphics/icon-512.png` and `store/graphics/feature-graphic.png`.

The design is a white ring (the loop of autopilot) broken by a tilted gold tile (the glitch). Edit the logo or the scripts, not the generated files.

---

## Release checklist

Steps marked **(owner)** need your Google account, money, identity or private keys and can only be done by you.

### 1. Google Play developer account (owner)
- [ ] Sign up at <https://play.google.com/console/signup>. Choose **personal** account (unless you have a registered organization).
- [ ] Pay the one-time US$25 registration fee.
- [ ] Complete identity verification (government ID) and verify your contact email and phone number.
- [ ] Verify access to a physical Android device when Play Console asks (done through the Play Console mobile app).

### 2. Create an upload key (owner, once)
This is the same release key the GitHub release pipeline uses (see `RELEASING.md`). Use it as the Play app signing key too (step 4), so people who installed Glitch from GitHub can also receive updates from Play.
```
keytool -genkeypair -v \
  -keystore ~/keys/glitch-upload.jks \
  -alias upload -keyalg RSA -keysize 2048 -validity 10000
```
- [ ] Store the `.jks` file **outside the repo** and back it up (password manager or encrypted drive). Never commit it.
- [ ] Create `keystore.properties` in the repo root (it must stay untracked; it is a secret):
  ```
  storeFile=/home/<you>/keys/glitch-upload.jks
  storePassword=...
  keyAlias=upload
  keyPassword=...
  ```

### 3. Build the signed release bundle (AAB)
- [ ] Check `app/build.gradle.kts`: `versionName = "1.0.0"` and a `versionCode` higher than any bundle already uploaded (start at 1). Every later upload needs a higher `versionCode`.
- [ ] Check `targetSdk` meets Play's current requirement for new apps (API 36 from 31 Aug 2026; the app targets 36).
- [ ] Build:
  ```
  ./gradlew :app:bundleRelease
  ```
  Output: `app/build/outputs/bundle/release/app-release.aab`. If signing is only partly configured, the build stops and names the missing keys.
- [ ] Check it is signed (must print `jar verified.`, not `jar is unsigned.`):
  ```
  jarsigner -verify app/build/outputs/bundle/release/app-release.aab
  ```
- [ ] Optional sanity check: install a release build on your phone and open a few apps from the menu.

### 4. Create the app and turn on Play App Signing (owner)
- [ ] Play Console > **Create app**: name "Glitch: Break the Autopilot", default language English (United States), App, Free. Accept the declarations.
- [ ] When you upload the first AAB, set up **Play App Signing** with **your own key**: choose "Use a different app signing key" / "Export and upload a key from Java keystore" and follow Play's steps with the release key from `RELEASING.md`. If you accept a Google-generated key instead, Play installs and GitHub installs have different signatures and can't update each other.

### 5. Host the privacy policy (owner)
Play needs a public URL. GitHub Pages is free once the repository is public:
- [ ] Make the repository public first: repo **Settings > General > Danger Zone > Change visibility > Public** (or `gh repo edit benisalla/glitch --visibility public --accept-visibility-change-consequences`). While it is private, Pages needs a paid plan and the README CI badge shows as broken to visitors.
- [ ] On GitHub: repo **Settings > Pages > Build and deployment**: Source "Deploy from a branch", Branch `main`, folder `/docs`. Save.
- [ ] After a minute the policy is at `https://benisalla.github.io/glitch/privacy-policy.html`. Open it in a private browser window to confirm it loads.
- [ ] Alternative: a public Gist (`gh gist create --public docs/privacy-policy.md`) or a Google Sites page.

### 6. Fill in the store listing
Play Console > **Grow > Store presence > Main store listing**:
- [ ] App name, short description, full description: paste from `fastlane/metadata/android/en-US/`.
- [ ] App icon: `graphics/icon-512.png`.
- [ ] Feature graphic: `graphics/feature-graphic.png`.
- [ ] Phone screenshots: upload the 5 ready-made images in `store/graphics/marketing/` in order (01 to 05). See "Screenshots" below to regenerate them.
- [ ] Store settings (category, contact email): see `data-safety.md` section 3. Category: **Tools**.

### 7. App content (Policy > App content)
- [ ] Privacy policy URL (step 5).
- [ ] App access, Ads (**No**), Content rating, Target audience, Data safety (**No data collected**), Advertising ID (**No**), the rest: copy the answers from `data-safety.md`.

### 8. Internal testing first
- [ ] **Testing > Internal testing** > Create release > upload `app-release.aab`. Release notes: `changelogs/default.txt`.
- [ ] Add yourself (and anyone helping) as testers via an email list, open the opt-in link on your phone, install from Play.
- [ ] Check on a real phone (ideally a Samsung): icon looks right (also with themed icons on), apps are listed, tapping opens them, returning gives a new order. Check the **Pre-launch report** in Play Console for crashes.

### 9. Closed testing: 12 testers for 14 days (owner, required for new personal accounts)
Personal developer accounts created after 13 November 2023 must run a **closed test with at least 12 testers who stay opted in for at least 14 consecutive days** before they can apply for production access. (Checked against Play Console Help, "App testing requirements for new personal developer accounts", in October 2026. Re-check the page before starting, the numbers have changed before.)
- [ ] **Testing > Closed testing** > create a track, upload the same AAB (or a newer one).
- [ ] Add at least 12 testers (Google Group or email list). Allow a margin: invite 15 or more, in case someone opts out.
- [ ] Testers must opt in through the link and install the app. Ask them to keep it installed for the whole 14 days and to open it now and then.
- [ ] Fix anything they report and upload updated builds to the same track if needed.
- [ ] After 14 days: **Dashboard > Apply for production** and answer the questions about your test. Google reviews the application (usually up to about 7 days).

Accounts registered as an organization, or personal accounts created before that date, can skip this step.

### 10. Production release
- [ ] **Production** > Create release > promote the tested bundle (or upload it), add the release notes.
- [ ] Countries: all, or the ones you choose.
- [ ] **Publishing overview** > Send for review. First reviews can take several days.
- [ ] After approval, the app goes live (or waits for you to publish, if you turned on managed publishing).

---

## Screenshots to capture
Ready to upload: `store/graphics/marketing/01-hero.png` … `05-privacy.png` (1080 x 1920). They are built from the raw captures in `store/screenshots/` by `python3 store/tools/make_screenshots.py`; re-capture those and re-run the script whenever the UI changes.

Play requirements for phone screenshots: **at least 2** (up to 8), PNG or JPEG, max 8 MB each, 16:9 or 9:16, each side between 320 px and 3840 px. Use **1080 x 1920 or larger** in portrait; at least 4 such screenshots are needed for the app to be eligible for some Play promotion surfaces.

Use an emulator or a phone with only common apps installed so no personal app names appear. Open Glitch, then capture the two raw screenshots the generator reads, one in light mode and one in dark mode:
```
adb shell cmd uimode night no
adb exec-out screencap -p > store/screenshots/phone-1-light.png
adb shell cmd uimode night yes
sleep 2   # let the menu redraw in dark mode
adb exec-out screencap -p > store/screenshots/phone-2-dark.png
adb shell cmd uimode night no
python3 store/tools/make_screenshots.py
```
Switching the theme is a configuration change, so the menu keeps its order; that is expected. The script writes the five marketing images to `store/graphics/marketing/`.

Do not add text overlays that claim benefits such as better focus or memory.

## Things only the owner can do
- Create and pay for the Play developer account, verify identity and device.
- Create and safely back up the upload keystore and its passwords; write `keystore.properties`.
- Turn on GitHub Pages for the privacy policy (or host it elsewhere).
- Recruit 12+ closed testers and keep them for 14 consecutive days.
- Submit forms, apply for production, and press "Send for review".
