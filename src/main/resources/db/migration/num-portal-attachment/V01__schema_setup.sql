DROP TABLE IF EXISTS attachment;

CREATE TABLE attachment(
        id BIGSERIAL PRIMARY KEY,
        name VARCHAR(255) NOT NULL,
        description VARCHAR (512),
        upload_date timestamp NOT NULL,
        type varchar(125) NOT NULL ,
        content bytea,
        author_id varchar(250)
);