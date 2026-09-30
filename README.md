# Burton Sonos

A local Android controller for a Sonos household. The phone talks to speakers on the same Wi-Fi using SSDP discovery and the speakers’ UPnP/SOAP APIs. There is no Sonos cloud account, no remote access, and no music-service login in this app.

Current version is in [`version.txt`](version.txt) (`1.1.0`). Signed APKs are published on [GitHub Releases](https://github.com/rconnelly/burton-sonos-android/releases).

## What it does

- **System** — live rooms and groups, per-group volume, play/pause into Now Playing
- **Sources** — queue, favorites, Sonos playlists, local library, shares, TuneIn, line-in, TV
- **Search** — library search; tap to play, overflow for favorites / playlists / queue actions
- **Groups** — current topology plus named sets you can save and form later
- **Alarms** — household alarms already set on the system, plus create / edit / enable
- **Now Playing** — transport, volume (including hardware keys), grouping sheet; the mini bar hides on this screen

First launch hydrates the last household from local cache and returns as soon as one speaker answers, then fills in the rest.

## Requirements

- Android 8.0+ (API 26)
- Same Wi-Fi as the Sonos system
- Nearby devices permission on Android 13+ (`NEARBY_WIFI_DEVICES`); fine location on older versions (SSDP multicast)
- Speakers reachable on HTTP port **1400** (the app allows cleartext LAN traffic)

## Docs

| Doc | Contents |
| --- | --- |
| [Using the app](docs/using.md) | Screens, permissions, and what lives on-device vs on the speakers |
| [Architecture](docs/architecture.md) | Packages, discovery, SOAP, caching, polling |
| [Development](docs/development.md) | Build, run, test, project layout |
| [Releases](docs/releases.md) | SemVer, signing, CI, GitHub Releases |

## Quick start (debug)

```bash
./gradlew :app:installDebug
```

Debug builds use application id `com.burton.sonos.debug`. Release builds need a keystore; see [docs/releases.md](docs/releases.md).

```bash
./gradlew testDebugUnitTest
```

## License and scope

This is a household LAN controller. It does not replace the official Sonos app for account, software updates, or adding streaming services.
