ALTER TABLE user_details
    ALTER COLUMN organization_id TYPE INTEGER
        USING (CASE
                   WHEN organization_id = '12345a' THEN 1
                   WHEN organization_id = '12345b' THEN 2
                   WHEN organization_id = '12345c' THEN 3
                   WHEN organization_id = '12345d' THEN 4
                   WHEN organization_id = '12345e' THEN 5
        END);

INSERT INTO organization (id, name)
SELECT us.organization_id, 'Organization A'
FROM user_details us
where us.organization_id = 1
LIMIT 1;

INSERT INTO organization (id, name)
SELECT us.organization_id, 'Organization B'
FROM user_details us
where us.organization_id = 2
LIMIT 1;

INSERT INTO organization (id, name)
SELECT us.organization_id, 'Organization C'
FROM user_details us
where us.organization_id = 3
LIMIT 1;

INSERT INTO organization (id, name)
SELECT us.organization_id, 'Organization D'
FROM user_details us
where us.organization_id = 4
LIMIT 1;

INSERT INTO organization (id, name)
SELECT us.organization_id, 'Organization E'
FROM user_details us
where us.organization_id = 5
LIMIT 1;

ALTER TABLE user_details
    ADD FOREIGN KEY (organization_id) REFERENCES organization (id) ON DELETE NO ACTION ON UPDATE NO ACTION;
