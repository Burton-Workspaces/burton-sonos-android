# Releases

Versioning is **SemVer**. The Gradle `versionName` and `versionCode` both come from [`version.txt`](../version.txt):

```
versionCode = MAJOR * 1_000_000 + MINOR * 1_000 + PATCH
```

Tags look like `v1.1.0` (`include-v-in-tag` in `release-please-config.json`).

## Conventional Commits

Merges to `master` **must** use [Conventional Commits](https://www.conventionalcommits.org/en/v1.0.0/). CI rejects other subjects (and pull request titles). Install the local hook with `./scripts/install-git-hooks.sh`. Details: [CONTRIBUTING.md](../CONTRIBUTING.md).

| Prefix | Effect |
| --- | --- |
| `feat:` | minor bump (pre-1.0 also uses minor for features; `bump-minor-pre-major` is on) |
| `fix:` | patch |
| `feat!:` / `BREAKING CHANGE:` | major |
| `chore:`, `docs:`, `ci:` | no version bump unless configured otherwise |

The release PR updates `version.txt`, `CHANGELOG.md`, and `.release-please-manifest.json`. Merging it tags `vX.Y.Z` and creates the GitHub Release.

## CI

| Workflow | When | What |
| --- | --- | --- |
| [`.github/workflows/ci.yml`](../.github/workflows/ci.yml) | PR and push to `master` | `testDebugUnitTest` |
| [`.github/workflows/conventional-commits.yml`](../.github/workflows/conventional-commits.yml) | PR (including title edits) and push to `master` | Conventional Commit subjects |
| [`.github/workflows/release.yml`](../.github/workflows/release.yml) | push to `master` | release-please; if a release was created, pack APK |
| [`.github/workflows/release-assets.yml`](../.github/workflows/release-assets.yml) | tag `v*.*.*`, workflow_call, or `workflow_dispatch` | test, signed `assembleRelease`, upload `burton-sonos-<version>.apk` |

Pack **skips** when the tag push commit message is the release-please bump (`chore(release)` / `: release `), so the APK is built once from the release-please job instead of twice.

The tag must match `version.txt` (without the `v`). Checkout uses the tag ref.

SDK setup lives in [`.github/actions/setup-android-ci`](../.github/actions/setup-android-ci/action.yml): Temurin 17, Android SDK `platform-tools`, `local.properties` `sdk.dir`.

## Signing

Local:

1. Copy `keystore.properties.example` → `keystore.properties`
2. Create a JKS (alias `burton` by convention)
3. `./gradlew assembleRelease` → `app/build/outputs/apk/release/app-release.apk`

GitHub repository secrets:

| Secret | Role |
| --- | --- |
| `KEYSTORE_BASE64` | Base64 of the JKS (required) |
| `KEYSTORE_PASSWORD` | Store password (required) |
| `KEY_ALIAS` | Defaults to `burton` |
| `KEY_PASSWORD` | Defaults to the store password |

CI writes `release.jks` and `keystore.properties` on the runner. Never commit those files or the keystore.

## Manual APK retry

GitHub Actions → **Release assets** → Run workflow → tag `vX.Y.Z` (must already exist and match `version.txt`).
