ALTER TABLE study
  ADD COLUMN goal TEXT,
  ADD COLUMN categories TEXT,
  ADD COLUMN keywords TEXT,
  ADD COLUMN financed BOOLEAN,
  ADD COLUMN start_date DATE,
  ADD COLUMN end_date DATE;

UPDATE study SET goal='Default' where goal IS NULL;
UPDATE study SET financed=false where financed IS NULL;
UPDATE study SET start_date='2001-10-05' where start_date IS NULL;
UPDATE study SET end_date='2001-10-05' where end_date IS NULL;

ALTER TABLE study ALTER COLUMN goal SET NOT NULL;
ALTER TABLE study ALTER COLUMN financed SET NOT NULL;
ALTER TABLE study ALTER COLUMN start_date SET NOT NULL;
ALTER TABLE study ALTER COLUMN end_date SET NOT NULL;
