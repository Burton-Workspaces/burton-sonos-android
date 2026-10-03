#!/usr/bin/env bash
# One-time publish setup: private F-Droid working tree, Pages checkout,
# app signing keystore, then GitHub Actions secrets for this repo.
set -euo pipefail

usage() {
  cat <<'EOF' >&2
Usage: scripts/setup-fdroid-and-secrets.sh

Idempotent. Reuses ~/fdroid and ../burton-sonos-fdroid when they already exist.
Copies a sibling Burton JKS when this repo has no keystore yet, then sets
KEYSTORE_BASE64 / KEYSTORE_PASSWORD (and KEY_ALIAS / KEY_PASSWORD if needed).

Optional environment:
  FDROID_ROOT           fdroid init directory (default: $HOME/fdroid)
  FDROID_PAGES_DIR      Pages checkout (default: ../burton-sonos-fdroid)
  FDROID_REPO_URL       Written into config.yml (default: Burton Pages catalog)
  GH_REPO               owner/name (default: git remote, else Burton-Workspaces/burton-sonos-android)
  CREATE_REPO           1 to `gh repo create` when GH_REPO is missing (default: 1)
  REUSE_KEYSTORE_FROM   sibling app directory that already has keystore.properties
  ANDROID_HOME          passed to `fdroid init --no-prompt` when creating a new tree

Does not: assemble an APK, publish to Pages, or print keystore passwords.
EOF
  exit 1
}

[[ $# -eq 0 ]] || usage

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
export PATH="${HOME}/.local/bin:${PATH}"

PAGES_DEFAULT="$(cd "$ROOT/.." && pwd)/burton-sonos-fdroid"
FDROID_ROOT="${FDROID_ROOT:-$HOME/fdroid}"
FDROID_PAGES_DIR="${FDROID_PAGES_DIR:-$PAGES_DEFAULT}"
FDROID_REPO_URL="${FDROID_REPO_URL:-https://burton-workspaces.github.io/burton-sonos-fdroid/fdroid/repo}"
CREATE_REPO="${CREATE_REPO:-1}"
PACKAGE_ID="com.burton.sonos"
PAGES_REPO="Burton-Workspaces/burton-sonos-fdroid"

prop() {
  local file="$1" key="$2"
  grep "^${key}=" "$file" | cut -d= -f2-
}

resolve_repo() {
  if [[ -n "${GH_REPO:-}" ]]; then
    printf '%s\n' "$GH_REPO"
    return
  fi
  if origin="$(git remote get-url origin 2>/dev/null)"; then
    if [[ "$origin" =~ github.com[:/]([^/]+)/([^/.]+) ]]; then
      printf '%s/%s\n' "${BASH_REMATCH[1]}" "${BASH_REMATCH[2]}"
      return
    fi
  fi
  printf '%s\n' "Burton-Workspaces/burton-sonos-android"
}

ensure_fdroidserver() {
  if ! command -v pipx >/dev/null; then
    echo "pipx is required to install fdroidserver (not Debian apt)." >&2
    exit 1
  fi
  if ! command -v fdroid >/dev/null; then
    echo "Installing fdroidserver with pipx"
    pipx install fdroidserver
  fi
  if [[ "$(command -v fdroid)" == /usr/bin/fdroid ]]; then
    echo "Using /usr/bin/fdroid (likely Debian 2.2.1). That Androguard cannot scan AGP 8.7 APKs." >&2
    echo "Install a current fdroidserver first:" >&2
    echo "  pipx install fdroidserver && export PATH=\"\$HOME/.local/bin:\$PATH\"" >&2
    exit 1
  fi
  echo "fdroid: $(command -v fdroid)"
}

ensure_fdroid_root() {
  mkdir -p "$FDROID_ROOT"
  FDROID_ROOT="$(cd "$FDROID_ROOT" && pwd)"
  if [[ -f "$FDROID_ROOT/config.yml" ]]; then
    echo "F-Droid working tree already exists: $FDROID_ROOT"
  else
    echo "Initializing F-Droid working tree in $FDROID_ROOT"
    local android_home="${ANDROID_HOME:-}"
    if [[ -z "$android_home" && -f "$ROOT/local.properties" ]]; then
      android_home="$(prop "$ROOT/local.properties" sdk.dir)"
    fi
    (
      cd "$FDROID_ROOT"
      if [[ -n "$android_home" ]]; then
        fdroid init --no-prompt --android-home "$android_home"
      else
        fdroid init --no-prompt
      fi
    )
  fi
  chmod 0600 "$FDROID_ROOT/config.yml"
  if grep -q '^repo_url:' "$FDROID_ROOT/config.yml"; then
    local current
    current="$(grep '^repo_url:' "$FDROID_ROOT/config.yml" | awk '{print $2}')"
    if [[ "$current" != "$FDROID_REPO_URL" ]]; then
      echo "Setting repo_url to $FDROID_REPO_URL"
      sed -i "s|^repo_url:.*|repo_url: ${FDROID_REPO_URL}|" "$FDROID_ROOT/config.yml"
    fi
  else
    printf '\nrepo_url: %s\n' "$FDROID_REPO_URL" >> "$FDROID_ROOT/config.yml"
  fi
  mkdir -p "$FDROID_ROOT/repo" "$FDROID_ROOT/metadata"
  local meta_src="$ROOT/fdroid/metadata/${PACKAGE_ID}.yml"
  local meta_dst="$FDROID_ROOT/metadata/${PACKAGE_ID}.yml"
  if [[ -f "$meta_src" && ! -f "$meta_dst" ]]; then
    cp "$meta_src" "$meta_dst"
    echo "Wrote $meta_dst"
  fi
}

ensure_pages_checkout() {
  if [[ -d "$FDROID_PAGES_DIR/.git" ]]; then
    FDROID_PAGES_DIR="$(cd "$FDROID_PAGES_DIR" && pwd)"
    echo "Pages checkout already exists: $FDROID_PAGES_DIR"
    return
  fi
  if [[ -e "$FDROID_PAGES_DIR" ]]; then
    echo "FDROID_PAGES_DIR exists but is not a git checkout: $FDROID_PAGES_DIR" >&2
    exit 1
  fi
  echo "Cloning $PAGES_REPO into $FDROID_PAGES_DIR"
  gh repo clone "$PAGES_REPO" "$FDROID_PAGES_DIR"
  FDROID_PAGES_DIR="$(cd "$FDROID_PAGES_DIR" && pwd)"
}

write_pages_env() {
  local env_file="$ROOT/fdroid-pages.env"
  if [[ -f "$env_file" ]]; then
    echo "fdroid-pages.env already exists"
    return
  fi
  cat > "$env_file" <<EOF
FDROID_ROOT=$FDROID_ROOT
FDROID_PAGES_DIR=$FDROID_PAGES_DIR
EOF
  echo "Wrote $env_file"
}

find_sibling_keystore() {
  if [[ -n "${REUSE_KEYSTORE_FROM:-}" ]]; then
    printf '%s\n' "$REUSE_KEYSTORE_FROM"
    return
  fi
  local sibling
  for sibling in burton-app-hub burton-finance burton-groupme burton-issues burton-meeting burton-photos-android burton-pod burton-slack burton-weather burton-whatsapp-android; do
    if [[ -f "$ROOT/../$sibling/keystore.properties" ]]; then
      printf '%s\n' "$ROOT/../$sibling"
      return
    fi
  done
}

ensure_app_keystore() {
  if [[ -f "$ROOT/keystore.properties" ]]; then
    echo "Using existing $ROOT/keystore.properties"
    return
  fi
  local sibling
  sibling="$(find_sibling_keystore || true)"
  if [[ -n "$sibling" && -f "$sibling/keystore.properties" ]]; then
    local store
    store="$(prop "$sibling/keystore.properties" storeFile)"
    if [[ -z "$store" || ! -f "$sibling/$store" ]]; then
      echo "Sibling $sibling has keystore.properties but storeFile is missing." >&2
      exit 1
    fi
    echo "Reusing signing key from $sibling"
    cp "$sibling/keystore.properties" "$ROOT/keystore.properties"
    if [[ ! -f "$ROOT/$store" ]]; then
      cp "$sibling/$store" "$ROOT/$store"
    fi
    return
  fi
  echo "No sibling keystore found. Creating $ROOT/release.jks (alias burton)"
  local store_pass
  store_pass="$(python3 -c 'import secrets; print(secrets.token_urlsafe(24))')"
  keytool -genkeypair \
    -keystore "$ROOT/release.jks" \
    -alias burton \
    -keyalg RSA \
    -keysize 2048 \
    -validity 10000 \
    -storepass "$store_pass" \
    -keypass "$store_pass" \
    -dname "CN=Burton Sonos, O=Burton Workspaces, C=US"
  cat > "$ROOT/keystore.properties" <<EOF
storeFile=release.jks
storePassword=$store_pass
keyAlias=burton
keyPassword=$store_pass
EOF
  echo "Wrote keystore.properties (gitignored)"
}

ensure_github_repo() {
  local repo="$1"
  if gh repo view "$repo" >/dev/null 2>&1; then
    echo "GitHub repo exists: $repo"
    return
  fi
  if [[ "$CREATE_REPO" != 1 ]]; then
    echo "GitHub repo $repo does not exist. Create it, or rerun with CREATE_REPO=1." >&2
    exit 1
  fi
  echo "Creating GitHub repo $repo"
  gh repo create "$repo" --public --description "Local Android controller for a Sonos household"
  if ! git remote get-url origin >/dev/null 2>&1; then
    git remote add origin "git@github.com:${repo}.git"
    echo "Added origin remote (no push; this tree has to be committed first)"
  fi
}

set_github_secrets() {
  local repo="$1"
  if ! command -v gh >/dev/null; then
    echo "gh is required to write repository secrets." >&2
    exit 1
  fi
  if ! gh auth status >/dev/null 2>&1; then
    echo "gh is not logged in. Run: gh auth login" >&2
    exit 1
  fi
  local props="$ROOT/keystore.properties"
  local store alias store_pass key_pass
  store="$(prop "$props" storeFile)"
  alias="$(prop "$props" keyAlias)"
  store_pass="$(prop "$props" storePassword)"
  key_pass="$(prop "$props" keyPassword)"
  if [[ -z "$store" || ! -f "$ROOT/$store" ]]; then
    echo "storeFile from keystore.properties is missing: $store" >&2
    exit 1
  fi
  if [[ -z "$store_pass" ]]; then
    echo "storePassword is empty in keystore.properties" >&2
    exit 1
  fi
  local keystore_b64
  keystore_b64="$(python3 -c 'import base64, pathlib, sys; print(base64.b64encode(pathlib.Path(sys.argv[1]).read_bytes()).decode())' "$ROOT/$store")"
  echo "Setting KEYSTORE_BASE64 and KEYSTORE_PASSWORD on $repo"
  gh secret set KEYSTORE_BASE64 --repo "$repo" --body "$keystore_b64"
  gh secret set KEYSTORE_PASSWORD --repo "$repo" --body "$store_pass"
  if [[ -n "$alias" && "$alias" != burton ]]; then
    echo "Setting KEY_ALIAS"
    gh secret set KEY_ALIAS --repo "$repo" --body "$alias"
  fi
  if [[ -n "$key_pass" && "$key_pass" != "$store_pass" ]]; then
    echo "Setting KEY_PASSWORD"
    gh secret set KEY_PASSWORD --repo "$repo" --body "$key_pass"
  fi
  echo "Secrets now on $repo:"
  gh secret list --repo "$repo"
}

echo "== F-Droid working tree =="
ensure_fdroidserver
ensure_fdroid_root
ensure_pages_checkout
write_pages_env

echo
echo "== App signing keystore =="
ensure_app_keystore

echo
echo "== GitHub repository secrets =="
REPO="$(resolve_repo)"
ensure_github_repo "$REPO"
set_github_secrets "$REPO"

echo
echo "Done."
echo "F-Droid root:   $FDROID_ROOT"
echo "Pages checkout: $FDROID_PAGES_DIR"
echo "GitHub repo:    $REPO"
echo "Publish later with:"
echo "  source fdroid-pages.env"
echo "  ./scripts/upload-release-apk.sh"
echo "  ./scripts/publish-fdroid-pages.sh"
