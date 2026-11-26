USE home_school_platform;

SET NAMES utf8mb4;

INSERT INTO notice
    (id, publisher_id, scope_type, grade_id, class_id, title, content,
     status, published_at)
VALUES
    (1, 1, 'ALL', NULL, NULL, '平台试运行通知',
     '家校协同平台基础功能已开放，请使用学校分配的账号登录。',
     'PUBLISHED', CURRENT_TIMESTAMP(3)),
    (2, 2, 'CLASS', 1, 1, '本周班级学习安排',
     '请同学们按时完成本周学习任务，家长可在平台查看相关记录。',
     'PUBLISHED', CURRENT_TIMESTAMP(3))
ON DUPLICATE KEY UPDATE
    title = VALUES(title),
    content = VALUES(content),
    scope_type = VALUES(scope_type),
    grade_id = VALUES(grade_id),
    class_id = VALUES(class_id),
    status = VALUES(status),
    published_at = VALUES(published_at);

