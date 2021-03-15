CREATE TABLE study_transition
(
    id              serial PRIMARY KEY,
    from_status     varchar(25),
    to_status       varchar(25) NOT NULL,
    create_date     timestamp,
    study_id        integer references study (id) ON DELETE NO ACTION ON UPDATE NO ACTION NOT NULL,
    user_details_id varchar(250) REFERENCES user_details (user_id) ON UPDATE CASCADE
);
