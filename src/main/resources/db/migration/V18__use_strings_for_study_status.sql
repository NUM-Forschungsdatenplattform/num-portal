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

UPDATE study SET status='DRAFT' where status='0';
UPDATE study SET status='PENDING' where status='1';
UPDATE study SET status='REVIEWING' where status='2';
UPDATE study SET status='CHANGE_REQUEST' where status='3';
UPDATE study SET status='DENIED' where status='4';
UPDATE study SET status='APPROVED' where status='5';
UPDATE study SET status='PUBLISHED' where status='6';
UPDATE study SET status='CLOSED' where status='7';
