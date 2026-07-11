-- Delete existing data first to prevent NOT NULL constraint errors when adding UUID columns
DELETE FROM bill_cycle;
ALTER TABLE bill_cycle DROP COLUMN subscription_id;
ALTER TABLE bill_cycle ADD COLUMN subscription_id UUID NOT NULL;

DELETE FROM invoice;
ALTER TABLE invoice DROP COLUMN subscription_id;
ALTER TABLE invoice ADD COLUMN subscription_id UUID NOT NULL;
