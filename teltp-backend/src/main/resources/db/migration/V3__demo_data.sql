-- Demo content so the catalogue, enrolment and learning views are populated on first run.
-- Reference numbers use a high block (09xxx) to avoid colliding with API-generated numbers.

-- A pricing plan for the one paid demo course.
INSERT INTO pricing_plans (uuid, created_at, updated_at, created_by, updated_by, version, deleted,
                           name, charge_model, amount, currency, cycle_days, active)
VALUES (UUID(), NOW(6), NOW(6), 'system', 'system', 0, 0,
        'Standard Course Fee', 'ONE_TIME', 150000.00, 'TZS', NULL, 1);

-- Four published courses across four training domains (category resolved by name).
INSERT INTO courses (uuid, created_at, updated_at, created_by, updated_by, version, deleted,
                     reference_number, title, description, category_id, delivery_mode, status,
                     duration_hours, instructor_uuid, pricing_plan_uuid)
VALUES
 (UUID(), NOW(6), NOW(6), 'system', 'system', 0, 0,
  'TELTP-CRS-2026-09001', 'Industrial Cybersecurity Fundamentals',
  'Protect industrial control systems and OT networks. Threat models, hardening, incident response, and the basics of cyber resilience for manufacturers.',
  (SELECT id FROM categories WHERE name = 'ICT & Digital Transformation' AND parent_id IS NULL),
  'ONLINE', 'PUBLISHED', 24, NULL, NULL),

 (UUID(), NOW(6), NOW(6), 'system', 'system', 0, 0,
  'TELTP-CRS-2026-09002', 'Quality Management Systems (ISO 9001)',
  'Implement and audit a quality management system aligned to ISO 9001. Process approach, documentation, internal audits, and continual improvement.',
  (SELECT id FROM categories WHERE name = 'Industrial Technology' AND parent_id IS NULL),
  'HYBRID', 'PUBLISHED', 40, NULL,
  (SELECT uuid FROM pricing_plans WHERE name = 'Standard Course Fee' LIMIT 1)),

 (UUID(), NOW(6), NOW(6), 'system', 'system', 0, 0,
  'TELTP-CRS-2026-09003', 'Research Methods & Innovation Management',
  'From research design to commercialization. Methodology, data discipline, managing innovation pipelines, and moving findings toward industrial application.',
  (SELECT id FROM categories WHERE name = 'Research & Innovation' AND parent_id IS NULL),
  'ONLINE', 'PUBLISHED', 16, NULL, NULL),

 (UUID(), NOW(6), NOW(6), 'system', 'system', 0, 0,
  'TELTP-CRS-2026-09004', 'Renewable Energy Systems for Industry',
  'Solar, biomass and clean-energy systems for industrial settings. Sizing, integration, and operating sustainable energy for manufacturing.',
  (SELECT id FROM categories WHERE name = 'Environmental Management' AND parent_id IS NULL),
  'IN_PERSON', 'PUBLISHED', 32, NULL, NULL);

-- Modules per course (course resolved by reference number). Publishing requires >= 1 module.
INSERT INTO course_modules (uuid, created_at, updated_at, created_by, updated_by, version, deleted,
                            course_id, title, order_index)
SELECT UUID(), NOW(6), NOW(6), 'system', 'system', 0, 0, c.id, m.title, m.order_index
FROM (
    SELECT 'TELTP-CRS-2026-09001' AS ref, 'Threat Landscape & OT Risk' AS title, 1 AS order_index
    UNION ALL SELECT 'TELTP-CRS-2026-09001', 'Hardening & Network Segmentation', 2
    UNION ALL SELECT 'TELTP-CRS-2026-09001', 'Incident Response', 3
    UNION ALL SELECT 'TELTP-CRS-2026-09002', 'QMS Foundations & the Process Approach', 1
    UNION ALL SELECT 'TELTP-CRS-2026-09002', 'Documentation & Records', 2
    UNION ALL SELECT 'TELTP-CRS-2026-09002', 'Internal Audit & Continual Improvement', 3
    UNION ALL SELECT 'TELTP-CRS-2026-09003', 'Research Design', 1
    UNION ALL SELECT 'TELTP-CRS-2026-09003', 'Innovation Pipelines & Commercialization', 2
    UNION ALL SELECT 'TELTP-CRS-2026-09004', 'Energy Sources & Sizing', 1
    UNION ALL SELECT 'TELTP-CRS-2026-09004', 'Integration & Operations', 2
) AS m
JOIN courses c ON c.reference_number = m.ref;
