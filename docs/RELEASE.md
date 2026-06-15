# Release build & signing

How to produce a signed, upload-ready App Bundle (AAB) for the Play Store.

## Signing config

`app/build.gradle.kts` reads release-signing secrets from `local.properties`
(git-ignored) or environment variables (CI). **No secret is committed.** If no
keystore is configured, the release build falls back to debug signing so it
still completes — but that artifact is **not** uploadable to Play.

Required keys:

| Key | Meaning |
|---|---|
| `RELEASE_STORE_FILE` | path to the keystore, relative to the repo root |
| `RELEASE_STORE_PASSWORD` | keystore password |
| `RELEASE_KEY_ALIAS` | key alias |
| `RELEASE_KEY_PASSWORD` | key password |

## One-time: create a keystore

```bash
keytool -genkeypair -v \
  -keystore vibecheck-release.jks \
  -alias vibecheck \
  -keyalg RSA -keysize 2048 -validity 10000
```

Keep this file and its passwords **safe and backed up** — losing them means you
can't ship updates under the same app identity. `*.jks` / `*.keystore` are
git-ignored; never commit them.

Then add to `local.properties` (also git-ignored):

```
RELEASE_STORE_FILE=vibecheck-release.jks
RELEASE_STORE_PASSWORD=********
RELEASE_KEY_ALIAS=vibecheck
RELEASE_KEY_PASSWORD=********
```

> Recommended: enable **Play App Signing** in the Play Console so Google manages
> the app signing key; you upload with an upload key created as above.

## Build the AAB

```bash
./gradlew :app:bundleRelease
# -> app/build/outputs/bundle/release/app-release.aab
```

This build uses the real Firebase/data stack (`USE_FAKE_DATA=false`), R8
minification, and resource shrinking. Upload `app-release.aab` to a Play Console
track (internal testing first).

## Before uploading — confirm

- [ ] Real `app/google-services.json` in place (not the `demo-vibecheck` placeholder)
- [ ] Real `MAPS_API_KEY` in `local.properties`
- [ ] `versionCode` bumped in `app/build.gradle.kts` (Play rejects duplicates)
- [ ] Functions deployed + service account linked (see [SETUP.md](SETUP.md))
- [ ] Smoke-test the AAB via an internal-testing track on a real device

## Verify a build locally

```bash
./gradlew :app:assembleRelease        # APK (debug-signed if no keystore)
./gradlew :app:bundleRelease          # AAB
```
