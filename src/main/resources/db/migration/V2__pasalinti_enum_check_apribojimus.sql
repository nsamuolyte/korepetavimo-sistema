-- V2: pašalina CHECK apribojimus, kuriuos Hibernate sukūrė enum stulpeliams
-- (dėl jų nauja enum reikšmė, pvz. GUARDIAN_LINK, sukeldavo klaidą).

ALTER TABLE users             DROP CONSTRAINT IF EXISTS users_role_check;
ALTER TABLE guardian_students DROP CONSTRAINT IF EXISTS guardian_students_status_check;
ALTER TABLE availabilities    DROP CONSTRAINT IF EXISTS availabilities_day_of_week_check;
ALTER TABLE lessons           DROP CONSTRAINT IF EXISTS lessons_status_check;
ALTER TABLE payments          DROP CONSTRAINT IF EXISTS payments_status_check;
ALTER TABLE payouts           DROP CONSTRAINT IF EXISTS payouts_status_check;
ALTER TABLE notifications     DROP CONSTRAINT IF EXISTS notifications_type_check;