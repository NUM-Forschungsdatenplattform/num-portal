DROP TABLE IF EXISTS aql_category;

CREATE TABLE aql_category (
  id serial PRIMARY KEY,
  name json NOT NULL
);

ALTER TABLE aql ADD COLUMN category_id integer references aql_category (id) ON DELETE NO ACTION ON UPDATE NO ACTION
