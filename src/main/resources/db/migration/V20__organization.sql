DROP TABLE IF EXISTS organization;
DROP TABLE IF EXISTS maildomain;

CREATE TABLE organization
(
    id              serial PRIMARY KEY,
    name            varchar(50) UNIQUE NOT NULL,
    description     varchar(250)
);

CREATE TABLE maildomain
(
    id              serial PRIMARY KEY,
    name            varchar(50) UNIQUE NOT NULL,
    organization_id int references organization (id) ON DELETE NO ACTION ON UPDATE NO ACTION NOT NULL
);