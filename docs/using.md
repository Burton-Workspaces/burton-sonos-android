# Using Burton Sonos

Burton Sonos controls speakers that are already on your LAN. Put the phone on the **same Wi-Fi** as the household. It will not find speakers on another VLAN, guest network, or through the Sonos cloud.

## Permissions

On first launch the app asks for discovery access:

| Android | Permission | Why |
| --- | --- | --- |
| 13+ | Nearby Wi-Fi devices | SSDP multicast to find speakers; not used for location |
| 12 and older | Fine location | Required by the platform for Wi-Fi multicast discovery |

Without that grant, the app cannot scan. Internet permission is only for HTTP to speakers (`http://<ip>:1400`) and album art URLs those speakers return.

## Screens

Bottom navigation, left to right: **System**, **Sources**, **Search**, **Groups**, **Alarms**. A compact now-playing bar sits above the tabs on every screen except the full Now Playing page.

### System

Lists current zone groups from `GetZoneGroupState`. Each row shows art, title, a volume slider for that group, and a grouping affordance. Tap a row to open **Now Playing** for that group.

The last household and selected group are cached on the phone so the next cold start can paint rooms before a full scan finishes.

### Now Playing

Full transport for the selected group: art, metadata, play/pause/skip, volume. Phone volume keys adjust that group’s volume while this screen is open. The mini bar is hidden here so it does not stack on the full player.

**Group** opens a sheet around the current coordinator: join other rooms with `x-rincon:<coordinator>`, or ungroup with `BecomeCoordinatorOfStandaloneGroup`.

### Sources

Browse objects already indexed on the household:

- Queue (`Q:0`), Sonos Favorites (`FV:2`), Sonos Playlists (`SQ:`)
- Local library (artists, albums, tracks, imported playlists)
- Music shares (`S:`), TuneIn (`R:0/0`)
- Line-in and TV inputs when a player advertises them

Line-in and TV start playback immediately on the selected group. Everything else opens a browse list.

The sync icon in the top right starts a household **Scan for new content** (`RefreshShareIndex`). It disables while the library index is running (`GetShareIndexInProgress`) and turns back on when that finishes.

### Search

Searches the local ContentDirectory (`A:ALBUMARTIST`, `A:ALBUM`, `A:TRACKS`, `A:PLAYLISTS`, `A:COMPOSER`, `A:GENRE`). Tap a playable row to play now on the selected group. The overflow sheet can:

- Save to Sonos Favorites
- Add to an existing Sonos playlist or create one
- Play now, play next, add to end of queue, replace queue

### Groups

**On this system** is live topology (every current zone group, including singles and multi-room sets). Multi-room groups can be saved or ungrouped.

**Saved groups** are named member lists stored in DataStore on the phone. Forming a saved group joins those rooms around a coordinator and peels off anyone who should not be in the set. Live multi-room groups are also imported as named presets so they survive after you ungroup.

### Alarms

Loads `ListAlarms` from a coordinator (household-wide AlarmClock). Existing alarms show time, recurrence, room, and an enable switch. You can add, edit, or delete alarms on the speakers; this is not a local-only reminder list.

### File an issue

Shake the phone, or long-press **About** in Settings. Burton Issues opens on New issue with this app already selected. Nothing is posted until you submit; Back cancels.

## What is stored on the phone

DataStore (`burton_sonos`):

- Last speaker IP and cached household (rooms, groups, player capabilities)
- Selected group id
- Named groups you created or imported

Alarms, queues, favorites, and playlists live on the Sonos system. Clearing app data only drops the local cache and named presets.
