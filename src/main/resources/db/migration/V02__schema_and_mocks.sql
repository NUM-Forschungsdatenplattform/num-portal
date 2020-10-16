DROP TABLE IF EXISTS cohort;
DROP TABLE IF EXISTS study;
DROP TABLE IF EXISTS cohort_group;

CREATE TABLE cohort_group (
  id serial PRIMARY KEY,
  type varchar(250) NOT NULL,
  description varchar(250),
  operator varchar(100),
  parent_group_id integer references cohort_group(id) ON DELETE NO ACTION ON UPDATE NO ACTION,
  phenotype_id integer references phenotype(id) ON DELETE NO ACTION ON UPDATE NO ACTION
);

CREATE TABLE cohort (
  id serial PRIMARY KEY,
  name varchar(250) NOT NULL,
  description varchar(250),
  cohort_group_id integer references cohort_group(id) ON DELETE NO ACTION ON UPDATE NO ACTION
);

CREATE TABLE study (
  id serial PRIMARY KEY,
  name varchar(250) NOT NULL,
  description varchar(250),
  cohort_id integer references cohort(id) ON DELETE NO ACTION ON UPDATE NO ACTION
);

INSERT INTO study (name, description) VALUES ('1. Study name', '1. Study description');
INSERT INTO study (name, description) VALUES ('2. Study name', '2. Study description');