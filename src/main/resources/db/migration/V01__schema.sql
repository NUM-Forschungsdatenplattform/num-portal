--
-- Organization
--
CREATE TABLE organization
(
    id          serial PRIMARY KEY,
    name        varchar(50) UNIQUE NOT NULL,
    description varchar(250)
);

--
-- Mail domain
--
CREATE TABLE maildomain
(
    id              serial PRIMARY KEY,
    name            varchar(50) UNIQUE                                                       NOT NULL,
    organization_id int references organization (id) ON DELETE NO ACTION ON UPDATE NO ACTION NOT NULL
);

--
-- User details
--
CREATE TABLE user_details
(
    user_id         varchar(250) PRIMARY KEY,
    approved        boolean NOT NULL,
    organization_id integer references organization (id) ON DELETE NO ACTION ON UPDATE NO ACTION

);

--
-- Content
--
CREATE TABLE content
(
    id      serial PRIMARY KEY,
    type    varchar(20) NOT NULL,
    content text
);

--
-- Cohort group
--
CREATE TABLE cohort_group
(
    id              serial PRIMARY KEY,
    type            varchar(250) NOT NULL,
    description     text,
    operator        varchar(100),
    parameters      json,
    parent_group_id integer references cohort_group (id) ON DELETE NO ACTION ON UPDATE NO ACTION,
    query           json         NOT NULL
);

--
-- Cohort
--
CREATE TABLE cohort
(
    id              serial PRIMARY KEY,
    name            varchar(250) NOT NULL,
    description     text,
    cohort_group_id integer references cohort_group (id) ON DELETE NO ACTION ON UPDATE NO ACTION
);

--
-- Project
--
CREATE TABLE project
(
    id                 serial PRIMARY KEY,
    name               varchar(250) NOT NULL,
    description        text,
    simple_description text,
    first_hypotheses   text,
    second_hypotheses  text,
    goal               text         NOT NULL,
    categories         text,
    keywords           text,
    status             varchar(25),
    create_date        timestamp,
    modified_date      timestamp,
    financed           boolean      NOT NULL,
    used_outside_eu    boolean,
    start_date         date         NOT NULL,
    end_date           date         NOT NULL,
    templates          json,
    coordinator_id     varchar(250),
    cohort_id          integer references cohort (id) ON DELETE NO ACTION ON UPDATE NO ACTION
);

--
-- Project transitions
--
CREATE TABLE project_transition
(
    id              serial PRIMARY KEY,
    from_status     varchar(25),
    to_status       varchar(25)                                                             NOT NULL,
    create_date     timestamp,
    project_id      integer references project (id) ON DELETE NO ACTION ON UPDATE NO ACTION NOT NULL,
    user_details_id varchar(250) REFERENCES user_details (user_id) ON UPDATE CASCADE
);

--
-- Project users
--
CREATE TABLE project_users
(
    project_id      int REFERENCES project (id) ON UPDATE CASCADE,
    user_details_id varchar(250) REFERENCES user_details (user_id) ON UPDATE CASCADE,
    CONSTRAINT project_template_pkey PRIMARY KEY (project_id, user_details_id)
);

--
-- Comment
--
CREATE TABLE comment
(
    id          serial PRIMARY KEY,
    text        text,
    project_id  integer references project (id) ON DELETE NO ACTION ON UPDATE NO ACTION               NOT NULL,
    author_id   varchar(50) references user_details (user_id) ON DELETE NO ACTION ON UPDATE NO ACTION NOT NULL,
    create_date timestamp
);

--
-- Category
--
CREATE TABLE aql_category
(
    id   serial PRIMARY KEY,
    name json NOT NULL
);

--
-- Aql
--
CREATE TABLE aql
(
    id            serial PRIMARY KEY,
    name          varchar(250),
    query         text                                                                                  NOT NULL,
    public_aql    boolean,
    create_date   timestamp,
    modified_date timestamp,
    use           text,
    purpose       text,
    owner_id      varchar(50) references user_details (user_id) ON DELETE NO ACTION ON UPDATE NO ACTION NOT NULL,
    category_id   integer references aql_category (id) ON DELETE NO ACTION ON UPDATE NO ACTION
);




