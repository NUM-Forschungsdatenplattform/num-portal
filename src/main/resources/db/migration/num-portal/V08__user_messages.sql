DROP TABLE IF EXISTS message;

CREATE TABLE message(
                           id BIGSERIAL PRIMARY KEY,
                           title VARCHAR(255) NOT NULL,
                           text text,
                           start_date timestamp NOT NULL,
                           end_date timestamp NOT NULL,
                           type varchar(125) NOT NULL ,
                           mark_as_deleted boolean
);