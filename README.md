# Android SMS Forwarder

Back up every SMS on an Android phone to a server you control - the moment a
message arrives, and once a day as a full sweep - so you never lose your texts
when you switch phones. The server also forwards a full copy of every stored
message to your own Telegram, reliably (queued, rate-limited, and re-tried
until it lands).

> Built for a simple need: *"I need an app that scans all SMS and uploads to an
> API server every day, and whenever a new SMS is received - I keep losing SMS
> when I switch phones."*

## Two parts

| Part                     | Language | What it is                                                        |
| ------------------------ | -------- | ----------------------------------------------------------------- |
| [`android/`](android/)   | Kotlin   | The phone app: reads SMS, uploads on arrival and daily.           |
| [`backend/`](backend/)   | [Salam](https://github.com/SalamLang/Salam) | A small JSON API over **SQLite** that stores messages and forwards each to Telegram. |

```
  Android app (Kotlin)                                  Salam backend
  reads SMS, uploads on          --  POST /api/sms  -->  stores in SQLite,
  arrival and daily              <-- GET  /api/sms  ---  forwards each row
                                     (restore new phone)          |
                                                                  v
                                                              Telegram
                                                      ("message myself as a bot")
```

## The JSON contract

`POST /api/sms` takes one message, an array, or `{ "messages": [ ... ] }`:

```json
{
  "address": "+15551234567",
  "body": "Your OTP is 123456",
  "date": "1735689600000",
  "type": 1,
  "device": "pixel-7",
  "tz_offset_minutes": 210,
  "tz_name": "Asia/Tehran"
}
```

`date` is **UTC epoch milliseconds sent as a string** (a bare integer that
large is truncated by the decoder); `type` is `1` for received, `2` for sent.
The two `tz_*` fields are optional and say where the phone was, so the server
can show the time the handset displayed - they are recorded, never applied.
Messages are de-duplicated on `(address, body, date, type)`, so the real-time
push and the daily sweep can overlap freely. Every route except `GET /health`
requires the shared secret in the `X-API-Key` header.

## Quick start

**Backend** (needs the `salam` compiler and `libsqlite3`):

```sh
cd backend
salam build main.salam --output=sms-backend
cp ../.env.example ../.env   # then edit ../.env: set API_KEY, and BOT_TOKEN + TELEGRAM_CHAT_ID for Telegram
./sms-backend                # reads .env from the current dir, then the parent; listens on :8080
```

Config comes from the environment; the service loads a `.env` file (current
directory first, then the parent) so a plain `./sms-backend` just works. Real
environment variables still win over the file. Put the server behind HTTPS (a
reverse proxy) before pointing a phone at it.

**Android** (needs Android Studio / the Android SDK, JDK 17):

```sh
cd android
gradle wrapper --gradle-version 8.9
./gradlew installDebug
```

Then on the phone: grant SMS permission, enter the server URL + the same
`API_KEY`, tap **Save**, **Test connection**, and **Sync now**. It runs itself
after that.

## Design notes

- **The bot token lives on the server, never the phone**, so a lost phone leaks
  no Telegram credentials.
- **Telegram delivery cannot silently drop a message.** Each stored row is
  marked delivered only after Telegram accepts it; a background worker sends
  them one at a time (about one per second, so a bulk restore never trips
  Telegram's rate limit) and retries transient failures. Because the "not yet
  sent" state lives in SQLite, a crash or restart resumes exactly where it left
  off.
- **All SQL lives in one layer** (`backend/store/queries.salam`) and reaches the
  database only as bound parameters - no query is built by string concatenation.
- **The phone keeps a high-water mark** (how far it has read through the SMS
  provider) and only sends what is newer, so the daily sweep is cheap after the
  first full scan.
- **Every instant is UTC, everywhere.** Phones in different countries - and on
  half-hour offsets like Tehran's +03:30 - all upload the same absolute number,
  and each also reports its own UTC offset so the server can show the
  wall-clock time that phone displayed. Timestamps arriving in the wrong unit
  (seconds instead of milliseconds) or from a phone whose clock is plainly
  wrong are corrected at the door, and the app warns its owner when its clock
  disagrees with the server's.
- **Backups keep running with the app closed:** a manifest SMS receiver catches
  new texts, a persistent foreground service keeps the app alive (auto-started
  on boot, surviving app-swipe and system-kill), and WorkManager handles the
  daily sweep and retries. An in-app prompt requests battery-optimisation
  exemption to keep it punctual.

## License

See [LICENSE](LICENSE).
