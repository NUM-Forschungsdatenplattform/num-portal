
-- create user --
CREATE ROLE "num-attachment" WITH LOGIN PASSWORD 'num-attachment';

-- create database --
CREATE DATABASE "num-attachment" WITH OWNER = 'num-attachment' ENCODING = 'UTF8' CONNECTION LIMIT = -1;
GRANT ALL PRIVILEGES ON DATABASE "num-attachment" TO "num-attachment";

\c num-attachment
CREATE SCHEMA IF NOT EXISTS num AUTHORIZATION "num-attachment";
GRANT  ALL PRIVILEGES  ON SCHEMA num  TO "num-attachment";
GRANT INSERT, SELECT, UPDATE, DELETE, TRUNCATE, REFERENCES ON ALL TABLES IN SCHEMA num TO "num-attachment";
GRANT EXECUTE ON ALL FUNCTIONS IN SCHEMA num TO "num-attachment";
GRANT  ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA  num  TO "num-attachment";