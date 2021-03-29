-- Copyright 2021 Vitagroup AG
--
-- Licensed under the Apache License, Version 2.0 (the "License");
-- you may not use this file except in compliance with the License.
-- You may obtain a copy of the License at
--
--      http://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing, software
-- distributed under the License is distributed on an "AS IS" BASIS,
-- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
-- See the License for the specific language governing permissions and
-- limitations under the License.

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