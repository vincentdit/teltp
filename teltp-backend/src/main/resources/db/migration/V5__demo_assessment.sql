-- A demo EXAM (auto-graded MCQs) on the Industrial Cybersecurity course, so the
-- assessment-taking flow is demonstrable without manual authoring.

INSERT INTO assessments (uuid, created_at, updated_at, created_by, updated_by, version, deleted,
                         reference_number, course_uuid, title, type, pass_mark, time_limit_minutes)
SELECT 'demo-cyber-exam', NOW(6), NOW(6), 'system', 'system', 0, 0,
       'TELTP-ASMT-2026-09001', c.uuid,
       'Industrial Cybersecurity — Final Assessment', 'EXAM', 60, 20
FROM courses c WHERE c.reference_number = 'TELTP-CRS-2026-09001';

INSERT INTO questions (uuid, created_at, updated_at, created_by, updated_by, version, deleted,
                       assessment_id, prompt, type, points)
SELECT q.uuid, NOW(6), NOW(6), 'system', 'system', 0, 0, a.id, q.prompt, 'MULTIPLE_CHOICE', 25
FROM assessments a
JOIN (
    SELECT 'demo-cyber-q1' uuid, 'What does "OT" stand for in an industrial security context?' prompt
    UNION ALL SELECT 'demo-cyber-q2', 'Which model is commonly used to segment industrial networks into zones?'
    UNION ALL SELECT 'demo-cyber-q3', 'What is the primary purpose of network segmentation in OT?'
    UNION ALL SELECT 'demo-cyber-q4', 'What is the best first step when responding to a suspected OT intrusion?'
) q ON a.reference_number = 'TELTP-ASMT-2026-09001';

INSERT INTO answer_options (uuid, created_at, updated_at, created_by, updated_by, version, deleted,
                            question_id, option_text, correct)
SELECT UUID(), NOW(6), NOW(6), 'system', 'system', 0, 0, q.id, o.option_text, o.correct
FROM (
    SELECT 'demo-cyber-q1' qref, 'Operational Technology' option_text, 1 correct
    UNION ALL SELECT 'demo-cyber-q1', 'Online Transaction', 0
    UNION ALL SELECT 'demo-cyber-q1', 'Optical Transport', 0
    UNION ALL SELECT 'demo-cyber-q1', 'Open Telemetry', 0

    UNION ALL SELECT 'demo-cyber-q2', 'The Purdue model', 1
    UNION ALL SELECT 'demo-cyber-q2', 'The OSI model', 0
    UNION ALL SELECT 'demo-cyber-q2', 'The waterfall model', 0
    UNION ALL SELECT 'demo-cyber-q2', 'The STRIDE model', 0

    UNION ALL SELECT 'demo-cyber-q3', 'To contain threats and limit lateral movement', 1
    UNION ALL SELECT 'demo-cyber-q3', 'To increase network throughput', 0
    UNION ALL SELECT 'demo-cyber-q3', 'To reduce cabling costs', 0
    UNION ALL SELECT 'demo-cyber-q3', 'To simplify IP addressing', 0

    UNION ALL SELECT 'demo-cyber-q4', 'Isolate affected systems and preserve evidence', 1
    UNION ALL SELECT 'demo-cyber-q4', 'Immediately wipe and reboot every device', 0
    UNION ALL SELECT 'demo-cyber-q4', 'Ignore it until the next maintenance window', 0
    UNION ALL SELECT 'demo-cyber-q4', 'Disable all logging to save disk space', 0
) o
JOIN questions q ON q.uuid = o.qref;
