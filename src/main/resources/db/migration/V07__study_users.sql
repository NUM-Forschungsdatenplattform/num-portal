ALTER TABLE study
    ADD COLUMN coordinator_id varchar(250);

CREATE TABLE study_users
(
    study_id int REFERENCES study (id) ON UPDATE CASCADE,
    user_details_id varchar(250) REFERENCES user_details (user_id) ON UPDATE CASCADE,
    CONSTRAINT study_template_pkey PRIMARY KEY (study_id, user_details_id)
);