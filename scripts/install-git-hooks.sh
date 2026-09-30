#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
GIT_DIR="$(git rev-parse --git-dir)"
HOOKS="$GIT_DIR/hooks"
mkdir -p "$HOOKS"
cp "$ROOT/.githooks/commit-msg" "$HOOKS/commit-msg"
chmod +x "$HOOKS/commit-msg" "$ROOT/scripts/conventional-commit.sh"
echo "Installed commit-msg hook to $HOOKS/commit-msg"
