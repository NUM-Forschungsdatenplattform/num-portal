CREATE TABLE translation (
        id BIGSERIAL PRIMARY KEY,
        entity_group VARCHAR(255) NOT NULL,
        entity_id BIGINT null,
        property VARCHAR (124) NOT NULL,
        language_code VARCHAR (3) NOT NULL,
        value VARCHAR (2048) NOT NULL
);
INSERT INTO translation(entity_group, property, language_code, value) VALUES ('PROJECT_STATUS', 'DRAFT', 'en', 'Draft');
INSERT INTO translation(entity_group, property, language_code, value) VALUES ('PROJECT_STATUS', 'DRAFT', 'de', 'Entwurf');

INSERT INTO translation(entity_group, property, language_code, value) VALUES ('PROJECT_STATUS', 'PENDING', 'en', 'Pending approval');
INSERT INTO translation(entity_group, property, language_code, value) VALUES ('PROJECT_STATUS', 'PENDING', 'de', 'Genehmigung ausstehend');

INSERT INTO translation(entity_group, property, language_code, value) VALUES ('PROJECT_STATUS', 'REVIEWING', 'en', 'In review');
INSERT INTO translation(entity_group, property, language_code, value) VALUES ('PROJECT_STATUS', 'REVIEWING', 'de', 'Überprüfung');

INSERT INTO translation(entity_group, property, language_code, value) VALUES ('PROJECT_STATUS', 'CHANGE_REQUEST', 'en', 'Change request');
INSERT INTO translation(entity_group, property, language_code, value) VALUES ('PROJECT_STATUS', 'CHANGE_REQUEST', 'de', 'Änderung notwendig');

INSERT INTO translation(entity_group, property, language_code, value) VALUES ('PROJECT_STATUS', 'DENIED', 'en', 'Denied');
INSERT INTO translation(entity_group, property, language_code, value) VALUES ('PROJECT_STATUS', 'DENIED', 'de', 'Abgelehnt');

INSERT INTO translation(entity_group, property, language_code, value) VALUES ('PROJECT_STATUS', 'APPROVED', 'en', 'Approved');
INSERT INTO translation(entity_group, property, language_code, value) VALUES ('PROJECT_STATUS', 'APPROVED', 'de', 'Genehmigt');

INSERT INTO translation(entity_group, property, language_code, value) VALUES ('PROJECT_STATUS', 'PUBLISHED', 'en', 'Started');
INSERT INTO translation(entity_group, property, language_code, value) VALUES ('PROJECT_STATUS', 'PUBLISHED', 'de', 'Gestartet');

INSERT INTO translation(entity_group, property, language_code, value) VALUES ('PROJECT_STATUS', 'CLOSED', 'en', 'Finished');
INSERT INTO translation(entity_group, property, language_code, value) VALUES ('PROJECT_STATUS', 'CLOSED', 'de', 'Beendet');

INSERT INTO translation(entity_group, property, language_code, value) VALUES ('PROJECT_STATUS', 'ARCHIVED', 'en', 'Archived');
INSERT INTO translation(entity_group, property, language_code, value) VALUES ('PROJECT_STATUS', 'ARCHIVED', 'de', 'Archiviert');