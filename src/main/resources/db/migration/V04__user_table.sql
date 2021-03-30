DROP TABLE IF EXISTS user_details;

CREATE TABLE user_details (
  id serial PRIMARY KEY,
  user_id varchar(250) NOT NULL,
  organization_id varchar(250),
  approved boolean
);
