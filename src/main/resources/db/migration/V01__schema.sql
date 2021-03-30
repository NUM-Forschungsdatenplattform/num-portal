
DROP TABLE IF EXISTS phenotype;

CREATE TABLE phenotype (
  id serial PRIMARY KEY,
  name varchar(250) NOT NULL,
  description varchar(250) NOT NULL,
  query json NOT NULL
);
