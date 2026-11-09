-- Кастодиальные кошельки. Приватный ключ только в зашифрованном виде (AES-256-GCM).
CREATE TABLE wallets
(
    id                    UUID PRIMARY KEY,
    type                  VARCHAR(16)  NOT NULL,
    address               VARCHAR(42)  NOT NULL UNIQUE,
    encrypted_private_key TEXT         NOT NULL,
    created_at            TIMESTAMPTZ  NOT NULL,
    CONSTRAINT ck_wallets_type CHECK (type IN ('TREASURY', 'CLIENT', 'MERCHANT'))
);
-- Казна в системе ровно одна
CREATE UNIQUE INDEX ux_wallets_single_treasury ON wallets (type) WHERE type = 'TREASURY';

CREATE TABLE merchants
(
    id           UUID PRIMARY KEY,
    name         VARCHAR(255) NOT NULL,
    api_key_hash VARCHAR(64)  NOT NULL UNIQUE,
    wallet_id    UUID         NOT NULL REFERENCES wallets (id),
    created_at   TIMESTAMPTZ  NOT NULL
);

CREATE TABLE clients
(
    id          UUID PRIMARY KEY,
    external_id VARCHAR(128) NOT NULL UNIQUE,
    wallet_id   UUID REFERENCES wallets (id),
    created_at  TIMESTAMPTZ  NOT NULL
);

CREATE TABLE payments
(
    id                 UUID PRIMARY KEY,
    merchant_id        UUID           NOT NULL REFERENCES merchants (id),
    client_id          UUID REFERENCES clients (id),
    client_external_id VARCHAR(128)   NOT NULL,
    amount             NUMERIC(36, 18) NOT NULL,
    currency           VARCHAR(8)     NOT NULL,
    status             VARCHAR(32)    NOT NULL,
    tx_hash            VARCHAR(66),
    failure_code       VARCHAR(64),
    failure_message    TEXT,
    idempotency_key    VARCHAR(255)   NOT NULL,
    request_hash       VARCHAR(64)    NOT NULL,
    submitted_at       TIMESTAMPTZ,
    created_at         TIMESTAMPTZ    NOT NULL,
    updated_at         TIMESTAMPTZ    NOT NULL,
    CONSTRAINT ck_payments_amount_positive CHECK (amount > 0)
);
CREATE UNIQUE INDEX ux_payments_merchant_idempotency ON payments (merchant_id, idempotency_key);
CREATE INDEX ix_payments_status ON payments (status);
CREATE INDEX ix_payments_merchant_created ON payments (merchant_id, created_at DESC);

CREATE TABLE refunds
(
    id              UUID PRIMARY KEY,
    payment_id      UUID            NOT NULL REFERENCES payments (id),
    amount          NUMERIC(36, 18) NOT NULL,
    status          VARCHAR(16)     NOT NULL,
    tx_hash         VARCHAR(66),
    failure_code    VARCHAR(64),
    failure_message TEXT,
    idempotency_key VARCHAR(255)    NOT NULL,
    request_hash    VARCHAR(64)     NOT NULL,
    submitted_at    TIMESTAMPTZ,
    created_at      TIMESTAMPTZ     NOT NULL,
    updated_at      TIMESTAMPTZ     NOT NULL,
    CONSTRAINT ck_refunds_amount_positive CHECK (amount > 0)
);
CREATE UNIQUE INDEX ux_refunds_payment_idempotency ON refunds (payment_id, idempotency_key);
CREATE INDEX ix_refunds_status ON refunds (status);

-- Аудит всех переходов статуса платежа
CREATE TABLE payment_status_history
(
    id          BIGSERIAL PRIMARY KEY,
    payment_id  UUID        NOT NULL REFERENCES payments (id),
    from_status VARCHAR(32),
    to_status   VARCHAR(32) NOT NULL,
    changed_at  TIMESTAMPTZ NOT NULL,
    note        TEXT
);
CREATE INDEX ix_payment_status_history_payment ON payment_status_history (payment_id, id);

-- Раздачи ETH из казны
CREATE TABLE treasury_transfers
(
    id           UUID PRIMARY KEY,
    to_wallet_id UUID            NOT NULL REFERENCES wallets (id),
    amount       NUMERIC(36, 18) NOT NULL,
    tx_hash      VARCHAR(66),
    status       VARCHAR(16)     NOT NULL,
    created_at   TIMESTAMPTZ     NOT NULL
);
CREATE INDEX ix_treasury_transfers_status ON treasury_transfers (status);
