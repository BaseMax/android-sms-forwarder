-- SMS Forwarder - SQLite schema
--
-- One row per stored SMS. The application creates these objects at startup
-- (see store.salam / Init), so this file is documentation and a way to
-- inspect or bootstrap the database by hand:
--
--   sqlite3 sms.db < schema.sql
--
-- De-duplication is structural: the same message re-uploaded by a daily sync
-- collapses onto the row a real-time push already created, because
-- (address, body, date, type) is unique and inserts use INSERT OR IGNORE.

CREATE TABLE IF NOT EXISTS messages (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    device      TEXT    NOT NULL DEFAULT '',   -- which phone sent it up
    address     TEXT    NOT NULL DEFAULT '',   -- sender (inbox) or recipient (sent)
    body        TEXT    NOT NULL DEFAULT '',   -- message text
    date        INTEGER NOT NULL DEFAULT 0,    -- device timestamp, epoch milliseconds
    type        INTEGER NOT NULL DEFAULT 1,    -- 1 = inbox (received), 2 = sent
    received_at INTEGER NOT NULL DEFAULT (strftime('%s','now'))  -- server ingest time, epoch seconds
);

-- The de-dup key. Two uploads of the same SMS are the same row.
CREATE UNIQUE INDEX IF NOT EXISTS ux_messages_natural
    ON messages(address, body, date, type);

-- Restore path ("give me everything since T") reads by date.
CREATE INDEX IF NOT EXISTS ix_messages_date ON messages(date);
