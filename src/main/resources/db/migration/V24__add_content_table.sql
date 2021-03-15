DROP TABLE IF EXISTS content;

CREATE TABLE content
(
    id              serial PRIMARY KEY,
    type            varchar(20) NOT NULL,
    content         TEXT
);