ALTER TABLE aql DROP COLUMN description;
ALTER TABLE aql ADD COLUMN use varchar(250);
ALTER TABLE aql ADD COLUMN purpose varchar(250);