CREATE TABLE IF NOT EXISTS url_mappings (
    id SERIAL PRIMARY KEY,
    short_url VARCHAR(7) NOT NULL UNIQUE,
    long_url TEXT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_id ON url_mappings(id);
CREATE INDEX IF NOT EXISTS idx_short_url ON url_mappings(short_url);
CREATE INDEX IF NOT EXISTS idx_long_url ON url_mappings(long_url);
