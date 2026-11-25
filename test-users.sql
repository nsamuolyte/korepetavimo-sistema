CREATE EXTENSION IF NOT EXISTS pgcrypto;

WITH test_users AS (
    SELECT
        n,
        CASE
            WHEN n <= 7 THEN 'STUDENT'
            WHEN n <= 14 THEN 'GUARDIAN'
            ELSE 'TUTOR'
        END AS role,
        CASE
            WHEN n <= 7 THEN 'mokinys' || n
            WHEN n <= 14 THEN 'tevas' || (n - 7)
            ELSE 'tutor' || (n - 14)
        END AS login,
        CASE
            WHEN n <= 7 THEN 'Mokinys2026!'
            WHEN n <= 14 THEN 'Tevas2026!'
            ELSE 'Tutor2026!'
        END AS password
    FROM generate_series(1, 21) AS n
)
INSERT INTO users (
    first_name, last_name, email,
    password_hash, role, active
)
SELECT
    CASE role
        WHEN 'STUDENT' THEN 'Mokinys ' || n
        WHEN 'GUARDIAN' THEN 'Globėjas ' || (n - 7)
        ELSE 'Korepetitorius ' || (n - 14)
    END,
    'Test',
    login || '@test.local',
    crypt(password, gen_salt('bf', 12)),
    role,
    true
FROM test_users
ON CONFLICT (email) DO NOTHING;
