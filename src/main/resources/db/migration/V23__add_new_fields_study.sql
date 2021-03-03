ALTER TABLE study
  ADD COLUMN goal TEXT,
  ADD COLUMN financed BOOLEAN,
  ADD COLUMN start_date DATE,
  ADD COLUMN end_date DATE;

DROP TABLE IF EXISTS study_categories;

CREATE TABLE study_categories (
  study_id int REFERENCES study(id) ON DELETE NO ACTION ON UPDATE NO ACTION NOT NULL,
	category varchar(100) NOT NULL,
	CONSTRAINT study_categories_pkey PRIMARY KEY (study_id, category)
);

DROP TABLE IF EXISTS study_keywords;

CREATE TABLE study_keywords (
  study_id integer REFERENCES study(id) ON DELETE NO ACTION ON UPDATE NO ACTION NOT NULL,
	keyword varchar(100) NOT NULL,
	CONSTRAINT study_keywords_pkey PRIMARY KEY (study_id, keyword)
);

UPDATE study SET goal='Default' where goal IS NULL;
UPDATE study SET financed=false where financed IS NULL;
UPDATE study SET start_date='2001-10-05' where start_date IS NULL;
UPDATE study SET end_date='2001-10-05' where end_date IS NULL;

ALTER TABLE study ALTER COLUMN goal SET NOT NULL;
ALTER TABLE study ALTER COLUMN financed SET NOT NULL;
ALTER TABLE study ALTER COLUMN start_date SET NOT NULL;
ALTER TABLE study ALTER COLUMN end_date SET NOT NULL;
