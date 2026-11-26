USE home_school_platform;

SET NAMES utf8mb4;

INSERT INTO assignment
    (id, class_id, teacher_id, subject_id, title, content, deadline,
     max_score, status, published_at)
VALUES
    (1, 1, 1, 2, '第一章课后练习',
     '完成课本第一章课后练习，并在截止时间前提交。',
     '2027-01-15 23:59:59.000', 100.00, 'PUBLISHED', CURRENT_TIMESTAMP(3))
ON DUPLICATE KEY UPDATE
    title = VALUES(title),
    content = VALUES(content),
    deadline = VALUES(deadline),
    max_score = VALUES(max_score),
    status = VALUES(status),
    published_at = VALUES(published_at);

