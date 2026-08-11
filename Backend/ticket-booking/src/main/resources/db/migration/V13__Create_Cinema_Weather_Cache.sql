-- Create cinema_weather_cache table for storing cinema location coordinates and hourly forecast JSON caches
CREATE TABLE IF NOT EXISTS cinema_weather_cache (
    cinema_id UUID PRIMARY KEY REFERENCES cinemas(id) ON DELETE CASCADE,
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    last_weather_data TEXT,
    last_fetched_at TIMESTAMP,
    provider VARCHAR(50)
);
