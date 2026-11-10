
CREATE TABLE room_photos (
                             id BIGSERIAL PRIMARY KEY,
                             room_id BIGINT NOT NULL
                                 REFERENCES rooms(id) ON DELETE CASCADE,
                             filename VARCHAR(100) NOT NULL UNIQUE,
                             created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_room_photos_room_id
    ON room_photos(room_id);
