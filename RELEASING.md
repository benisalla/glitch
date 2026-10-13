# Releasing Glitch

Pushing a tag like `v1.0.1` runs `.github/workflows/release.yml`. It runs the same checks as CI, checks that the tag matches `versionName`, builds and signs the APK and AAB, verifies the signature, checks that a second clean build is byte-identical, and publishes a GitHub Release with these files:

| File | What it is |
| --- | --- |
| `glitch.apk` | The APK people install. Always-latest link: <https://github.com/benisalla/glitch/releases/latest/download/glitch.apk> |
| `glitch-<version>.apk` | The same APK, with the version in its name |
| `glitch-<version>.aab` | Signed bundle for app stores (Google Play) |
| `glitch-<version>-mapping.txt` | R8 mapping, to read obfuscated crash stack traces |
| `SHA256SUMS.txt` | SHA-256 checksums of the files above |

The release notes are `store/fastlane/metadata/android/en-US/changelogs/default.txt` plus install instructions.

A version with a suffix, such as `1.1.0-beta.1`, is published as a pre-release, so the always-latest link skips it.

To test everything except publishing, run the workflow by hand: **Actions > Release > Run workflow**. The signed files are kept as the `glitch-release` workflow artifact for 7 days.

## One-time setup

### 1. Create the release key

```sh
keytool -genkeypair -v -keystore glitch-release.jks -alias glitch \
  -keyalg RSA -keysize 4096 -validity 10000
```

Use a strong password. Keep `glitch-release.jks` **outside** the repository and never commit it.

**Back it up now**, together with its passwords, in at least two safe places (for example a password manager and an encrypted USB drive). Android only installs an update if it is signed with the same key as the installed app. If you lose this key, nobody who installed Glitch from GitHub can update to a new version. They would have to uninstall and reinstall.

### 2. Add the four repository secrets

```sh
base64 -w0 glitch-release.jks | gh secret set GLITCH_KEYSTORE_BASE64 -R benisalla/glitch
gh secret set GLITCH_KEYSTORE_PASSWORD -R benisalla/glitch   # prompts for the value
gh secret set GLITCH_KEY_ALIAS -R benisalla/glitch           # glitch
gh secret set GLITCH_KEY_PASSWORD -R benisalla/glitch        # same as the store password if you did not set a separate one
```

On macOS use `base64 -i glitch-release.jks | tr -d '\n'` instead of `base64 -w0`. `gh secret set` without `--body` asks for the value, so passwords do not end up in your shell history.

If any secret is missing, the release fails with a message naming it. It never publishes an unsigned APK.

## Each release

1. In `app/build.gradle.kts`, increase `versionCode` by 1 and set `versionName` (for example `1.0.1`).
2. Update `store/fastlane/metadata/android/en-US/changelogs/default.txt` with what changed. Use a few short lines in plain language, because this text becomes the release notes.
3. Merge these changes to `main` through a pull request, as usual.
4. Tag the merged commit on `main` and push the tag:
   ```sh
   git switch main && git pull
   git tag v1.0.1 && git push origin v1.0.1
   ```
5. Watch **Actions > Release**. When it finishes, the release is on the Releases page and the always-latest link serves the new APK.

If the tag does not match `versionName`, the workflow stops before building. To fix it, delete the tag (`git push origin :refs/tags/v1.0.1 && git tag -d v1.0.1`), fix the version, then tag again. If a release was already published, do not reuse its version. Bump to the next one instead.

## Signing and Google Play

The GitHub APK is signed with your key. If Glitch later goes to Google Play, the Play version must be signed with **the same key**, or people who installed from GitHub cannot update from Play (Android sees a different signature and refuses the update).

When you create the app in Play Console and set up Play App Signing, do **not** accept a Google-generated app signing key. Choose to use your own key and upload this release key, following Play Console's instructions (it gives you the PEPK tool to export the key, encrypted). Then create a separate upload key for Play if you want one.

## Safety rules built into the pipeline

- Only code on `main` is signed: a tag must point at a commit on `main`, and a dry run must be started from `main`. Merge first, then tag the merge commit.
- Pre-releases (`v1.2.0-beta.1`) never become "latest". A patch to an older line (e.g. `v1.0.2` after `v1.1.0`) is published without taking over the always-latest `glitch.apk` link.
- Before adding any collaborator with write access, also: (1) move the four secrets into a GitHub Environment named `release` limited to `v*` tags and `main`, with yourself as required reviewer, and add `environment: release` to the `build` job; (2) add a tag ruleset so only you can create `v*` tags.
