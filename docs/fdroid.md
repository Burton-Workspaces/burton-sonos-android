# F-Droid / Droidify repository

GitHub Releases are APK downloads. [Droidify](https://github.com/Droid-ify/client) and the official F-Droid client do **not** subscribe to those. They need an **F-Droid repository**: a public HTTPS folder with signed APKs plus a signed catalog (`index-v1.jar`).

This is a **self-hosted simple binary repo** of the same APKs CI already signs. It is not submission to [f-droid.org](https://f-droid.org/), which rebuilds from source and signs with F-Droid’s key.

Official HOWTO: [Setup an F-Droid App Repo](https://f-droid.org/docs/Setup_an_F-Droid_App_Repo/).

## Two keys

Do not mix these up.

| Key | File | What it signs | Used where |
| --- | --- | --- | --- |
| **App signing key** | `release.jks` (alias `burton`) | The APK | Android update verification. Same key as [build-automation.md](build-automation.md) / GitHub Releases. |
| **Repo signing key** | Created by `fdroid init` (`keystore.jks` in the fdroid working directory) | The repo index (`index-v1.jar`) | Droidify / F-Droid **Fingerprint** |

The Fingerprint is the **SHA-256 of the repo certificate**, 64 hex characters with no colons. Clients pin it so a fake catalog at the same URL cannot replace yours. It is **not** the APK keystore fingerprint.

Keep the repo keystore and `config.yml` private and backed up. Never publish `config.yml` (it contains passwords). Never commit either keystore to git.

Rotating the **repo** key means every user must re-add the repository. Rotating `release.jks` is worse: Android will refuse APK updates.

## Create the repo

Do this on a machine that is **not** the public web server (laptop is fine).

```bash
pipx install fdroidserver
export PATH="$HOME/.local/bin:$PATH"
which fdroid   # must be $HOME/.local/bin/fdroid, not /usr/bin/fdroid
mkdir -p ~/fdroid && cd ~/fdroid
fdroid init
chmod 0600 config.yml
```

Do **not** use Debian’s `apt install fdroidserver` (2.2.1). That stack’s Androguard cannot scan APKs from Android Gradle Plugin 8.7 (`res1 must be zero!` / `resources.arsc`). `./scripts/publish-fdroid-pages.sh` prepends `~/.local/bin` and refuses `/usr/bin/fdroid` for that reason.

If `config.yml` sets `repo_icon` to `repo/icons/icon.png`, change it to a PNG that lives in `~/fdroid` (for example `fdroid-icon.png`). That warning is unrelated to the Androguard crash.

That writes `config.yml` and a new `keystore.jks`. Edit `config.yml` for `repo_name`, `repo_url`, and `repo_description`. Set `repo_url` to:

`https://burton-workspaces.github.io/burton-sonos-fdroid/fdroid/repo`

The hosted catalog is [Burton-Workspaces/burton-sonos-fdroid](https://github.com/Burton-Workspaces/burton-sonos-fdroid). Clone it **next to** this app repo (`../burton-sonos-fdroid`). Pages is served from `main` at `/`. The `/fdroid/repo` path is filled by the publish script. The Fingerprint is written to that repo’s `FINGERPRINT` file on first publish.

## Publish with the Pages script

`scripts/publish-fdroid-pages.sh` copies a signed APK into your private `fdroid` working tree, runs `fdroid update`, then mirrors **only** `repo/` into [burton-sonos-fdroid](https://github.com/Burton-Workspaces/burton-sonos-fdroid) and pushes.

It will not use this Android repo as the Pages target, and it will not copy `config.yml` or the repo keystore. `FDROID_PAGES_DIR` defaults to `../burton-sonos-fdroid` when that clone exists.

```bash
export FDROID_ROOT=~/fdroid
cp fdroid-pages.env.example fdroid-pages.env   # optional; source it if you want
./scripts/publish-fdroid-pages.sh 1.3.0
```

| Input | Role |
| --- | --- |
| `<version>` | Must match `version.txt` (`1.3.0` or `v1.3.0`) |
| `FDROID_ROOT` | Directory from `fdroid init` (private; holds `config.yml` and the repo keystore) |
| `FDROID_PAGES_DIR` | Git checkout of `burton-sonos-fdroid` (defaults to `../burton-sonos-fdroid`) |
| `FDROID_ASSEMBLE=1` | Always run `assembleRelease` (default: assemble only if `burton-sonos-<version>.apk` is missing) |
| `FDROID_PAGES_PUSH=0` | Commit in the Pages checkout but do not `git push` |

What it does, in order:

1. Checks the version against `version.txt`.
2. Uses `burton-sonos-<version>.apk` in the app repo root if present (from `./scripts/upload-release-apk.sh` or an earlier assemble); otherwise runs `assembleRelease`.
3. Copies that APK into `$FDROID_ROOT/repo/`.
4. Runs `fdroid update --create-metadata` the first time (no `metadata/*.yml` yet), otherwise `fdroid update`.
5. `rsync`s `$FDROID_ROOT/repo/` → `$FDROID_PAGES_DIR/fdroid/repo/` (`--delete` so the hosted index matches). Touches `.nojekyll` so GitHub Pages does not process the tree as Jekyll.
6. Commits `Publish Burton Sonos <version>` in the Pages repo and pushes unless `FDROID_PAGES_PUSH=0`.
7. Prints the repo SHA-256 **Fingerprint** and the `?fingerprint=` add-repo URL when it can.

Typical release sequence (full walkthrough: [releases.md](releases.md)):

```bash
./scripts/upload-release-apk.sh 1.3.0
FDROID_ROOT=~/fdroid ./scripts/publish-fdroid-pages.sh 1.3.0
```

After the first `fdroid update --create-metadata`, edit `$FDROID_ROOT/metadata/com.burton.sonos.yml` (name, license, summary) and run the script again so the catalog is not a stub.

Keep `chmod 0600` on `$FDROID_ROOT/config.yml`. You can instead set `serverwebroot` in `config.yml` and use `fdroid deploy`; the Pages script is the path this repo documents.

## Read the Fingerprint

The Pages script prints it after a successful `fdroid update`. You can also read it from the repo keystore:

```bash
keytool -list -v -keystore ~/fdroid/keystore.jks | grep SHA256
```

Strip colons and spaces. You should have 64 hex characters.

From the signed index:

```bash
rsa=$(unzip -Z -1 ~/fdroid/repo/index-v1.jar | grep '\.RSA$' | head -n1)
unzip -p ~/fdroid/repo/index-v1.jar "$rsa" \
  | openssl pkcs7 -inform DER -print_certs \
  | openssl x509 -noout -fingerprint -sha256
```

This value is stable until you rotate the **repo** key. Publish it next to the repo URL (README, website, QR). Do **not** put the `release.jks` SHA-256 in Droidify’s Fingerprint field.

## Manual index (optional)

The Pages script runs `fdroid update` for you. To do it by hand, copy a **release** APK (`burton-sonos-<version>.apk`, not `com.burton.sonos.debug`) into `$FDROID_ROOT/repo/` and run `fdroid update --create-metadata` once, then `fdroid update` after editing `metadata/com.burton.sonos.yml`.

`CurrentVersionCode` in that metadata can pin the “stable” update line while still listing newer APKs as optional, if you ever ship betas in the same repo.

## Host `repo/` over HTTPS

The Pages script already publishes **only** `repo/` (APKs, icons, `index-v1.jar`, `index.xml`). Do not upload `config.yml` or `keystore.jks`.

If you host by hand, the customary path is `/fdroid/repo` so clients recognize the URL:

`https://<host>/fdroid/repo`

GitHub Pages for this org, after the script has run against a repo named `burton-sonos-fdroid`:

`https://burton-workspaces.github.io/burton-sonos-fdroid/fdroid/repo`

## Add the repo in Droidify or F-Droid

Droidify / F-Droid → **Repositories** → **+**:

- **Address:** `https://<host>/fdroid/repo`
- **Fingerprint:** the 64-character SHA-256 from above

One link (paste into the client, or open on the phone):

```
https://<host>/fdroid/repo?fingerprint=<64HEX>
```

HTTPS intent that opens the add-repo screen when F-Droid or Droidify is installed:

```
fdroidrepos://<host>/fdroid/repo?fingerprint=<64HEX>
```

Toggle the new repo on if the catalog does not appear immediately.

## What not to do

- Do not treat GitHub Releases as an F-Droid repo.
- Do not put the app-signing (`release.jks`) fingerprint in the client’s Fingerprint field.
- Do not commit `keystore.jks`, `release.jks`, or `config.yml`.
- Do not expect f-droid.org inclusion from this setup; that is a separate, rebuild-from-source process.

## Related

- Step-by-step local build and publish: [releases.md](releases.md)
- Signed APKs and tags: [releases.md](releases.md)
- Local assemble + GitHub Release upload: `./scripts/upload-release-apk.sh <version>`
- F-Droid index + GitHub Pages: `./scripts/publish-fdroid-pages.sh <version>`
- App and CI signing: [build-automation.md](build-automation.md)
