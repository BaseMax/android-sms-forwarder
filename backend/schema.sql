CREATE TABLE IF NOT EXISTS messages (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    device      TEXT    NOT NULL DEFAULT '',
    address     TEXT    NOT NULL DEFAULT '',
    body        TEXT    NOT NULL DEFAULT '',
    date        INTEGER NOT NULL DEFAULT 0,
    type        INTEGER NOT NULL DEFAULT 1,
    received_at INTEGER NOT NULL DEFAULT 0,
    tz_offset   INTEGER NOT NULL DEFAULT 0,
    tz_name     TEXT    NOT NULL DEFAULT ''
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_messages_natural
    ON messages(address, body, date, type);

CREATE INDEX IF NOT EXISTS ix_messages_date ON messages(date);

PRAGMA user_version = 1;
