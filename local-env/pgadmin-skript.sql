-- Entfernt Duplikate aus dem ehr.status table bei denen party_ref_value + party_ref_scheme nicht uniquie sind

begin;
CREATE TEMP TABLE duplicates AS (
 select ehr_id, pi.party_ref_value,party_ref_scheme, Row_number() OVER (PARTITION BY pi.party_ref_value, party_ref_scheme) from ehr.status join ehr.party_identified pi on pi.id = status.party and pi.sys_tenant = status.sys_tenant
);
DELETE FROM ehr.status WHERE ehr_id IN (
    SELECT ehr_id FROM duplicates WHERE row_number> 1);

DELETE FROM ehr.ehr WHERE id IN (
    SELECT ehr_id FROM duplicates WHERE row_number> 1);
commit;

-- Erstelle eine temporäre Tabelle
CREATE TEMP TABLE temp_codex_codes (
    codex_code TEXT
);

-- Füge alle Test codex_PSN die noch nicht genutzt werden als CSV in die erstellte temporäre Tabelle
COPY temp_codex_codes FROM '/path/to/data.csv' DELIMITER ',' CSV HEADER;

-- Füge alle Test codex_PSN die noch nicht genutzt werden in die erstellre temporäre Tabelle
INSERT INTO temp_codex_codes (codex_code) VALUES
    ('codex_AMP13W'),
    ('codex_57MFPX'),
    ('codex_2GXDKR'),
    ('codex_K9Q4TQ'),
    ('codex_HA8LTY'),
    ('codex_Z6F3M2'),
    ('codex_Z55RAL'),
    ('codex_L6KUTL'),
    ('codex_FWU58K'),
    ('codex_03JTZ9'),
    ('codex_394LW3'),
    ('codex_PRF4MQ'),
    ('codex_XAQ6ZX'),
    ('codex_QMW1QD'),
    ('codex_RF98PY'),
    ('codex_QMAH7Y'),
    ('codex_FAGALE'),
    ('codex_FDUXX7'),
    ('codex_GHY6J8'),
    ('codex_LQU8HY'),
    ('codex_UWFWUY'),
    ('codex_MU214J'),
    ('codex_LDTZFN'),
    ('codex_LHRZ9C'),
    ('codex_GN14AU'),
    ('codex_FT265N'),
    ('codex_JWGXLE'),
    ('codex_D7G9MM'),
    ('codex_6ADW20'),
    ('codex_HUA22Z'),
    ('codex_8Z1RRK'),
    ('codex_9L0GCY'),
    ('codex_06P70D'),
    ('codex_N1TLCP'),
    ('codex_RX2TNZ'),
    ('codex_KP08FK'),
    ('codex_P42JC2'),
    ('codex_XP8AWK'),
    ('codex_5RL7QY'),
    ('codex_JHMGEK'),
    ('codex_2683UU');

-- Hier wird jede Zeile in party_identified überprüft, ob der party_ref_value einen codex_PSN enthält , wenn nicht
-- wird dieser durch einen Wert der temp_codex_codes table ersetzt
-- Dies geschieht solange bis alle codex_PSN aus dem temp table benutzt wurden.
WITH updated_rows AS (
    SELECT id, codex_code
    FROM ehr.party_identified
    CROSS JOIN temp_codex_codes
    WHERE party_ref_value IS null
       OR party_ref_value NOT LIKE '%codex_%'
    LIMIT (SELECT COUNT(*) FROM temp_codex_codes)
)
UPDATE ehr.party_identified
SET party_ref_value = (
    SELECT codex_code
    FROM temp_codex_codes
    WHERE temp_codex_codes.codex_code = updated_rows.codex_code
)
FROM updated_rows
WHERE ehr.party_identified.id = updated_rows.id;

--new

WITH temp_codex_codes_with_rownum AS (
    SELECT codex_code, ROW_NUMBER() OVER () AS rownum
    FROM temp_codex_codes
),
     party_identified_with_rownum AS (
         SELECT id, ROW_NUMBER() OVER () AS rownum
         FROM ehr.party_identified
         WHERE party_ref_value NOT LIKE '%codex_%'
    LIMIT (SELECT COUNT(*) FROM temp_codex_codes)
),
updated_rows AS (
           SELECT p.id, t.codex_code
           FROM party_identified_with_rownum p
               JOIN temp_codex_codes_with_rownum t
           ON p.rownum = t.rownum
               )

UPDATE ehr.party_identified
SET party_ref_value = updated_rows.codex_code
    FROM updated_rows
WHERE ehr.party_identified.id = updated_rows.id;
