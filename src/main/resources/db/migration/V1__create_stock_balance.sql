CREATE TABLE stock_balance (
    account_id    VARCHAR(100) NOT NULL,
    sku           VARCHAR(100) NOT NULL,
    quantity      INTEGER      NOT NULL DEFAULT 0,
    updated_at    TIMESTAMP    NOT NULL DEFAULT NOW(),
    PRIMARY KEY (account_id, sku)
);