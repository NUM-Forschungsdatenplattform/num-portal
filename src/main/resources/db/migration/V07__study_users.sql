-- Copyright 2021 Vitagroup AG
--
-- Licensed under the Apache License, Version 2.0 (the "License");
-- you may not use this file except in compliance with the License.
-- You may obtain a copy of the License at
--
--      http://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing, software
-- distributed under the License is distributed on an "AS IS" BASIS,
-- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
-- See the License for the specific language governing permissions and
-- limitations under the License.

ALTER TABLE study
    ADD COLUMN coordinator_id varchar(250);

CREATE TABLE study_users
(
    study_id int REFERENCES study (id) ON UPDATE CASCADE,
    user_details_id varchar(250) REFERENCES user_details (user_id) ON UPDATE CASCADE,
    CONSTRAINT study_template_pkey PRIMARY KEY (study_id, user_details_id)
);