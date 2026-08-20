-- Seed reference data: the five personas and the top-level training taxonomy.

-- Roles (Module 1 personas). created_by/updated_by 'system'; version 0; not deleted.
INSERT INTO roles (uuid, created_at, updated_at, created_by, updated_by, version, deleted, name, description) VALUES
 (UUID(), NOW(6), NOW(6), 'system', 'system', 0, 0, 'ADMIN',            'Platform administrator'),
 (UUID(), NOW(6), NOW(6), 'system', 'system', 0, 0, 'INSTRUCTOR',       'Course author and trainer'),
 (UUID(), NOW(6), NOW(6), 'system', 'system', 0, 0, 'STUDENT',          'Individual learner'),
 (UUID(), NOW(6), NOW(6), 'system', 'system', 0, 0, 'CORPORATE_CLIENT', 'Organization training coordinator'),
 (UUID(), NOW(6), NOW(6), 'system', 'system', 0, 0, 'FINANCE_OFFICER',  'Billing and revenue officer');

-- Top-level training categories (Module 4).
INSERT INTO categories (uuid, created_at, updated_at, created_by, updated_by, version, deleted, name, description, parent_id) VALUES
 (UUID(), NOW(6), NOW(6), 'system', 'system', 0, 0, 'Industrial Technology',          'Manufacturing, materials, quality, and industrial processes', NULL),
 (UUID(), NOW(6), NOW(6), 'system', 'system', 0, 0, 'ICT & Digital Transformation',   'Software, data, cybersecurity, and digital skills', NULL),
 (UUID(), NOW(6), NOW(6), 'system', 'system', 0, 0, 'Research & Innovation',          'Research methods, innovation management, and commercialization', NULL),
 (UUID(), NOW(6), NOW(6), 'system', 'system', 0, 0, 'Environmental Management',       'Environment, energy, and sustainability', NULL),
 (UUID(), NOW(6), NOW(6), 'system', 'system', 0, 0, 'Entrepreneurship',               'Business development and enterprise growth', NULL);

-- Representative second-level subcategories (demonstrate the hierarchy; parent resolved by name).
INSERT INTO categories (uuid, created_at, updated_at, created_by, updated_by, version, deleted, name, description, parent_id)
SELECT UUID(), NOW(6), NOW(6), 'system', 'system', 0, 0, sub.name, sub.description, p.id
FROM (
    SELECT 'ICT & Digital Transformation' AS parent_name, 'Cybersecurity' AS name, 'Information security and cyber resilience' AS description
    UNION ALL SELECT 'ICT & Digital Transformation', 'Data Analytics', 'Data analysis and business intelligence'
    UNION ALL SELECT 'Industrial Technology', 'Quality Management', 'Standards, testing, and quality assurance'
    UNION ALL SELECT 'Environmental Management', 'Renewable Energy', 'Solar, biomass, and clean energy systems'
) AS sub
JOIN categories p ON p.name = sub.parent_name AND p.parent_id IS NULL;
