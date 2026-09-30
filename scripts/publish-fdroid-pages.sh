#!/usr/bin/env bash
set -euo pipefail

usage() {
  echo "Usage: $0 <version>" >&2
  echo "  version  SemVer matching version.txt, with or without a v prefix (1.3.0 or v1.3.0)" >&2
  echo >&2
  echo "Required environment:" >&2
  echo "  FDROID_ROOT       Directory from \`fdroid init\` (config.yml + repo keystore)" >&2
  echo >&2
  echo "Optional:" >&2
  echo "  FDROID_PAGES_DIR    Git checkout of Burton-Workspaces/burton-sonos-fdroid" >&2
  echo "                      (defaults to ../burton-sonos-fdroid when that clone exists)" >&2
  echo "  FDROID_PAGES_PUSH   Set to 0 to commit without pushing (default: 1)" >&2
  echo "  FDROID_ASSEMBLE     Set to 1 to always run assembleRelease (default: only if APK is missing)" >&2
  exit 1
}

[[ $# -eq 1 ]] || usage

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
export PATH="${HOME}/.local/bin:${PATH}"

raw="$1"
version="${raw#v}"
if [[ ! "$version" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
  echo "Version must be MAJOR.MINOR.PATCH, got '$raw'" >&2
  exit 1
fi

actual="$(tr -d '[:space:]' < version.txt)"
if [[ "$version" != "$actual" ]]; then
  echo "Version $version does not match version.txt ($actual)." >&2
  exit 1
fi

if [[ -z "${FDROID_PAGES_DIR:-}" ]]; then
  sibling="$(cd "$ROOT/.." && pwd)/burton-sonos-fdroid"
  if [[ -d "$sibling/.git" ]]; then
    FDROID_PAGES_DIR="$sibling"
  fi
fi

: "${FDROID_ROOT:?Set FDROID_ROOT to the directory created by fdroid init}"
: "${FDROID_PAGES_DIR:?Clone Burton-Workspaces/burton-sonos-fdroid next to this repo, or set FDROID_PAGES_DIR}"

FDROID_ROOT="$(cd "$FDROID_ROOT" && pwd)"
FDROID_PAGES_DIR="$(cd "$FDROID_PAGES_DIR" && pwd)"

if [[ "$FDROID_ROOT" == "$FDROID_PAGES_DIR" ]]; then
  echo "FDROID_PAGES_DIR must not be the fdroid working directory (that would publish config.yml and the repo keystore)." >&2
  exit 1
fi
if [[ -f "$FDROID_PAGES_DIR/app/build.gradle.kts" ]]; then
  echo "FDROID_PAGES_DIR looks like this app repo. Use a separate GitHub Pages checkout." >&2
  exit 1
fi
if [[ ! -f "$FDROID_ROOT/config.yml" ]]; then
  echo "No config.yml in FDROID_ROOT. Run fdroid init there first." >&2
  exit 1
fi
if [[ ! -d "$FDROID_PAGES_DIR/.git" ]]; then
  echo "FDROID_PAGES_DIR must be a git checkout (GitHub Pages repo)." >&2
  exit 1
fi
if ! command -v fdroid >/dev/null; then
  echo "fdroid is required. Debian 2.2.1 cannot scan this APK; install a current fdroidserver:" >&2
  echo "  pipx install fdroidserver" >&2
  echo "  export PATH=\"\$HOME/.local/bin:\$PATH\"" >&2
  exit 1
fi
if [[ "$(command -v fdroid)" == /usr/bin/fdroid ]]; then
  echo "Using $(command -v fdroid) (likely Debian 2.2.1)." >&2
  echo "That Androguard cannot parse AGP 8.7 APKs (res1 must be zero!)." >&2
  echo "Install a current fdroidserver and put it first on PATH:" >&2
  echo "  pipx install fdroidserver" >&2
  echo "  export PATH=\"\$HOME/.local/bin:\$PATH\"" >&2
  echo "Then: which fdroid   # should print $HOME/.local/bin/fdroid" >&2
  exit 1
fi

apk="$ROOT/burton-sonos-${version}.apk"
if [[ "${FDROID_ASSEMBLE:-0}" == 1 || ! -f "$apk" ]]; then
  if [[ ! -f "$ROOT/keystore.properties" ]]; then
    echo "Missing keystore.properties. Copy keystore.properties.example and point storeFile at your JKS." >&2
    exit 1
  fi
  ./gradlew assembleRelease
  cp app/build/outputs/apk/release/app-release.apk "$apk"
fi

mkdir -p "$FDROID_ROOT/repo"
cp "$apk" "$FDROID_ROOT/repo/"

(
  cd "$FDROID_ROOT"
  if [[ ! -d metadata ]] || [[ -z "$(find metadata -name '*.yml' -print -quit 2>/dev/null)" ]]; then
    update_args=(update --create-metadata)
  else
    update_args=(update)
  fi
  if ! fdroid "${update_args[@]}"; then
    echo "fdroid update failed. If you saw 'res1 must be zero', Debian's androguard is too old for this APK." >&2
    echo "  pipx install fdroidserver && export PATH=\"\$HOME/.local/bin:\$PATH\"" >&2
    exit 1
  fi
)

public="$FDROID_PAGES_DIR/fdroid/repo"
mkdir -p "$public"
touch "$FDROID_PAGES_DIR/.nojekyll"
if command -v rsync >/dev/null; then
  rsync -a --delete "$FDROID_ROOT/repo/" "$public/"
else
  rm -rf "$public"
  mkdir -p "$public"
  cp -a "$FDROID_ROOT/repo/." "$public/"
fi

fingerprint=""
index_jar="$FDROID_ROOT/repo/index-v1.jar"
if [[ -f "$index_jar" ]] && command -v openssl >/dev/null; then
  rsa_entry="$(unzip -Z -1 "$index_jar" | grep '\.RSA$' | head -n1 || true)"
  if [[ -n "$rsa_entry" ]]; then
    fingerprint="$(
      unzip -p "$index_jar" "$rsa_entry" \
        | openssl pkcs7 -inform DER -print_certs 2>/dev/null \
        | openssl x509 -noout -fingerprint -sha256 2>/dev/null \
        | sed 's/^SHA256 Fingerprint=//' \
        | tr -d ': \n' \
        | tr '[:lower:]' '[:upper:]'
    )" || fingerprint=""
  fi
fi

(
  cd "$FDROID_PAGES_DIR"
  if [[ -n "$fingerprint" ]]; then
    printf '%s\n' "$fingerprint" > FINGERPRINT
    git add FINGERPRINT
  fi
  git add .nojekyll fdroid
  if git diff --cached --quiet; then
    echo "Pages tree already up to date."
  else
    git commit -m "Publish Burton Sonos ${version}"
    if [[ "${FDROID_PAGES_PUSH:-1}" == 1 ]]; then
      git push
    else
      echo "Committed locally. Push skipped (FDROID_PAGES_PUSH=0)."
    fi
  fi
)

echo "Published $apk into $public"
if [[ -n "$fingerprint" ]]; then
  echo "Fingerprint: $fingerprint"
fi
if origin="$(git -C "$FDROID_PAGES_DIR" remote get-url origin 2>/dev/null)"; then
  if [[ "$origin" =~ github.com[:/]([^/]+)/([^/.]+) ]]; then
    org="${BASH_REMATCH[1],,}"
    name="${BASH_REMATCH[2]}"
    echo "Repo URL: https://${org}.github.io/${name}/fdroid/repo"
    if [[ -n "$fingerprint" ]]; then
      echo "Add in Droidify: https://${org}.github.io/${name}/fdroid/repo?fingerprint=${fingerprint}"
    fi
  fi
fi
