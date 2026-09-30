# Build automation setup

This repo already contains the GitHub Actions workflows. Setup is **repository settings + signing secrets**, not new YAML.

After this is done:

- Every push/PR to `master` runs unit tests and Conventional Commit checks
- Conventional `feat` / `fix` commits on `master` open a release-please PR
- Merging that PR tags `vX.Y.Z`, creates a GitHub Release, and uploads `burton-sonos-<version>.apk`

Day-to-day versioning is in [releases.md](releases.md). Commit message rules are in [CONTRIBUTING.md](../CONTRIBUTING.md).

## What is already in git

| Path | Role |
| --- | --- |
| [`.github/workflows/ci.yml`](../.github/workflows/ci.yml) | `testDebugUnitTest` on PR and `master` |
| [`.github/workflows/conventional-commits.yml`](../.github/workflows/conventional-commits.yml) | Commit subjects and PR titles |
| [`.github/workflows/release.yml`](../.github/workflows/release.yml) | release-please; packs APK when a release is created |
| [`.github/workflows/release-assets.yml`](../.github/workflows/release-assets.yml) | Signed `assembleRelease`, upload APK |
| [`.github/actions/setup-android-ci`](../.github/actions/setup-android-ci/action.yml) | Temurin 17, Android SDK `platform-tools`, `local.properties` |
| [`release-please-config.json`](../release-please-config.json) | SemVer, `CHANGELOG.md`, tags `vX.Y.Z` |
| [`scripts/install-git-hooks.sh`](../scripts/install-git-hooks.sh) | Local `commit-msg` hook |

Release-please only runs when `github.repository` is `Burton-Workspaces/burton-sonos-android`. Forks still get CI tests.

## 1. Enable Actions

**Settings → Actions → General**

- Allow Actions (default actions + reusable workflows is enough)
- **Workflow permissions:** Read and write
- Check **Allow GitHub Actions to create and approve pull requests**

Without that checkbox, release-please can push `release-please--branches--master` but fails with *GitHub Actions is not permitted to create or approve pull requests*.

`GITHUB_TOKEN` is enough. Do not put a personal access token in the workflows for this.

## 2. Local keystore (machine)

Gitignore already excludes `keystore.properties`, `*.jks`, and `*.keystore`.

```bash
cp keystore.properties.example keystore.properties
```

Create a JKS if you do not have one (alias `burton` matches CI’s default):

```bash
keytool -genkeypair -v \
  -keystore release.jks \
  -alias burton \
  -keyalg RSA -keysize 2048 -validity 10000
```

Fill `keystore.properties`:

```
storeFile=release.jks
storePassword=…
keyAlias=burton
keyPassword=…
```

`storeFile` is a path relative to the repo root. Confirm a local signed build:

```bash
./gradlew assembleRelease
```

That writes `app/build/outputs/apk/release/app-release.apk`. Never commit the JKS or `keystore.properties`.

## 3. GitHub secrets (CI signing)

There is no env var named `KEYSTORE_BASE64` on your machine. CI reconstructs `release.jks` and `keystore.properties` from secrets:

| GitHub secret | Local source |
| --- | --- |
| `KEYSTORE_BASE64` (**required**) | Base64 of the **file** named in `storeFile` (the JKS bytes, not a line in `keystore.properties`) |
| `KEYSTORE_PASSWORD` (**required**) | `storePassword` |
| `KEY_ALIAS` | `keyAlias` (omit to use `burton`) |
| `KEY_PASSWORD` | `keyPassword` (omit to reuse the store password) |

From a `burton-sonos-android` checkout that already has `keystore.properties` and the JKS it names:

```bash
cd ~/Code/burton-sonos-android

# required: APK signing keystore (single-line base64 of the JKS file)
gh secret set KEYSTORE_BASE64 --repo Burton-Workspaces/burton-sonos-android \
  --body "$(base64 -w0 "$(grep ^storeFile= keystore.properties | cut -d= -f2-)")"

# required: storePassword
gh secret set KEYSTORE_PASSWORD --repo Burton-Workspaces/burton-sonos-android \
  --body "$(grep ^storePassword= keystore.properties | cut -d= -f2-)"

# optional if keyAlias is not burton
gh secret set KEY_ALIAS --repo Burton-Workspaces/burton-sonos-android \
  --body "$(grep ^keyAlias= keystore.properties | cut -d= -f2-)"

# optional if keyPassword differs from storePassword
gh secret set KEY_PASSWORD --repo Burton-Workspaces/burton-sonos-android \
  --body "$(grep ^keyPassword= keystore.properties | cut -d= -f2-)"
```

On macOS, `base64 -w0` is not available; use `base64 -i "$(grep ^storeFile= keystore.properties | cut -d= -f2-)" | tr -d '\n'` inside the `KEYSTORE_BASE64` `--body`.

Omit `KEY_ALIAS` / `KEY_PASSWORD` if you use alias `burton` and the same password as the store. CI defaults those.

Check:

```bash
gh secret list --repo Burton-Workspaces/burton-sonos-android
```

Pack a tagged APK after that (the tag must already exist and match `version.txt`):

```bash
gh workflow run "Release assets" --repo Burton-Workspaces/burton-sonos-android -f tag=v1.4.1
```

That workflow rebuilds `keystore.properties` from these secrets, runs `assembleRelease`, and uploads `burton-sonos-<version>.apk`.

You can also set the same values in **Settings → Secrets and variables → Actions → New repository secret**. Encode the keystore (single line, no wraps) with `base64 -w0 "$(grep ^storeFile= keystore.properties | cut -d= -f2-)"`, then paste it into `KEYSTORE_BASE64` and paste `storePassword` into `KEYSTORE_PASSWORD`.

If these secrets are empty, **Release assets** fails at “Configure release signing” even when tests pass. You can still `assembleRelease` locally and `gh release upload vX.Y.Z burton-sonos-X.Y.Z.apk`.

## 4. Optional: branch protection

**Settings → Branches → Add rule** for `master`:

- Require status checks: **Unit tests**, **Conventional commits**
- Do not require **Release** / **Release assets** on every push; those run after version bumps and need secrets

## 5. Optional: local commit hook

```bash
./scripts/install-git-hooks.sh
```

CI still rejects non-conventional subjects on `master` and on pull requests.

## 6. Verify

1. Push a `docs:` or `ci:` commit (no version bump). **CI** and **Conventional commits** should be green. **Release** should succeed with pack skipped.
2. Confirm secrets: **Actions → Release assets → Run workflow** with tag `v1.4.1` (or the current `version.txt` with a `v` prefix). The job must pass “Configure release signing” and upload `burton-sonos-<version>.apk`.
3. Cut a real release with a `feat:` or `fix:` on `master`, merge the release-please PR. See [releases.md](releases.md).

## Troubleshooting

| Symptom | Likely cause |
| --- | --- |
| *GitHub Actions is not permitted to create or approve pull requests* | Missing “create and approve pull requests” checkbox |
| *Set KEYSTORE_BASE64 and KEYSTORE_PASSWORD* | Secrets not created, misspelled, or empty |
| Tag exists, GitHub Release has no APK | Pack failed (secrets) or was skipped; run **Release assets** with that tag, or upload a locally signed APK |
| *Tag does not match version.txt* | Pack checked out a tag whose `version.txt` is not that SemVer |
| Release-please never opens a PR | Commits since the last tag are not `feat:` / `fix:` / `perf:` |
| Fork has no release-please job | Hard-coded to `Burton-Workspaces/burton-sonos-android` |
