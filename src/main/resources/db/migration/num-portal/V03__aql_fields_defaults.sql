UPDATE aql
SET use_translated = use
WHERE use_translated IS NULL
  AND use IS NOT NULL
  AND use IS DISTINCT FROM use_translated;

UPDATE aql
SET name_translated = name
WHERE name_translated IS NULL
  AND name IS NOT NULL
  AND name IS DISTINCT FROM name_translated;

UPDATE aql
SET purpose_translated = purpose
WHERE purpose_translated IS NULL
  AND purpose IS NOT NULL
  AND purpose IS DISTINCT FROM purpose_translated;
