# SMS Forwarder - Android app (Kotlin)

Scans every SMS on the phone and uploads it to your [backend](../backend) - the
moment a message arrives, once a day as a full sweep, and continuously in the
background via an always-on service - so nothing is lost when you switch phones.
When Telegram alerts are configured on the server, each new incoming SMS also
pings you there.

## How it stays running

```
  boot / app update  ->  BootReceiver  ->  start service + daily sync
  app opened         ->  MainActivity  ->  start service

  incoming SMS       ->  SmsReceiver   ->  WorkManager (expedited)  --+
  (works even when app closed)                                       |
  daily / heartbeat / "Sync now"  ->  SyncWorker (sweep)  -----------+--> POST /api/sms -> backend -> Telegram
```

Three independent mechanisms keep backups flowing even when the app is closed:

1. **Manifest `SmsReceiver`** - the system delivers `SMS_RECEIVED` to it and
   starts the app process if needed, so a new SMS is captured **without the app
   being open**. This alone covers real-time forwarding.
2. **`SmsForwarderService`** - a persistent **foreground service** (a quiet
   ongoing notification). It keeps the app a live, visible process that OEM
   battery managers are far less likely to kill, and runs a slow heartbeat that
   re-triggers a sync. Declared `stopWithTask="false"` (surviving app-swipe) and
   returns `START_STICKY` (the OS restarts it after a kill).
3. **WorkManager** - the daily `PeriodicWorkRequest` (persisted across reboots)
   and the expedited per-message uploads, with network constraints and
   exponential-backoff retries.

**Auto-start:** `BootReceiver` re-arms everything on `BOOT_COMPLETED`,
`QUICKBOOT_POWERON`, and `MY_PACKAGE_REPLACED`. Starting the foreground service
is done only from these allowed contexts and from the Activity - never from
`Application.onCreate` (which also runs on background broadcast starts and would
crash on Android 12+).

**Battery exemption:** the in-app **Allow** button opens the system prompt to
exempt the app from battery optimisation - the single biggest factor in whether
an OEM keeps a background app alive. The Home screen shows whether it is granted.

## Components

| File | Role |
| ---- | ---- |
| `receiver/SmsReceiver` | Wakes on each incoming SMS, reassembles multipart, enqueues an expedited upload. |
| `service/SmsForwarderService` | Always-on foreground service + heartbeat sync. |
| `service/ServiceController` | Start the service; report whether it is running. |
| `service/PowerSettings` | Battery-optimisation status + exemption intent. |
| `service/Notifications` | The service notification channel and builder. |
| `work/SyncWorker` | The one uploader: provider sweep, or direct upload of received messages. |
| `work/SyncScheduler` | Daily job, "Sync now", and expedited incoming upload. |
| `receiver/BootReceiver` | Re-arm service + sync after boot / update. |
| `data/SmsRepository` | Reads `Telephony.Sms.CONTENT_URI` (inbox + sent); returns normalised UTC messages plus the raw provider cursor. |
| `data/Settings` | Server URL, API key, device label, sync cursor, last-sync time, clock skew, counter (DataStore). |
| `MainActivity` + `ui/HomeScreen` | Redesigned one-screen Compose UI (status hero, protection, server, backup). |
| `ui/theme/Theme` | Material 3 brand theme (light/dark + dynamic color). |

De-duplication happens on the server (`(address, body, date, type)` is unique),
so the overlap between the real-time push, the heartbeat, and the daily sweep is
harmless.

## Architecture

The code is split into small, single-purpose layers, each depending only on the
ones below it:

- `core/` shared helpers: `AppLog` (one log tag), `Permissions`, and
  `TimeUtils` - the only file that normalises, formats or reasons about a
  timestamp.
- `data/` the sources: `Settings` (DataStore), `SmsRepository` (SMS provider),
  the `SmsMessageDto` model and its `MessageCodec`.
- `network/` Retrofit `SmsApi` and the `ApiClient` factory.
- `domain/` `BackupManager`, the one place that builds the API, uploads, sweeps
  the provider, and records progress. Every entry point (worker, service,
  view model) calls it, so that logic exists once.
- `work/`, `service/`, `receiver/` the Android entry points, each thin.
- `ui/` a `MainViewModel` holds the state (`HomeUiState`); `HomeScreen` and the
  `ui/home` + `ui/components` composables are stateless and driven by it, so
  `MainActivity` only wires the view model to framework calls.

## Design & branding

- **Material 3** throughout, with a teal-green brand palette and full light/dark
  schemes; dynamic color is used on Android 12+ when available.
- The Home screen leads with a **status hero** (permission needed -> add server -> active), then grouped cards: *Always-on protection* (service + battery),
  *Server*, and *Backup* (counts, last sync, sync button).
- **Adaptive launcher icon** (`res/drawable/ic_launcher_foreground` + a gradient
  `ic_launcher_background`) with a **monochrome** layer for Android 13+ themed
  icons, plus a dedicated white **status-bar icon** for the service notification.

## Time and timezones

Every instant that leaves this phone is **UTC epoch milliseconds**, serialized
as a JSON string because the backend's decoder truncates a bare integer that
large. Nothing is converted on the way out and no local time is ever uploaded.

That is not a simplification, it is what Android already gives us:
`Telephony.Sms.DATE` and `SmsMessage.timestampMillis` are absolute instants,
identical on two handsets in two countries that receive the same message.
`core/TimeUtils` is the only file that touches any of this, and it does three
things:

- **Normalises.** A handful of carriers and restore tools report the provider's
  `date` in seconds, which read as milliseconds would file every message in
  January 1970; a phone whose clock was never set reports a date that would
  sort and de-duplicate wrongly forever. Both are corrected with the same
  thresholds the server uses, so the two ends never disagree about what a
  number meant.
- **Reports the zone, without applying it.** Each message carries
  `tz_offset_minutes` and `tz_name` - the offset in force **at that message's
  own instant**, so a text received before a daylight-saving change keeps the
  offset that was true then, and a phone that has since flown abroad does not
  relabel its older messages. The server stores them beside the instant and
  uses them only to render the wall-clock time the handset showed. `ApiClient`
  also sends `X-Tz-Offset` / `X-Tz-Name` once per request, as a default for
  messages that do not carry their own.
- **Renders local time in exactly one place** - `TimeUtils.formatForPeople`,
  used only by the Home screen, and it names the zone so a reader who has
  travelled knows whose clock they are looking at.

The app also watches its own clock: every upload response carries the server's
time, `BackupManager` records the difference, and the Backup card says so when
the two are more than two minutes apart. A wrong phone date does not stop a
backup - it files messages under the wrong day - so it is worth surfacing.

Two stored values are deliberately kept apart in `Settings`, because they look
alike and are not:

| Value | What it is |
| ----- | ---------- |
| `syncCursor` | How far the sweep has read through the SMS provider, in the provider's own **raw** units. A position in a table; never formatted, never compared to a clock. |
| `lastSyncMs` | When the last sync actually finished, as a UTC instant. This is what "Last sync" shows. |

Feeding a normalised timestamp back into the provider's `DATE > ?` on a device
that stores seconds would match nothing and stall the sync silently, which is
why the cursor stays raw.

## Build

Requires Android Studio (Koala or newer) or a command-line Android SDK with
JDK 17. `minSdk = 26`, `targetSdk = 34`.

```sh
gradle wrapper --gradle-version 8.9   # generate the wrapper jar (not committed)
./gradlew :app:assembleDebug          # app/build/outputs/apk/debug/app-debug.apk
./gradlew installDebug                 # onto a connected device
```

## First-time setup on the phone

1. Launch the app and tap **Grant SMS permission** (SMS access must be approved
   at runtime; it cannot be granted from a store listing).
2. Tap **Allow** on the *Battery exemption* row so the OS keeps the service
   alive.
3. Enter the **Server URL** and the **API key** that matches the backend's
   `API_KEY`, set a **Device label**, tap **Save**, then **Test connection**.
4. Tap **Sync all messages now** to upload the existing history. From then on it
   runs on its own - including after a reboot.

## Permissions

| Permission | Why |
| ---------- | --- |
| `READ_SMS` | Read the SMS store for the scan and daily sweep |
| `RECEIVE_SMS` | Wake on each incoming message |
| `INTERNET`, `ACCESS_NETWORK_STATE` | Upload, and gate uploads on connectivity |
| `RECEIVE_BOOT_COMPLETED` | Start the service and re-arm the sync after a reboot |
| `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SPECIAL_USE` | Run the always-on service |
| `POST_NOTIFICATIONS` (Android 13+) | Show the ongoing service notification |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | Offer the battery-exemption prompt |

## Caveats

- The foreground service uses the `specialUse` type: honest for "continuously
  watch for SMS", but a Play Store release would need a justification review.
  Side-loading for personal use is unaffected. (Real-time capture does not
  actually depend on the service - the manifest receiver handles it - so if you
  prefer, the service can be dropped without losing forwarding.)
- Reading SMS is a sensitive permission; a Play Store release needs a
  declaration.
- Point the app at an **HTTPS** backend so the API key and message bodies are
  encrypted in transit.
