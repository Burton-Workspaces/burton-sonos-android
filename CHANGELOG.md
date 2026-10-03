# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).
Automatic releases are produced by [release-please](https://github.com/googleapis/release-please)
from [Conventional Commits](https://www.conventionalcommits.org/).

## [1.8.0](https://github.com/Burton-Workspaces/burton-sonos-android/compare/v1.7.0...v1.8.0) (2026-10-03)


### Features

* file issues by shaking or long-pressing About ([d84542c](https://github.com/Burton-Workspaces/burton-sonos-android/commit/d84542cf52207118c22f2b928617456e4b5bc271))

## [1.7.0](https://github.com/Burton-Workspaces/burton-sonos-android/compare/v1.6.0...v1.7.0) (2026-10-01)


### Features

* add grayscale album art option in settings ([ce4f794](https://github.com/Burton-Workspaces/burton-sonos-android/commit/ce4f794c17f3555bc330c68a760bd1a057115687))


### Bug Fixes

* **ui:** pin Save on editor screens and match alarm/group chrome ([4228d8b](https://github.com/Burton-Workspaces/burton-sonos-android/commit/4228d8b5811033537082854d207b5d3d620bc681))

## [1.6.0](https://github.com/Burton-Workspaces/burton-sonos-android/compare/v1.5.0...v1.6.0) (2026-09-30)


### Features

* align F-Droid Pages publish with burton-slack ([c89bdbd](https://github.com/Burton-Workspaces/burton-sonos-android/commit/c89bdbd1dae30523170bad2159ee3b2e30250f55))


### Bug Fixes

* **alarms:** parse ListAlarms without double-unescaping metadata ([c8ecfe0](https://github.com/Burton-Workspaces/burton-sonos-android/commit/c8ecfe0cf832901711ba83ba3b040dad7fec1728))
* **groups:** show household Areas such as Downstairs ([1020c32](https://github.com/Burton-Workspaces/burton-sonos-android/commit/1020c32b93543660a045a2f9d3375776458056bf))

## [1.5.0](https://github.com/Burton-Workspaces/burton-sonos-android/compare/v1.4.1...v1.5.0) (2026-09-30)


### Features

* move named groups and alarms into settings ([bf88e3a](https://github.com/Burton-Workspaces/burton-sonos-android/commit/bf88e3a4ea001cbe41decbccdf9cf1e2a3fda246))


### Bug Fixes

* require pipx fdroidserver for AGP 8.7 APKs ([e28e58e](https://github.com/Burton-Workspaces/burton-sonos-android/commit/e28e58ea141104bb7f0aa074be08667ae3d11698))

## [1.4.1](https://github.com/Burton-Workspaces/burton-sonos-android/compare/v1.4.0...v1.4.1) (2026-09-30)


### Bug Fixes

* remove extra top inset above screen titles ([b382799](https://github.com/Burton-Workspaces/burton-sonos-android/commit/b382799b49c585f45ccffafae9b5613118e46a5b))

## [1.4.0](https://github.com/Burton-Workspaces/burton-sonos-android/compare/v1.3.0...v1.4.0) (2026-09-30)


### Features

* open alarms, groups, and settings as full-screen modals ([ea17708](https://github.com/Burton-Workspaces/burton-sonos-android/commit/ea17708a4eb743fc1b3cbd68793561f212d3b078))

## [1.3.0](https://github.com/rconnelly/burton-sonos-android/compare/v1.2.0...v1.3.0) (2026-09-30)


### Features

* **sources:** scan for new library content ([0a380a1](https://github.com/rconnelly/burton-sonos-android/commit/0a380a1447d37ef7513d1852fd588b074554cd06))

## [1.2.0](https://github.com/rconnelly/burton-sonos-android/compare/v1.1.0...v1.2.0) (2026-09-30)


### Features

* **sources:** scan for new library content ([0a380a1](https://github.com/rconnelly/burton-sonos-android/commit/0a380a1447d37ef7513d1852fd588b074554cd06))

## [Unreleased]

## [1.1.0] - 2026-09-29

### Added
- Named groups you can create, rename, delete, and form around saved speakers
- Search overflow actions for favorites, Sonos playlists, play now/next, queue, and replace queue
- Pulsing panel placeholders on first load
- Per-room volume sliders on the System screen
- Hardware volume keys on Now Playing

### Changed
- Bottom navigation and now playing stay visible on every screen, including Search
- Live speaker grouping moved off Now Playing into its own modal
- First load uses a cached household and returns as soon as a speaker is found
