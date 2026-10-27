ALTER TABLE parking_sessions ADD COLUMN license_plate_key VARCHAR(20);

-- Match Java's [\s\p{Z}-]: ASCII whitespace, Unicode separators and hyphens.
-- Keep every historical session; conflicting ACTIVE keys make this migration
-- fail atomically so an operator can reconcile the records explicitly.
UPDATE parking_sessions
SET license_plate_key = UPPER(REGEXP_REPLACE(
    license_plate,
    U&'[\0009-\000D\0020\00A0\1680\2000-\200A\2028\2029\202F\205F\3000-]+',
    '', 'g'
));

ALTER TABLE parking_sessions
    ALTER COLUMN license_plate_key SET NOT NULL,
    ADD CONSTRAINT ck_parking_sessions_license_plate_key
        CHECK (license_plate_key ~ '^[A-Z0-9]{4,20}$'),
    ADD CONSTRAINT ck_parking_sessions_license_plate_key_matches
        CHECK (license_plate_key = UPPER(REGEXP_REPLACE(
            license_plate,
            U&'[\0009-\000D\0020\00A0\1680\2000-\200A\2028\2029\202F\205F\3000-]+',
            '', 'g'
        )));

CREATE UNIQUE INDEX uq_parking_sessions_active_license_plate_key
    ON parking_sessions (license_plate_key)
    WHERE status = 'ACTIVE';

DROP INDEX uq_parking_sessions_active_license_plate;

CREATE INDEX idx_parking_sessions_license_plate_key_check_in
    ON parking_sessions (license_plate_key, check_in_time DESC, id DESC);
