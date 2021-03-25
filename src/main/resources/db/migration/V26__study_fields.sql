ALTER TABLE study ADD COLUMN simple_description text;
ALTER TABLE study ADD COLUMN used_outside_eu boolean;

UPDATE study SET used_outside_eu = false where used_outside_eu IS NULL;