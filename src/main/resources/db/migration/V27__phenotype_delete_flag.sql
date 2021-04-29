ALTER TABLE phenotype ADD COLUMN deleted boolean;
UPDATE phenotype SET deleted = false where deleted IS NULL;