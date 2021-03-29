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

DROP TABLE IF EXISTS aql;

CREATE TABLE aql
(
    id              serial PRIMARY KEY,
    name            varchar(50),
    description     varchar(250),
    query           varchar     NOT NULL,
    public_aql      boolean,
    owner_id        varchar(50) references user_details(user_id) ON DELETE NO ACTION ON UPDATE NO ACTION NOT NULL,
    organization_id varchar(50),
    create_date     timestamp,
    modified_date   timestamp
);