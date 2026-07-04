CREATE TABLE tariff (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR NOT NULL,
    version INT NOT NULL DEFAULT 1,
    name VARCHAR NOT NULL,
    type VARCHAR NOT NULL,
    monthly_fee NUMERIC(10,2) NOT NULL,
    minutes_included INT DEFAULT 0,
    sms_included INT DEFAULT 0,
    data_mb_included INT DEFAULT 0,
    status VARCHAR DEFAULT 'ACTIVE',
    effective_from TIMESTAMP WITH TIME ZONE NOT NULL,
    effective_to TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    CONSTRAINT uq_tariff_code_version UNIQUE (code, version)
);

CREATE TABLE addon (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR NOT NULL,
    version INT NOT NULL DEFAULT 1,
    name VARCHAR NOT NULL,
    type VARCHAR NOT NULL,
    validity_days INT DEFAULT 30,
    price NUMERIC(10,2) NOT NULL,
    data_mb INT DEFAULT 0,
    minutes INT DEFAULT 0,
    sms_count INT DEFAULT 0,
    status VARCHAR DEFAULT 'ACTIVE',
    effective_from TIMESTAMP WITH TIME ZONE NOT NULL,
    effective_to TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    CONSTRAINT uq_addon_code_version UNIQUE (code, version)
);

CREATE TABLE tariff_addon (
    tariff_id BIGINT NOT NULL,
    addon_id BIGINT NOT NULL,
    PRIMARY KEY (tariff_id, addon_id),
    CONSTRAINT fk_tariff_addon_tariff FOREIGN KEY (tariff_id) REFERENCES tariff(id),
    CONSTRAINT fk_tariff_addon_addon FOREIGN KEY (addon_id) REFERENCES addon(id)
);

CREATE TABLE outbox_event (
    id BIGSERIAL PRIMARY KEY,
    aggregate_type VARCHAR NOT NULL,
    aggregate_id VARCHAR NOT NULL,
    event_type VARCHAR NOT NULL,
    payload JSONB NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);
