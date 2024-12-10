DROP TABLE IF EXISTS message;

CREATE TABLE message
(
    id              serial PRIMARY KEY,
    title           VARCHAR(255) NOT NULL,
    text            text,
    start_date      timestamp    NOT NULL,
    end_date        timestamp    NOT NULL,
    type            varchar(125) NOT NULL,
    mark_as_deleted boolean,
    sessionBased    boolean
);

CREATE TABLE read_message_by_users
(
    message_id      int REFERENCES message (id) ON UPDATE CASCADE,
    user_details_id varchar(250) REFERENCES user_details (user_id) ON UPDATE CASCADE,
    CONSTRAINT message_template_pkey PRIMARY KEY (message_id, user_details_id)
);