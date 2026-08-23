-- Ready-to-use accounts for testing each role. All share the password: Password123
-- (BCrypt, strength 10). Change or remove before any real deployment.

INSERT INTO users (uuid, created_at, updated_at, created_by, updated_by, version, deleted,
                   username, email, password_hash, first_name, last_name, profession,
                   nida_verified, active, data_processing_consent)
VALUES
 (UUID(), NOW(6), NOW(6), 'system', 'system', 0, 0, 'admin',      'admin@tirdo.go.tz',      '$2a$10$.yzsffisYpqxyLpE50c1BOXqO0PbaNMOmPsZREtxHTTo7mOyt6yPS', 'System',   'Administrator', 'Administrator',       0, 1, 1),
 (UUID(), NOW(6), NOW(6), 'system', 'system', 0, 0, 'instructor', 'instructor@tirdo.go.tz', '$2a$10$.yzsffisYpqxyLpE50c1BOXqO0PbaNMOmPsZREtxHTTo7mOyt6yPS', 'Ines',     'Mushi',         'Instructor',          0, 1, 1),
 (UUID(), NOW(6), NOW(6), 'system', 'system', 0, 0, 'student',    'student@tirdo.go.tz',    '$2a$10$.yzsffisYpqxyLpE50c1BOXqO0PbaNMOmPsZREtxHTTo7mOyt6yPS', 'Sadiki',   'Juma',          'Trainee',             0, 1, 1),
 (UUID(), NOW(6), NOW(6), 'system', 'system', 0, 0, 'finance',    'finance@tirdo.go.tz',    '$2a$10$.yzsffisYpqxyLpE50c1BOXqO0PbaNMOmPsZREtxHTTo7mOyt6yPS', 'Fatma',    'Ndosi',         'Finance Officer',     0, 1, 1),
 (UUID(), NOW(6), NOW(6), 'system', 'system', 0, 0, 'corporate',  'corporate@tirdo.go.tz',  '$2a$10$.yzsffisYpqxyLpE50c1BOXqO0PbaNMOmPsZREtxHTTo7mOyt6yPS', 'Charles',  'Komba',         'Corporate Liaison',   0, 1, 1);

-- Assign one role per account.
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
JOIN roles r ON r.name = CASE u.username
    WHEN 'admin'      THEN 'ADMIN'
    WHEN 'instructor' THEN 'INSTRUCTOR'
    WHEN 'student'    THEN 'STUDENT'
    WHEN 'finance'    THEN 'FINANCE_OFFICER'
    WHEN 'corporate'  THEN 'CORPORATE_CLIENT'
END
WHERE u.username IN ('admin','instructor','student','finance','corporate');
