CREATE TABLE catalog_run_state (
    id                INTEGER      PRIMARY KEY DEFAULT 1,
    is_running        BOOLEAN      NOT NULL DEFAULT FALSE,
    started_at        TIMESTAMPTZ,
    last_heartbeat_at TIMESTAMPTZ,
    instance_id       VARCHAR(255),
    CONSTRAINT single_row CHECK (id = 1)
);

INSERT INTO catalog_run_state (id, is_running) VALUES (1, false);
