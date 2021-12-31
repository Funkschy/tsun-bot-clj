CREATE TABLE IF NOT EXISTS user (
    id INTEGER PRIMARY KEY,
    name TEXT,
    role INTEGER CHECK(role <= 2 AND role >= 0)
)
