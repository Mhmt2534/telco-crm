INSERT INTO tariff (code, version, name, type, monthly_fee, minutes_included, sms_included, data_mb_included, status, effective_from) 
VALUES 
('TARIFF_PREMIUM_20GB', 1, 'Premium 20GB', 'POSTPAID', 150.00, 1000, 1000, 20480, 'ACTIVE', NOW()),
('100GB_FIBER', 1, '100GB Fiber Home', 'POSTPAID', 250.00, 0, 0, 102400, 'ACTIVE', NOW()),
('MINI_1GB', 1, 'Mini 1GB', 'PREPAID', 50.00, 500, 500, 1024, 'ACTIVE', NOW());

INSERT INTO addon (code, version, name, type, validity_days, price, data_mb, minutes, sms_count, status, effective_from) 
VALUES
('ROAMING_PACK', 1, 'Europe Roaming Pack', 'VAS', 30, 200.00, 5120, 100, 100, 'ACTIVE', NOW()),
('GAMER_UNLIMITED', 1, 'Gamer Unlimited Data', 'DATA', 30, 75.00, 102400, 0, 0, 'ACTIVE', NOW());
