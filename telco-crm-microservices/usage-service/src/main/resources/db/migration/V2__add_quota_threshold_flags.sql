-- ALTER quotas table to add stateful flags for threshold warnings and exceeding status
ALTER TABLE quotas
ADD COLUMN voice_threshold_reached BOOLEAN NOT NULL DEFAULT false,
ADD COLUMN sms_threshold_reached BOOLEAN NOT NULL DEFAULT false,
ADD COLUMN data_threshold_reached BOOLEAN NOT NULL DEFAULT false,
ADD COLUMN voice_exceeded BOOLEAN NOT NULL DEFAULT false,
ADD COLUMN sms_exceeded BOOLEAN NOT NULL DEFAULT false,
ADD COLUMN data_exceeded BOOLEAN NOT NULL DEFAULT false;
