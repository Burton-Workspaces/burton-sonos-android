# Architecture

The app is a single Gradle module (`:app`), Kotlin, Jetpack Compose, Hilt, and OkHttp. UI never talks SOAP directly; screens collect `SonosRepository` state.

```
ui/          Compose screens and ViewModels (Hilt)
domain/      Player, ZoneGroup, Household, Alarm, NamedGroup, NowPlaying, …
data/
  discovery  SSDP (then mDNS) for speaker IPs
  soap       Envelope + SOAPACTION against /…/Control
  parse      ZoneGroupState, DIDL-Lite, alarm list, tiny JSON cache
  repository SonosControl (UPnP actions) + LocalPrefs (DataStore) + poll loop
di/          OkHttp client (short LAN timeouts)
```

## Discovery

`SpeakerDiscovery` sends SSDP `M-SEARCH` for Sonos and generic UPnP, holding a Wi-Fi multicast lock. It returns as soon as one Sonos `LOCATION` is parsed (typical port 1400). If SSDP finds nothing, it falls back to NSD `_sonos._tcp`.

`SonosControl.householdFrom(ip)` then:

1. GET `/xml/device_description.xml`
2. `ZoneGroupTopology/GetZoneGroupState`
3. `DeviceProperties/GetHouseholdID`

Topology XML is the source of truth for groups and members, including invisible satellites.

## Control plane

HTTP SOAP 1.1 to each player’s `http://<ip>:1400` control URLs. `SoapClient` builds the envelope, sets `SOAPACTION`, and maps response children to strings. Nested XML (for example `CurrentAlarmList` or `ZoneGroupState` when the speaker returns elements instead of escaped text) is serialized so parsers still see tags and attributes.

| Service | Typical actions |
| --- | --- |
| AVTransport | play/pause/skip, `SetAVTransportURI`, grouping via `x-rincon:`, `BecomeCoordinatorOfStandaloneGroup` |
| GroupRenderingControl / RenderingControl | group volume, mute |
| ContentDirectory | `Browse` for sources, search prefixes, playlists, favorites |
| AlarmClock | `ListAlarms`, `CreateAlarm`, `UpdateAlarm`, `DestroyAlarm` |

Grouping a room sets its AVTransport URI to `x-rincon:<coordinatorUuid>`. Line-in uses `x-rincon-stream:<uuid>`; TV uses `x-sonos-htastream:<uuid>:spdif`.

## Snapshot and polling

`SonosRepository` is a process singleton. `start()`:

1. Hydrates cached household so UI is not an empty spinner
2. Tries `lastSpeakerIp`, else SSDP
3. Applies selected playback first (fast path), then remaining groups’ now-playing **and** alarms in parallel
4. Polls `refresh(scan = false)` every 2 seconds

Volume sliders debounce SOAP `SetGroupVolume` (~90 ms) and keep the dragged value while that job is in flight.

Alarms are loaded from coordinators first, then other visible players, so a subwoofer or satellite without AlarmClock does not empty the Alarms tab.

## Local cache

`LocalPrefs` (DataStore) encodes household and named groups with a small JSON helper (`TinyJson`). Named group ids that start with `live-` are imports of current multi-room topology (stable on sorted member UUIDs). User-created groups use random UUIDs.

## UI shell

`MainActivity` hosts a `NavHost` and a persistent bottom bar. Hardware volume is intercepted only while the Now Playing route (`room/{groupId}`) is showing. Tab order is System → Sources → Search → Groups → Alarms.
