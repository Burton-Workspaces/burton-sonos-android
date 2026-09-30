# Development

## Tooling

- JDK **17**
- Android SDK compile/target **35**, min **26**
- Android Gradle Plugin 8.7.3, Kotlin 2.0.21, Compose BOM 2024.12.01
- Hilt 2.53.1 (KSP)

Point Gradle at the SDK with `local.properties` (`sdk.dir=…`). That file is gitignored.

## Commands

```bash
./gradlew :app:assembleDebug
./gradlew :app:installDebug
./gradlew testDebugUnitTest
```

Release assemble is blocked unless `keystore.properties` exists and `storeFile` points at a real keystore. Copy [`keystore.properties.example`](../keystore.properties.example) and keep `keystore.properties`, `*.jks`, and `*.keystore` out of git (see `.gitignore`).

Debug application id is `com.burton.sonos.debug` so it can sit next to a signed install.

## Layout

```
app/src/main/java/com/burton/sonos/
  MainActivity.kt              permissions, nav, mini player, volume keys
  data/discovery/              SSDP / mDNS
  data/soap/                   SoapClient, service URNs and paths
  data/parse/                  ZoneGroupState, DIDL, alarms, XML, JSON
  data/repository/             SonosControl, SonosRepository, DataStore
  domain/                      models
  ui/rooms, room, sources, search, browse, groups, group, alarms, components, theme
app/src/test/java/…/data/parse Parser and cache tests (no device)
```

Parser tests cover topology XML, DIDL (including SOAP-escaped and nested ampersands), alarm lists (escaped and nested `CurrentAlarmList`), named-group cache, and library search object ids. Run those before changing SOAP parsing.

## Network while debugging

The emulator often cannot see SSDP on a home LAN. Use a physical device on the same Wi-Fi as the speakers. Charles/mitm is rarely useful: traffic is unencrypted HTTP to port 1400 but multicast discovery will not traverse typical proxy setups.

If rooms never appear: confirm `NEARBY_WIFI_DEVICES` / location, multicast not blocked, and that `http://<speaker>:1400/xml/device_description.xml` loads in the phone’s browser.

## Versioning while developing

Do not hand-edit `CHANGELOG.md` or `version.txt` on feature branches. Those are owned by [release-please](releases.md) from Conventional Commits on `master`.

Commit subjects must follow Conventional Commits. Install the hook once:

```bash
./scripts/install-git-hooks.sh
```

See [CONTRIBUTING.md](../CONTRIBUTING.md).
