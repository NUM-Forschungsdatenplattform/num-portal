DROP TABLE IF EXISTS aql;

CREATE TABLE aql
(
    id              serial PRIMARY KEY,
    name            varchar(50),
    description     varchar(250),
    query           varchar     NOT NULL,
    public_aql      boolean,
    owner_id        varchar(50) references user_details(user_id) ON DELETE NO ACTION ON UPDATE NO ACTION NOT NULL,
    organization_id varchar(50),
    create_date     timestamp,
    modified_date   timestamp
);