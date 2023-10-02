INSERT INTO organization (name) VALUES ('Organization A');
INSERT INTO organization (name) VALUES ('Organization B');

INSERT INTO user_details(user_id, organization_id, approved)
VALUES ('b59e5edb-3121-4e0a-8ccb-af6798207a72', (select id from organization where name = 'Organization A'), true);

INSERT INTO user_details(user_id, organization_id, approved)
VALUES ('b59e5edb-3121-4e0a-8ccb-af6798207a73', (select id from organization where name = 'Organization B'), false);
