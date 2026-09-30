# Contributing

## Commit messages

This repo **requires** [Conventional Commits](https://www.conventionalcommits.org/en/v1.0.0/). Release-please uses those subjects to bump SemVer, write `CHANGELOG.md`, and open a release PR.

```
<type>(optional-scope): description
```

| Type | Release |
| --- | --- |
| `feat` | minor |
| `fix`, `perf` | patch |
| `feat!` / `BREAKING CHANGE:` footer | major |
| `docs`, `style`, `refactor`, `test`, `build`, `ci`, `chore`, `deps`, `revert` | no bump (changelog rules still apply) |

Examples:

```
feat: show live groups on the Groups tab
fix(alarms): load household AlarmClock lists
docs: add using guide
ci: enforce conventional commits
chore(release): 1.2.0
```

Git merge subjects (`Merge pull request …`) and `git revert` default subjects are allowed. Everything else is rejected.

### Local hook

```bash
./scripts/install-git-hooks.sh
```

That copies `.githooks/commit-msg` into `.git/hooks` so `git commit` fails before a bad subject is created.

### CI

Pushes to `master` and pull requests run **Conventional commits**:

- PR **title** and every non-merge commit on the branch
- Every commit in a push to `master`

Require that check in GitHub branch protection if you want GitHub to block the merge button.

Do not hand-edit `version.txt` or `CHANGELOG.md` on feature work; see [docs/releases.md](docs/releases.md). First-time Actions setup: [docs/build-automation.md](docs/build-automation.md).
