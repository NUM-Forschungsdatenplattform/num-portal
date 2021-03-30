ALTER TABLE phenotype
    ADD COLUMN owner_id varchar(50) references user_details(user_id) ON DELETE NO ACTION ON UPDATE NO ACTION;