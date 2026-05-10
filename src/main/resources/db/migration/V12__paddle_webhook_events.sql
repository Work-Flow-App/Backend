CREATE TABLE paddle_webhook_events (
    event_id       VARCHAR(100)  NOT NULL,
    event_type     VARCHAR(100)  NOT NULL,
    processed_at   DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (event_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;