#!/usr/bin/env bash
set -euo pipefail

usage() {
  echo "Usage: $0 [version]" >&2
  echo "  version  SemVer matching version.txt, with or without a v prefix (1.0.0 or v1.0.0)" >&2
  echo "           Defaults to version.txt when omitted." >&2
  exit 1
}

if [[ "${1:-}" == "-h" || "${1:-}" == "--help" ]]; then
  usage
fi
if [[ $# -gt 1 ]]; then
  usage
fi

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

if [[ $# -eq 1 ]]; then
  raw="$1"
else
  raw="$(tr -d '[:space:]' < version.txt)"
fi
version="${raw#v}"
if [[ ! "$version" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
  echo "Version must be SemVer 2.0 MAJOR.MINOR.PATCH, got '$raw'" >&2
  exit 1
fi
tag="v${version}"

actual="$(tr -d '[:space:]' < version.txt)"
if [[ "$version" != "$actual" ]]; then
  echo "Version $version does not match version.txt ($actual)." >&2
  echo "Merge the release-please PR (or check out tag $tag) before packing." >&2
  exit 1
fi

if [[ ! -f keystore.properties ]]; then
  echo "Missing keystore.properties. Copy keystore.properties.example and point storeFile at your JKS." >&2
  exit 1
fi

if ! command -v gh >/dev/null; then
  echo "gh is required to upload the APK." >&2
  exit 1
fi

./gradlew assembleRelease

apk="burton-sonos-${version}.apk"
cp app/build/outputs/apk/release/app-release.apk "$apk"

if gh release view "$tag" >/dev/null 2>&1; then
  echo "Release $tag already exists"
else
  gh release create "$tag" --title "$tag" --generate-notes
fi
gh release upload "$tag" "$apk" --clobber
echo "Uploaded $apk to $tag"
