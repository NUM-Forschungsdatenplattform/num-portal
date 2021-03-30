DROP TABLE IF EXISTS user_details;

CREATE TABLE user_details (
  user_id varchar(250) PRIMARY KEY,
  organization_id varchar(250),
  approved boolean
);
