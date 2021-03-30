INSERT INTO organization (id, name) VALUES (1, 'Organization A');
INSERT INTO organization (id, name) VALUES (2, 'Organization B');

INSERT INTO user_details(user_id, organization_id, approved)
VALUES ('b59e5edb-3121-4e0a-8ccb-af6798207a72', 1, true);

INSERT INTO user_details(user_id, organization_id, approved)
VALUES ('b59e5edb-3121-4e0a-8ccb-af6798207a73', 2, false);
