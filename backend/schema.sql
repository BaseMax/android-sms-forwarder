-- SMS Forwarder - SQLite schema
--
-- One row per stored SMS. The application creates these objects at startup
-- (see store.salam / Init), so this file is documentation and a way to
-- inspect or bootstrap the database by hand:
--
--   sqlite3 sms.db < schema.sql
--
-- TIME. Both instants below are UTC epoch MILLISECONDS. Nothing in this
-- database is in local time, and no column needs a timezone to be read:
-- `date` and `received_at` are absolute points in time, comparable across
-- every phone that ever uploads here. `tz_offset` records where the phone
-- was so a reader can be shown the wall-clock time it displayed; it is
-- descriptive only, and never enters ordering, filtering or de-duplication.
--
-- De-duplication is structural: the same message re-uploaded by a daily sync
-- collapses onto the row a real-time push already created, because
-- (address, body, date, type) is unique and inserts use INSERT OR IGNORE.
-- Because `date` is normalised to UTC milliseconds before it gets here, that
-- holds even when the two uploads spelled the timestamp in different units.

CREATE TABLE IF NOT EXISTS messages (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    device      TEXT    NOT NULL DEFAULT '',   -- which phone sent it up
    address     TEXT    NOT NULL DEFAULT '',   -- sender (inbox) or recipient (sent)
    body        TEXT    NOT NULL DEFAULT '',   -- message text
    date        INTEGER NOT NULL DEFAULT 0,    -- when it happened: UTC epoch MILLISECONDS
    type        INTEGER NOT NULL DEFAULT 1,    -- 1 = inbox (received), 2 = sent
    received_at INTEGER NOT NULL DEFAULT 0,    -- server ingest time: UTC epoch MILLISECONDS
    tz_offset   INTEGER NOT NULL DEFAULT 0,    -- the phone's UTC offset in minutes at `date`
                                               --   (210 = +03:30 Tehran, -300 = -05:00 New York)
    tz_name     TEXT    NOT NULL DEFAULT ''    -- that zone's IANA name, e.g. 'Asia/Tehran'
);

-- The de-dup key. Two uploads of the same SMS are the same row. Built on the
-- normalised UTC `date`, so the same message uploaded from two countries -
-- or in two different units - still collapses onto one row.
CREATE UNIQUE INDEX IF NOT EXISTS ux_messages_natural
    ON messages(address, body, date, type);

-- Restore path ("give me everything since T") reads by date.
CREATE INDEX IF NOT EXISTS ix_messages_date ON messages(date);

-- Schema generation, so store.salam can tell a database it has already
-- migrated from one written by an older build. Generation 1 is the one
-- above; generation 0 kept `received_at` in SECONDS and had neither timezone
-- column, and is upgraded in place on the next start.
PRAGMA user_version = 1;

-- Reading a row back by hand, if you want wall-clock text out of SQLite:
--
--   SELECT datetime(date / 1000, 'unixepoch')                          AS utc,
--          datetime(date / 1000, 'unixepoch', tz_offset || ' minutes') AS on_the_phone,
--          address, body
--   FROM messages ORDER BY date DESC LIMIT 20;
