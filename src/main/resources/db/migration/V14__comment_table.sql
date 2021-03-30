DROP TABLE IF EXISTS comment;

CREATE TABLE comment
(
    id          serial PRIMARY KEY,
    text        varchar(250),
    study_id    integer references study (id) ON DELETE NO ACTION ON UPDATE NO ACTION                 NOT NULL,
    author_id   varchar(50) references user_details (user_id) ON DELETE NO ACTION ON UPDATE NO ACTION NOT NULL,
    create_date timestamp
);