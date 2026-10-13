# Play Console answers: Data safety, App content, category

Copy these answers into Play Console. They describe the app as built today (1.1.0): no permissions, no `INTERNET`, no SDKs that collect data, package visibility only through the `<queries>` entry for `MAIN`/`LAUNCHER`. From 1.1.0 Glitch can optionally be the home app (a `HOME` intent filter, which is not a permission), so the answers below are unchanged. **If any of that changes, review this file before the next release.**

Play Console menu names change from time to time. If a question below is worded a little differently in the console, answer by meaning.

---

## 1. Data safety (Policy > App content > Data safety)

### Data collection and security
| Question | Answer |
|---|---|
| Does your app collect or share any of the required user data types? | **No** |

Answering **No** ends the questionnaire. Play skips the follow-up questions about encryption in transit and data deletion, because nothing is collected. (Encryption in transit would not apply anyway: the app has no network access.)

**Why "No" is correct.** The app reads the list of launchable apps (name and icon) to draw its menu, and stores your dock choices (up to 4 app names) on the phone. Google's Data safety guidance says that data accessed and processed only on the device, and never sent off it, does not count as "collected". The app has no `INTERNET` permission, so it cannot send anything off the device. Nothing is shared with third parties.

### Resulting store label
The listing will show "No data collected" and "No data shared with third parties".

---

## 2. App content (Policy > App content)

| Section | Answer |
|---|---|
| **Privacy policy** | URL of the hosted `docs/privacy-policy.md` (see `store/README.md`, step 5), e.g. `https://<github-user>.github.io/<repo>/privacy-policy.html` |
| **App access** | All functionality is available without special access (no login, no account). |
| **Ads** | **No**, my app does not contain ads. |
| **Content rating** | Complete the IARC questionnaire. See below. |
| **Target audience and content** | See below. |
| **News app** | No. |
| **Data safety** | See section 1. |
| **Government app** | No. |
| **Financial features** | My app does not provide any financial features. |
| **Health apps** | My app does not have any health features. |
| **Advertising ID** | **No**, the app does not use an advertising ID. (It does not declare `com.google.android.gms.permission.AD_ID` and has no ads or analytics SDK.) |
| **Sensitive permissions / Permissions declarations** | None needed. The app requests no permissions, also when it is the home app. Being selectable as a home app needs no Play declaration. |
| **Package visibility (`QUERY_ALL_PACKAGES`)** | **No declaration needed.** The app does not request `QUERY_ALL_PACKAGES`. It uses a narrow `<queries>` intent filter for `MAIN`/`LAUNCHER`, which needs no Play declaration. |
| **Accessibility API, foreground service, exact alarms, etc.** | Not used, no declarations. |

### Content rating questionnaire (IARC)
1. Email address: `ismailbenalla52@gmail.com`.
2. Category: **"All Other App Types"** (also listed as utility/productivity/other; it is not a game, social or reference app).
3. Answer **No** to every content question: violence, fear, sexuality, nudity, profanity/crude humor, drugs, alcohol, tobacco, gambling, simulated gambling, controlled substances.
4. User interaction / data questions:
   - Can users interact or exchange content with each other? **No**
   - Does the app share the user's current physical location with other users? **No**
   - Does the app allow users to purchase digital goods? **No**
   - Does the app contain unrestricted internet access (e.g. a browser)? **No**
   - Is the app a web browser or search engine? **No**
5. Expected result: the lowest rating in each region (e.g. ESRB Everyone, PEGI 3, IARC 3+).

### Target audience and content (suggestion)
- Target age groups: **13-15, 16-17, 18 and over**. Do **not** tick any group under 13: that would bring the app under the Families policy and its extra requirements, and the app is not designed for children.
- "Could your store listing unintentionally appeal to children?" **No.** The listing is plain text and simple shapes, no cartoon characters or child-oriented themes.

---

## 3. Store settings (Grow > Store presence > Store settings)

| Field | Suggestion |
|---|---|
| App or game | App |
| **Category** | **Personalization** (recommended from 1.1.0) or **Tools**. From 1.1.0 Glitch can be set as the home screen, and Play lists home-screen launchers under Personalization. It also still works as a normal app, so Tools remains acceptable. Changing the category later is allowed. |
| Tags | Optional. Pick up to 5 from Play's list that describe a simple utility. A launcher/home-screen tag is fine from 1.1.0 (it is an optional mode). Skip any tag that implies a focus/productivity benefit. |
| Contact email (public) | `ismailbenalla52@gmail.com` |
| Website (optional) | The GitHub Pages root, e.g. `https://<github-user>.github.io/<repo>/` |
| Phone (optional) | Leave empty. |
| External marketing | Your choice; it does not affect the app. |
