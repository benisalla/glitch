# Shuffling the Samsung app drawer: what is possible

Research date: 2026-10-13. Scope: Samsung Galaxy, One UI 6/7/8 (Android 14–16), stock One UI Home (`com.sec.android.app.launcher`).
Question from the owner: keep One UI Home exactly as it is and only randomize the order of apps in the app drawer (and maybe the home screen), for example on every unlock. No replacement launcher.

## Bottom line

1. **No supported way exists.** Android has no public API that lets one app reorder another launcher's drawer or home screen. `LauncherApps` and `ShortcutManager` only read, launch or *request* new items.
2. **One UI Home locks its layout database.** Firmware checks show the layout provider `content://com.sec.android.app.launcher.settings` needs `com.samsung.android.launcher.permission.READ/WRITE_SETTINGS`, both `signatureOrSystem` (verified in One UI 8 firmware). Third-party apps cannot get these.
3. **ADB/Shizuku does not help.** The shell user (`com.android.shell`) does not request those Samsung permissions (checked on One UI 5 and One UI 8 firmware). The layout backup/restore broadcasts need another `signatureOrSystem` permission. `adb shell content …` against the launcher fails, and `cmd shortcut` has no layout commands.
4. **Root would work, but it isn't realistic.** The drawer's custom order lives in the `appOrder` table of `launcher.db`. Root could rewrite it. But One UI 8 removed bootloader unlocking (US models never had it), and rooting permanently trips Knox (Secure Folder, Samsung Wallet and Health stop working). No existing Xposed module shuffles the drawer.
5. **Accessibility automation is possible in theory, unusable in practice.** A bot could drag icons in custom-order edit mode, but it would take over the screen for minutes, break with every One UI update, and draw heavy Play policy scrutiny.
6. **Good Lock / Home Up has no shuffle or scriptable layout file.** Home Up has grid, folder and backup/restore options. Its backup format is undocumented, and I found nothing that edits it.
7. **The built-in Sort menu has only Custom order and Alphabetical.** There is no random option.
8. **Samsung closed the Edge panel path.** One UI 7 disabled third-party Edge panels (Samsung's own support page says so).
9. **What *does* work on a stock phone:** a Glitch **home-screen widget** that shows shuffled app icons inside One UI Home. Second best: Glitch-owned **pinned shortcuts** whose target app is rewritten on each shuffle. The real drawer stays in its normal order either way.
10. **Recommendation:** be honest with the owner: "shuffle the real Samsung drawer" can't ship. Offer a "Shuffled apps" widget (with an optional shortcut-slot variant) that sits on the normal One UI home screen. It needs owner approval as a scope change.

## Comparison table

| Approach | Works on stock Samsung without root? | Needs PC/ADB setup? | Survives reboot? | Play Store ok? | User experience | Effort |
|---|---|---|---|---|---|---|
| Public APIs (`LauncherApps`, `ShortcutManager`, AppWidget) to reorder the drawer | **No**. No such API exists | – | – | – | – | – |
| One UI Home layout provider (`com.sec.android.app.launcher.settings`) | **No**. `signatureOrSystem` permission | – | – | – | – | – |
| Shizuku / ADB shell (`content`, `am broadcast`, `cmd shortcut`) | **No**. Shell lacks the launcher permissions | Yes (and still fails) | – | – | – | – |
| Smart Switch / Samsung Cloud / Home Up backup → edit → restore | Unverified; very likely no (format undocumented, possibly encrypted; restore is manual) | PC for Smart Switch | Yes (it's a one-off restore) | – | Manual, minutes per shuffle, risks losing the layout | High, research-grade |
| Root + edit `launcher.db` `appOrder` / LSPosed hook | Yes, but only on rooted phones | Yes (unlock + flash) | Yes | No (needs root; can't be a normal user feature) | Exactly what the owner wants | M to build, but **One UI 8 blocks bootloader unlock**, Knox trips permanently |
| AccessibilityService dragging icons in edit mode | Technically yes | No (but Android 13+ "restricted settings" step for sideloads) | Yes | Risky: declaration, video, disclosure; likely rejected or flagged | Bad: hijacks the screen for minutes, fragile | XL, never reliable |
| Knox SDK (enterprise) | Only on Knox-managed devices; no drawer-order API found | MDM enrollment | Yes | Not a consumer product | N/A | – |
| **Glitch widget with shuffled icons on the One UI home screen** | **Yes** | No | Yes | **Yes** | Good: same launcher and drawer; a grid of shuffled apps on a home page | M |
| Glitch pinned-shortcut "slots" rewritten on each shuffle | Yes (behavior on One UI needs device testing) | No | Yes | Yes | Looks like normal icons/folder; may show a Glitch badge; one-time pin dialogs | M |
| Third-party Edge panel with shuffled apps | **No** on One UI 7+ (feature disabled by Samsung) | – | – | – | – | – |
| Glitch as the home app (already exists in v1.1) | Yes | No | Yes | Yes | Replaces One UI Home, which the owner explicitly does not want | Done |

## Details

### 1. Public APIs: no app can reorder another launcher

- **`LauncherApps`** lets a launcher *read* activities and shortcuts and *start* them. Its pin-request API (`PinItemRequest`) works the other way: the launcher *accepts* items that apps ask to pin. It has no method to move or order items in any launcher. https://developer.android.com/reference/android/content/pm/LauncherApps
- **`ShortcutManager.requestPinShortcut` / `AppWidgetManager.requestPinAppWidget`** ask the launcher to add *one new item owned by the calling app*. The user confirms, and the launcher picks the position. Apps can *update* their own pinned shortcuts (`updateShortcuts`), but cannot move them. Updates are rate-limited while the app is in the background (the limit resets when the app comes to the foreground). The docs say nothing about apps controlling placement. https://developer.android.com/develop/ui/views/launch/shortcuts/managing-shortcuts
- **AOSP Launcher3's favorites provider** is guarded by `${applicationId}.permission.READ_SETTINGS / WRITE_SETTINGS`, both declared `signatureOrSystem`. Many old apps list `com.android.launcher.permission.WRITE_SETTINGS` or `com.sec.android.app.launcher.permission.WRITE_SETTINGS` in their manifests. Since Android 6, a non-system app can never be granted these. https://android.googlesource.com/platform/packages/apps/Launcher3/+/refs/heads/main/AndroidManifest-common.xml
- Android has no standard "layout API" for launchers. Each launcher stores its layout privately. So it is a launcher-by-launcher problem, and One UI Home doesn't open its layout to others (next section).

### 2. Samsung specifically

**One UI Home's provider and permissions (verified from firmware).**
I pulled `TouchWizHome_2017.apk` (One UI Home **17.5.05.29**) and `Shell.apk` from the Galaxy S25 Ultra firmware `S938BXXU9CZDP` (Android 16 / One UI 8). The firmware dump is https://dumps.tadiphone.dev/dumps/samsung/pa3q. I read the manifests with `aapt2`:

- `com.android.launcher3.LauncherProvider`, authority `com.sec.android.app.launcher.settings`, is `exported=true`. It has `readPermission=com.samsung.android.launcher.permission.READ_SETTINGS` and `writePermission=com.samsung.android.launcher.permission.WRITE_SETTINGS`. Both permissions are declared with `protectionLevel=0x3` (`signature|privileged`, i.e. `signatureOrSystem`).
- The legacy name `com.sec.android.app.launcher.permission.WRITE_SETTINGS` is still requested by many third-party apps. Code search shows it in Alipay, Feishu, cleaners and others. That is a leftover from old TouchWiz badge/shortcut hacks, and it grants nothing.
- Layout backup/restore goes through `com.sec.android.app.launcher.bnr.SmartSwitchReceiver`. Its actions include `com.sec.android.intent.action.REQUEST_BACKUP_HOMELAYOUT`, `REQUEST_RESTORE_HOMELAYOUT` and `REQUEST_REBUILD_HOMELAYOUT`. The receiver is protected by `com.sec.permission.BACKUP_RESTORE_HOMESCREEN`, also `signatureOrSystem`.
- The app has `allowBackup=false`, so `adb backup` can't extract its data.
- Strings in the launcher's dex code show the storage: `launcher.db` with tables `favorites`, `workspaceScreens` and **`appOrder`** (the drawer's custom order), plus backup files `/BackupRestore/homescreen.exml`. The drawer sort setting has the values `ALPHABETIC` and a custom order (`applistSortType`, `apps-sort-type`).
- A separate decompiled newer build, One UI Home 18.0.00.67 (targetSdk 37), declares the same provider and permissions as `signatureOrSystem`: https://github.com/VictorVasquezZT2005/Launcher3QuickStep/blob/main/oneui/AndroidManifest.xml
- Past bug: CVE-2022-33715 / SVE-2022-0897 (Oversecured). `LauncherProvider.call("put_restore_file", …)` let any app write files through path traversal. Samsung fixed it in August 2022. It shows the restore path exists, but using such a bug would be a security exploit, not a product feature. https://github.com/oversecured/Samsung_Vulnerabilities (folder "041. One UI Home — Overwriting of arbitrary files"), https://nvd.nist.gov/vuln/detail/CVE-2022-33715

**Third-party apps that reorder One UI Home.** I found **none**. Search covered GitHub, XDA, Samsung Community and the Play Store. Forum threads about drawer order end with "use custom order by hand, Home Up, or another launcher". https://xdaforums.com/t/how-can-i-sort-the-app-drawer.4644729/ , https://eu.community.samsung.com/t5/galaxy-s25-series/one-ui7-app-drawer/td-p/11601231
Old apps that import layouts *from* TouchWiz did read `content://com.sec.android.app.launcher.settings/favorites`, for example Evie Launcher's `TouchWizLauncher` importer. That was possible on old Samsung builds before the permission was locked. They never wrote to it.

**Good Lock: Home Up** (package `com.samsung.android.app.homestar`).
- Home Up is Samsung-signed. One UI Home declares Home Up-specific hooks (`com.samsung.android.app.homestar.permission.HOMESTAR_DATA`, `content://com.samsung.android.app.homestar.provider`) and a `signatureOrSystem` plugin permission. That's why Home Up can do what third-party apps can't.
- Documented features: home and apps-screen grid, apps-screen background blur, label hiding, pop-up folders and folder styles, Task Changer, Share Manager. https://addrom.com/unlock-deep-samsung-customization-a-detailed-guide-to-home-up-good-lock/
  - One review says it can control "category grouping, header styles, and how apps sort when you add new ones". https://www.whistleout.com/CellPhones/Guides/samsung-good-lock-homeup I could not confirm the exact options.
  - **No source mentions a random/shuffle sort.**
- **Back up and restore**: name a backup, restore it from a list, and optionally run automatic backups. Sources disagree on the intervals: "1 hour / 1 day" versus "1 day / 1 week". Files are stored on internal storage. https://samlover.com/2025/02/06/good-lock-home-ups-new-feature-makes-home-screen-restoration-effortless/
  - **The file format, exact location, and whether it includes the apps-screen order are undocumented. I could not verify them.**
  - Nothing I found suggests these files can be edited and re-imported. Even if they could, restoring is a manual tap in Home Up, not scriptable, and not something a third-party app can trigger.

**One UI Home's built-in Sort.** The drawer menu (⋮ → Sort) offers **Custom order** or **Alphabetical order**. In One UI 7+, Alphabetical forces the vertical list layout, and Custom order gives horizontal pages that you arrange by hand. There is no random option and no API to set the order. https://beebom.com/how-switch-vertical-app-drawer-one-ui-samsung-galaxy/ , https://www.sammyfans.com/2024/12/15/one-ui-7-restore-app-drawer-pages-on-samsung-galaxy-smartphones/

**Smart Switch / Samsung Cloud layout backup.**
- Smart Switch PC backups include a `HOMESCREEN` folder. https://phone.fyicenter.com/1427_Smart_Switch_Backup_File_Location_on_Mac.html
- The launcher code shows the layout goes through `homescreen.exml` files. I found no public description of the format. The `.exml` name suggests encrypted XML, but that is **unverified**.
- In theory: back up, decrypt, shuffle the apps-screen entries, re-encrypt, restore. That is undocumented, needs a PC and a full restore each time, and could wipe the layout. Not a product.

### 3. Elevated paths

**Shizuku / ADB (shell UID 2000).**
- Shizuku runs code with the same rights as `adb shell`, the `com.android.shell` package. https://shizuku.rikka.app/
- I dumped the permissions of Samsung's `Shell.apk` from One UI 5 (A53, Android 13) and One UI 8 (S25 Ultra, Android 16). The only launcher-related ones are `com.android.launcher.permission.INSTALL_SHORTCUT/UNINSTALL_SHORTCUT`. There are **no** `com.samsung.android.launcher.permission.*` or `com.sec.permission.BACKUP_RESTORE_HOMESCREEN`.
- Consequences, by inference from the manifests; I couldn't run them on a device:
  - `adb shell content query --uri content://com.sec.android.app.launcher.settings/favorites` should fail with a `SecurityException`, and the same goes for `update`/`insert`.
  - `am broadcast -a com.sec.android.intent.action.REQUEST_RESTORE_HOMELAYOUT` won't reach the receiver, because shell doesn't hold the required permission.
  - `adb backup` is blocked (`allowBackup=false`).
  - `run-as` works only for debuggable apps.
  - `cmd shortcut` offers throttling, default-launcher and clear commands, but nothing for layout.
- **Verdict: not possible without root.** Worth one quick check on a real device to confirm the `SecurityException`, but the manifests leave little room.

**Root (Magisk/KernelSU + LSPosed).**
- A module could hook One UI Home's apps-list sorter. The dex contains `AlphabeticOrderItemListManager`, `applistSortType` and `sortItems`. Alternatively, a root script could rewrite `appOrder` in `/data/data/com.sec.android.app.launcher/databases/launcher.db` and restart the launcher.
- Modules that hook One UI Home do exist, for example:
  - **OneUIX** ("hide search bar on app screen" and more): https://github.com/SoClear/OneUIX
  - **samsung-feature-extension** (icon/label customization): https://github.com/fvffv/samsung-feature-extension
- **I found no module that shuffles or randomizes the drawer.** It would have to be written.
- Blockers for normal users:
  - Rooting requires an unlocked bootloader. **One UI 8 removed the OEM-unlock option**, even on global models (US models already lacked it). https://www.androidauthority.com/samsung-bootloader-unlocking-disabled-on-all-phones-3581366/ , https://9to5google.com/2025/07/26/samsung-galaxy-one-ui-8-bootloader-unlock/
  - Unlocking trips the Knox warranty fuse permanently: Secure Folder, Samsung Wallet/Pay and Health stop working, and Samsung says only a board replacement resets it. https://docs.samsungknox.com/admin/knox-platform-for-enterprise/kbas/kba-115013151307/
  - Hooks break with launcher updates from the Galaxy Store.
- **Verdict:** a hobby project for an already-rooted older phone. Never a feature for normal users or the Play Store.

### 4. Accessibility automation

- An `AccessibilityService` can do long-press-and-drag (`dispatchGesture` with continued strokes, API 26+). So in principle it could open the drawer, enter edit mode and move icons into a random permutation.
- Cost: with 60–100 apps across several pages, each move is a long-press, a drag and maybe a page edge-scroll. That's several minutes of the phone moving on its own. Doing it "on every unlock" would make the phone unusable right when the user wants it.
- It is fragile. Coordinates, timings and edit-mode behavior change with One UI versions, grid sizes, folders and fold/flip screens. One bad drop creates a folder.
- Android 13+ "restricted settings" makes sideloaded apps go through an extra unlock step before accessibility can be enabled.
- Play policy: Glitch is not an accessibility tool. It would need an in-app prominent disclosure with affirmative consent, a Play Console declaration and a demo video. Autonomous actions are prohibited, while deterministic scripted automation is allowed. A launcher-rearranging bot invites review trouble. https://support.google.com/googleplay/android-developer/answer/10964491
- **Verdict:** not worth building.

### 5. Alternatives that keep One UI intact

**A. Glitch "Shuffled apps" home-screen widget (best).**
- How it works:
  - An `AppWidgetProvider` with a collection layout (grid) shows N app icons and labels in a shuffled order.
  - The user places it once with the normal One UI widget picker, or Glitch calls `requestPinAppWidget`.
  - Each cell launches its app through a fill-in intent on a small Glitch trampoline activity, which also asks for the next shuffle.
- When it reshuffles:
  - After each launch from the widget.
  - Periodically (`updatePeriodMillis` minimum 30 min, or WorkManager).
  - When Glitch opens.
- Limitation: "Exactly on every unlock" is **not** available to a widget without a running process. `ACTION_USER_PRESENT` and `ACTION_SCREEN_ON` can't be received through the manifest since Android 8. https://developer.android.com/develop/background-work/background-tasks/broadcasts/broadcast-exceptions It would take a persistent foreground service with a notification, which is not recommended.
- Experience:
  - One UI Home, wallpaper, dock, drawer and other widgets stay exactly as they are.
  - The owner can put the widget on the first home page, or replace their icon grid with it, so the place they normally tap is shuffled.
  - The drawer itself stays unshuffled. The widget is the shuffled surface.
- Fits the existing code: it reuses `AppCatalog` and the shuffle logic.
- It's new scope (a widget), so it needs owner approval per AGENTS.md.
- No permissions needed. Play-safe.

**B. Pinned-shortcut "slots" (closest to real icons).**
- How it works:
  - Glitch asks the user to pin K shortcuts once (`requestPinShortcut`, one confirmation each), ideally into one One UI folder or a home page.
  - On each shuffle, Glitch calls `updateShortcuts` to change each slot's label, icon and target app.
  - This is allowed for pinned shortcuts, and the rate limit resets whenever Glitch is in the foreground.
- Result: what looks like ordinary icons/a folder whose contents rearrange.
- Caveats, all to be verified on a Galaxy:
  - One UI may draw a small Glitch badge on shortcuts.
  - Updated icons may not refresh immediately.
  - Some developers report One UI shortcuts reverting to the owning app's icon/label (Samsung developer forum thread "Shortcut icons losing label and icon on Samsung UI launcher"; the thread now returns 404, so this is unverified).
  - Uninstalling or clearing Glitch removes all slots.
- Good as an experiment once the widget exists.

**C. Edge panel with shuffled apps (dead).**
- Samsung's support page says One UI 7 disabled adding third-party developers' panels to the Edge panel. Already installed panels can only be restored, not newly added. https://www.samsung.com/az/support/mobile-devices/how-to-download-edge-panels-one-ui-7 (Azerbaijani; English coverage: https://www.sammyfans.com/2024/11/29/samsung-confirms-termination-of-edge-panels-downloads-with-one-ui-7/ )
- The built-in "Apps edge" is configured by hand, with no API.

**D. A "shuffled folder" widget.**
- This is variant A styled like a One UI folder: a 1×1 or 2×2 widget that opens a Glitch popup grid of shuffled apps. Third-party apps can't create real One UI folders.
- It's cheaper than A, but adds a tap and a Glitch screen. Close to "stuck in another app's menu", which the owner dislikes.

### 6. Other things checked

- **Samsung Modes and Routines / Bixby:** I found no action that reorders the home screen or apps screen.
- **Knox SDK:** enterprise only. It needs device admin/Knox licensing and offers `addHomeShortcut` and kiosk layouts, but I found no drawer-order API. Not applicable to consumers. https://docs.samsungknox.com/dev/knox-sdk/sample-app-tutorials/set-up-kiosk-mode/enable-kiosk-mode/
- **Glitch's own home mode (v1.1):** fully shuffles the home grid, but it is a replacement launcher, which the owner rejected.

## Recommendation for the owner

**What you asked for (real One UI drawer, randomized on each unlock) can't be shipped to normal users.** Samsung locks the launcher's layout behind system-only permissions, and even ADB/Shizuku doesn't have them. The only true path is rooting, which One UI 8 phones can no longer do and which permanently disables Knox features.

**Closest honest version: "same phone, a shuffled app surface on your home screen".**
1. Build a **Glitch "Shuffled apps" widget**:
   - Placed on your normal One UI home screen.
   - Reshuffles after each launch from it, whenever Glitch opens, and on a timer.
   - Leave One UI Home, drawer, dock and settings untouched.
   - Optional habit trick: move your usual home-screen icons into the drawer or a folder, so the widget becomes the main place you tap.
2. Then **prototype the pinned-shortcut slots** on a real Galaxy to see whether they look and refresh like native icons. If they do, offer them as "Shuffled folder" mode.

Caveats to tell the owner plainly:
- The Samsung app drawer itself stays in its own order.
- "Every unlock" becomes "after each use and every so often" unless we add a persistent notification service, which I advise against.
- The widget is a scope change and needs the owner's explicit approval.

## Open items to verify on a device
- `adb shell content query --uri content://com.sec.android.app.launcher.settings/favorites` → expect `SecurityException` (from the manifests; not run).
- One UI rendering of updated pinned shortcuts: badge, refresh, icon retention.
- Home Up backup file location/format and whether it includes the apps-screen order.

## Sources
- AOSP Launcher3 manifest: https://android.googlesource.com/platform/packages/apps/Launcher3/+/refs/heads/main/AndroidManifest-common.xml
- `LauncherApps`: https://developer.android.com/reference/android/content/pm/LauncherApps
- Shortcuts management: https://developer.android.com/develop/ui/views/launch/shortcuts/managing-shortcuts
- Implicit broadcast exceptions: https://developer.android.com/develop/background-work/background-tasks/broadcasts/broadcast-exceptions
- Play accessibility policy: https://support.google.com/googleplay/android-developer/answer/10964491
- Samsung firmware dumps (One UI 8, S938B): https://dumps.tadiphone.dev/dumps/samsung/pa3q ; (One UI 5, A536B): https://github.com/SelynCatto/samsung_a53x_dump
- Decompiled One UI Home 18.0: https://github.com/VictorVasquezZT2005/Launcher3QuickStep/tree/main/oneui
- Oversecured One UI Home CVE-2022-33715: https://github.com/oversecured/Samsung_Vulnerabilities , https://nvd.nist.gov/vuln/detail/CVE-2022-33715
- Home Up guides: https://addrom.com/unlock-deep-samsung-customization-a-detailed-guide-to-home-up-good-lock/ , https://www.whistleout.com/CellPhones/Guides/samsung-good-lock-homeup , https://samlover.com/2025/02/06/good-lock-home-ups-new-feature-makes-home-screen-restoration-effortless/
- One UI drawer sort: https://beebom.com/how-switch-vertical-app-drawer-one-ui-samsung-galaxy/ , https://www.sammyfans.com/2024/12/15/one-ui-7-restore-app-drawer-pages-on-samsung-galaxy-smartphones/ , https://xdaforums.com/t/how-can-i-sort-the-app-drawer.4644729/
- Edge panels in One UI 7: https://www.samsung.com/az/support/mobile-devices/how-to-download-edge-panels-one-ui-7 , https://www.sammyfans.com/2024/11/29/samsung-confirms-termination-of-edge-panels-downloads-with-one-ui-7/
- Bootloader/Knox: https://www.androidauthority.com/samsung-bootloader-unlocking-disabled-on-all-phones-3581366/ , https://9to5google.com/2025/07/26/samsung-galaxy-one-ui-8-bootloader-unlock/ , https://docs.samsungknox.com/admin/knox-platform-for-enterprise/kbas/kba-115013151307/
- LSPosed modules for One UI Home: https://github.com/SoClear/OneUIX , https://github.com/fvffv/samsung-feature-extension
- Shizuku: https://shizuku.rikka.app/
- Smart Switch backup layout: https://phone.fyicenter.com/1427_Smart_Switch_Backup_File_Location_on_Mac.html
- Knox SDK kiosk: https://docs.samsungknox.com/dev/knox-sdk/sample-app-tutorials/set-up-kiosk-mode/enable-kiosk-mode/
