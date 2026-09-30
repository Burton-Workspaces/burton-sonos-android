#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
CHECK="$ROOT/scripts/conventional-commit.sh"

pass() {
  "$CHECK" --subject "$1"
}

fail() {
  if "$CHECK" --subject "$1" >/dev/null 2>&1; then
    echo "expected reject: $1" >&2
    exit 1
  fi
}

pass "feat: show live groups on the Groups tab"
pass "fix(alarms): load household AlarmClock lists"
pass "feat(search)!: replace queue from overflow"
pass "chore(release): 1.2.0"
pass "docs: add using guide"
pass "ci: enforce conventional commits"
pass "feat: x"
pass "ci(workflows): check PR titles"
pass "perf: debounce group volume SOAP"
pass "deps: bump okhttp"
pass "revert: undo broken alarm parse"
pass "Merge pull request #12 from rconnelly/feat-groups"
pass "Merge branch 'master' into feat-groups"
pass "Revert \"feat: show live groups on the Groups tab\""

fail "Load live groups and household alarms, and document the app."
fail "Feat: wrong case"
fail "feat:no-space"
fail "feat:"
fail "feat: "
fail "updated alarms"
fail "chore(release) missing colon description"
fail "fix(): empty scope"

echo "conventional-commit checks passed"
