ALTER TABLE study
    ADD COLUMN first_hypotheses  varchar(250),
    ADD COLUMN second_hypotheses varchar(250),
    ADD COLUMN status varchar(25),
    ADD COLUMN create_date timestamp,
    ADD COLUMN modified_date timestamp;