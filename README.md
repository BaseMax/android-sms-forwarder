# Android SMS Forwarder

Back up every SMS on an Android phone to a server you control - the moment a
message arrives, and once a day as a full sweep - so you never lose your texts
when you switch phones. Each new incoming SMS can also ping you on Telegram.

> Built for a simple need: *"I need an app that scans all SMS and uploads to an
> API server every day, and whenever a new SMS is received - I keep losing SMS
> when I switch phones."*

## Two parts

| Part                     | Language | What it is                                                        |
| ------------------------ | -------- | ----------------------------------------------------------------- |
| [`android/`](android/)   | Kotlin   | The phone app: reads SMS, uploads on arrival and daily.           |
| [`backend/`](backend/)   | [Salam](https://github.com/SalamLang/Salam) | A small JSON API over **SQLite** that stores messages and sends Telegram alerts. |

```
  Android app (Kotlin)                                  Salam backend
  reads SMS, uploads on          --  POST /api/sms  -->  stores in SQLite,
  arrival and daily              <-- GET  /api/sms  ---  sends Telegram alert
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
  "date": "1735680000000",
  "type": 1,
  "device": "pixel-7"
}
```

`date` is epoch **milliseconds sent as a string** (a bare integer that large is
truncated by the decoder); `type` is `1` for received, `2` for sent. Messages
are de-duplicated on `(address, body, date, type)`, so the real-time push and
the daily sweep can overlap freely. Every route except `GET /health` requires
the shared secret in the `X-API-Key` header.

Full API and per-component notes are in each half's README:
[backend/README.md](backend/README.md) and [android/README.md](android/README.md).

## Quick start

**Backend** (needs the `salam` compiler and `libsqlite3`):

```sh
cd backend
salam build main.salam --output=sms-backend
export API_KEY="$(openssl rand -hex 32)"
export BOT_TOKEN="123456:abc..."      # optional, from @BotFather
export TELEGRAM_CHAT_ID="987654321"   # optional, your own chat id
./sms-backend                          # listens on :8080
```

Put it behind HTTPS (a reverse proxy) before pointing a phone at it.

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
- **All SQL is in one file** (`backend/store.salam`) and reaches the database
  only as bound parameters - no query is built by string concatenation.
- **The phone keeps a high-water mark** (the newest timestamp uploaded) and only
  sends what is newer, so the daily sweep is cheap after the first full scan.
- **Backups keep running with the app closed:** a manifest SMS receiver catches
  new texts, a persistent foreground service keeps the app alive (auto-started
  on boot, surviving app-swipe and system-kill), and WorkManager handles the
  daily sweep and retries. An in-app prompt requests battery-optimisation
  exemption to keep it punctual. See [android/README.md](android/README.md).

## License

See [LICENSE](LICENSE).
