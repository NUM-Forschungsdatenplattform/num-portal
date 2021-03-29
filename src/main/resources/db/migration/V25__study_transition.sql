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

CREATE TABLE study_transition
(
    id              serial PRIMARY KEY,
    from_status     varchar(25),
    to_status       varchar(25) NOT NULL,
    create_date     timestamp,
    study_id        integer references study (id) ON DELETE NO ACTION ON UPDATE NO ACTION NOT NULL,
    user_details_id varchar(250) REFERENCES user_details (user_id) ON UPDATE CASCADE
);
