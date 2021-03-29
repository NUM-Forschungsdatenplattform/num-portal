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

DROP TABLE IF EXISTS organization;
DROP TABLE IF EXISTS maildomain;

CREATE TABLE organization
(
    id              serial PRIMARY KEY,
    name            varchar(50) UNIQUE NOT NULL,
    description     varchar(250)
);

CREATE TABLE maildomain
(
    id              serial PRIMARY KEY,
    name            varchar(50) UNIQUE NOT NULL,
    organization_id int references organization (id) ON DELETE NO ACTION ON UPDATE NO ACTION NOT NULL
);