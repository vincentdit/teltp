-- Rotating notices on the sign-in screen. Administrators replace these for events.

CREATE TABLE portal_slides (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    uuid        VARCHAR(36)  NOT NULL,
    created_at  DATETIME(6)  NOT NULL,
    updated_at  DATETIME(6)  NOT NULL,
    created_by  VARCHAR(100),
    updated_by  VARCHAR(100),
    version     BIGINT       DEFAULT 0,
    deleted     BIT          NOT NULL DEFAULT 0,
    title       VARCHAR(160) NOT NULL,
    body        VARCHAR(500),
    image_key   VARCHAR(255),
    image_mime  VARCHAR(80),
    link_url    VARCHAR(500),
    sort_order  INT          NOT NULL DEFAULT 0,
    active      BIT          NOT NULL DEFAULT 1,
    PRIMARY KEY (id),
    UNIQUE KEY uk_portal_slides_uuid (uuid)
) ENGINE=InnoDB;

INSERT INTO portal_slides
    (uuid, created_at, updated_at, created_by, version, deleted, title, body, sort_order, active)
VALUES
    ('b1000000-0000-4000-8000-000000000001', NOW(6), NOW(6), 'system', 0, 0,
     'National industrial skills',
     'Programmes for engineers, technicians, researchers, and enterprise teams.',
     1, 1),
    ('b1000000-0000-4000-8000-000000000002', NOW(6), NOW(6), 'system', 0, 0,
     'Certificates you can verify',
     'Each certificate carries a public code that anyone can check.',
     2, 1),
    ('b1000000-0000-4000-8000-000000000003', NOW(6), NOW(6), 'system', 0, 0,
     'Events and announcements',
     'Administrators can replace these slides when a new programme or event opens.',
     3, 1);
