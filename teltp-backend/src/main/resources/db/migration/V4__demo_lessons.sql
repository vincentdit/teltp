-- Lessons for the demo courses so course completion (which counts mandatory lessons) is reachable,
-- which in turn makes the certificate flow demonstrable end-to-end. Targets only the 0900x demo
-- courses; modules are resolved by (course reference number + module order_index).

INSERT INTO lessons (uuid, created_at, updated_at, created_by, updated_by, version, deleted,
                     module_id, title, content, order_index, estimated_minutes, mandatory)
SELECT UUID(), NOW(6), NOW(6), 'system', 'system', 0, 0,
       cm.id, x.lesson_title, x.content, x.lesson_order, x.est_minutes, 1
FROM (
    -- ref, module_order, lesson_order, lesson_title, content, est_minutes
    SELECT 'TELTP-CRS-2026-09001' ref, 1 module_order, 1 lesson_order, 'What is OT and why it is targeted' lesson_title, 'Operational technology, ICS/SCADA basics, and why industrial environments are attractive targets.' content, 25 est_minutes
    UNION ALL SELECT 'TELTP-CRS-2026-09001', 1, 2, 'Mapping your threat landscape', 'Identifying assets, entry points, and likely adversaries for an industrial network.', 30
    UNION ALL SELECT 'TELTP-CRS-2026-09001', 2, 1, 'Network segmentation', 'Zones, conduits, and the Purdue model for separating IT and OT.', 30
    UNION ALL SELECT 'TELTP-CRS-2026-09001', 2, 2, 'Hardening devices and access', 'Baseline hardening, least privilege, and secure remote access.', 25
    UNION ALL SELECT 'TELTP-CRS-2026-09001', 3, 1, 'Detection and response', 'Monitoring, alerting, and a basic incident response playbook for OT.', 30

    UNION ALL SELECT 'TELTP-CRS-2026-09002', 1, 1, 'The process approach', 'Clause structure of ISO 9001 and thinking in processes.', 25
    UNION ALL SELECT 'TELTP-CRS-2026-09002', 1, 2, 'Context and leadership', 'Interested parties, scope, and management commitment.', 25
    UNION ALL SELECT 'TELTP-CRS-2026-09002', 2, 1, 'Documented information', 'What must be documented, version control, and records.', 20
    UNION ALL SELECT 'TELTP-CRS-2026-09002', 3, 1, 'Planning and running internal audits', 'Audit programme, evidence, findings, and nonconformities.', 35
    UNION ALL SELECT 'TELTP-CRS-2026-09002', 3, 2, 'Corrective action and improvement', 'Root cause analysis and the continual improvement loop.', 30

    UNION ALL SELECT 'TELTP-CRS-2026-09003', 1, 1, 'Framing a research question', 'From problem to a testable, well-scoped question.', 25
    UNION ALL SELECT 'TELTP-CRS-2026-09003', 1, 2, 'Method and data discipline', 'Choosing methods and keeping data trustworthy and reproducible.', 30
    UNION ALL SELECT 'TELTP-CRS-2026-09003', 2, 1, 'From findings to product', 'Innovation pipelines, TRLs, and paths to commercialization.', 30

    UNION ALL SELECT 'TELTP-CRS-2026-09004', 1, 1, 'Energy sources for industry', 'Solar, biomass, and hybrid systems: strengths and trade-offs.', 25
    UNION ALL SELECT 'TELTP-CRS-2026-09004', 1, 2, 'Sizing a system', 'Load assessment and sizing generation and storage.', 30
    UNION ALL SELECT 'TELTP-CRS-2026-09004', 2, 1, 'Integration and operations', 'Grid-tie vs off-grid, controls, and ongoing operations.', 30
) AS x
JOIN courses c ON c.reference_number = x.ref
JOIN course_modules cm ON cm.course_id = c.id AND cm.order_index = x.module_order;
