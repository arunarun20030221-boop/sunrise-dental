-- =====================================================================================
-- Reference data. Idempotent: safe to run on every start.
--
-- The two password hashes below are real BCrypt hashes, generated with the same
-- BCryptPasswordEncoder the application authenticates with and verified to match:
--     admin      -> admin123
--     reception1 -> reception123
--
-- These are development seed accounts. Change both passwords before any real deployment;
-- the README documents how. The plaintext appears nowhere in the database or the code -
-- only these one-way hashes, each with its own salt.
-- =====================================================================================

INSERT INTO staff (username, password_hash, full_name, role)
VALUES ('admin',
        '$2a$10$DXCXTbxrvMqNitrpbUJB6eMgkgMpERo5n4J5XQ/7YYl5M88W9qiY.',
        'Clinic Administrator',
        'ADMIN')
ON CONFLICT (username) DO NOTHING;

INSERT INTO staff (username, password_hash, full_name, role)
VALUES ('reception1',
        '$2a$10$.6JwmVhDmoJ9DI9ch7XHDu0jbcW8rSJXBEPqEvegsGJF2IpDiwaze',
        'Front Desk - Anusha',
        'RECEPTIONIST')
ON CONFLICT (username) DO NOTHING;

-- Dentists. No unique constraint on name, so guard with NOT EXISTS rather than ON CONFLICT.
INSERT INTO dentist (name, speciality)
SELECT 'Dr. Silva', 'General Dentistry'
WHERE NOT EXISTS (SELECT 1 FROM dentist WHERE name = 'Dr. Silva');

INSERT INTO dentist (name, speciality)
SELECT 'Dr. Fernando', 'Orthodontics'
WHERE NOT EXISTS (SELECT 1 FROM dentist WHERE name = 'Dr. Fernando');

INSERT INTO dentist (name, speciality)
SELECT 'Dr. Perera', 'Endodontics'
WHERE NOT EXISTS (SELECT 1 FROM dentist WHERE name = 'Dr. Perera');

-- Treatment price list. The 'code' selects the pricing strategy at billing time, so these
-- codes must match the constants in com.sunrise.dental.service.pricing.
INSERT INTO treatment_type (code, name, base_cost, duration_minutes)
VALUES ('STANDARD',   'Check-up and Scaling', 3500.00,  30),
       ('EXTRACTION', 'Tooth Extraction',     6000.00,  45),
       ('ROOT_CANAL', 'Root Canal Treatment', 15000.00, 60),
       ('COSMETIC',   'Teeth Whitening',      12000.00, 90),
       ('FILLING',    'Composite Filling',    4500.00,  40)
ON CONFLICT (code) DO NOTHING;
