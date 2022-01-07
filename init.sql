CREATE TABLE IF NOT EXISTS user (
    id INTEGER PRIMARY KEY,
    role INTEGER CHECK(role <= 2 AND role >= 0)
)
