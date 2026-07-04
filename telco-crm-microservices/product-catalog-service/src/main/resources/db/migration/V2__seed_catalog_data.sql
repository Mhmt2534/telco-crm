-- Seed Data for Tariffs
INSERT INTO tariff (code, version, name, type, monthly_fee, data_mb_included, minutes_included, sms_included, status, effective_from)
VALUES ('POSTPAID_50', 1, 'Postpaid 50GB', 'POSTPAID', 299.90, 51200, 1000, 100, 'ACTIVE', NOW());

INSERT INTO tariff (code, version, name, type, monthly_fee, data_mb_included, minutes_included, sms_included, status, effective_from)
VALUES ('PREPAID_10', 1, 'Prepaid 10GB', 'PREPAID', 99.90, 10240, 500, 50, 'ACTIVE', NOW());

-- Seed Data for Addons
INSERT INTO addon (code, version, name, type, price, data_mb, validity_days, status, effective_from)
VALUES ('DATA_5GB', 1, '5GB Ek Paket', 'DATA', 49.90, 5120, 30, 'ACTIVE', NOW());

INSERT INTO addon (code, version, name, type, price, validity_days, status, effective_from)
VALUES ('VAS_MUZIK', 1, 'Müzik Platformu', 'VAS', 29.90, 30, 'ACTIVE', NOW());

-- Seed Tariff-Addon Relationships
INSERT INTO tariff_addon (tariff_id, addon_id)
SELECT t.id, a.id
FROM tariff t, addon a
WHERE t.code = 'POSTPAID_50' AND a.code IN ('DATA_5GB', 'VAS_MUZIK');
