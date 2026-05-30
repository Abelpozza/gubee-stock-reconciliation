CREATE TABLE stock_events (
    id                  BIGSERIAL    PRIMARY KEY,
    event_id            VARCHAR(100) NOT NULL UNIQUE,
    type                VARCHAR(50)  NOT NULL,
    account_id          VARCHAR(100) NOT NULL,
    sku                 VARCHAR(100) NOT NULL,
    marketplace         VARCHAR(100),
    external_order_id   VARCHAR(100),
    quantity            INTEGER,
    available           INTEGER,
    quantity_sent       INTEGER,
    reason              VARCHAR(255),
    occurred_at         TIMESTAMP    NOT NULL,
    received_at         TIMESTAMP    NOT NULL DEFAULT NOW(),
    status              VARCHAR(20)  NOT NULL DEFAULT 'PROCESSED',
    ignore_reason       VARCHAR(255)
);

CREATE INDEX idx_stock_events_account_sku
    ON stock_events (account_id, sku);

CREATE INDEX idx_stock_events_status
    ON stock_events (status);

CREATE INDEX idx_stock_events_order
    ON stock_events (account_id, sku, external_order_id, type);