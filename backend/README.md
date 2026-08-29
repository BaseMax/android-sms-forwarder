# SMS Forwarder - backend (Salam)

A tiny JSON API that receives SMS from the Android app, stores them in SQLite,
and (optionally) pings you on Telegram whenever a new message arrives. Written
in the [Salam](https://github.com/SalamLang/Salam) language.

## Why it exists

The phone loses SMS when you switch devices. The app uploads every message here
- once when a new SMS lands, and once a day as a full sweep - so the archive
lives on a server you control and can be pulled back onto a new phone.

## Files

One job per file, each depending only on the ones below it:

| File           | Role                                                            |
| -------------- | --------------------------------------------------------------- |
| `main.salam`   | Startup: read config, open the store, register routes, listen.  |
| `routes.salam` | The four endpoints. No SQL, no Telegram, no JSON text.          |
| `api.salam`    | What every route shares: replies, auth, query parameters.       |
| `ingest.salam` | A request body → stored messages → a report.                    |
| `store.salam`  | Storage: the `Sms` row type and every SQL statement, behind one mutex. |
| `notify.salam` | Telegram notifier.                                              |
| `config.salam` | The environment, read once. The only file that calls `os.Env`.  |
| `clock.salam`  | What an instant is: normalisation, UTC formatting, ISO parsing.  |
| `parse.salam`  | Numbers and fields out of untyped text and JSON. Knows nothing else. |
| `schema.sql`   | The table, for reference / manual inspection.                   |

Responses are never built as text. Each one is a struct - `store.Sms`,
`routes.Page`, `ingest.Report` - and `json.Marshal` derives its encoder at
compile time, so a field added to a struct changes the wire format and there
is no second place to keep in step.

## Build & run

You need the `salam` compiler on your PATH (see the Salam repo's `install.sh`)
and SQLite installed (`libsqlite3`).

```sh
# from this directory
salam build main.salam --output=sms-backend

# configure - the service refuses to start without API_KEY
export API_KEY="$(openssl rand -hex 32)"
export DB_PATH=sms.db
export PORT=8080
# optional Telegram alerts:
export BOT_TOKEN="123456:abc..."
export TELEGRAM_CHAT_ID="987654321"

./sms-backend
```

During development you can also run it without producing a binary:

```sh
salam run main.salam
```

### From CI

`.github/workflows/backend-linux.yml` builds the same executable for Linux
x86_64 on every push and pull request: it installs the pinned Salam release,
compiles `main.salam`, smoke tests the result (health, auth, ingest, read
back) and uploads `sms-backend-linux-x86_64.tar.gz` as a build artifact. On a
pull request the download link is posted as a comment, the way the Android
APK is. The binary is dynamically linked, so the host needs glibc and
`libsqlite3.so.0`.

## API

Every route except `GET /health` requires the header `X-API-Key: <API_KEY>`.
An unknown or missing key returns `401`.

### `POST /api/sms` - ingest

Accepts one message object, an array of them, or `{ "messages": [ ... ] }`.
The same endpoint serves both the real-time push and the daily bulk sync;
re-uploading a message you already sent is harmless (see de-duplication).

Message shape:

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

- `address` - sender (received) or recipient (sent).
- `body` - the text.
- `date` - when the message happened, in **UTC epoch milliseconds, as a JSON
  string**. It is a string on purpose: a bare integer that large is truncated
  by the JSON decoder. Numbers are still accepted but may lose precision - send
  a string. Epoch **seconds**, microseconds and nanoseconds are also accepted
  and converted; see [Time and timezones](#time-and-timezones).
- `type` - `1` = received/inbox, `2` = sent. Defaults to `1`.
- `device` - free-form label for the phone. May also be supplied once for the
  whole request via the `X-Device-Id` header.
- `tz_offset_minutes` - the phone's UTC offset in minutes when the message
  happened: `210` for Tehran (+03:30), `-300` for New York in winter. Optional;
  it is recorded, never applied. `tz_offset` is accepted as a shorthand, and
  the `X-Tz-Offset` header sets it once for a whole request.
- `tz_name` - the IANA zone, e.g. `"Asia/Tehran"`. Optional, for a human to
  read. `timezone` is accepted as a synonym, and `X-Tz-Name` sets it per
  request.

Neither timezone field is required. A client that sends nothing is treated as
UTC, and its messages are stored just as correctly - the offset only affects
how a stored message is *rendered* back, never which instant it is.

A newly stored **inbound** (`type: 1`) message triggers a Telegram alert when
alerts are configured.

Response:

```json
{
  "received": 2, "stored": 2, "duplicates": 0,
  "server_time_ms": 1787967036000,
  "server_time_utc": "2026-08-29T01:30:36.000Z"
}
```

`server_time_*` is the server's clock at the moment it answered. The app
compares it with its own and warns its owner when the two disagree, which is
the one time-related fault a phone cannot detect on its own.

### `GET /api/sms` - read back (restore path)

Query parameters:

- `since` - only messages with `date >= since`. Either UTC epoch milliseconds
  or an ISO-8601 timestamp: `?since=1735689600000`,
  `?since=2025-01-01T00:00:00Z` and `?since=2025-01-01T03:30:00%2B03:30` are
  the same request. An ISO string with no offset is read as UTC. Default `0`
  (all).
- `limit` - page size, capped at `1000`. Default `100`.
- `offset` - for paging. Default `0`.

Newest first:

```json
{
  "items": [
    {
      "id": 2, "device": "pixel-7", "address": "BANK", "body": "OTP 123456",
      "date": 1735689600789,
      "date_utc": "2025-01-01T00:00:00.789Z",
      "date_local": "2025-01-01T03:30:00.789+03:30",
      "type": 1,
      "tz_offset_minutes": 210,
      "tz_name": "Asia/Tehran",
      "received_at": 1787967036000,
      "received_at_utc": "2026-08-29T01:30:36.000Z"
    }
  ],
  "count": 42,
  "since": 0,
  "since_utc": "1970-01-01T00:00:00.000Z",
  "server_time_ms": 1787967047000,
  "server_time_utc": "2026-08-29T01:30:47.000Z"
}
```

`count` is the total matching `since`, so a client can page through it. `since`
is echoed back as the server understood it, so a client that sent a date - or
the wrong unit - can see what it became.

### `GET /api/stats`

```json
{
  "total": 42,
  "server_time_ms": 1787967060000,
  "server_time_utc": "2026-08-29T01:31:00.000Z"
}
```

### `GET /health` (no auth)

```json
{
  "status": "ok", "service": "sms-forwarder", "notifications": true,
  "timezone": "UTC",
  "server_time_ms": 1787967036000,
  "server_time_utc": "2026-08-29T01:30:36.000Z"
}
```

`timezone` is a constant. It is the service promising that every instant it
hands out is UTC, so a client never has to discover where the server is - and
a server moved to another country does not change what its answers mean. This
route needs no API key, which also makes it the clock a phone checks itself
against.

## Time and timezones

Phones are in different countries, on different networks, with clocks of
varying quality. The service handles that with one rule and one conversion.

**The rule: every instant here is UTC epoch milliseconds.** In the database,
on the wire, in a Telegram alert. Nothing is stored in local time, and the
machine's own timezone is never read - a server moved from Frankfurt to
Singapore keeps answering identically.

That works because an SMS timestamp is *already* absolute. Android's
`Telephony.Sms.DATE` and `SmsMessage.timestampMillis` are epoch milliseconds,
not wall-clock readings, so two handsets on opposite sides of the world that
receive the same message upload the same number. Nothing needs converting. The
only real risk is a number arriving in a form that *looks* like epoch
milliseconds but is not, so that is the one thing ingest checks
(`clock.NormalizeMs`):

| What arrives | What is stored | Why |
| ------------ | -------------- | --- |
| `1735689600000` (ms) | unchanged | already right |
| `1735689600` (seconds) | `1735689600000` | plenty of gateways and export tools send seconds; read as ms it would land in Jan 1970 |
| `1735689600000000` (µs) | `1735689600000` | same |
| `1735689600000000000` (ns) | `1735689600000` | same |
| `315532800000` (a phone stuck in 1980) | the ingest time | a wrong clock would poison ordering and de-duplication forever |
| more than a day in the future | the ingest time | likewise |
| `0`, negative, absent | the ingest time | nothing usable was said |

The four unit bands cannot overlap for any date between the year 2000 and the
year 33000, so the unit is recoverable from the magnitude alone. No timezone
is ever applied during this: the number already names an instant, and shifting
it would be the bug rather than the fix.

**Where the phone was is recorded, not applied.** The wall-clock time the
owner actually saw is the one thing UTC alone loses, so the app sends its UTC
offset alongside each message and the server keeps it in `tz_offset`. Reading
a message back, you get both:

```json
"date":       1735689600789,
"date_utc":   "2025-01-01T00:00:00.789Z",
"date_local": "2025-01-01T03:30:00.789+03:30"
```

Those are the same instant, twice: one absolute, one as the handset displayed
it. Because the offset travels in the string, `date_local` parses back to
exactly `date`. An offset - not a zone name - is all that rendering needs,
which is why the server carries no timezone database. Half-hour and
quarter-hour zones (Tehran +03:30, Kabul +04:30, Kathmandu +05:45, Eucla
+08:45) and daylight saving all come out right, because the phone reports the
offset that was in force at that message's own instant rather than a fixed one.

Consequences worth knowing:

- **De-duplication is timezone-proof.** The unique key is
  `(address, body, date, type)` on the normalised UTC value, so the same
  message uploaded from two countries, or in two different units, still
  collapses onto one row.
- **`since` is unambiguous.** `?since=` takes epoch milliseconds or ISO-8601;
  an ISO string without an offset is read as UTC, never as the server's local
  time.
- **A wrong phone clock is visible.** Every response carries `server_time_ms`,
  and the app compares it against its own to warn its owner. The server does
  not silently "correct" a plausible-but-wrong timestamp - only implausible
  ones are replaced.
- **Alerts spell out both readings.** A Telegram notification shows the UTC
  instant and, when the phone said where it was, the time the handset showed.

Existing databases are migrated on the next start: generation 0 kept
`received_at` in *seconds* while `date` was in milliseconds, and had no
timezone columns. `PRAGMA user_version` records the generation, so the upgrade
runs once.

## De-duplication

`(address, body, date, type)` is unique, and inserts use `INSERT OR IGNORE`, so
the daily full sync collapses onto the rows the real-time push already created.
`stored` in the response counts only genuinely new rows. `date` is the
normalised UTC value, so two uploads of the same message agree even when they
spelled the timestamp differently or came from phones in different countries.

## Notes on the design

- **One SQLite connection, one mutex.** The server handles each request on its
  own thread; every database call takes a process-wide lock. For a personal SMS
  feed the traffic is tiny and serialising it is simpler and safer than a pool.
- **No SQL outside `store.salam`.** Values reach SQL only as bound parameters,
  so there is no string-built query anywhere in the program.
- **Secrets come from the environment**, never the phone or the source: the
  Telegram bot token lives here, so a stolen phone leaks no bot.
- **One file owns time.** `clock.salam` is the only place that decides what a
  timestamp means or how it is written, and the only place that formats one.
  No other file does date arithmetic, and none reads the machine's timezone.

## Deploying

Put it behind TLS (a reverse proxy such as Caddy or nginx) so the API key and
message bodies are encrypted in transit, and keep `API_KEY` long and random.
The SQLite file is the whole database - back it up.
