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
  ADD COLUMN goal TEXT,
  ADD COLUMN categories TEXT,
  ADD COLUMN keywords TEXT,
  ADD COLUMN financed BOOLEAN,
  ADD COLUMN start_date DATE,
  ADD COLUMN end_date DATE;

UPDATE study SET goal='Default' where goal IS NULL;
UPDATE study SET financed=false where financed IS NULL;
UPDATE study SET start_date='2001-10-05' where start_date IS NULL;
UPDATE study SET end_date='2001-10-05' where end_date IS NULL;

ALTER TABLE study ALTER COLUMN goal SET NOT NULL;
ALTER TABLE study ALTER COLUMN financed SET NOT NULL;
ALTER TABLE study ALTER COLUMN start_date SET NOT NULL;
ALTER TABLE study ALTER COLUMN end_date SET NOT NULL;
