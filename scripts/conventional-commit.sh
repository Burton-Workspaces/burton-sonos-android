#!/usr/bin/env bash
# Validate Conventional Commits (https://www.conventionalcommits.org/en/v1.0.0/).
# Types match what release-please treats as changelog / version bumps.
set -euo pipefail

TYPES='feat|fix|docs|style|refactor|perf|test|build|ci|chore|revert|deps'
SUBJECT_RE="^(${TYPES})(\\([^)]+\\))?(!)?: [^[:space:]].*"

usage() {
  cat <<'EOF'
Usage:
  conventional-commit.sh --subject <text>
  conventional-commit.sh --file <commit-msg-file>
  conventional-commit.sh --range <from-sha> <to-sha>
EOF
}

skip_subject() {
  local subject="$1"
  case "$subject" in
    Merge\ pull\ request*|Merge\ branch*|Merge\ remote-tracking\ branch*)
      return 0
      ;;
    Revert\ *)
      return 0
      ;;
  esac
  return 1
}

valid_subject() {
  local subject="$1"
  [[ "$subject" =~ $SUBJECT_RE ]]
}

fail_subject() {
  local subject="$1"
  cat >&2 <<EOF
Commit message is not a Conventional Commit:

  ${subject}

Use:
  <type>(optional-scope): description

Types: feat, fix, docs, style, refactor, perf, test, build, ci, chore, revert, deps
Examples:
  feat: show live groups on the Groups tab
  fix(alarms): load household AlarmClock lists
  feat!: require nearby devices permission
  chore(release): 1.2.0

See https://www.conventionalcommits.org/en/v1.0.0/
EOF
  return 1
}

check_subject() {
  local subject="$1"
  if skip_subject "$subject"; then
    return 0
  fi
  if valid_subject "$subject"; then
    return 0
  fi
  fail_subject "$subject"
}

subject_from_file() {
  local file="$1"
  local line
  while IFS= read -r line || [[ -n "$line" ]]; do
    case "$line" in
      \#*) continue ;;
      '') continue ;;
      *) printf '%s\n' "$line"; return 0 ;;
    esac
  done <"$file"
  return 1
}

check_range() {
  local from="$1"
  local to="$2"
  local zeros='0000000000000000000000000000000000000000'
  local subject failed=0
  if [[ "$from" == "$zeros" || -z "$from" ]]; then
    subject="$(git log -1 --format=%s "$to")"
    check_subject "$subject" || failed=1
    return "$failed"
  fi
  while IFS= read -r subject; do
    [[ -z "$subject" ]] && continue
    if ! check_subject "$subject"; then
      failed=1
    fi
  done < <(git log --format=%s --no-merges "$from..$to")
  return "$failed"
}

main() {
  case "${1:-}" in
    --subject)
      shift
      [[ $# -ge 1 ]] || { usage >&2; exit 2; }
      check_subject "$*"
      ;;
    --file)
      shift
      [[ $# -eq 1 ]] || { usage >&2; exit 2; }
      local subject
      subject="$(subject_from_file "$1")" || {
        echo "Commit message has no subject line." >&2
        exit 1
      }
      check_subject "$subject"
      ;;
    --range)
      shift
      [[ $# -eq 2 ]] || { usage >&2; exit 2; }
      check_range "$1" "$2"
      ;;
    -h|--help)
      usage
      ;;
    *)
      usage >&2
      exit 2
      ;;
  esac
}

main "$@"
