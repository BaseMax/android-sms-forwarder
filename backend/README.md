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
  "date": "1735680000000",
  "type": 1,
  "device": "pixel-7"
}
```

- `address` - sender (received) or recipient (sent).
- `body` - the text.
- `date` - device timestamp in **epoch milliseconds, as a JSON string**. It is
  a string on purpose: a bare integer that large is truncated by the JSON
  decoder. Numbers are still accepted but may lose precision - send a string.
- `type` - `1` = received/inbox, `2` = sent. Defaults to `1`.
- `device` - free-form label for the phone. May also be supplied once for the
  whole request via the `X-Device-Id` header.

A newly stored **inbound** (`type: 1`) message triggers a Telegram alert when
alerts are configured.

Response:

```json
{ "received": 2, "stored": 2, "duplicates": 0 }
```

### `GET /api/sms` - read back (restore path)

Query parameters:

- `since` - only messages with `date >= since` (epoch ms). Default `0` (all).
- `limit` - page size, capped at `1000`. Default `100`.
- `offset` - for paging. Default `0`.

Newest first:

```json
{
  "items": [
    { "id": 2, "device": "pixel-7", "address": "BANK", "body": "OTP 123456",
      "date": 1735680100000, "type": 1, "received_at": 1735680101 }
  ],
  "count": 42
}
```

`count` is the total matching `since`, so a client can page through it.

### `GET /api/stats`

```json
{ "total": 42 }
```

### `GET /health` (no auth)

```json
{ "status": "ok", "service": "sms-forwarder", "notifications": true }
```

## De-duplication

`(address, body, date, type)` is unique, and inserts use `INSERT OR IGNORE`, so
the daily full sync collapses onto the rows the real-time push already created.
`stored` in the response counts only genuinely new rows.

## Notes on the design

- **One SQLite connection, one mutex.** The server handles each request on its
  own thread; every database call takes a process-wide lock. For a personal SMS
  feed the traffic is tiny and serialising it is simpler and safer than a pool.
- **No SQL outside `store.salam`.** Values reach SQL only as bound parameters,
  so there is no string-built query anywhere in the program.
- **Secrets come from the environment**, never the phone or the source: the
  Telegram bot token lives here, so a stolen phone leaks no bot.

## Deploying

Put it behind TLS (a reverse proxy such as Caddy or nginx) so the API key and
message bodies are encrypted in transit, and keep `API_KEY` long and random.
The SQLite file is the whole database - back it up.
