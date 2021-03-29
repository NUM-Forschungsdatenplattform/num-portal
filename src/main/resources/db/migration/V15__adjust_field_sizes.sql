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

ALTER TABLE aql ALTER COLUMN query TYPE text;
ALTER TABLE aql ALTER COLUMN name TYPE varchar(250);
ALTER TABLE cohort ALTER COLUMN description TYPE text;
ALTER TABLE cohort_group ALTER COLUMN description TYPE text;
ALTER TABLE comment ALTER COLUMN text TYPE text;
ALTER TABLE phenotype ALTER COLUMN description TYPE text;
ALTER TABLE study ALTER COLUMN description TYPE text;
ALTER TABLE study ALTER COLUMN first_hypotheses TYPE text;
ALTER TABLE study ALTER COLUMN second_hypotheses TYPE text;
