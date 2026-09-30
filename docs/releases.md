# Releases

How versions are cut once automation is already configured. **First-time GitHub Actions, permissions, and signing secrets:** [build-automation.md](build-automation.md).

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

The **Release** workflow uses `GITHUB_TOKEN`. The repository must allow Actions to open PRs:

**Settings → Actions → General → Workflow permissions**
- Read and write permissions
- **Allow GitHub Actions to create and approve pull requests**

Without that checkbox, release-please can push `release-please--branches--master` but the job fails with *GitHub Actions is not permitted to create or approve pull requests*.

Label updates after tagging are skipped (`skip-labeling: true`) so a GitHub API blip cannot fail the job after the GitHub Release already exists. The APK pack still runs from that workflow and from tag pushes (`release-assets.yml` uploads with `--clobber`).

## CI

| Workflow | When | What |
| --- | --- | --- |
| [`.github/workflows/ci.yml`](../.github/workflows/ci.yml) | PR and push to `master` | `testDebugUnitTest` |
| [`.github/workflows/conventional-commits.yml`](../.github/workflows/conventional-commits.yml) | PR (including title edits) and push to `master` | Conventional Commit subjects |
| [`.github/workflows/release.yml`](../.github/workflows/release.yml) | push to `master` | release-please; if a release was created, pack APK |
| [`.github/workflows/release-assets.yml`](../.github/workflows/release-assets.yml) | tag `v*.*.*`, workflow_call, or `workflow_dispatch` | test, signed `assembleRelease`, upload `burton-sonos-<version>.apk` |

The tag must match `version.txt` (without the `v`). Checkout uses the tag ref. Duplicate uploads use `--clobber`.

SDK setup lives in [`.github/actions/setup-android-ci`](../.github/actions/setup-android-ci/action.yml): Temurin 17, Android SDK `platform-tools`, `local.properties` `sdk.dir`.

## Signing

Local and CI signing, including how `KEYSTORE_BASE64` maps to your JKS, is documented in [build-automation.md](build-automation.md).

GitHub repository secrets used by [`.github/workflows/release-assets.yml`](../.github/workflows/release-assets.yml):

| Secret | Local source |
| --- | --- |
| `KEYSTORE_BASE64` | Base64 of the JKS named in `storeFile` |
| `KEYSTORE_PASSWORD` | `storePassword` |
| `KEY_ALIAS` | `keyAlias` (optional; default `burton`) |
| `KEY_PASSWORD` | `keyPassword` (optional; default store password) |

Never commit `keystore.properties` or the keystore.

## Manual APK retry

GitHub Actions → **Release assets** → Run workflow → tag `vX.Y.Z` (must already exist and match `version.txt`).
